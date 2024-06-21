/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cthing.annotations.AccessForTesting;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Performs the reading of the properties file(s) and generation of the constants class.
 */
public class PropertyKeyConstantsTask extends SourceTask {

    private static final Logger LOGGER = Logging.getLogger(PropertyKeyConstantsTask.class);
    private static final Pattern PROP_SEP_REGEX = Pattern.compile("[.\\-]");
    private static final Pattern WORD_REGEX = Pattern.compile("[\\W_\\-]+|(?<=\\p{Ll})(?=\\p{Lu})");

    private final Property<String> classname;
    private final DirectoryProperty outputDirectory;
    private final Property<SourceAccess> sourceAccess;
    private final Property<SourceLayout> sourceLayout;

    public PropertyKeyConstantsTask() {
        setGroup("Generate Constants");

        final PropertyKeyConstantsExtension extension = getProject().getExtensions()
                                                                    .getByType(PropertyKeyConstantsExtension.class);

        final ObjectFactory objects = getProject().getObjects();
        this.classname = objects.property(String.class);
        this.outputDirectory = objects.directoryProperty();
        this.sourceAccess = objects.property(SourceAccess.class).convention(extension.getSourceAccess());
        this.sourceLayout = objects.property(SourceLayout.class).convention(extension.getSourceLayout());

        // If there are no properties files, don't do anything.
        onlyIf(task -> !getSource().isEmpty());
    }

    /**
     * Obtains the fully qualified name for the generated class (e.g. org.cthing.myapp.PropertyConstants).
     *
     * @return Fully qualified class name.
     */
    @Input
    public Property<String> getClassname() {
        return this.classname;
    }

    /**
     * Obtains the location on the filesystem for the generated class.
     *
     * @return Output directory.
     */
    @OutputDirectory
    public DirectoryProperty getOutputDirectory() {
        return this.outputDirectory;
    }

    /**
     * Obtains the access modifier for the generated constants. The default is
     * {@link PropertyKeyConstantsExtension#getSourceAccess()}.
     *
     * @return Access modifier for the generated constants.
     */
    @Input
    public Property<SourceAccess> getSourceAccess() {
        return this.sourceAccess;
    }

    /**
     * Obtains the layout for the generated source code. The default is
     * {@link PropertyKeyConstantsExtension#getSourceLayout()}.
     *
     * @return Layout for the generated source code.
     */
    @Input
    public Property<SourceLayout> getSourceLayout() {
        return this.sourceLayout;
    }

    /**
     * Generates the property key constants class.
     */
    @TaskAction
    public void generateConstants() {
        final Provider<String> pathname = this.classname.map(cname -> cname.replace('.', '/') + ".java");
        final File classFile = this.outputDirectory.file(pathname).get().getAsFile();
        final File parentFile = classFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new GradleException("Could not create directories " + parentFile);
        }

        final String cname = this.classname.get();
        final int pos = cname.lastIndexOf('.');
        assert pos != -1;
        final String packageName = cname.substring(0, pos);
        final String className = cname.substring(pos + 1);

        try (PrintWriter writer = new PrintWriter(classFile, StandardCharsets.UTF_8)) {
            LOGGER.info("Writing constants class {}.{}", packageName, className);
            writeConstants(writer, packageName, className);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    /**
     * Performs the work of writing the Java class file containing the properties file keys as constants.
     *
     * @param writer  Writes the file
     * @param packageName  The Java package containing the class
     * @param className  Name of the top level class (not qualified by the package name)
     */
    private void writeConstants(final PrintWriter writer, final String packageName, final String className) {
        final String modifier = this.sourceAccess.get() == SourceAccess.PUBLIC ? "public " : "";
        final String propFilesComment = getSource().getFiles()
                                                   .stream()
                                                   .map(File::getName)
                                                   .sorted()
                                                   .map(fname -> " *   <li>" + fname + "</li>")
                                                   .collect(Collectors.joining("\n"));
        writer.format("""
                      //
                      // DO NOT EDIT - File generated by the org.cthing.property-key-constants Gradle plugin.
                      //

                      package %s;

                      /**
                       * Constants for property keys in:
                       * <ul>
                      %s
                       * </ul>
                       */
                      @SuppressWarnings("all")
                      %sfinal class %s {
                      """, packageName, propFilesComment, modifier, className);

        switch (this.sourceLayout.get()) {
            case NESTED_CLASSES -> writeNestedClasses(writer, modifier);
            case FLAT_WITH_PREFIX -> writeFlatWithPrefix(writer, modifier);
            case FLAT_WITHOUT_PREFIX -> writeFlatWithoutPrefix(writer, modifier);
            default -> throw new GradleException("Unknown source layout");
        }

        writer.format("""

                          private %s() { }
                      }
                      """, className);
    }

    /**
     * Writes the property key constants wrapped in a nested class for each property file.
     *
     * @param writer Writes the file
     * @param modifier Access modifier string
     */
    private void writeNestedClasses(final PrintWriter writer, final String modifier) {
        getSource().getFiles().forEach(propertiesFile -> {
            LOGGER.info("Processing properties file ${propertiesFile}");
            final String innerClassName = toCamelCase(getBasename(propertiesFile));

            writer.format("""

                              %sstatic final class %s {
                          """, modifier, innerClassName);

            final List<String> propNames = readPropertyNames(propertiesFile);
            propNames.forEach(propName -> {
                final String constantName = PROP_SEP_REGEX.matcher(propName).replaceAll("_").toUpperCase();
                writer.format("        %sstatic final String %s = \"%s\";%n", modifier, constantName, propName);
            });

            writer.format("""

                                  private %s() { }
                              }
                          """, innerClassName);
        });
    }

    /**
     * Write the property key constants as top level fields with a prefix based on the property file basename
     * added to each constant.
     *
     * @param writer Writes the file
     * @param modifier Access modifier string
     */
    private void writeFlatWithPrefix(final PrintWriter writer, final String modifier) {
        getSource().getFiles().forEach(propertiesFile -> {
            LOGGER.info("Processing properties file ${propertiesFile}");
            final String prefix = toUpperCase(getBasename(propertiesFile));

            final List<String> propNames = readPropertyNames(propertiesFile);
            writer.println();
            propNames.forEach(propName -> {
                final String constantName = PROP_SEP_REGEX.matcher(propName).replaceAll("_").toUpperCase();
                writer.format("    %sstatic final String %s_%s = \"%s\";%n", modifier, prefix, constantName, propName);
            });
        });
    }

    /**
     * Write the property key constants as top level fields. Beware that if multiple property files are specified,
     * constant names may collide using this layout.
     *
     * @param writer Writes the file
     * @param modifier Access modifier string
     */
    private void writeFlatWithoutPrefix(final PrintWriter writer, final String modifier) {
        getSource().getFiles().forEach(propertiesFile -> {
            LOGGER.info("Processing properties file ${propertiesFile}");

            final List<String> propNames = readPropertyNames(propertiesFile);
            writer.println();
            propNames.forEach(propName -> {
                final String constantName = PROP_SEP_REGEX.matcher(propName).replaceAll("_").toUpperCase();
                writer.format("    %sstatic final String %s = \"%s\";%n", modifier, constantName, propName);
            });
        });
    }

    /**
     * Loads the specified Java properties file and returns the property keys.
     *
     * @param propertiesFile  Properties file whose keys are to be returned
     * @return Sorted keys in the specified properties file.
     */
    private List<String> readPropertyNames(final File propertiesFile) {
        final Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(getProject().file(propertiesFile).toPath())) {
            properties.load(inputStream);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
        return properties.stringPropertyNames().stream().sorted().toList();
    }

    /**
     * Extracts the name of the specified file without its extension.
     * <pre>
     * abc.txt -> abc
     * /x/y/z/abc.txt -> abc
     * abc. -> abc
     * abc -> abc
     * .abc -> .abc
     * </pre>
     *
     * @param file File whose basename is desired
     * @return Name of the file without its extension. The name of the file (i.e. {@link File#getName()}) is returned
     *      if it did not have an extension.
     */
    @AccessForTesting
    static String getBasename(final File file) {
        final String filename = file.getName();
        final int pos = filename.lastIndexOf('.');
        return pos < 1 ? filename : filename.substring(0, pos);
    }

    /**
     * Converts the specified string to CamelCase.
     * <pre>
     * h -> H
     * hello -> Hello
     * Hello -> Hello
     * HELLO -> Hello
     * hello_world -> HelloWorld
     * Hello_World -> HelloWorld
     * hello-world -> HelloWorld
     * hello.world -> HelloWorld
     * hello__world -> HelloWorld
     * HELLO_WORLD -> HelloWorld
     * HelloWorld -> HelloWorld
     * "" -> ""
     * - -> ""
     * _ -> ""
     * . -> ""
     * </pre>
     *
     * @param str  String to convert to camel case. Underscores are considered word separators.
     * @return Specified string converted to camel case.
     */
    @AccessForTesting
    @SuppressWarnings("Convert2streamapi")
    static String toCamelCase(final String str) {
        if (str.isEmpty()) {
            return str;
        }

        final String[] words = WORD_REGEX.split(str);
        if (words.length == 1) {
            final String word = words[0];
            return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
        }

        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1).toLowerCase(Locale.ENGLISH));
        }
        return builder.toString();
    }

    /**
     * Converts the specified string to all upper case. Changes in case are separated by underscores.
     * <pre>
     * "" -> ""
     * h -> H
     * hello -> HELLO
     * Hello -> HELLO
     * HELLO -> HELLO
     * helloWorld -> HELLO_WORLD
     * HelloWorld -> HELLO_WORLD
     * hello_world -> HELLO_WORLD
     * Hello_World -> HELLO_WORLD
     * hello-world -> HELLO_WORLD
     * hello.world -> HELLO_WORLD
     * hello__world -> HELLO_WORLD
     * HELLO_WORLD -> HELLO_WORLD
     * - -> ""
     * _ -> ""
     * </pre>
     *
     * @param str String to convert to upper case.
     * @return Specified string converted to upper case.
     */
    @AccessForTesting
    static String toUpperCase(final String str) {
        if (str.isEmpty()) {
            return str;
        }

        final String[] words = WORD_REGEX.split(str);
        if (words.length == 1) {
            return words[0].toUpperCase(Locale.ENGLISH);
        }

        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            if (!builder.isEmpty()) {
                builder.append('_');
            }
            builder.append(word.toUpperCase(Locale.ENGLISH));
        }
        return builder.toString();
    }
}

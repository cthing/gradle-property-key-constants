/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.properties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.file.PathUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginIntegTest {
    private static final Path BASE_DIR = Path.of(System.getProperty("buildDir"), "integTest");
    private static final Path WORKING_DIR;

    static {
        try {
            Files.createDirectories(BASE_DIR);
            WORKING_DIR = Files.createTempDirectory(BASE_DIR, "working");
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Path projectDir;

    public static Stream<Arguments> gradleVersionProvider() {
        return Stream.of(
                arguments("8.2"),
                arguments(GradleVersion.current().getVersion())
        );
    }

    @BeforeEach
    public void setup() throws IOException {
        this.projectDir = Files.createTempDirectory(BASE_DIR, "project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testEmptySourceSets(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.cthing.property-key-constants")
                }
                """);

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SKIPPED);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testNestedClassesPublic(final String gradleVersion) throws IOException {
        copyProject("nested-classes-public");

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        Class<?> cls = loadClass("org.cthing.test.Constants");
        assertThat(cls).isPublic().isFinal();

        cls = loadClass("org.cthing.test.Constants$Prop1");
        assertThat(cls).isPublic().isStatic().isFinal();
        verifyConstant(cls, "KEY1", "key1", SourceAccess.PUBLIC);
        verifyConstant(cls, "KEY2", "key2", SourceAccess.PUBLIC);

        cls = loadClass("org.cthing.test.Constants$Prop2");
        assertThat(cls).isPublic().isStatic().isFinal();
        verifyConstant(cls, "ABC_DEF_17", "abc.def.17", SourceAccess.PUBLIC);
        verifyConstant(cls, "UVW_XYZ_18", "uvw.xyz.18", SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testNestedClassesPackagePrivate(final String gradleVersion) throws IOException {
        copyProject("nested-classes-package");

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        Class<?> cls = loadClass("org.cthing.test.Constants");
        assertThat(cls).isPackagePrivate().isFinal();

        cls = loadClass("org.cthing.test.Constants$Prop1");
        assertThat(cls).isPackagePrivate().isStatic().isFinal();
        verifyConstant(cls, "KEY1", "key1", SourceAccess.PACKAGE);
        verifyConstant(cls, "KEY2", "key2", SourceAccess.PACKAGE);

        cls = loadClass("org.cthing.test.Constants$Prop2");
        assertThat(cls).isPackagePrivate().isStatic().isFinal();
        verifyConstant(cls, "ABC_DEF_17", "abc.def.17", SourceAccess.PACKAGE);
        verifyConstant(cls, "UVW_XYZ_18", "uvw.xyz.18", SourceAccess.PACKAGE);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testFlatWithPrefixPublic(final String gradleVersion) throws IOException {
        copyProject("flat-with-prefix-public");

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        final Class<?> cls = loadClass("org.cthing.test.Constants");
        assertThat(cls).isPublic().isFinal();

        verifyConstant(cls, "PROP1_KEY1", "key1", SourceAccess.PUBLIC);
        verifyConstant(cls, "PROP1_KEY2", "key2", SourceAccess.PUBLIC);

        verifyConstant(cls, "PROP2_ABC_DEF_17", "abc.def.17", SourceAccess.PUBLIC);
        verifyConstant(cls, "PROP2_UVW_XYZ_18", "uvw.xyz.18", SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testFlatWithPrefixPackage(final String gradleVersion) throws IOException {
        copyProject("flat-with-prefix-package");

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        final Class<?> cls = loadClass("org.cthing.test.Constants");
        assertThat(cls).isPackagePrivate().isFinal();

        verifyConstant(cls, "PROP1_KEY1", "key1", SourceAccess.PACKAGE);
        verifyConstant(cls, "PROP1_KEY2", "key2", SourceAccess.PACKAGE);

        verifyConstant(cls, "PROP2_ABC_DEF_17", "abc.def.17", SourceAccess.PACKAGE);
        verifyConstant(cls, "PROP2_UVW_XYZ_18", "uvw.xyz.18", SourceAccess.PACKAGE);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testFlatWithoutPrefixPublic(final String gradleVersion) throws IOException {
        copyProject("flat-without-prefix-public");

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        final Class<?> cls = loadClass("org.cthing.test.Constants");
        assertThat(cls).isPublic().isFinal();

        verifyConstant(cls, "KEY1", "key1", SourceAccess.PUBLIC);
        verifyConstant(cls, "KEY2", "key2", SourceAccess.PUBLIC);

        verifyConstant(cls, "ABC_DEF_17", "abc.def.17", SourceAccess.PUBLIC);
        verifyConstant(cls, "UVW_XYZ_18", "uvw.xyz.18", SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testFlatWithoutPrefixPackage(final String gradleVersion) throws IOException {
        copyProject("flat-without-prefix-package");

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        final Class<?> cls = loadClass("org.cthing.test.Constants");
        assertThat(cls).isPackagePrivate().isFinal();

        verifyConstant(cls, "KEY1", "key1", SourceAccess.PACKAGE);
        verifyConstant(cls, "KEY2", "key2", SourceAccess.PACKAGE);

        verifyConstant(cls, "ABC_DEF_17", "abc.def.17", SourceAccess.PACKAGE);
        verifyConstant(cls, "UVW_XYZ_18", "uvw.xyz.18", SourceAccess.PACKAGE);
    }

    private void copyProject(final String projectName) throws IOException {
        final URL projectUrl = getClass().getResource("/" + projectName);
        assertThat(projectUrl).isNotNull();
        PathUtils.copyDirectory(Path.of(projectUrl.getPath()), this.projectDir);

        for (final String propName : List.of("prop1", "prop2")) {
            final URL propUrl = getClass().getResource("/" + propName + ".properties");
            assertThat(propUrl).isNotNull();
            PathUtils.copyFileToDirectory(propUrl, this.projectDir);
        }
    }

    private GradleRunner createGradleRunner(final String gradleVersion) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withTestKitDir(WORKING_DIR.toFile())
                           .withArguments("generatePropertyKeyConstants", "build")
                           .withPluginClasspath()
                           .withGradleVersion(gradleVersion);
    }

    private void verifyBuild(final BuildResult result, final TaskOutcome outcome) {
        final BuildTask genTask = result.task(":generatePropertyKeyConstants");
        assertThat(genTask).isNotNull();
        assertThat(genTask.getOutcome()).as(result.getOutput()).isEqualTo(outcome);

        if (outcome != SUCCESS) {
            return;
        }

        final BuildTask buildTask = result.task(":build");
        assertThat(buildTask).isNotNull();
        assertThat(buildTask.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        final Path actualSource = this.projectDir.resolve("build/generated-src/property-key-constants/main/org/cthing/test/Constants.java");
        assertThat(actualSource).isRegularFile();

        final Path expectedSource = this.projectDir.resolve("Constants.java");
        assertThat(actualSource).hasSameTextualContentAs(expectedSource, StandardCharsets.UTF_8);

        final Path classFile = this.projectDir.resolve("build/classes/java/main/org/cthing/test/Constants.class");
        assertThat(classFile).isRegularFile();
    }

    private void verifyConstant(final Class<?> cls, final String fieldName, final String fieldValue,
                                final SourceAccess access) throws IOException {
        try {
            assertThat(cls).hasDeclaredFields(fieldName);

            final Field field = cls.getDeclaredField(fieldName);
            assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
            if (access == SourceAccess.PUBLIC) {
                assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
            } else {
                assertThat(Modifier.isPublic(field.getModifiers())).isFalse();

                field.setAccessible(true);
            }
            assertThat((String)field.get(null)).isEqualTo(fieldValue);
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new IOException(ex);
        }
    }

    private Class<?> loadClass(final String classname) throws IOException {
        final Path classesDir = this.projectDir.resolve("build/classes/java/main");
        try (URLClassLoader loader = new URLClassLoader(new URL[] { classesDir.toUri().toURL() })) {
            return loader.loadClass(classname);
        } catch (final ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }
}

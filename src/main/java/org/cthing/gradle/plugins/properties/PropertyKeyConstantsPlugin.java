/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;


/**
 * Plugin which creates the constants generation tasks for each source set in the project.
 */
public class PropertyKeyConstantsPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "propertyKeyConstants";

    /**
     * Applies the plugin to the specified project.
     *
     * @param project Project to which the plugin should be applied.
     */
    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        final PropertyKeyConstantsExtension extension = project.getExtensions().create(EXTENSION_NAME, PropertyKeyConstantsExtension.class, project);

        // For each Java source set, create a task for generating constants from property file keys.
        project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().all(sourceSet -> {
            // Use the Gradle naming scheme for the task name.
            final String taskName = sourceSet.getTaskName("generate", "PropertyKeyConstants");

            // Use the Gradle convention for generated source output directory naming (e.g. AntlrTask).
            final Provider<Directory> taskOutputDirectory =
                    project.getLayout()
                           .getBuildDirectory()
                           .dir("generated-src/property-key-constants/" + sourceSet.getName());

            // Create the constants generation task.
            final TaskProvider<PropertyKeyConstantsTask> constantsTask =
                    project.getTasks().register(taskName, PropertyKeyConstantsTask.class, task -> {
                        task.setDescription(String.format("Generates constants for the %s properties files",
                                                          sourceSet.getName()));
                        task.getSourceAccess().convention(extension.getSourceAccess());
                        task.getSourceLayout().convention(extension.getSourceLayout());
                        task.getOutputDirectory().convention(taskOutputDirectory);
                    });

            // Add the generated constants source file to the source set
            sourceSet.getJava().srcDir(taskOutputDirectory);

            // Generate the constants source file before trying to compile it.
            project.getTasks()
                   .named(sourceSet.getCompileJavaTaskName())
                   .configure(compileTask -> compileTask.dependsOn(constantsTask));

        });
    }
}

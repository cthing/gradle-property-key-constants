/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.properties;

import java.io.File;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginApplyTest {

    @Test
    public void testApply(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.property-key-constants");

        final Object extensionObj = project.getExtensions().findByName(PropertyKeyConstantsPlugin.EXTENSION_NAME);
        assertThat(extensionObj).isNotNull();
        assertThat(extensionObj).isInstanceOf(PropertyKeyConstantsExtension.class);

        final PropertyKeyConstantsExtension extension = (PropertyKeyConstantsExtension)extensionObj;
        assertThat(extension.getSourceAccess().get()).isEqualTo(SourceAccess.PUBLIC);
        assertThat(extension.getSourceLayout().get()).isEqualTo(SourceLayout.NESTED_CLASSES);

        final Task mainTask = project.getTasks().findByName("generatePropertyKeyConstants");
        assertThat(mainTask).isNotNull().isInstanceOf(PropertyKeyConstantsTask.class);

        final Task testTask = project.getTasks().findByName("generateTestPropertyKeyConstants");
        assertThat(testTask).isNotNull().isInstanceOf(PropertyKeyConstantsTask.class);

        final PropertyKeyConstantsTask task = (PropertyKeyConstantsTask)mainTask;
        assertThat(task.getClassname().isPresent()).isFalse();
        assertThat(task.getOutputDirectory().get().getAsFile().getPath())
                .endsWith("build/generated-src/property-key-constants/main");
        assertThat(task.getSourceAccess().get()).isEqualTo(SourceAccess.PUBLIC);
        assertThat(task.getSourceLayout().get()).isEqualTo(SourceLayout.NESTED_CLASSES);
    }

    public static Stream<Arguments> basenameProvider() {
        return Stream.of(
                arguments("foo.txt", "foo"),
                arguments("foo.bar.txt", "foo.bar"),
                arguments(".foo", ".foo"),
                arguments("foo", "foo"),
                arguments("foo.", "foo"),
                arguments("a/b/foo", "foo"),
                arguments("a/b/foo.txt", "foo"),
                arguments("", ""),
                arguments(".", ".")
        );
    }

    @ParameterizedTest
    @MethodSource("basenameProvider")
    public void testGetBasename(final String original, final String expected) {
        assertThat(PropertyKeyConstantsTask.getBasename(new File(original))).isEqualTo(expected);
    }

    public static Stream<Arguments> camelCaseProvider() {
        return Stream.of(
                arguments("", ""),
                arguments("h", "H"),
                arguments("hello", "Hello"),
                arguments("Hello", "Hello"),
                arguments("HELLO", "Hello"),
                arguments("hello_world", "HelloWorld"),
                arguments("Hello_World", "HelloWorld"),
                arguments("hello-world", "HelloWorld"),
                arguments("hello.world", "HelloWorld"),
                arguments("hello__world", "HelloWorld"),
                arguments("HELLO_WORLD", "HelloWorld"),
                arguments("HelloWorld", "HelloWorld"),
                arguments("-", ""),
                arguments("_", "")
        );
    }

    @ParameterizedTest
    @MethodSource("camelCaseProvider")
    public void testToCamelCase(final String original, final String expected) {
        assertThat(PropertyKeyConstantsTask.toCamelCase(original)).isEqualTo(expected);
    }

    public static Stream<Arguments> upperCaseProvider() {
        return Stream.of(
                arguments("", ""),
                arguments("h", "H"),
                arguments("hello", "HELLO"),
                arguments("Hello", "HELLO"),
                arguments("HELLO", "HELLO"),
                arguments("helloWorld", "HELLO_WORLD"),
                arguments("HelloWorld", "HELLO_WORLD"),
                arguments("hello_world", "HELLO_WORLD"),
                arguments("Hello_World", "HELLO_WORLD"),
                arguments("hello-world", "HELLO_WORLD"),
                arguments("hello.world", "HELLO_WORLD"),
                arguments("hello__world", "HELLO_WORLD"),
                arguments("HELLO_WORLD", "HELLO_WORLD"),
                arguments("-", ""),
                arguments("_", "")
        );
    }

    @ParameterizedTest
    @MethodSource("upperCaseProvider")
    public void testToUpperCase(final String original, final String expected) {
        assertThat(PropertyKeyConstantsTask.toUpperCase(original)).isEqualTo(expected);
    }
}

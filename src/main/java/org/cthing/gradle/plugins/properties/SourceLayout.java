/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.properties;

/**
 * Specifies the layout for the generated constants source code.
 */
public enum SourceLayout {

    /**
     * The key constants for each property file are surrounded by a nested class with a name derived from the
     * property file basename. This is the default layout.
     */
    NESTED_CLASSES,

    /**
     * The key constants for each property file are defined directly in the generated class. The
     * constants are all prefixed with the name of the property file in uppercase.
     */
    FLAT_WITH_PREFIX,

    /**
     * The key constants for each property file are defined directly in the generated class. This
     * is a convenient layout when one property file is specified. If multiple property files are
     * specified, the generated constant names may collide.
     */
    FLAT_WITHOUT_PREFIX,
}

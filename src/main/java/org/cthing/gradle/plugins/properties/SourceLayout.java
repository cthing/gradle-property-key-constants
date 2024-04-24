/*
 * Copyright 2024 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

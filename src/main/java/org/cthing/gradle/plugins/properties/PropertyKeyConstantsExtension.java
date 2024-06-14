/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.properties;

import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;


/**
 * Global configuration for the property key constants plugin.
 */
public class PropertyKeyConstantsExtension {

    private final Property<SourceAccess> sourceAccess;
    private final Property<SourceLayout> sourceLayout;

    public PropertyKeyConstantsExtension(final Project project) {
        final ObjectFactory objects = project.getObjects();
        this.sourceAccess = objects.property(SourceAccess.class).convention(SourceAccess.PUBLIC);
        this.sourceLayout = objects.property(SourceLayout.class).convention(SourceLayout.NESTED_CLASSES);
    }

    /**
     * Obtains the access modifier for the generated constants. The default is {@link SourceAccess#PUBLIC}.
     *
     * @return Access modifier for the generated constants.
     */
    public Property<SourceAccess> getSourceAccess() {
        return this.sourceAccess;
    }

    /**
     * Obtains the layout for the generated source code. The default is {@link SourceLayout#NESTED_CLASSES}.
     *
     * @return Layout for the generated source code.
     */
    public Property<SourceLayout> getSourceLayout() {
        return this.sourceLayout;
    }
}

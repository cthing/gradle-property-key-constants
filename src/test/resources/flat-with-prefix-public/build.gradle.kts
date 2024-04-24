import org.cthing.gradle.plugins.properties.SourceLayout

plugins {
    java
    id("org.cthing.property-key-constants")
}

propertyKeyConstants {
    sourceLayout = SourceLayout.FLAT_WITH_PREFIX
}

tasks {
    generatePropertyKeyConstants {
        classname = "org.cthing.test.Constants"
        source(file("prop1.properties"), file("prop2.properties"))
    }
}

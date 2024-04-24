import org.cthing.gradle.plugins.properties.SourceAccess
import org.cthing.gradle.plugins.properties.SourceLayout

plugins {
    java
    id("org.cthing.property-key-constants")
}

propertyKeyConstants {
    sourceAccess = SourceAccess.PACKAGE
    sourceLayout = SourceLayout.FLAT_WITHOUT_PREFIX
}

tasks {
    generatePropertyKeyConstants {
        classname = "org.cthing.test.Constants"
        source(file("prop1.properties"), file("prop2.properties"))
    }
}

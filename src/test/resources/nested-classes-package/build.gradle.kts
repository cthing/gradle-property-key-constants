import org.cthing.gradle.plugins.properties.SourceAccess

plugins {
    java
    id("org.cthing.property-key-constants")
}

propertyKeyConstants {
    sourceAccess = SourceAccess.PACKAGE
}

tasks {
    generatePropertyKeyConstants {
        classname = "org.cthing.test.Constants"
        source(file("prop1.properties"), file("prop2.properties"))
    }
}

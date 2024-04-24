plugins {
    java
    id("org.cthing.property-key-constants")
}

tasks {
    generatePropertyKeyConstants {
        classname = "org.cthing.test.Constants"
        source(file("prop1.properties"), file("prop2.properties"))
    }
}

import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.Confidence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

repositories {
    mavenCentral()
}

plugins {
    `java-gradle-plugin`
    checkstyle
    jacoco
    signing
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.versions)
}

val baseVersion = "1.0.1"
val isSnapshot = true

val isCIServer = System.getenv("CTHING_CI") != null
val buildNumber = if (isCIServer) System.currentTimeMillis().toString() else "0"
version = if (isSnapshot) "$baseVersion-$buildNumber" else baseVersion
group = "org.cthing"
description = "A Gradle plugin that generates a Java source file with constants for keys in properties files."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

gradlePlugin {
    website = "https://github.com/cthing/gradle-property-key-constants"
    vcsUrl = "https://github.com/cthing/gradle-property-key-constants"

    plugins {
        create("locc4jPlugin") {
            id = "org.cthing.property-key-constants"
            displayName = "Java constants from property file keys"
            description = "A Gradle plugin that generates a Java source file with constants for keys in properties files."
            tags = listOf("properties", "keys", "constants")
            implementationClass = "org.cthing.gradle.plugins.properties.PropertyKeyConstantsPlugin"
        }
    }
}

dependencies {
    implementation(libs.cthingAnnots)

    testImplementation(libs.assertJ)
    testImplementation(libs.commonsIO)
    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)

    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)

    spotbugsPlugins(libs.spotbugsContrib)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    configFile = file("dev/checkstyle/checkstyle.xml")
    configDirectory = file("dev/checkstyle")
    isShowViolations = true
}

spotbugs {
    toolVersion = libs.versions.spotbugs
    ignoreFailures = false
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter = file("dev/spotbugs/suppressions.xml")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<JavaCompile> {
        options.release = libs.versions.java.get().toInt()
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))
    }

    withType<Jar> {
        manifest.attributes(mapOf("Implementation-Title" to project.name,
                                  "Implementation-Vendor" to "C Thing Software",
                                  "Implementation-Version" to project.version))
    }

    withType<Javadoc> {
        val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())
        with(options as StandardJavadocDocletOptions) {
            breakIterator(false)
            encoding("UTF-8")
            bottom("Copyright &copy; $year C Thing Software")
            addStringOption("Xdoclint:all,-missing", "-quiet")
            addStringOption("Werror", "-quiet")
            memberLevel = JavadocMemberLevel.PUBLIC
            outputLevel = JavadocOutputLevel.QUIET
        }
    }

    check {
        dependsOn(buildHealth)
    }

    spotbugsMain {
        reports.create("html").required = true
    }

    spotbugsTest {
        isEnabled = false
    }

    publishPlugins {
        doFirst {
            if (isSnapshot) {
                throw GradleException("Cannot publish a developer build to the Gradle Plugin Portal")
            }
            if (!project.hasProperty("gradle.publish.key") || !project.hasProperty("gradle.publish.secret")) {
                throw GradleException("Gradle Plugin Portal credentials not defined")
            }
        }
    }

    withType<JacocoReport> {
        dependsOn("test")
        with(reports) {
            xml.required = false
            csv.required = false
            html.required = true
            html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
        }
    }

    withType<Test> {
        useJUnitPlatform()

        systemProperty("buildDir", layout.buildDirectory.get().asFile)
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    withType<Sign>().configureEach {
        onlyIf("Signing credentials are present") {
            hasProperty("signing.keyId") && hasProperty("signing.password") && hasProperty("signing.secretKeyRingFile")
        }
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = "current"
        outputFormatter = "plain,xml,html"
        outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.absolutePath

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

publishing {
    val repoUrl = if (isSnapshot) findProperty("cthing.nexus.snapshotsUrl") else findProperty("cthing.nexus.candidatesUrl")
    if (repoUrl != null) {
        repositories {
            maven {
                name = "CThingMaven"
                setUrl(repoUrl)
                credentials {
                    username = property("cthing.nexus.user") as String
                    password = property("cthing.nexus.password") as String
                }
            }
        }
    }
}

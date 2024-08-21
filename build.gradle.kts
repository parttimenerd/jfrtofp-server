
group = "me.bechberger"
description = "Bundle of jfrtofp converter with a custom Firefox Profiler"

inner class ProjectInfo {
    val longName = "Bundle of the JFR to FirefoxProfiler converter with a custom Firefox Profiler"
    val website = "https://github.com/parttimenerd/jfrtofp-server"
    val scm = "git@github.com:parttimenerd/$name.git"
}

fun properties(key: String) = project.findProperty(key).toString()

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "hours")
    resolutionStrategy.cacheChangingModulesFor(0, "hours")
}

repositories {
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"

    id("com.github.johnrengelman.shadow") version "7.1.2"

    id("maven-publish")

    id("java-library")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

apply { plugin("com.github.johnrengelman.shadow") }

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // This dependency is used by the application.
    implementation("org.junit.jupiter:junit-jupiter:5.10.1")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("info.picocli:picocli:4.7.5")
    implementation("io.javalin:javalin:4.6.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("me.bechberger:jfrtofp:0.0.4-SNAPSHOT") {
        this.isChanging = true
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    // Define the main class for the application.
    mainClass.set("me.bechberger.jfrtofp.server.Main")
}

tasks.register<Copy>("copyHooks") {
    from("bin/pre-commit")
    into(".git/hooks")
}

//tasks.findByName("build")?.dependsOn(tasks.findByName("copyHooks"))

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("jfrtofp-server")
                packaging = "jar"
                description.set(project.description)
                inceptionYear.set("2022")
                url.set("https://github.com/parttimenerd/jfrtofp-server")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("parttimenerd")
                        name.set("Johannes Bechberger")
                        email.set("me@mostlynerdless.de")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/parttimenerd/jfrtofp-server")
                    developerConnection.set("scm:git:https://github.com/parttimenerd/jfrtofp-server")
                    url.set("https://github.com/parttimenerd/jfrtofp-server")
                }
            }
        }
    }
    repositories {
        maven {
            name = "Sonatype"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = properties("sonatypeTokenUsername")
                password = properties("sonatypeToken")
            }
        }
    }
}

signing {
    //sign(publishing.publications["mavenJava"])
}

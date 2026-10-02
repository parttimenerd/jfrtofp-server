
group = "me.bechberger"
description = "Bundle of jfrtofp converter with a custom Firefox Profiler"

class ProjectInfo {
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
    mavenLocal()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://maven.pkg.github.com/parttimenerd/jfrtofp")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: properties("gpr.user")
            password = System.getenv("GPR_TOKEN") ?: System.getenv("GITHUB_TOKEN") ?: properties("gpr.token")
        }
    }
}

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.11"

    id("maven-publish")

    id("java-library")
    id("signing")
    id("com.gradleup.nmcp") version "0.1.5"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

apply { plugin("com.gradleup.shadow") }

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // This dependency is used by the application.
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("info.picocli:picocli:4.7.7")
    implementation("io.javalin:javalin:7.2.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("me.bechberger:jfrtofp:0.0.13") {
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
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/parttimenerd/jfrtofp-server")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
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
}

nmcp {
    centralPortal {
        username = properties("sonatypeTokenUsername")
        password = properties("sonatypeToken")
        publishingType = "AUTOMATIC"
    }
}

signing {
    val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
    if (signingKey != null) {
        useInMemoryPgpKeys(
            signingKey,
            providers.gradleProperty("signingInMemoryKeyPassword").orNull,
        )
        sign(publishing.publications["mavenJava"])
    }
}

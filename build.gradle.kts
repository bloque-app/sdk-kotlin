plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "app.bloque.sdk"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }

    plugins.withType<MavenPublishPlugin> {
        configure<PublishingExtension> {
            publications {
                register<MavenPublication>("maven") {
                    afterEvaluate {
                        from(components["java"])
                    }

                    pom {
                        name.set(project.name)
                        description.set("Bloque SDK for Kotlin/Java")
                        url.set("https://github.com/bloque-app/sdk-kotlin")

                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }

                        developers {
                            developer {
                                id.set("bloque")
                                name.set("Bloque Team")
                                email.set("dev@bloque.team")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/bloque-app/sdk-kotlin.git")
                            developerConnection.set("scm:git:ssh://github.com/bloque-app/sdk-kotlin.git")
                            url.set("https://github.com/bloque-app/sdk-kotlin")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "OSSRH"
                    url = uri("https://central.sonatype.com/api/v1/publisher/upload")
                    credentials {
                        username = System.getenv("MAVEN_USERNAME")
                        password = System.getenv("MAVEN_PASSWORD")
                    }
                }
            }
        }
    }
}

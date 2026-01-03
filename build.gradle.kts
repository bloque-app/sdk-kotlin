plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "app.bloque.sdk"
    version = "0.1.0-SNAPSHOT"

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
                    }
                }
            }
        }
    }
}

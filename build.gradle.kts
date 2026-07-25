plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = "app.bloque.sdk"
    version = "0.0.27"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "java-library")
    apply(plugin = "com.vanniktech.maven.publish")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
        signAllPublications()

        coordinates(group.toString(), project.name, version.toString())

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

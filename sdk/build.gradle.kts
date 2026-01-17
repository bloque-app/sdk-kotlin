plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":sdk-core"))
    api(project(":sdk-accounts"))
    api(project(":sdk-identity"))
    api(project(":sdk-compliance"))
    api(project(":sdk-orgs"))
    api(project(":sdk-swap"))
}

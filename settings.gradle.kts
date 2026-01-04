plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "bloque-sdk"

include("sdk-core")
include("sdk-accounts")
include("sdk-identity")
include("sdk-compliance")
include("sdk-orgs")
include("sdk")

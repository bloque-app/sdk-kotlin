plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":sdk"))
}

// Disable publishing for examples module
tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }

sourceSets {
    main {
        kotlin {
            srcDirs("kotlin")
        }
        java {
            srcDirs("java")
        }
    }
}

// Create run tasks for each Kotlin example
val kotlinExamples = listOf(
    "Register" to "app.bloque.examples.RegisterExampleKt",
    "Transfer" to "app.bloque.examples.TransferExampleKt",
    "SharedLedger" to "app.bloque.examples.SharedLedgerExampleKt",
    "BatchTransfer" to "app.bloque.examples.BatchTransferExampleKt",
    "ColBankWithdrawal" to "app.bloque.examples.ColBankWithdrawalExampleKt",
    "Bancolombia" to "app.bloque.examples.BancolombiaExampleKt",
    "Breb" to "app.bloque.examples.BrebExampleKt",
    "BrebSwap" to "app.bloque.examples.BrebSwapExampleKt",
    "ComplianceGates" to "app.bloque.examples.ComplianceGatesExampleKt"
)

kotlinExamples.forEach { (name, className) ->
    tasks.register<JavaExec>("run$name") {
        group = "examples"
        description = "Run the $name example"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set(className)
    }
}

// Create run tasks for each Java example
val javaExamples = listOf(
    "RegisterJava" to "app.bloque.examples.RegisterExample",
    "TransferJava" to "app.bloque.examples.TransferExample",
    "SharedLedgerJava" to "app.bloque.examples.SharedLedgerExample",
    "SwapJava" to "app.bloque.examples.SwapExample",
    "BatchTransferJava" to "app.bloque.examples.BatchTransferExample",
    "BrebJava" to "app.bloque.examples.BrebExample",
    "BrebSwapJava" to "app.bloque.examples.BrebSwapExample"
)

javaExamples.forEach { (name, className) ->
    tasks.register<JavaExec>("run$name") {
        group = "examples"
        description = "Run the $name example (Java)"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set(className)
    }
}

// Default application main class (for ./gradlew :examples:run)
application {
    mainClass.set("app.bloque.examples.RegisterExampleKt")
}

// Task to list all available examples
tasks.register("listExamples") {
    group = "examples"
    description = "List all available example tasks"
    doLast {
        println("\n📚 Available Kotlin Examples:")
        kotlinExamples.forEach { (name, _) ->
            println("  ./gradlew :examples:run$name")
        }
        println("\n☕ Available Java Examples:")
        javaExamples.forEach { (name, _) ->
            println("  ./gradlew :examples:run$name")
        }
        println("\n💡 Default (runs RegisterExample):")
        println("  ./gradlew :examples:run")
        println()
    }
}

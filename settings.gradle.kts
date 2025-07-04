plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "ktor-panel"
include("lib", "exposed-example", "hibernate-example")

// Map the project names to their actual directories
project(":exposed-example").projectDir = file("examples/exposed")
project(":hibernate-example").projectDir = file("examples/hibernate")

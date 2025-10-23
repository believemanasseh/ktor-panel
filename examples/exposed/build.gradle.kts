//plugins {
//    id("org.jetbrains.kotlin.jvm") version "2.1.0"
//    id("io.ktor.plugin") version "3.0.2"
//    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
//    id("org.jetbrains.kotlin.plugin.jpa") version "2.1.0" apply false
//    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.0" apply false
//}
//
//group = "com.example"
//version = "0.0.1"
//
//application {
//    mainClass.set("io.ktor.server.netty.EngineMain")
//}
//
//repositories {
//    mavenCentral()
//}
//
//dependencies {
//    implementation("xyz.daimones:ktor-panel:0.3.2")
//    implementation("io.ktor:ktor-server-core-jvm:3.0.2")
//    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.2")
//    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.2")
//    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
//    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
//    implementation("com.h2database:h2:2.2.224")
//    implementation("io.ktor:ktor-server-netty-jvm:3.0.2")
//    implementation("ch.qos.logback:logback-classic:1.5.15")
//    implementation("io.ktor:ktor-server-config-yaml-jvm:3.0.2")
//    implementation("org.mindrot:jbcrypt:0.4")
//
//    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.2")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
//}


plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.jpa) apply false
    alias(libs.plugins.allopen) apply false
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.h2)
    implementation(libs.jbcrypt)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
}


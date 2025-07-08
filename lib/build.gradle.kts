plugins {
    id("jacoco")
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.jpa)
    alias(libs.plugins.allopen)
    `java-library`
    `maven-publish`
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

group = "xyz.daimones"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // Internal dependencies
    implementation(libs.jbcrypt)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.core)
    implementation(platform(libs.mongodb.driver.bom))
    implementation(libs.mongodb.driver.kotlin.coroutine)
    implementation(libs.bson.kotlinx)

    // Test dependencies
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.logback.classic)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.hibernate.core)

    // Test runtime dependencies
    testRuntimeOnly(libs.hibernate.core)

    // These dependencies are exported to consumers,
    // that is to say found on their compile classpath.
    api(libs.commons.math3)
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.json)
    api(libs.exposed.java.time)
    api(libs.ktor.server.core)
    api(libs.ktor.server.mustache)
    api(libs.jakarta.persistence.api)
}

java {
    // Apply a specific Java toolchain to ease working on different environments.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("ktor-panel") {
            artifactId = "ktor-panel"
            from(components["java"])
            pom {
                name = "Ktor Panel"
                description = "An admin interface library for ktor applications."
                url = "https://github.com/believemanasseh/ktor-panel"
                licenses {
                    license {
                        name = "The 3-Clause BSD License"
                        url = "https://opensource.org/license/bsd-3-clause/"
                    }
                }
                developers {
                    developer {
                        id = "believemanasseh"
                        name = "Illucid Mind"
                        email = "believemanasseh@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/believemanasseh/ktor-panel.git"
                    developerConnection = "scm:git:ssh://github.com/believemanasseh/ktor-panel.git"
                    url = "https://github.com/believemanasseh/ktor-panel"
                }
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

//tasks.withType<Test> {
//    testLogging {
//        showStandardStreams = true
//    }
//}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // Ensure jacocoTestReport runs after tests
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // Ensure tests run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

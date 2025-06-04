plugins {
    id("jacoco")
    alias(libs.plugins.jvm)
    `java-library`
    `maven-publish`
}

group = "xyz.daimones"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.server.mustache)

    // Test dependencies
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.logback.classic)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.platform.launcher)

    // This dependency is exported to consumers,
    // that is to say found on their compile classpath.
    api(libs.commons.math3)

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
        create<MavenPublication>("ktor-admin") {
            artifactId = "ktor-admin"
            from(components["java"])
            pom {
                name = "Ktor Admin"
                description = "An admin interface library for ktor applications."
                url = "https://github.com/believemanasseh/ktor-admin"
                licenses {
                    license {
                        name = "The 3-Clause BSD License"
                        url = "https://opensource.org/license/bsd-3-clause/"
                    }
                }
                developers {
                    developer {
                        id = "believemanasseh"
                        name = "Believe Manasseh"
                        email = "believemanasseh@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/believemanasseh/ktor-admin.git"
                    developerConnection = "scm:git:ssh://github.com/believemanasseh/ktor-admin.git"
                    url = "https://github.com/believemanasseh/ktor-admin"
                }
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

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

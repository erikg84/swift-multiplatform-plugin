plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

group = "io.multiplatform"
version = providers.gradleProperty("VERSION").getOrElse("1.0.0-SNAPSHOT")

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

gradlePlugin {
    website = "https://github.com/erikg84/swift-multiplatform-plugin"
    vcsUrl = "https://github.com/erikg84/swift-multiplatform-plugin"

    plugins {
        create("swiftMultiplatform") {
            id = "io.multiplatform.swift.sdk"
            implementationClass = "io.multiplatform.swift.sdk.SwiftMultiplatformPlugin"
            displayName = "Swift Multiplatform"
            description = "Build Android AAR + iOS XCFramework from a single Swift source tree"
            tags = listOf("swift", "android", "ios", "xcframework", "cross-platform", "multiplatform")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/erikg84/swift-multiplatform-plugin")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:8.7.3")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }

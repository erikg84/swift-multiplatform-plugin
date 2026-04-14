# 01 — Project Scaffold

## Goal
Set up the generalized plugin project with proper package naming, Gradle Plugin Portal compatibility, and composite build structure.

## Package Rename

| Prototype | Generalized |
|-----------|------------|
| `com.dallaslabs.gradle.swift` | `io.multiplatform.swift.sdk` |
| Plugin ID: `com.dallaslabs.swift-multiplatform` | Plugin ID: `io.multiplatform.swift.sdk` |
| Group: `com.dallaslabs` | Group: `io.multiplatform` |

## Directory Structure

```
swift-multiplatform-plugin/
├── plugin/
│   ├── src/main/kotlin/io/multiplatform/swift/sdk/
│   │   ├── SwiftMultiplatformPlugin.kt
│   │   ├── SwiftMultiplatformExtension.kt
│   │   ├── config/
│   │   │   ├── AndroidConfig.kt
│   │   │   ├── IosConfig.kt
│   │   │   ├── PublishingConfig.kt
│   │   │   ├── MavenConfig.kt
│   │   │   └── SwiftRegistryConfig.kt
│   │   ├── tasks/
│   │   │   ├── SwiftResolveTask.kt
│   │   │   ├── SwiftBuildAndroidTask.kt
│   │   │   ├── CopyJniLibsTask.kt
│   │   │   ├── BootstrapSwiftkitCoreTask.kt
│   │   │   ├── GenerateIosManifestTask.kt
│   │   │   ├── SwiftBuildIosTask.kt
│   │   │   ├── AssembleXCFrameworkTask.kt
│   │   │   ├── ZipXCFrameworkTask.kt
│   │   │   ├── PublishGcsTask.kt
│   │   │   ├── PublishRegistryTask.kt
│   │   │   └── SwiftTestTask.kt
│   │   └── util/
│   │       ├── SwiftToolchain.kt
│   │       └── PackageSwiftGenerator.kt
│   ├── src/test/kotlin/io/multiplatform/swift/sdk/
│   │   ├── SwiftMultiplatformPluginTest.kt
│   │   ├── PackageSwiftGeneratorTest.kt
│   │   ├── SwiftToolchainTest.kt
│   │   └── DslParsingTest.kt
│   └── build.gradle.kts
├── example/
│   ├── Sources/ExampleSDK/ExampleSDK.swift
│   ├── Package.swift
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .gitignore
├── .github/workflows/
│   ├── ci.yml
│   └── publish.yml
├── LICENSE
├── PROPOSAL.md
├── README.md
└── CHANGELOG.md
```

## plugin/build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"  // Gradle Plugin Portal
}

group = "io.multiplatform"
version = providers.gradleProperty("VERSION").getOrElse("1.0.0-SNAPSHOT")

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

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

dependencies {
    implementation("com.android.tools.build:gradle:8.7.3")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }
```

## Key Differences from Prototype

1. Package: `io.multiplatform.swift.sdk` (not `com.dallaslabs`)
2. Plugin ID: `io.multiplatform.swift.sdk`
3. `com.gradle.plugin-publish` plugin for Gradle Plugin Portal
4. Tags for discoverability
5. `config/` subdirectory for config classes
6. `util/` subdirectory for toolchain + Package.swift generator
7. `example/` project included
8. `docs/` directory
9. `CHANGELOG.md` for version tracking
10. `LICENSE` file (Apache 2.0)

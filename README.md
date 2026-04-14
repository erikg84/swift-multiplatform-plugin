# Swift Multiplatform Gradle Plugin

Build both **Android AAR** and **iOS XCFramework** from a single Swift source tree — the Swift equivalent of the Kotlin Multiplatform Gradle plugin.

## The Problem

Swift can target both iOS and Android, but the build tooling requires two separate Package.swift files, symlinks, shell scripts, and split CI pipelines. This plugin replaces all of that with one `build.gradle.kts`.

## Quick Start

```kotlin
// build.gradle.kts
plugins {
    id("io.github.erikg84.swift-multiplatform") version "1.0.0"
}

swiftMultiplatform {
    moduleName("MySwiftSDK")
    sources("Sources/MySwiftSDK")
    version("1.0.0")

    android {
        abis("arm64-v8a", "x86_64")
        minSdk(28)
        jextract(true)
        namespace("com.example.myswiftsdk")
    }

    ios {
        deploymentTarget("15.0")
    }

    publishing {
        maven {
            groupId("com.example")
            artifactId("my-swift-sdk")
            url("gcs://my-bucket/maven")
        }
    }
}
```

```bash
./gradlew buildAll      # Android AAR + iOS XCFramework
./gradlew publishAll    # Publish to Maven + Swift Registry
```

## What It Replaces

| Before | After |
|--------|-------|
| 2 Package.swift files | 1 |
| Symlink to share sources | Gone |
| Shell scripts for XCFramework | Plugin task |
| Split CI jobs | `./gradlew publishAll` |
| Manual POM management | Automatic |

## Status

**In development.** See [PROPOSAL.md](PROPOSAL.md) for the full design and [workplan/](workplan/) for implementation details.

Based on the working prototype at [swift-multiplatform-gradle-plugin](https://github.com/erikg84/swift-multiplatform-gradle-plugin) which has been validated against a production Swift SDK.

## Prerequisites

- JDK 17
- Swift 6.3+ with [Swift SDK for Android](https://www.swift.org/documentation/articles/swift-sdk-for-android-getting-started.html)
- Xcode 16+ (for iOS XCFramework builds)
- [swiftly](https://github.com/swiftlang/swiftly) (Swift version manager)

## License

Apache 2.0

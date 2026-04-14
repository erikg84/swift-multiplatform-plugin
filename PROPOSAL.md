# Swift Multiplatform Gradle Plugin

A Gradle plugin that builds both **Android AAR** and **iOS XCFramework** artifacts from a single Swift source tree — the Swift equivalent of the Kotlin Multiplatform Gradle plugin.

## Problem

Swift can now target both iOS and Android (via the official Swift SDK for Android + swift-java). But the build tooling hasn't caught up:

- SPM and Gradle are separate build systems with no integration
- iOS builds need `xcodebuild` with `platforms: [.iOS]` in Package.swift
- Android builds need `swift build --swift-sdk` with `platforms: [.macOS]` and swift-java
- These require **incompatible Package.swift configurations** — you can't satisfy both from one file
- Teams resort to symlinks, duplicate configs, shell scripts, and split CI pipelines

This plugin eliminates all of that.

## Solution

```kotlin
plugins {
    id("io.github.erikg84.swift-multiplatform") version "1.0.0"
}

swiftMultiplatform {
    moduleName = "MySwiftSDK"
    sources = "Sources/MySwiftSDK"
    version = "1.0.0"

    android {
        abis("arm64-v8a", "x86_64")
        minSdk(28)
        jextract(true)
    }

    ios {
        deploymentTarget("15.0")
    }

    publishing {
        maven {
            groupId = "com.example"
            artifactId = "my-swift-sdk"
            url = "gcs://my-bucket/maven"  // or any Maven repo URL
        }
        swiftRegistry {
            url = "https://my-gitea.com/api/packages/my-org/swift"
            token = providers.environmentVariable("REGISTRY_TOKEN")
        }
    }
}
```

Then:
```bash
./gradlew buildAll      # Android AAR + iOS XCFramework
./gradlew publishAll    # Publishes to Maven + GCS + Swift Registry
./gradlew swiftTest     # Runs host-platform tests
```

## Key Design Principles

### 1. Zero Vendor Lock-in
- Publishing targets are pluggable: Maven Central, GCS, S3, Artifactory, any Maven-compatible repo
- Swift Package Registry support for any SE-0292 compliant server (Gitea, Artifactory, custom)
- No hardcoded URLs, credentials, or infrastructure assumptions

### 2. Proper Maven Publications
- AAR published via `from(components["release"])` with full transitive dependency metadata
- POM includes all dependencies (swiftkit-core, etc.) so consumers don't need manual additions
- Gradle Module Metadata (.module) published alongside for modern Gradle consumers

### 3. Clean iOS Builds
- Plugin generates a temporary iOS-compatible Package.swift at build time
- Strips Android-only dependencies (swift-java) that don't support iOS
- Restores the original after build — no permanent file modifications
- Supports custom build scripts for complex XCFramework scenarios

### 4. Single Source of Truth
- One Package.swift for dependency declarations
- One build.gradle.kts for build configuration
- One gradle.properties for version management
- No symlinks, no duplicate configs, no shell scripts

## What's Different from the Prototype

The prototype (`swift-multiplatform-gradle-plugin`) was built for a specific project. This version:

| Issue in Prototype | Fix in This Version |
|---|---|
| Hardcoded GCS/Gitea/Dallas Labs references | Fully configurable publishing targets |
| AAR POM missing transitive deps (`artifact()`) | Proper `from(component)` with deferred wiring |
| iOS build needs external shell script hack | Plugin generates temp Package.swift internally |
| Extension properties need `.set()` calls | Convenience methods for clean DSL |
| No error messages for misconfiguration | Actionable validation with clear error messages |
| Only tested against one SDK | Example project + integration test suite |
| Published to GitHub Packages (auth required) | Published to Gradle Plugin Portal (public) |
| Plugin ID tied to `com.dallaslabs` | Generic `io.github.erikg84.swift-multiplatform` |

## Architecture

```
swift-multiplatform-plugin/
├── plugin/                           # The plugin (composite build)
│   ├── src/main/kotlin/
│   │   └── io/github/erikg84/swiftmultiplatform/
│   │       ├── SwiftMultiplatformPlugin.kt
│   │       ├── SwiftMultiplatformExtension.kt
│   │       ├── config/
│   │       │   ├── AndroidConfig.kt
│   │       │   ├── IosConfig.kt
│   │       │   └── PublishingConfig.kt
│   │       ├── tasks/
│   │       │   ├── SwiftResolveTask.kt
│   │       │   ├── SwiftBuildAndroidTask.kt
│   │       │   ├── CopyJniLibsTask.kt
│   │       │   ├── BootstrapSwiftkitCoreTask.kt
│   │       │   ├── GenerateIosManifestTask.kt  ← NEW: generates temp Package.swift
│   │       │   ├── SwiftBuildIosTask.kt
│   │       │   ├── AssembleXCFrameworkTask.kt
│   │       │   ├── ZipXCFrameworkTask.kt
│   │       │   ├── PublishGcsTask.kt
│   │       │   ├── PublishRegistryTask.kt      ← NEW: generic, not Gitea-specific
│   │       │   └── SwiftTestTask.kt
│   │       └── util/
│   │           ├── SwiftToolchain.kt
│   │           └── PackageSwiftGenerator.kt    ← NEW: generates iOS Package.swift
│   └── src/test/kotlin/
├── example/                          # Working example SDK
│   ├── Sources/ExampleSDK/
│   ├── Package.swift
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/
│   ├── getting-started.md
│   ├── configuration.md
│   ├── publishing.md
│   └── ci-integration.md
└── .github/workflows/
    ├── ci.yml
    └── publish.yml
```

## Publishing Strategy

### Plugin Distribution
- **Gradle Plugin Portal** (primary) — public, no auth, standard `plugins { id(...) }` resolution
- **Maven Central** (backup) — for organizations that proxy Gradle Plugin Portal
- **GitHub Packages** (development) — SNAPSHOT versions during development

### Plugin ID
```
io.github.erikg84.swift-multiplatform
```

Follows Gradle convention: reverse domain of the GitHub org.

## Roadmap

### v1.0.0 — Core
- [ ] Android cross-compilation (per-ABI, swift-java, JExtract)
- [ ] iOS XCFramework (generated temp Package.swift, xcodebuild)
- [ ] Proper Maven publication with transitive deps
- [ ] Swift Package Registry publishing (SE-0292)
- [ ] Clean DSL with convenience methods
- [ ] Validation with actionable errors
- [ ] Example project
- [ ] Published to Gradle Plugin Portal

### v1.1.0 — Polish
- [ ] Gradle build cache support
- [ ] Incremental build detection
- [ ] S3 publishing support
- [ ] Maven Central publishing support
- [ ] Artifactory publishing support

### v1.2.0 — Advanced
- [ ] Multi-module Swift package support
- [ ] Custom framework bundling (CocoaPods, Carthage)
- [ ] Swift-on-server target (Linux .so)

## License

Apache 2.0

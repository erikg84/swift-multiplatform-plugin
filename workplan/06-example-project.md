# 06 — Example Project

## Goal
A minimal but complete example SDK that demonstrates the plugin. Should be buildable out of the box on any Mac with Swift + Android SDK installed.

## Structure

```
example/
├── Sources/ExampleSDK/
│   ├── Greeting.swift
│   └── Calculator.swift
├── Tests/ExampleSDKTests/
│   └── GreetingTests.swift
├── Package.swift
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Sources/ExampleSDK/Greeting.swift

```swift
import Foundation

public struct Greeting {
    public let name: String

    public init(name: String) {
        self.name = name
    }

    public func hello() -> String {
        return "Hello, \(name)!"
    }

    public func goodbye() -> String {
        return "Goodbye, \(name)!"
    }
}
```

## Package.swift

```swift
// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "ExampleSDK",
    platforms: [.iOS(.v15), .macOS(.v15)],
    products: [
        .library(name: "ExampleSDK", type: .dynamic, targets: ["ExampleSDK"]),
    ],
    dependencies: [
        .package(url: "https://github.com/swiftlang/swift-java.git", from: "0.1.2"),
    ],
    targets: [
        .target(
            name: "ExampleSDK",
            dependencies: [
                .product(name: "SwiftJava", package: "swift-java"),
            ],
            swiftSettings: [.swiftLanguageMode(.v5)],
            plugins: [
                .plugin(name: "JExtractSwiftPlugin", package: "swift-java"),
            ]
        ),
        .testTarget(name: "ExampleSDKTests", dependencies: ["ExampleSDK"]),
    ]
)
```

## build.gradle.kts

```kotlin
plugins {
    id("io.github.erikg84.swift-multiplatform")
}

swiftMultiplatform {
    moduleName("ExampleSDK")
    sources("Sources/ExampleSDK")
    version(providers.gradleProperty("VERSION"))

    android {
        abis("arm64-v8a", "x86_64")
        minSdk(28)
        jextract(true)
        namespace("io.github.erikg84.examplesdk")
    }

    ios {
        deploymentTarget("15.0")
        stripDependencies("swift-java")
    }
}
```

## settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "ExampleSDK"
```

## gradle.properties

```properties
VERSION=1.0.0
```

## Usage

```bash
cd example

# Build both platforms
./gradlew buildAll

# iOS only
./gradlew assembleXCFramework

# Android only
./gradlew assembleRelease

# Run tests
swift test
```

## Purpose
- Proves the plugin works end-to-end
- Serves as copy-paste starter for new users
- Used in CI to validate plugin changes don't break consumer builds

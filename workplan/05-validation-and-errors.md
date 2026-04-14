# 05 — Validation and Error Messages

## Goal
Fail fast with clear, actionable error messages instead of cryptic Gradle/xcodebuild failures.

## Validation Points

### Configuration Time (plugin apply)

```kotlin
// In SwiftMultiplatformPlugin.afterEvaluate:
fun validate(ext: SwiftMultiplatformExtension) {
    require(ext.moduleName.isPresent) {
        """
        swiftMultiplatform.moduleName is required.

        Add to your build.gradle.kts:
          swiftMultiplatform {
              moduleName("YourModuleName")
          }
        """.trimIndent()
    }

    require(ext.sourcesDir.isPresent) {
        "swiftMultiplatform.sources is required. Set the path to your Swift sources directory."
    }

    require(ext.version.isPresent) {
        """
        swiftMultiplatform.version is required.

        Either set it directly:
          swiftMultiplatform { version("1.0.0") }
        Or from gradle.properties:
          swiftMultiplatform { version(providers.gradleProperty("VERSION_NAME")) }
        """.trimIndent()
    }

    val sourcesDir = File(project.projectDir, ext.sourcesDir.get())
    require(sourcesDir.exists() && sourcesDir.isDirectory) {
        "Sources directory not found: ${sourcesDir.absolutePath}\n" +
        "Ensure swiftMultiplatform.sources points to an existing directory."
    }

    require(File(project.projectDir, "Package.swift").exists()) {
        "Package.swift not found in project root.\n" +
        "The swift-multiplatform plugin requires a Package.swift for dependency management."
    }
}
```

### Task Execution Time

#### SwiftResolveTask
```kotlin
// Check swift is available before running
val swift = SwiftToolchain.findSwiftOrNull()
    ?: throw GradleException("""
        Swift compiler not found.

        Install Swift from https://swift.org/install
        Or install swiftly: https://github.com/swiftlang/swiftly

        Searched: /opt/homebrew/bin/swift, /usr/bin/swift
    """.trimIndent())
```

#### SwiftBuildAndroidTask
```kotlin
// Check Swift SDK for Android is installed
val sdkPath = SwiftToolchain.findSwiftSdkPathOrNull()
    ?: throw GradleException("""
        Swift SDK for Android not found.

        Install with:
          swift sdk install https://download.swift.org/swift-6.3-release/android-sdk/...

        Searched: ~/Library/org.swift.swiftpm/swift-sdks/, ~/.config/swiftpm/swift-sdks/
    """.trimIndent())

// Check specific ABI SDK bundle exists
val bundle = File(sdkPath, sdkBundleName.get())
if (!bundle.exists()) {
    throw GradleException("""
        Swift SDK bundle not found: ${sdkBundleName.get()}

        Available bundles in $sdkPath:
          ${File(sdkPath).listFiles()?.joinToString("\n  ") ?: "(empty)"}

        Install the correct SDK version or set:
          android { swiftSdk("correct-bundle-name") }
    """.trimIndent())
}
```

#### SwiftBuildIosTask
```kotlin
// Check xcodebuild is available
if (!File("/usr/bin/xcodebuild").exists()) {
    throw GradleException("""
        xcodebuild not found. Xcode must be installed for iOS builds.

        Install Xcode from the Mac App Store or:
          xcode-select --install
    """.trimIndent())
}
```

#### PublishRegistryTask
```kotlin
// Validate credentials before attempting upload
if (token.get().isBlank()) {
    throw GradleException("""
        Swift Package Registry token is empty.

        Set it via:
          Environment variable: REGISTRY_TOKEN=your_token
          Gradle property: -PREGISTRY_TOKEN=your_token
          Or in build.gradle.kts:
            swiftRegistry { token(providers.environmentVariable("REGISTRY_TOKEN")) }
    """.trimIndent())
}
```

## Error Format Convention

All errors follow the pattern:
```
What failed.

How to fix it.

Context (paths searched, values found, etc.)
```

## SwiftToolchain — Safe Discovery Methods

Add `*OrNull` variants that return null instead of throwing:

```kotlin
object SwiftToolchain {
    fun findSwiftOrNull(): String? { ... }
    fun findSwift(): String = findSwiftOrNull()
        ?: throw GradleException("Swift not found. Install from https://swift.org/install")

    fun findSwiftlyOrNull(): String? { ... }
    fun findSwiftly(): String = findSwiftlyOrNull()
        ?: throw GradleException("swiftly not found. Install from https://swift.org/install")

    fun findSwiftSdkPathOrNull(): String? { ... }
    fun findSwiftSdkPath(): String = findSwiftSdkPathOrNull()
        ?: throw GradleException("Swift SDK for Android not found.")
}
```

# 04 — DSL Cleanup

## Problem
The prototype requires `.set()` calls for every property because the extension classes only define abstract properties without convenience methods. This makes the DSL verbose and unfamiliar to Kotlin Gradle users.

## Solution: Convenience Methods on Every Config Class

Every abstract property gets a corresponding setter method:

```kotlin
abstract class AndroidConfig {
    abstract val abis: ListProperty<String>
    abstract val minSdk: Property<Int>
    abstract val compileSdk: Property<Int>
    abstract val namespace: Property<String>
    abstract val swiftSdk: Property<String>
    abstract val swiftVersion: Property<String>
    abstract val jextractEnabled: Property<Boolean>
    abstract val excludeFiles: ListProperty<String>
    abstract val stripDependencies: ListProperty<String>

    init {
        abis.convention(listOf("arm64-v8a", "x86_64"))
        minSdk.convention(28)
        compileSdk.convention(36)
        swiftSdk.convention("swift-6.3-RELEASE_android.artifactbundle")
        swiftVersion.convention("6.3")
        jextractEnabled.convention(true)
        stripDependencies.convention(emptyList())
    }

    // Convenience methods — these make the DSL clean
    fun abis(vararg values: String) { abis.set(values.toList()) }
    fun minSdk(value: Int) { minSdk.set(value) }
    fun compileSdk(value: Int) { compileSdk.set(value) }
    fun namespace(value: String) { namespace.set(value) }
    fun swiftSdk(value: String) { swiftSdk.set(value) }
    fun swiftVersion(value: String) { swiftVersion.set(value) }
    fun jextract(enabled: Boolean) { jextractEnabled.set(enabled) }
    fun excludeFiles(vararg files: String) { excludeFiles.set(files.toList()) }
    fun stripDependencies(vararg deps: String) { stripDependencies.set(deps.toList()) }
}
```

This enables clean DSL:
```kotlin
android {
    abis("arm64-v8a", "x86_64")     // not abis.set(listOf(...))
    minSdk(28)                        // not minSdk.set(28)
    jextract(true)                    // not jextractEnabled.set(true)
    namespace("com.example.sdk")      // not namespace.set("com.example.sdk")
}
```

## All Config Classes Get the Same Treatment

### SwiftMultiplatformExtension
```kotlin
fun moduleName(value: String) { moduleName.set(value) }
fun sources(value: String) { sourcesDir.set(value) }
fun version(value: String) { version.set(value) }
fun version(provider: Provider<String>) { version.set(provider) }
```

### IosConfig
```kotlin
fun targets(vararg values: String) { targets.set(values.toList()) }
fun deploymentTarget(value: String) { minimumDeployment.set(value) }
fun frameworkName(value: String) { frameworkName.set(value) }
fun buildScript(path: String) { buildScript.set(path) }
fun stripDependencies(vararg deps: String) { stripDependencies.set(deps.toList()) }
fun stripPlugins(vararg plugins: String) { stripPlugins.set(plugins.toList()) }
```

### MavenConfig
```kotlin
fun groupId(value: String) { groupId.set(value) }
fun artifactId(value: String) { artifactId.set(value) }
fun url(value: String) { url.set(value) }
```

### SwiftRegistryConfig (renamed from GiteaConfig)
```kotlin
fun url(value: String) { url.set(value) }
fun url(provider: Provider<String>) { url.set(provider) }
fun token(value: String) { token.set(value) }
fun token(provider: Provider<String>) { token.set(provider) }
fun scope(value: String) { scope.set(value) }
fun packageName(value: String) { packageName.set(value) }
fun authorName(value: String) { authorName.set(value) }
```

## Result: Clean Consumer DSL

```kotlin
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
        frameworkName("MySwiftSDK")
        stripDependencies("swift-java")
    }

    publishing {
        maven {
            groupId("com.example")
            artifactId("my-swift-sdk")
            url("gcs://my-bucket/maven")
        }
        swiftRegistry {
            url(providers.environmentVariable("REGISTRY_URL"))
            token(providers.environmentVariable("REGISTRY_TOKEN"))
            scope("my-org")
            packageName("my-swift-sdk")
        }
    }
}
```

Zero `.set()` calls. Reads like a configuration file.

package io.multiplatform.swift.sdk.config

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class AndroidConfig {
    abstract val abis: ListProperty<String>
    abstract val swiftSdk: Property<String>
    abstract val swiftVersion: Property<String>
    abstract val minSdk: Property<Int>
    abstract val compileSdk: Property<Int>
    abstract val namespace: Property<String>
    abstract val jextractEnabled: Property<Boolean>
    abstract val excludeFiles: ListProperty<String>
    abstract val republishSwiftkitCore: Property<Boolean>

    init {
        abis.convention(listOf("arm64-v8a", "x86_64"))
        swiftSdk.convention("swift-6.3-RELEASE_android.artifactbundle")
        swiftVersion.convention("6.3")
        minSdk.convention(28)
        compileSdk.convention(36)
        jextractEnabled.convention(true)
        republishSwiftkitCore.convention(true)
    }

    fun abis(vararg values: String) { abis.set(values.toList()) }
    fun swiftSdk(value: String) { swiftSdk.set(value) }
    fun swiftVersion(value: String) { swiftVersion.set(value) }
    fun minSdk(value: Int) { minSdk.set(value) }
    fun compileSdk(value: Int) { compileSdk.set(value) }
    fun namespace(value: String) { namespace.set(value) }
    fun jextract(enabled: Boolean) { jextractEnabled.set(enabled) }
    fun excludeFiles(vararg files: String) { excludeFiles.set(files.toList()) }
}

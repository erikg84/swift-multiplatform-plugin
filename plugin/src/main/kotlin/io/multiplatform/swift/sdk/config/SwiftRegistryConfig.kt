package io.multiplatform.swift.sdk.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

abstract class SwiftRegistryConfig {
    abstract val url: Property<String>
    abstract val token: Property<String>
    abstract val scope: Property<String>
    abstract val packageName: Property<String>
    abstract val authorName: Property<String>

    init {
        authorName.convention("Swift Multiplatform SDK")
    }

    fun url(value: String) { url.set(value) }
    fun url(provider: Provider<String>) { url.set(provider) }
    fun token(value: String) { token.set(value) }
    fun token(provider: Provider<String>) { token.set(provider) }
    fun scope(value: String) { scope.set(value) }
    fun packageName(value: String) { packageName.set(value) }
    fun authorName(value: String) { authorName.set(value) }
}

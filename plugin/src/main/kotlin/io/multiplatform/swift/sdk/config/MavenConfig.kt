package io.multiplatform.swift.sdk.config

import org.gradle.api.provider.Property

abstract class MavenConfig {
    abstract val groupId: Property<String>
    abstract val artifactId: Property<String>
    abstract val url: Property<String>

    fun groupId(value: String) { groupId.set(value) }
    fun artifactId(value: String) { artifactId.set(value) }
    fun url(value: String) { url.set(value) }
}

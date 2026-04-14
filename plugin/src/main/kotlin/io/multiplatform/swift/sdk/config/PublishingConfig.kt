package io.multiplatform.swift.sdk.config

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class PublishingConfig @Inject constructor(
    objects: ObjectFactory
) {
    val maven: MavenConfig = objects.newInstance(MavenConfig::class.java)
    val swiftRegistry: SwiftRegistryConfig = objects.newInstance(SwiftRegistryConfig::class.java)

    fun maven(action: Action<MavenConfig>) = action.execute(maven)
    fun swiftRegistry(action: Action<SwiftRegistryConfig>) = action.execute(swiftRegistry)
}

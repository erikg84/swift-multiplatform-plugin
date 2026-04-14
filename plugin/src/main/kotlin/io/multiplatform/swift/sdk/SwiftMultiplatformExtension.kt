package io.multiplatform.swift.sdk

import io.multiplatform.swift.sdk.config.AndroidConfig
import io.multiplatform.swift.sdk.config.IosConfig
import io.multiplatform.swift.sdk.config.PublishingConfig
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

abstract class SwiftMultiplatformExtension @Inject constructor(
    objects: ObjectFactory
) {
    abstract val moduleName: Property<String>
    abstract val sourcesDir: Property<String>
    abstract val version: Property<String>

    val android: AndroidConfig = objects.newInstance(AndroidConfig::class.java)
    val ios: IosConfig = objects.newInstance(IosConfig::class.java)
    val publishing: PublishingConfig = objects.newInstance(PublishingConfig::class.java)

    fun moduleName(value: String) { moduleName.set(value) }
    fun sources(value: String) { sourcesDir.set(value) }
    fun version(value: String) { version.set(value) }
    fun version(provider: Provider<String>) { version.set(provider) }

    fun android(action: Action<AndroidConfig>) = action.execute(android)
    fun ios(action: Action<IosConfig>) = action.execute(ios)
    fun publishing(action: Action<PublishingConfig>) = action.execute(publishing)
}

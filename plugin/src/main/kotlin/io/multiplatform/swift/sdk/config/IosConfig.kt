package io.multiplatform.swift.sdk.config

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class IosConfig {
    abstract val targets: ListProperty<String>
    abstract val minimumDeployment: Property<String>
    abstract val frameworkName: Property<String>
    abstract val buildScript: Property<String>
    abstract val stripDependencies: ListProperty<String>
    abstract val stripPlugins: ListProperty<String>

    init {
        targets.convention(listOf("ios-arm64", "ios-simulator-arm64"))
        minimumDeployment.convention("15.0")
        stripDependencies.convention(listOf("swift-java"))
        stripPlugins.convention(listOf("JExtractSwiftPlugin"))
    }

    fun targets(vararg values: String) { targets.set(values.toList()) }
    fun deploymentTarget(value: String) { minimumDeployment.set(value) }
    fun frameworkName(value: String) { frameworkName.set(value) }
    fun buildScript(path: String) { buildScript.set(path) }
    fun stripDependencies(vararg deps: String) { stripDependencies.set(deps.toList()) }
    fun stripPlugins(vararg plugins: String) { stripPlugins.set(plugins.toList()) }
}

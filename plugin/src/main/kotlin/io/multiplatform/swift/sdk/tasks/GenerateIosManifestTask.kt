package io.multiplatform.swift.sdk.tasks

import io.multiplatform.swift.sdk.util.PackageSwiftGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Generates a temporary iOS-compatible Package.swift by stripping
 * Android-only dependencies (swift-java). Backs up the original
 * and replaces it in-place so xcodebuild reads the iOS version.
 */
abstract class GenerateIosManifestTask : DefaultTask() {
    @get:InputFile abstract val originalManifest: RegularFileProperty
    @get:Input abstract val moduleName: Property<String>
    @get:Input abstract val iosPlatformVersion: Property<String>
    @get:Input abstract val stripDependencies: ListProperty<String>

    init {
        description = "Generates iOS-compatible Package.swift (strips Android-only deps)"
        group = "swift"
    }

    @TaskAction
    fun generate() {
        val original = originalManifest.get().asFile
        val backup = File(original.parentFile, "Package.swift.build-backup")

        // Backup original
        original.copyTo(backup, overwrite = true)
        logger.lifecycle("Backed up Package.swift → Package.swift.build-backup")

        // Extract iOS-safe dependencies from the original
        val iosDeps = PackageSwiftGenerator.extractIosDependencies(
            original, stripDependencies.get()
        )
        logger.lifecycle("iOS dependencies (${iosDeps.size}): ${iosDeps.map { it.first.substringAfterLast("/") }}")

        // Generate iOS-only manifest and overwrite the original
        PackageSwiftGenerator.generate(
            outputFile = original,
            moduleName = moduleName.get(),
            iosPlatformVersion = iosPlatformVersion.get(),
            iosDependencies = iosDeps,
        )
        logger.lifecycle("Swapped Package.swift to iOS-only manifest")
    }
}

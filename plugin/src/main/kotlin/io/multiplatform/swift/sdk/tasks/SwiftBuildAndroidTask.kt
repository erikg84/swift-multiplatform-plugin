package io.multiplatform.swift.sdk.tasks

import io.multiplatform.swift.sdk.util.SwiftToolchain
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class SwiftBuildAndroidTask : DefaultTask() {
    @get:Input abstract val abi: Property<String>
    @get:Input abstract val swiftTriple: Property<String>
    @get:Input abstract val swiftVersion: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Cross-compiles Swift for an Android ABI"
        group = "swift"
    }

    @TaskAction
    fun build() {
        logger.lifecycle("Building Swift for ${abi.get()} (${swiftTriple.get()})...")
        execOperations.exec {
            commandLine(
                SwiftToolchain.findSwiftly(), "run", "swift", "build",
                "+${swiftVersion.get()}",
                "--swift-sdk", swiftTriple.get(),
                "--disable-sandbox"
            )
            workingDir(project.projectDir)
        }
    }
}

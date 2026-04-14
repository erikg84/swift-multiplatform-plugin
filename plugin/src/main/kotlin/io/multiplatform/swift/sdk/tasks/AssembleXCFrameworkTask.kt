package io.multiplatform.swift.sdk.tasks

import io.multiplatform.swift.sdk.util.SwiftToolchain
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class AssembleXCFrameworkTask : DefaultTask() {
    @get:Input abstract val frameworkName: Property<String>
    @get:Input abstract val archivePaths: ListProperty<String>
    @get:Input @get:Optional abstract val buildScript: Property<String>
    @get:OutputDirectory abstract val xcframeworkDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Assembles an XCFramework from platform archives"
        group = "swift"
    }

    @TaskAction
    fun assemble() {
        val output = xcframeworkDir.get().asFile
        if (output.exists()) output.deleteRecursively()

        if (buildScript.isPresent && buildScript.get().isNotBlank()) {
            assembleViaScript(output)
        } else {
            assembleViaXcodebuild(output)
        }

        val infoPlist = File(output, "Info.plist")
        if (!infoPlist.exists()) {
            throw GradleException("XCFramework assembly failed: ${output.absolutePath}/Info.plist not found")
        }
        logger.lifecycle("XCFramework assembled: ${output.absolutePath}")
    }

    private fun assembleViaXcodebuild(output: File) {
        val args = mutableListOf(SwiftToolchain.findXcodebuild(), "-create-xcframework")
        for (archive in archivePaths.get()) {
            args += listOf("-archive", archive, "-framework", "${frameworkName.get()}.framework")
        }
        args += listOf("-output", output.absolutePath)
        execOperations.exec { commandLine(args) }
    }

    private fun assembleViaScript(output: File) {
        val script = File(project.projectDir, buildScript.get())
        if (!script.exists()) {
            throw GradleException("Build script not found: ${script.absolutePath}")
        }
        logger.lifecycle("Using custom build script: ${script.absolutePath}")
        execOperations.exec {
            commandLine("bash", script.absolutePath, output.parentFile.absolutePath)
            workingDir(project.projectDir)
        }
    }
}

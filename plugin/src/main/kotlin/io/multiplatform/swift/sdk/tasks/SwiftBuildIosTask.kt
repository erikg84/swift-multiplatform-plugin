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

abstract class SwiftBuildIosTask : DefaultTask() {
    @get:Input abstract val scheme: Property<String>
    @get:Input abstract val destination: Property<String>
    @get:Input abstract val minimumDeployment: Property<String>
    @get:OutputDirectory abstract val archivePath: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Archives Swift framework for an iOS platform"
        group = "swift"
    }

    @TaskAction
    fun archive() {
        val archiveArg = archivePath.get().asFile.absolutePath.removeSuffix(".xcarchive")
        logger.lifecycle("Archiving ${scheme.get()} for ${destination.get()}...")

        execOperations.exec {
            commandLine(
                SwiftToolchain.findXcodebuild(),
                "archive",
                "-scheme", scheme.get(),
                "-destination", destination.get(),
                "-archivePath", archiveArg,
                "-derivedDataPath", "${project.layout.buildDirectory.get().asFile}/derivedData",
                "SKIP_INSTALL=NO",
                "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
                "IPHONEOS_DEPLOYMENT_TARGET=${minimumDeployment.get()}",
                "CODE_SIGN_IDENTITY=",
                "CODE_SIGNING_REQUIRED=NO",
            )
            workingDir(project.projectDir)
        }
    }
}

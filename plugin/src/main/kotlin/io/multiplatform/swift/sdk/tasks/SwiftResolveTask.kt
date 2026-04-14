package io.multiplatform.swift.sdk.tasks

import io.multiplatform.swift.sdk.util.SwiftToolchain
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class SwiftResolveTask : DefaultTask() {
    @get:OutputDirectory
    abstract val checkoutsDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Resolves Swift Package Manager dependencies"
        group = "swift"
        checkoutsDir.convention(project.layout.projectDirectory.dir(".build/checkouts"))
    }

    @TaskAction
    fun resolve() {
        logger.lifecycle("Resolving Swift package dependencies...")
        execOperations.exec {
            commandLine(SwiftToolchain.findSwift(), "package", "resolve")
            workingDir(project.projectDir)
        }
    }
}

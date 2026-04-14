package io.multiplatform.swift.sdk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class PublishGcsTask : DefaultTask() {
    @get:InputFile abstract val zipFile: RegularFileProperty
    @get:Input abstract val gcsDestination: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Uploads XCFramework zip to Google Cloud Storage"
        group = "swift publishing"
    }

    @TaskAction
    fun upload() {
        val dest = gcsDestination.get()
        logger.lifecycle("Uploading to $dest...")
        execOperations.exec {
            commandLine("gcloud", "storage", "cp", zipFile.get().asFile.absolutePath, dest)
        }
        logger.lifecycle("Uploaded XCFramework to $dest")
    }
}

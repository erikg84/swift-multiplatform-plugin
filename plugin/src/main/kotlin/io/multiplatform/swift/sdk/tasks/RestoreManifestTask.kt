package io.multiplatform.swift.sdk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Restores the original Package.swift from backup after iOS builds.
 * Runs even if the build failed (wired via finalizedBy).
 */
abstract class RestoreManifestTask : DefaultTask() {

    init {
        description = "Restores original Package.swift after iOS build"
        group = "swift"
    }

    @TaskAction
    fun restore() {
        val backup = File(project.projectDir, "Package.swift.build-backup")
        val target = File(project.projectDir, "Package.swift")
        if (backup.exists()) {
            backup.copyTo(target, overwrite = true)
            backup.delete()
            logger.lifecycle("Restored original Package.swift")
        }
    }
}

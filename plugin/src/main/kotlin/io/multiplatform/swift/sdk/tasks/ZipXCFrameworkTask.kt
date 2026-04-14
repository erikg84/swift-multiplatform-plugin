package io.multiplatform.swift.sdk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class ZipXCFrameworkTask : DefaultTask() {
    @get:Input abstract val frameworkName: Property<String>
    @get:Input abstract val version: Property<String>
    @get:InputDirectory abstract val xcframeworkDir: DirectoryProperty
    @get:OutputFile abstract val zipFile: RegularFileProperty
    @get:OutputFile abstract val checksumFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Zips an XCFramework and computes its SHA-256 checksum"
        group = "swift"
    }

    @TaskAction
    fun zipAndChecksum() {
        val zip = zipFile.get().asFile
        zip.parentFile.mkdirs()
        if (zip.exists()) zip.delete()

        execOperations.exec {
            commandLine("zip", "-qry", zip.absolutePath, "${frameworkName.get()}.xcframework")
            workingDir(xcframeworkDir.get().asFile.parentFile)
        }
        logger.lifecycle("Zipped: ${zip.absolutePath} (${zip.length() / 1024 / 1024} MB)")

        val checksum = checksumFile.get().asFile
        val stdout = ByteArrayOutputStream()
        try {
            execOperations.exec {
                commandLine("swift", "package", "compute-checksum", zip.absolutePath)
                standardOutput = stdout
            }
            checksum.writeText(stdout.toString().trim())
        } catch (_: Exception) {
            logger.warn("swift package compute-checksum failed, falling back to shasum")
            val shaOut = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("shasum", "-a", "256", zip.absolutePath)
                standardOutput = shaOut
            }
            checksum.writeText(shaOut.toString().trim().split(" ").first())
        }
        logger.lifecycle("Checksum: ${checksum.readText()}")
    }
}

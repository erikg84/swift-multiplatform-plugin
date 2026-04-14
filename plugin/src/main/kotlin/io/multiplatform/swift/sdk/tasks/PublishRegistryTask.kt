package io.multiplatform.swift.sdk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Publishes a source archive to any SE-0292 compliant Swift Package Registry.
 * Works with Gitea, Artifactory, or any server implementing the spec.
 */
abstract class PublishRegistryTask : DefaultTask() {
    @get:Input abstract val registryUrl: Property<String>
    @get:Input abstract val token: Property<String>
    @get:Input abstract val scope: Property<String>
    @get:Input abstract val packageName: Property<String>
    @get:Input abstract val version: Property<String>
    @get:Input abstract val frameworkName: Property<String>
    @get:Input abstract val xcframeworkUrl: Property<String>
    @get:Input abstract val xcframeworkChecksum: Property<String>
    @get:Input abstract val minimumDeployment: Property<String>
    @get:Input abstract val authorName: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Publishes source archive to Swift Package Registry"
        group = "swift publishing"
        authorName.convention("Swift Multiplatform SDK")
    }

    @TaskAction
    fun publish() {
        val tempDir = temporaryDir
        val ver = version.get()
        val name = packageName.get()
        val framework = frameworkName.get()
        val iosVer = minimumDeployment.get().substringBefore(".")

        // Create source archive with single top-level directory
        val archiveDir = File(tempDir, "$name-$ver").also { it.mkdirs() }

        File(archiveDir, "Package.swift").writeText(buildString {
            appendLine("// swift-tools-version: 5.9")
            appendLine("import PackageDescription")
            appendLine("let package = Package(")
            appendLine("    name: \"$framework\",")
            appendLine("    platforms: [.iOS(.v$iosVer), .macOS(.v12)],")
            appendLine("    products: [")
            appendLine("        .library(name: \"$framework\", targets: [\"$framework\"]),")
            appendLine("    ],")
            appendLine("    targets: [")
            appendLine("        .binaryTarget(")
            appendLine("            name: \"$framework\",")
            appendLine("            url: \"${xcframeworkUrl.get()}\",")
            appendLine("            checksum: \"${xcframeworkChecksum.get()}\"")
            appendLine("        ),")
            appendLine("    ]")
            appendLine(")")
        })

        val zipFile = File(tempDir, "$name-$ver.zip")
        execOperations.exec {
            commandLine("zip", "-qry", zipFile.absolutePath, archiveDir.name)
            workingDir(tempDir)
        }

        // Build metadata
        val author = authorName.get()
        val givenName = author.substringBefore(" ")
        val familyName = author.substringAfter(" ", "")
        val metadata = """{"@context":["http://schema.org/"],"@type":"SoftwareSourceCode","name":"$name","version":"$ver","description":"$framework for iOS","author":{"@type":"Person","givenName":"$givenName","familyName":"$familyName","name":"$author"},"programmingLanguage":{"@type":"ComputerLanguage","name":"Swift","url":"https://swift.org"}}"""

        val url = "${registryUrl.get()}/api/packages/${scope.get()}/swift/${scope.get()}/$name/$ver"
        val stdout = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(
                "curl", "-sS", "-o", "/dev/null", "-w", "%{http_code}",
                "-X", "PUT",
                "-H", "Accept: application/vnd.swift.registry.v1+json",
                "-H", "Authorization: token ${token.get()}",
                "-F", "metadata=$metadata;type=application/json",
                "-F", "source-archive=@${zipFile.absolutePath};type=application/zip",
                url
            )
            standardOutput = stdout
        }

        when (val code = stdout.toString().trim()) {
            "201" -> logger.lifecycle("Published ${scope.get()}.$name v$ver to Swift Package Registry")
            "409" -> logger.lifecycle("Version $ver already exists — skipping (idempotent)")
            else -> throw GradleException("Registry upload failed: HTTP $code for PUT $url")
        }
    }
}

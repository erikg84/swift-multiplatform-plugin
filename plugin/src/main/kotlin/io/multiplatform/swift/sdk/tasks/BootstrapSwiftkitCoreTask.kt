package io.multiplatform.swift.sdk.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class BootstrapSwiftkitCoreTask : DefaultTask() {
    @get:OutputDirectory abstract val mavenLocalMarker: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        description = "Bootstraps swiftkit-core into Maven Local (required by swift-java)"
        group = "swift"
        val home = System.getProperty("user.home")
        mavenLocalMarker.convention(
            project.layout.dir(project.provider { File("$home/.m2/repository/org/swift/swiftkit/swiftkit-core") })
        )
    }

    @TaskAction
    fun bootstrap() {
        val marker = mavenLocalMarker.get().asFile
        if (marker.exists() && marker.listFiles()?.isNotEmpty() == true) {
            logger.lifecycle("swiftkit-core already in Maven Local — skipping bootstrap")
            return
        }

        val checkoutsDir = File(project.projectDir, ".build/checkouts")
        val swiftJavaDir = checkoutsDir.listFiles()
            ?.filter { it.isDirectory && it.name == "swift-java" && File(it, "gradlew").exists() }
            ?.firstOrNull()
            ?: throw GradleException(
                "swift-java checkout with gradlew not found under $checkoutsDir\n" +
                "Run './gradlew swiftResolve' first."
            )

        logger.lifecycle("Bootstrapping swiftkit-core from $swiftJavaDir")

        val javaMajor = System.getProperty("java.version").split(".").first().toIntOrNull() ?: 17
        val filesToPatch = listOf(
            File(swiftJavaDir, "BuildLogic/src/main/kotlin/build-logic.java-common-conventions.gradle.kts"),
            File(swiftJavaDir, "SwiftKitCore/build.gradle.kts"),
        )
        for (f in filesToPatch) {
            if (f.exists()) {
                val content = f.readText()
                val patched = content.replace(
                    Regex("""JavaLanguageVersion\.of\(\d+\)"""),
                    "JavaLanguageVersion.of($javaMajor)"
                )
                if (patched != content) {
                    f.writeText(patched)
                    logger.lifecycle("  Patched ${f.name}: JavaLanguageVersion → $javaMajor")
                }
            }
        }

        File(swiftJavaDir, "BuildLogic/build").deleteRecursively()
        File(swiftJavaDir, ".gradle").deleteRecursively()

        execOperations.exec {
            commandLine(
                "${swiftJavaDir.absolutePath}/gradlew",
                ":SwiftKitCore:publishToMavenLocal",
                "-PskipSamples=true", "--no-daemon"
            )
            workingDir(swiftJavaDir)
        }
        logger.lifecycle("swiftkit-core bootstrapped to Maven Local")
    }
}

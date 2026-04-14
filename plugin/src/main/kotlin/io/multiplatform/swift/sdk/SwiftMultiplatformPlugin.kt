package io.multiplatform.swift.sdk

import com.android.build.gradle.LibraryExtension
import io.multiplatform.swift.sdk.tasks.*
import io.multiplatform.swift.sdk.util.SwiftToolchain
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.io.File

class SwiftMultiplatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("swiftMultiplatform", SwiftMultiplatformExtension::class.java)

        project.plugins.apply("com.android.library")

        // Android config must be set eagerly (AGP reads compileSdk during configuration)
        configureAndroidDefaults(project, ext)

        project.afterEvaluate {
            validate(project, ext)
            wireAndroidSourceSets(project, ext)
            registerAndroidTasks(project, ext)
            registerIosTasks(project, ext)
            registerPublishingTasks(project, ext)
            registerLifecycleTasks(project, ext)
        }
    }

    private fun validate(project: Project, ext: SwiftMultiplatformExtension) {
        if (!ext.moduleName.isPresent) throw GradleException(
            "swiftMultiplatform.moduleName is required.\n\n" +
            "Add to your build.gradle.kts:\n  swiftMultiplatform { moduleName(\"YourModuleName\") }"
        )
        if (!ext.sourcesDir.isPresent) throw GradleException(
            "swiftMultiplatform.sources is required.\n\n" +
            "Set the path to your Swift sources:\n  swiftMultiplatform { sources(\"Sources/YourModule\") }"
        )
        if (!ext.version.isPresent) throw GradleException(
            "swiftMultiplatform.version is required.\n\n" +
            "Set it directly or from gradle.properties:\n" +
            "  swiftMultiplatform { version(\"1.0.0\") }\n" +
            "  swiftMultiplatform { version(providers.gradleProperty(\"VERSION\")) }"
        )
        val sourcesDir = File(project.projectDir, ext.sourcesDir.get())
        if (!sourcesDir.exists()) throw GradleException(
            "Sources directory not found: ${sourcesDir.absolutePath}\n" +
            "Ensure swiftMultiplatform.sources points to an existing directory."
        )
        if (!File(project.projectDir, "Package.swift").exists()) throw GradleException(
            "Package.swift not found in ${project.projectDir}\n" +
            "The plugin requires a Package.swift for dependency management."
        )
    }

    private fun configureAndroidDefaults(project: Project, ext: SwiftMultiplatformExtension) {
        val android = project.extensions.getByType(LibraryExtension::class.java)
        val cfg = ext.android
        android.compileSdk = cfg.compileSdk.get()
        android.namespace = cfg.namespace.getOrElse("io.multiplatform.swift.sdk.generated")
        android.defaultConfig.minSdk = cfg.minSdk.get()
        android.compileOptions.sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        android.compileOptions.targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }

    private fun wireAndroidSourceSets(project: Project, ext: SwiftMultiplatformExtension) {
        val android = project.extensions.getByType(LibraryExtension::class.java)
        val jextractOutputDir = project.file(
            ".build/plugins/outputs/${project.name.lowercase()}/${ext.moduleName.get()}/destination/JExtractSwiftPlugin/src/generated/java"
        )
        val jniLibsDir = project.file("${project.layout.buildDirectory.get().asFile}/generated/jniLibs")
        val mainSourceSet = android.sourceSets.getByName("main")
        mainSourceSet.java.srcDir(jextractOutputDir)
        mainSourceSet.jniLibs.srcDir(jniLibsDir)
    }

    private fun registerAndroidTasks(project: Project, ext: SwiftMultiplatformExtension) {
        val cfg = ext.android

        val swiftResolve = project.tasks.register("swiftResolve", SwiftResolveTask::class.java)

        val bootstrapSwiftkitCore = project.tasks.register("bootstrapSwiftkitCore", BootstrapSwiftkitCoreTask::class.java)
        bootstrapSwiftkitCore.configure { dependsOn(swiftResolve) }

        val androidBuildTasks = cfg.abis.get().map { abi ->
            val triple = SwiftToolchain.tripleForAbi(abi, cfg.minSdk.get())
            project.tasks.register("buildSwiftAndroid_${abi.replace("-", "")}", SwiftBuildAndroidTask::class.java).also { tp ->
                tp.configure {
                    dependsOn(swiftResolve, bootstrapSwiftkitCore)
                    this.abi.set(abi)
                    swiftTriple.set(triple)
                    swiftVersion.set(cfg.swiftVersion)
                    outputDir.set(project.layout.projectDirectory.dir(".build/$triple/debug"))
                }
            }
        }

        val buildSwiftAndroid = project.tasks.register("buildSwiftAndroid")
        buildSwiftAndroid.configure {
            description = "Cross-compiles Swift for all Android ABIs"
            group = "swift"
            androidBuildTasks.forEach { dependsOn(it) }
        }

        val copyJniLibs = project.tasks.register("copyJniLibs", CopyJniLibsTask::class.java)
        copyJniLibs.configure {
            dependsOn(buildSwiftAndroid)
            abis.set(cfg.abis)
            minSdk.set(cfg.minSdk)
            sdkBundleName.set(cfg.swiftSdk)
            jniLibsDir.set(project.layout.buildDirectory.dir("generated/jniLibs"))
        }

        project.tasks.named("preBuild").configure { dependsOn(copyJniLibs) }
    }

    private fun registerIosTasks(project: Project, ext: SwiftMultiplatformExtension) {
        val iosConfig = ext.ios
        val frameworkName = iosConfig.frameworkName.getOrElse(ext.moduleName.get())
        val buildDir = project.layout.buildDirectory
        val version = ext.version.get()
        val useCustomScript = iosConfig.buildScript.isPresent

        // Generate iOS-only Package.swift (strips swift-java)
        val generateIosManifest = project.tasks.register("generateIosManifest", GenerateIosManifestTask::class.java)
        generateIosManifest.configure {
            originalManifest.set(project.layout.projectDirectory.file("Package.swift"))
            moduleName.set(ext.moduleName)
            iosPlatformVersion.set(iosConfig.minimumDeployment)
            stripDependencies.set(iosConfig.stripDependencies)
        }

        // Restore after build (success or failure)
        val restoreManifest = project.tasks.register("restoreManifest", RestoreManifestTask::class.java)

        if (!useCustomScript) {
            // Standard mode: xcodebuild archive
            val buildIosDevice = project.tasks.register("buildIosDevice", SwiftBuildIosTask::class.java)
            buildIosDevice.configure {
                dependsOn(generateIosManifest)
                finalizedBy(restoreManifest)
                scheme.set(ext.moduleName)
                destination.set("generic/platform=iOS")
                minimumDeployment.set(iosConfig.minimumDeployment)
                archivePath.set(buildDir.dir("archives/ios-device.xcarchive"))
            }

            val buildIosSimulator = project.tasks.register("buildIosSimulator", SwiftBuildIosTask::class.java)
            buildIosSimulator.configure {
                dependsOn(generateIosManifest)
                finalizedBy(restoreManifest)
                scheme.set(ext.moduleName)
                destination.set("generic/platform=iOS Simulator")
                minimumDeployment.set(iosConfig.minimumDeployment)
                archivePath.set(buildDir.dir("archives/ios-simulator.xcarchive"))
            }

            val deviceArchivePath = buildDir.map { it.dir("archives/ios-device.xcarchive").asFile.absolutePath }
            val simArchivePath = buildDir.map { it.dir("archives/ios-simulator.xcarchive").asFile.absolutePath }

            val assembleXCFramework = project.tasks.register("assembleXCFramework", AssembleXCFrameworkTask::class.java)
            assembleXCFramework.configure {
                dependsOn(buildIosDevice, buildIosSimulator)
                this.frameworkName.set(frameworkName)
                archivePaths.set(project.provider {
                    listOf(deviceArchivePath.get(), simArchivePath.get())
                })
                xcframeworkDir.set(buildDir.dir("xcframeworks/$frameworkName.xcframework"))
            }
        } else {
            // Custom script mode
            val assembleXCFramework = project.tasks.register("assembleXCFramework", AssembleXCFrameworkTask::class.java)
            assembleXCFramework.configure {
                this.frameworkName.set(frameworkName)
                this.buildScript.set(iosConfig.buildScript)
                xcframeworkDir.set(buildDir.dir("xcframeworks/$frameworkName.xcframework"))
            }
        }

        val zipXCFramework = project.tasks.register("zipXCFramework", ZipXCFrameworkTask::class.java)
        zipXCFramework.configure {
            dependsOn("assembleXCFramework")
            this.frameworkName.set(frameworkName)
            this.version.set(ext.version)
            xcframeworkDir.set(buildDir.dir("xcframeworks/$frameworkName.xcframework"))
            zipFile.set(buildDir.file("xcframeworks/$frameworkName-$version.xcframework.zip"))
            checksumFile.set(buildDir.file("xcframeworks/$frameworkName-$version.xcframework.sha256"))
        }
    }

    private fun registerPublishingTasks(project: Project, ext: SwiftMultiplatformExtension) {
        val pubConfig = ext.publishing
        val frameworkName = ext.ios.frameworkName.getOrElse(ext.moduleName.get())
        val version = ext.version.get()

        // Maven publication with proper transitive deps
        if (pubConfig.maven.url.isPresent) {
            project.plugins.apply("maven-publish")
            val publishingExt = project.extensions.getByType(PublishingExtension::class.java)

            val pub = publishingExt.publications.create("release", MavenPublication::class.java)
            pub.groupId = pubConfig.maven.groupId.get()
            pub.artifactId = pubConfig.maven.artifactId.get()
            pub.version = version

            // Wire Android component when available — includes transitive deps in POM
            var wired = false
            project.components.configureEach {
                if (name == "release" && !wired) {
                    wired = true
                    pub.from(this)
                }
            }

            // Fallback: if component never materialized, add AAR artifact + POM deps manually
            project.gradle.projectsEvaluated {
                if (!wired) {
                    val aarFile = project.file("build/outputs/aar/${project.name}-release.aar")
                    val artifact = pub.artifact(aarFile)
                    artifact.extension = "aar"
                    // Add swiftkit-core as explicit dependency in POM
                    pub.pom.withXml {
                        val deps = asNode().appendNode("dependencies")
                        val dep = deps.appendNode("dependency")
                        dep.appendNode("groupId", "org.swift.swiftkit")
                        dep.appendNode("artifactId", "swiftkit-core")
                        dep.appendNode("version", "1.0-SNAPSHOT")
                        dep.appendNode("scope", "runtime")
                    }
                }
            }

            val repoUrl = pubConfig.maven.url.get()
            publishingExt.repositories.apply { maven { setUrl(project.uri(repoUrl)) } }
        }

        val publishAndroid = project.tasks.register("publishAndroid")
        publishAndroid.configure {
            description = "Publishes Android AAR to Maven repository"
            group = "swift publishing"
            dependsOn("publishReleasePublicationToMavenRepository")
        }

        // iOS GCS publishing
        val publishIosGcs = project.tasks.register("publishIosGcs", PublishGcsTask::class.java)
        publishIosGcs.configure {
            dependsOn("zipXCFramework")
            zipFile.set(project.layout.buildDirectory.file("xcframeworks/$frameworkName-$version.xcframework.zip"))
            if (pubConfig.maven.url.isPresent) {
                val gsBase = SwiftToolchain.toGsUrl(pubConfig.maven.url.get()).removeSuffix("/")
                val group = pubConfig.maven.groupId.getOrElse("").replace(".", "/")
                val artifact = pubConfig.maven.artifactId.getOrElse(frameworkName.lowercase())
                gcsDestination.set("$gsBase/$group/$artifact-ios/$version/$artifact-ios-$version.zip")
            }
        }

        // Swift Registry publishing
        val registryUrl = pubConfig.swiftRegistry.url.orNull ?: System.getenv("REGISTRY_URL")
        val registryToken = pubConfig.swiftRegistry.token.orNull ?: System.getenv("REGISTRY_TOKEN")

        if (registryUrl != null && registryToken != null) {
            val publishRegistry = project.tasks.register("publishSwiftRegistry", PublishRegistryTask::class.java)
            publishRegistry.configure {
                dependsOn("zipXCFramework")
                this.registryUrl.set(registryUrl)
                this.token.set(registryToken)
                scope.set(pubConfig.swiftRegistry.scope.getOrElse(""))
                packageName.set(pubConfig.swiftRegistry.packageName.getOrElse(""))
                this.version.set(ext.version)
                this.frameworkName.set(frameworkName)
                minimumDeployment.set(ext.ios.minimumDeployment)
                authorName.set(pubConfig.swiftRegistry.authorName)

                if (pubConfig.maven.url.isPresent) {
                    val httpsBase = SwiftToolchain.toPublicUrl(pubConfig.maven.url.get()).removeSuffix("/")
                    val group = pubConfig.maven.groupId.getOrElse("").replace(".", "/")
                    val artifact = pubConfig.maven.artifactId.getOrElse(frameworkName.lowercase())
                    xcframeworkUrl.set("$httpsBase/$group/$artifact-ios/$version/$artifact-ios-$version.zip")
                }

                xcframeworkChecksum.set(project.provider {
                    val f = project.layout.buildDirectory
                        .file("xcframeworks/$frameworkName-$version.xcframework.sha256")
                        .get().asFile
                    if (f.exists()) f.readText().trim() else ""
                })
            }
        }
    }

    private fun registerLifecycleTasks(project: Project, ext: SwiftMultiplatformExtension) {
        project.tasks.register("buildAll").configure {
            description = "Builds both Android AAR and iOS XCFramework"
            group = "swift"
            dependsOn("assembleRelease", "assembleXCFramework")
        }

        project.tasks.register("swiftTest", SwiftTestTask::class.java)

        project.tasks.register("publishAll").configure {
            description = "Publishes all artifacts"
            group = "swift publishing"
            dependsOn("publishAndroid", "publishIosGcs")
            val hasRegistry = ext.publishing.swiftRegistry.url.isPresent || System.getenv("REGISTRY_URL") != null
            if (hasRegistry) dependsOn("publishSwiftRegistry")
        }
    }
}

package io.multiplatform.swift.sdk

import io.multiplatform.swift.sdk.util.PackageSwiftGenerator
import io.multiplatform.swift.sdk.util.SwiftToolchain
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SwiftMultiplatformPluginTest {

    @Test
    fun `ABI to triple mapping — arm64`() {
        assertEquals("aarch64-unknown-linux-android28", SwiftToolchain.tripleForAbi("arm64-v8a", 28))
    }

    @Test
    fun `ABI to triple mapping — x86_64`() {
        assertEquals("x86_64-unknown-linux-android28", SwiftToolchain.tripleForAbi("x86_64", 28))
    }

    @Test
    fun `ABI to triple mapping — armeabi-v7a`() {
        assertEquals("armv7-unknown-linux-android28", SwiftToolchain.tripleForAbi("armeabi-v7a", 28))
    }

    @Test
    fun `ABI mapping throws on unknown ABI`() {
        assertThrows(org.gradle.api.GradleException::class.java) {
            SwiftToolchain.tripleForAbi("mips", 28)
        }
    }

    @Test
    fun `runtime libs list is not empty and contains essentials`() {
        assertTrue(SwiftToolchain.RUNTIME_LIBS.contains("swiftCore"))
        assertTrue(SwiftToolchain.RUNTIME_LIBS.contains("Foundation"))
        assertTrue(SwiftToolchain.RUNTIME_LIBS.contains("dispatch"))
    }

    @Test
    fun `GCS URL normalization — gcs to gs`() {
        assertEquals("gs://bucket/path", SwiftToolchain.toGsUrl("gcs://bucket/path"))
    }

    @Test
    fun `GCS URL normalization — https to gs`() {
        assertEquals("gs://bucket/path", SwiftToolchain.toGsUrl("https://storage.googleapis.com/bucket/path"))
    }

    @Test
    fun `GCS URL normalization — gs passthrough`() {
        assertEquals("gs://bucket/path", SwiftToolchain.toGsUrl("gs://bucket/path"))
    }

    @Test
    fun `public URL from gcs`() {
        assertEquals(
            "https://storage.googleapis.com/bucket/path",
            SwiftToolchain.toPublicUrl("gcs://bucket/path")
        )
    }

    @Test
    fun `public URL from gs`() {
        assertEquals(
            "https://storage.googleapis.com/bucket/path",
            SwiftToolchain.toPublicUrl("gs://bucket/path")
        )
    }

    @Test
    fun `public URL passthrough for non-GCS`() {
        assertEquals(
            "https://my-artifactory.com/maven",
            SwiftToolchain.toPublicUrl("https://my-artifactory.com/maven")
        )
    }
}

class PackageSwiftGeneratorTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `generates iOS manifest without dependencies`() {
        val output = File(tempDir, "Package.swift")
        PackageSwiftGenerator.generate(output, "TestSDK", "15.0")

        val content = output.readText()
        assertTrue(content.contains("name: \"TestSDK\""))
        assertTrue(content.contains(".iOS(.v15)"))
        assertFalse(content.contains("swift-java"))
    }

    @Test
    fun `generates iOS manifest with dependencies`() {
        val output = File(tempDir, "Package.swift")
        PackageSwiftGenerator.generate(
            output, "TestSDK", "16.0",
            listOf("https://github.com/Swinject/Swinject.git" to "2.10.0")
        )

        val content = output.readText()
        assertTrue(content.contains("Swinject"))
        assertTrue(content.contains("2.10.0"))
        assertTrue(content.contains(".iOS(.v16)"))
    }

    @Test
    fun `extracts iOS dependencies and strips swift-java`() {
        val packageSwift = File(tempDir, "Package.swift")
        packageSwift.writeText("""
            .package(url: "https://github.com/Swinject/Swinject.git", from: "2.10.0"),
            .package(url: "https://github.com/swiftlang/swift-java.git", from: "0.1.2"),
        """)

        val deps = PackageSwiftGenerator.extractIosDependencies(packageSwift)
        assertEquals(1, deps.size)
        assertEquals("https://github.com/Swinject/Swinject.git", deps[0].first)
        assertEquals("2.10.0", deps[0].second)
    }

    @Test
    fun `strips custom dependencies`() {
        val packageSwift = File(tempDir, "Package.swift")
        packageSwift.writeText("""
            .package(url: "https://github.com/Swinject/Swinject.git", from: "2.10.0"),
            .package(url: "https://github.com/example/my-android-lib.git", from: "1.0.0"),
        """)

        val deps = PackageSwiftGenerator.extractIosDependencies(packageSwift, listOf("my-android-lib"))
        assertEquals(1, deps.size)
        assertEquals("https://github.com/Swinject/Swinject.git", deps[0].first)
    }
}

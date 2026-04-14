package io.multiplatform.swift.sdk.util

import org.gradle.api.GradleException
import java.io.File

object SwiftToolchain {

    private val ABI_TRIPLE_PREFIX = mapOf(
        "arm64-v8a" to "aarch64-unknown-linux-android",
        "armeabi-v7a" to "armv7-unknown-linux-android",
        "x86_64" to "x86_64-unknown-linux-android",
    )

    val ABI_SDK_LIB_DIR = mapOf(
        "arm64-v8a" to "swift-aarch64",
        "armeabi-v7a" to "swift-armv7",
        "x86_64" to "swift-x86_64",
    )

    val ABI_NDK_DIR = mapOf(
        "arm64-v8a" to "aarch64-linux-android",
        "armeabi-v7a" to "arm-linux-android",
        "x86_64" to "x86_64-linux-android",
    )

    val RUNTIME_LIBS = listOf(
        "swiftCore", "swift_Concurrency", "swift_StringProcessing",
        "swift_RegexParser", "swift_Builtin_float", "swift_math",
        "swiftAndroid", "dispatch", "BlocksRuntime",
        "swiftSwiftOnoneSupport", "swiftDispatch",
        "Foundation", "FoundationEssentials", "FoundationInternationalization",
        "_FoundationICU", "swiftSynchronization",
    )

    fun tripleForAbi(abi: String, minSdk: Int): String {
        val prefix = ABI_TRIPLE_PREFIX[abi]
            ?: throw GradleException("Unknown ABI: $abi. Supported: ${ABI_TRIPLE_PREFIX.keys}")
        return "$prefix$minSdk"
    }

    fun findSwiftOrNull(): String? {
        val candidates = listOf("/opt/homebrew/bin/swift", "/usr/bin/swift")
        return candidates.firstOrNull { File(it).exists() }
    }

    fun findSwift(): String = findSwiftOrNull()
        ?: throw GradleException(
            "Swift compiler not found.\n\n" +
            "Install Swift from https://swift.org/install\n" +
            "Searched: /opt/homebrew/bin/swift, /usr/bin/swift"
        )

    fun findSwiftlyOrNull(): String? {
        val home = System.getProperty("user.home")
        val candidates = listOf(
            "$home/.swiftly/bin/swiftly",
            "$home/.local/share/swiftly/bin/swiftly",
            "$home/.local/bin/swiftly",
            "/opt/homebrew/bin/swiftly",
            "/usr/local/bin/swiftly",
        )
        return candidates.firstOrNull { File(it).exists() }
    }

    fun findSwiftly(): String = findSwiftlyOrNull()
        ?: throw GradleException(
            "swiftly not found.\n\n" +
            "Install from https://swift.org/install\n" +
            "Or: brew install swiftly"
        )

    fun findSwiftSdkPathOrNull(): String? {
        val home = System.getProperty("user.home")
        val candidates = listOf(
            "$home/Library/org.swift.swiftpm/swift-sdks/",
            "$home/.config/swiftpm/swift-sdks/",
            "$home/.swiftpm/swift-sdks/",
        )
        return candidates.firstOrNull { File(it).exists() }
    }

    fun findSwiftSdkPath(): String = findSwiftSdkPathOrNull()
        ?: throw GradleException(
            "Swift SDK for Android not found.\n\n" +
            "Install with:\n  swift sdk install <swift-android-sdk-url>\n\n" +
            "See: https://www.swift.org/documentation/articles/swift-sdk-for-android-getting-started.html"
        )

    fun findXcodebuild(): String {
        val path = "/usr/bin/xcodebuild"
        if (!File(path).exists()) {
            throw GradleException(
                "xcodebuild not found.\n\n" +
                "Install Xcode from the Mac App Store or run:\n  xcode-select --install"
            )
        }
        return path
    }

    /** Converts any GCS URL format to gs:// for gcloud CLI. */
    fun toGsUrl(url: String): String = url
        .replace("gcs://", "gs://")
        .replace(Regex("https://storage\\.googleapis\\.com/"), "gs://")

    /** Converts any GCS URL format to public HTTPS for consumers. */
    fun toPublicUrl(url: String): String {
        val normalized = toGsUrl(url)
        return if (normalized.startsWith("gs://")) {
            normalized.replace("gs://", "https://storage.googleapis.com/")
        } else url
    }
}

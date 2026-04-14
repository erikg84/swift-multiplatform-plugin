# 08 — Testing

## Unit Tests (Gradle TestKit — run on any machine)

| Test | What it verifies |
|------|-----------------|
| `PluginAppliesTest` | Plugin applies without error, registers all tasks |
| `DslParsingTest` | Extension DSL parses all config values correctly |
| `ValidationTest` | Missing moduleName/sources/version throw clear errors |
| `AbiMappingTest` | ABI to triple mapping for all supported ABIs |
| `ToolchainDiscoveryTest` | findSwiftOrNull, findSwiftlyOrNull return expected results |
| `PackageSwiftGeneratorTest` | iOS manifest generated correctly, swift-java stripped |
| `GcsUrlNormalizationTest` | gcs:// → gs://, gcs:// → https:// conversions |

## Integration Tests (require Swift + Xcode — Mac runner only)

| Test | What it verifies |
|------|-----------------|
| `AndroidCrossCompileTest` | Swift builds for arm64-v8a, .so files produced |
| `XCFrameworkBuildTest` | XCFramework assembled with device + simulator slices |
| `ZipChecksumTest` | Zip + SHA-256 checksum computed correctly |
| `ExampleProjectBuildTest` | example/ project builds with `buildAll` |

## CI Configuration

Unit tests run on every PR (ubuntu-latest). Integration tests run on Mac runner (self-hosted or macos-latest).

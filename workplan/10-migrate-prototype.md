# 10 — Migrate Prototype Code

## Source: `erikg84/swift-multiplatform-gradle-plugin` (v0.1.x)

## Files to Port (with modifications)

| Prototype File | New Location | Changes |
|---------------|-------------|---------|
| `SwiftMultiplatformPlugin.kt` | Same name, new package | Split configureAndroid into eager/deferred, fix publication wiring |
| `SwiftMultiplatformExtension.kt` | Same | Add convenience methods |
| `AndroidConfig.kt` | `config/AndroidConfig.kt` | Add convenience methods, stripDependencies |
| `IosConfig.kt` | `config/IosConfig.kt` | Add convenience methods, stripDependencies, stripPlugins |
| `PublishingConfig.kt` | `config/PublishingConfig.kt` | Rename GiteaConfig → SwiftRegistryConfig, add convenience methods |
| `SwiftToolchain.kt` | `util/SwiftToolchain.kt` | Add OrNull variants, better error messages |
| `SwiftResolveTask.kt` | `tasks/` | Add validation |
| `SwiftBuildAndroidTask.kt` | `tasks/` | Add validation |
| `CopyJniLibsTask.kt` | `tasks/` | Port as-is |
| `BootstrapSwiftkitCoreTask.kt` | `tasks/` | Port as-is |
| `SwiftBuildIosTask.kt` | `tasks/` | Port, wire with GenerateIosManifestTask |
| `AssembleXCFrameworkTask.kt` | `tasks/` | Port, add custom script support |
| `ZipXCFrameworkTask.kt` | `tasks/` | Port as-is |
| `PublishGcsTask.kt` | `tasks/` | Port, fix URL normalization |
| `PublishGiteaTask.kt` | `tasks/PublishRegistryTask.kt` | Rename, generalize (not Gitea-specific) |
| `SwiftTestTask.kt` | `tasks/` | Port as-is |

## New Files (not in prototype)

| File | Purpose |
|------|---------|
| `tasks/GenerateIosManifestTask.kt` | Generates temp iOS Package.swift |
| `tasks/RestoreManifestTask.kt` | Restores original Package.swift after iOS build |
| `util/PackageSwiftGenerator.kt` | Template-based iOS manifest generation |
| `config/MavenConfig.kt` | Extracted from PublishingConfig |
| `config/SwiftRegistryConfig.kt` | Renamed from GiteaConfig, generalized |

## Key Fixes During Migration

1. **Publication wiring**: `from(component)` with single-shot guard OR explicit POM manipulation
2. **Android config timing**: compileSdk set eagerly, source sets wired in afterEvaluate
3. **Repository naming**: avoid setting name after adding to container
4. **GCS URL normalization**: `gcs://` → `gs://` for gcloud, `https://` for consumers
5. **Gitea env var fallback**: check both gradle properties and env vars
6. **DSL convenience methods**: every property gets a setter method
7. **iOS manifest**: GenerateIosManifestTask replaces shell script swap

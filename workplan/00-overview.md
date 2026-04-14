# Workplan Overview — Swift Multiplatform Gradle Plugin (Generalized)

## Source Material
The prototype at `erikg84/swift-multiplatform-gradle-plugin` (v0.1.x) is the starting point. This workplan covers what needs to change to make it production-quality and publicly distributable.

## Workplan Files

| File | Description |
|------|-------------|
| `01-project-scaffold.md` | New repo structure, package rename, Gradle Plugin Portal setup |
| `02-fix-maven-publication.md` | Solve AAR POM missing transitive deps — the #1 blocker |
| `03-ios-manifest-generation.md` | Replace shell script hack with in-plugin Package.swift generation |
| `04-dsl-cleanup.md` | Convenience methods, generalized config, no vendor lock-in |
| `05-validation-and-errors.md` | Fail-fast validation with actionable error messages |
| `06-example-project.md` | Working example SDK that demonstrates the plugin |
| `07-documentation.md` | Getting started guide, config reference, CI examples |
| `08-testing.md` | Gradle TestKit unit tests + integration tests |
| `09-gradle-plugin-portal.md` | Publishing to Gradle Plugin Portal (not GitHub Packages) |
| `10-migrate-prototype.md` | Port prototype code, adapt to new package/structure |

## Priority Order

1. **02 (Maven publication)** — dealbreaker for adoption, fix first
2. **03 (iOS manifest)** — second biggest pain point
3. **04 (DSL cleanup)** — usability
4. **01 (scaffold)** — package rename, structure
5. **10 (migrate)** — port prototype code
6. **05 (validation)** — polish
7. **06 (example)** — proves it works
8. **08 (testing)** — confidence
9. **07 (docs)** — adoption
10. **09 (Plugin Portal)** — distribution

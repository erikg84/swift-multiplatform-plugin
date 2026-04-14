# 09 — Gradle Plugin Portal Publishing

## Why Plugin Portal over GitHub Packages
- No auth required for consumers — just `plugins { id(...) version ... }`
- Public discovery — searchable on plugins.gradle.org
- Standard Gradle resolution — no extra repository config

## Setup

1. Create account at https://plugins.gradle.org
2. Get API key + secret
3. Add to `~/.gradle/gradle.properties`:
   ```
   gradle.publish.key=your_key
   gradle.publish.secret=your_secret
   ```
4. Plugin `build.gradle.kts` already has `com.gradle.plugin-publish`

## Publishing

```bash
# Publish to Plugin Portal
./gradlew :plugin:publishPlugins

# Verify at:
# https://plugins.gradle.org/plugin/io.github.erikg84.swift-multiplatform
```

## CI Workflow

```yaml
- name: Publish to Gradle Plugin Portal
  run: ./gradlew :plugin:publishPlugins
  env:
    GRADLE_PUBLISH_KEY: ${{ secrets.GRADLE_PUBLISH_KEY }}
    GRADLE_PUBLISH_SECRET: ${{ secrets.GRADLE_PUBLISH_SECRET }}
```

## Consumer Experience (after Portal publish)

```kotlin
// No repository configuration needed — just this:
plugins {
    id("io.github.erikg84.swift-multiplatform") version "1.0.0"
}
```

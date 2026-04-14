# 02 — Fix Maven Publication (Priority #1)

## Problem
The prototype uses `pub.artifact(aarFile)` which publishes the AAR but generates an empty POM — no transitive dependencies. Consumers must manually add `swiftkit-core` and any future deps.

## Root Cause
`from(components["release"])` fails when called from a plugin's `afterEvaluate` because the Android `release` SoftwareComponent is created lazily by AGP and isn't available at that point.

## Solution: Deferred Publication Wiring

Use AGP's `afterEvaluate` nesting or `components.configureEach` with proper single-shot guard:

```kotlin
private fun configurePublishing(project: Project, ext: SwiftMultiplatformExtension) {
    project.plugins.apply("maven-publish")
    val publishingExt = project.extensions.getByType(PublishingExtension::class.java)

    // Create publication shell
    val pub = publishingExt.publications.create("release", MavenPublication::class.java)
    pub.groupId = ext.publishing.maven.groupId.get()
    pub.artifactId = ext.publishing.maven.artifactId.get()
    pub.version = ext.version.get()

    // Wire the Android component when it becomes available
    var wired = false
    project.components.configureEach {
        if (name == "release" && !wired) {
            wired = true
            pub.from(this)
        }
    }

    // Configure repository
    val repoUrl = ext.publishing.maven.url.get()
    publishingExt.repositories.apply {
        maven { setUrl(project.uri(repoUrl)) }
    }
}
```

## What the POM Should Look Like

```xml
<project>
  <groupId>com.example</groupId>
  <artifactId>my-swift-sdk</artifactId>
  <version>1.0.0</version>
  <packaging>aar</packaging>
  <dependencies>
    <dependency>
      <groupId>org.swift.swiftkit</groupId>
      <artifactId>swiftkit-core</artifactId>
      <version>1.0-SNAPSHOT</version>
      <scope>runtime</scope>
    </dependency>
  </dependencies>
</project>
```

## Verification

```bash
./gradlew publishAll

# Check POM has dependencies
curl -s https://storage.googleapis.com/.../my-swift-sdk-1.0.0.pom | grep "swiftkit-core"
# Should return the dependency block
```

## Alternative: POM Manipulation

If `from(component)` timing remains unreliable across Gradle versions, fall back to explicit POM manipulation:

```kotlin
pub.pom.withXml {
    val deps = asNode().appendNode("dependencies")
    val dep = deps.appendNode("dependency")
    dep.appendNode("groupId", "org.swift.swiftkit")
    dep.appendNode("artifactId", "swiftkit-core")
    dep.appendNode("version", "1.0-SNAPSHOT")
    dep.appendNode("scope", "runtime")
}
```

This is less elegant but 100% reliable regardless of component timing.

## Testing

1. Publish locally: `./gradlew publishToMavenLocal`
2. Inspect POM at `~/.m2/repository/.../my-swift-sdk-1.0.0.pom`
3. Verify it contains `<dependency>` for swiftkit-core
4. Consumer project resolves transitively without explicit dep

# Config Doctor — project overview

_Last audited: 2026-11-17. Repository baseline inspected: `develop` at `300224550bf76ac0c63d75654fa8f367a58db79b`._

## Mission

Config Doctor is an IntelliJ IDEA plugin written in Kotlin. The original implementation detects a narrow class of Spring Boot YAML mistakes: a property path that appears to contain an accidental extra nesting segment even though the YAML is syntactically valid.

The recommended product direction is to evolve this foundation into a static, pre-run Spring configuration review tool for projects with multiple environment profiles. Metadata-driven completion and documentation are supporting authoring features, not the primary differentiator. The intended audience is users of IntelliJ IDEA without an Ultimate subscription; current product positioning is a proposal and requires Product Owner approval. See [Product direction](PRODUCT_DIRECTION.md).

## Repository snapshot

- Repository: `AlekseiBriukhachev/config-doctor`
- Default branch: `develop`
- Last code push observed: 2026-09-26
- Last commit observed on `develop`: `3002245`
- `develop` was 12 commits ahead of `master` at the audit snapshot.
- PRs #1–9 and #11 are merged. PR #10 was closed without being merged and contains no effective diff in the current GitHub response.
- Latest GitHub release: `1.1.0`
- `gradle.properties` declares plugin version `1.2.0`; a published 1.2.0 release was not observed.
- No GitHub Actions workflows or workflow runs were present in the inspected repository.
- GitHub reports no repository license. A `LICENSE` file is absent.
- No successful current build/test run was available during the audit. Treat build health as unverified.

## What exists today

### Implemented capabilities

1. **Plugin skeleton**
   - IntelliJ Platform Gradle Plugin 2.x.
   - Kotlin/JVM plugin implementation.
   - YAML plugin dependency declared in `plugin.xml`.
   - Startup activity emits a plugin-loaded log message.

2. **YAML model extraction**
   - `YamlPropertyPaths` walks YAML PSI and creates dotted paths from nested mappings.
   - It iterates YAML documents and extracts leaf keys; sequence contents are treated as one leaf value and mappings inside sequence items are not traversed. Dotted path serialization also needs to preserve quoted/map-key segment identity.
   - `ConfigFileDiscovery` searches YAML files by type and filters names through `ConfigFileName`.

3. **Configuration-file naming**
   - Recognizes the conventional `application.yml`, `application.yaml`, profile suffixes such as `application-local.yml`, and a custom dot-qualified naming convention.
   - It does not implement the complete Spring Boot ConfigData file-discovery model, arbitrary `spring.config.name`, imports or runtime property-source resolution.

4. **Suspicious path inspection**
   - `SuspiciousPathDetector` removes one path segment at a time and checks whether the resulting path exists in another discovered project property or in Spring metadata.
   - It also contains a profile-key mismatch heuristic based on simplistic singularization. This behavior is considered unsafe and is tracked by issue #15; do not extend it.

5. **Quick Fixes**
   - Collapse a redundant wrapper only when the PSI structure appears unambiguous.
   - Rename a profile key based on the heuristic described above.
   - Show Spring property metadata in a dialog.
   - Quick Fix preservation and stale-PSI behavior need broader verification.

6. **Spring metadata**
   - Reads `spring-configuration-metadata.json` property records.
   - Extracts property names, type, description, default value and a limited deprecation representation.
   - Contains a `FileBasedIndexExtension` and a project-level metadata service with a fallback scan.
   - The custom index is not declared in `plugin.xml` at the audit baseline. Metadata groups, hints, providers, ignored items and repeated metadata records are not modeled fully.

7. **Automated tests**
   - Tests exist for path extraction, file discovery, detector behavior, inspection highlighting, Quick Fixes, filename parsing and a realistic multi-file fixture.
   - Metadata index registration, cache invalidation, repeated findings across separate profile files, richer YAML constructs and large-project performance need coverage.

## Technical stack observed

| Component | Current repository value |
|---|---|
| Language | Kotlin 2.1.21 |
| JVM toolchain | 17 |
| Gradle wrapper | 9.0.0 |
| IntelliJ Platform Gradle Plugin | 2.18.1 |
| Target platform | Historical IntelliJ IDEA Community platform build (`IC`) 2023.3 |
| `pluginSinceBuild` | 233 |
| `pluginUntilBuild` | intentionally unset |
| YAML integration | bundled `org.jetbrains.plugins.yaml` |
| Metadata parser | Gson 2.10.1 |

These are repository settings at the audited commit, not a recommendation that they remain unchanged indefinitely. Compatibility must be verified against the selected minimum and current IDE builds before changing the range.

## Current boundaries and non-goals

- The plugin is not a full Spring Boot ConfigData or Environment resolver.
- It does not simulate the complete property-source precedence or the runtime application environment.
- It is not a generic YAML linter, formatter, schema engine or Kubernetes editor.
- It does not use AI or send project configuration to external services.
- It must not assume that missing Spring metadata makes a custom property invalid.
- It must not silently change YAML unless the intended transformation is proven safe.

## Key source areas

| Area | Location |
|---|---|
| Plugin descriptor | `src/main/resources/META-INF/plugin.xml` |
| YAML property paths | `plugin/yaml/YamlPropertyPaths.kt` |
| File discovery | `plugin/yaml/ConfigFileDiscovery.kt` |
| Filename model | `plugin/model/ConfigFileName.kt` |
| Analysis engine | `plugin/analysis/SuspiciousPathDetector.kt` |
| Inspection | `plugin/inspection/SuspiciousConfigPathInspection.kt` |
| Quick Fixes | `plugin/inspection/*Fix.kt` |
| Metadata loader/index/service | `plugin/metadata/*` |
| Automated tests | `src/test/kotlin/com/aleksei/configdoctor/plugin` |

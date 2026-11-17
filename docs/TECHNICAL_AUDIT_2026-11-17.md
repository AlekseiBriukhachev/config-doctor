# Technical audit — 2026-11-17

## Scope and confidence

Reviewed the public repository's `develop` tree, recent commit history, branch list, releases, pull requests, issue list, build files, plugin descriptor, production Kotlin sources and test sources.

Baseline: `develop` commit `300224550bf76ac0c63d75654fa8f367a58db79b` (2026-09-26).

This was a static code/configuration review. A clean local build, tests, Plugin Verifier and manual sandbox session were not executed in this audit. Findings below distinguish confirmed repository facts from risks that need reproduction.

## Repository and release findings

- `develop` is the default branch; at the audit baseline it is 12 commits ahead of `master`. Both branches were reported unprotected.
- PRs #1–9 and #11 are merged. PR #10 is closed without merge and has no effective diff in the current GitHub response.
- The latest GitHub release is 1.1.0, while Gradle declares plugin version 1.2.0.
- The code was last pushed on 2026-09-26. Repository metadata was later updated by issue activity, not by a new code push.
- No `.github/workflows` files or GitHub Actions runs were found.
- `gradlew` is tracked as mode `100644`, although README instructions use `./gradlew test`; a normal Unix checkout will not execute it directly.
- No `LICENSE` file or license metadata was found.
- `pluginSinceBuild=233` and IntelliJ platform dependency `2023.3` are configured; `pluginUntilBuild` is unset. Compatibility with current IDE releases has not been verified in this audit.

## Product/market note

IntelliJ IDEA has used a unified distribution since 2025.3: the core remains free, while advanced Spring tooling requires an Ultimate subscription. The product audience should therefore be described as users without an Ultimate subscription, not as users of a current separate Community Edition.

The competing plugin set is stronger than a generic YAML/Spring assistant proposition suggests. Spring Boot Assistant advertises property and value completion, documentation, undefined/deprecated/invalid-value inspections and navigation. Spring Explyt advertises broad Spring inspections, runtime context awareness and runtime property-value lenses. The proposed differentiation is static, pre-run cross-profile configuration review; this remains a hypothesis requiring user validation.

References: [unified IntelliJ IDEA](https://www.jetbrains.com/help/idea/intellij-idea-single-distribution.html), [Spring Boot Assistant source README](https://github.com/flikas/idea-spring-boot-assistant), [Spring Explyt listing/statistics](https://www.pluginstats.com/plugins/jetbrains/28675-spring-explyt).

## Findings

### A-01 — project-wide analysis is scoped to a visitor, not a project snapshot

**Location:** `SuspiciousConfigPathInspection.kt`, `buildVisitor()` and `collectAllConfigProperties()`.

The visitor-local lazy `findings` value calls metadata lookup and collects every recognized application YAML file on first relevant YAML key visit. The inspection is registered for YAML and does not first reject unrelated YAML files. If the platform creates a visitor per file, this repeats project-wide work for each YAML file.

**Impact:** possible excessive latency and repeated index/file work in large projects.

**Remediation:** return a no-op visitor for files outside the explicit Spring configuration scope; cache immutable analysis snapshots at project level with correct modification tracking; benchmark a project with many YAML files. See #12.

### A-02 — custom metadata index is not registered

**Location:** `SpringConfigurationMetadataIndex.kt`, `plugin.xml`.

The implementation extends `FileBasedIndexExtension` and the service queries its ID. The plugin descriptor at the audited commit declares the YAML inspection but no `fileBasedIndex` extension. IntelliJ Platform documentation specifies that a file-based index implementation must be registered in `com.intellij.fileBasedIndex`.

The service catches lookup failures and falls back to scanning, which can mask this defect.

**Impact:** intended indexed lookup is not reliably active; fallback scans may be used instead.

**Remediation:** add the supported extension-point declaration, then test index discovery and create/change/delete invalidation in a sandbox. See #13 and #18.

### A-03 — profile mismatch heuristic infers too much from key spelling

**Location:** `SuspiciousPathDetector.kt`, `findProfileOverrideMismatch()` and `singularize()`.

The detector treats pluralized leaves such as `server.ports` vs `server.port` as evidence of a mismatch and the inspection offers a rename Quick Fix. A custom key can be intentional; suffix normalization alone cannot prove the correct key.

**Impact:** false-positive diagnostics and potentially destructive automatic renaming. This violates the product's conservative-evidence principle.

**Remediation:** remove pluralization-only evidence and rename suggestions. Only report a profile override problem when a reliable source/metadata relationship establishes the target. Add negative custom-property tests. See #15.

### A-04 — findings can be collapsed across distinct source files

**Location:** `SuspiciousPathDetector.kt`, `properties.associateBy { it.path }` and `distinctBy { actual.path to relatedExpected.path to evidenceKind }`.

Both maps use only the logical property path. Findings for the same path in different profile files can be merged, losing source identity and potentially leaving one file unhighlighted.

**Impact:** missed diagnostics when the same broken path appears in multiple configuration files.

**Remediation:** preserve source-file/PSI identity in analysis and deduplicate only duplicate reports for the same source property. Add a two-profile regression test. See #16.

### A-05 — Quick Fixes need stronger PSI revalidation and preservation tests

**Location:** `ReplaceProfileMismatchKeyFix.kt` and `CollapseDuplicatedSegmentFix.kt`.

The profile rename fix reconstructs a new YAML key/value from the key and `value.text`, without verifying that the current PSI still matches the original finding. The collapse fix does recheck a structural condition but reconstructs text and indentation manually.

**Impact:** stale fixes may act on changed content; comments, block scalar style or other formatting may be altered. Actual loss was not reproduced during this static audit.

**Remediation:** revalidate immediately before mutation, prefer PSI-preserving changes, abort on ambiguity and add whole-file preservation tests. See #17.

### A-06 — metadata format is incomplete for authoring use cases

**Location:** `SpringConfigurationMetadata.kt`, `SpringConfigurationMetadataIndex.kt`, `SpringConfigurationMetadataService.kt`.

The model currently reads a subset of property data. It does not preserve metadata groups, value hints, providers, ignored properties, source type/method and all repeated metadata records. The index uses tab-delimited strings, which is a fragile serialization boundary for free text.

Spring Boot's documented format includes these additional sections and semantics.

**Impact:** current metadata is insufficient for high-quality key/value completion, documentation and source navigation.

**Remediation:** build a typed model, define merge/provenance policy, version the persisted index representation and test the indexed and fallback paths. See #22.

### A-07 — profile and YAML document semantics are only partially modeled

**Location:** `ConfigFileName.kt`, `ConfigFileDiscovery.kt`, `YamlPropertyPaths.kt`.

Discovery uses conventional `application*.yml/yaml` filenames and does not resolve arbitrary `spring.config.name`, locations or imports. YAML documents are iterated, but all leaf properties are associated with the containing file's single profile identity; document activation metadata is not interpreted.

**Impact:** the detector must not claim to calculate the complete active/effective Spring configuration. Complex ConfigData cases can require different semantics than the current static path comparison.

**Remediation:** document supported cases and progressively add profile/multi-document/import behavior with explicit tests. See #26.

### A-08 — YAML extraction does not preserve all collection/key semantics

**Location:** `YamlPropertyPaths.kt`, `PropertyPath.kt`.

The extractor recurses through `YAMLMapping` values but treats sequence values as one leaf. Mappings inside list items are not traversed. Property paths are flattened to dotted strings, which can make a quoted map key containing a dot indistinguishable from multiple nested mapping segments.

**Impact:** common list-of-object and map configurations may be absent from analysis or collide with different logical paths. This is a correctness blocker for a configuration matrix.

**Remediation:** introduce a canonical path representation that preserves segment identity and explicitly traverse supported sequence/map structures. Do not infer a Spring binder path when semantics are ambiguous. See #29.

### A-08 — no automated repository quality gate

**Location:** `.github/`, Gradle wrapper file mode.

There are no workflow files; the latest commit has no reported status checks. The Unix wrapper permission is non-executable.

**Impact:** no repository-level automation protects merges from compilation/test/plugin-verification regressions.

**Remediation:** fix the file mode and add PR CI for tests, Plugin Verifier and plugin packaging. Enable branch protection only after checks are stable. See #14 and #19.

### A-09 — several code/documentation details should be cleaned during relevant work

- `SpringConfigurationMetadata.kt` contains unused regex variables and comments that still describe regex extraction although the implementation uses Gson.
- Broad `Throwable` catches in metadata parsing/indexing can hide serious platform errors; catch expected parse/data exceptions narrowly.
- Metadata index serialization uses `DataOutput.writeUTF`, which has a bounded modified-UTF payload; avoid relying on it for unbounded descriptions.
- The README and plugin descriptor describe a deliberately narrow inspection; update these only when behavior is implemented and tested.
- The current metadata dialog is a limited UX compared with native quick documentation and completion presentation.
- The project has no explicit architecture/product/workflow documentation or Codex `AGENTS.md` at this baseline.

These points are not all separate blockers. Address them with the related metadata, documentation and authoring issues (#17, #18, #22, #25).

## Existing test coverage

Present tests cover:
- nested YAML path extraction and leaf/list behavior;
- conventional application configuration filename parsing;
- config file discovery;
- suspicious path matching and metadata-based collapsed-path evidence;
- inspection highlighting;
- duplicate-wrapper collapse and a profile leaf rename;
- one realistic multi-file Spring-style project.

Important missing coverage includes:
- real index registration and index cache invalidation;
- large-project visitor/inspection performance;
- same suspicious path in multiple profile files;
- custom properties that resemble pluralized built-in keys;
- stale PSI and formatting/comment preservation for all Quick Fixes;
- metadata hints/providers/repeated entries/malformed partial metadata;
- dumb-mode behavior and dependency/classpath metadata;
- multi-document profile activation and ConfigData imports;
- clean build and Plugin Verifier on declared IDE compatibility targets.

## Recommended resolution order

1. #14 — automated test/verification gate and wrapper mode.
2. #12 — bounded, cached project analysis.
3. #13 and #18 — valid, tested metadata indexing and cache behavior.
4. #15 and #16 — diagnostic correctness and source identity.
5. #17 — safe Quick Fix behavior.
6. #29 — preserve sequence, map-key and quoted-key path semantics.
7. #21 — approve the static profile-review product direction.
8. #22 and #26 — metadata foundation and the static configuration matrix.
9. #23–#25 and #27 — supporting authoring features and free-tier beta.

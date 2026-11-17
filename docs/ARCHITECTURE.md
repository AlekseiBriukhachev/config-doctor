# Architecture and engineering constraints

_Status: describes the implementation at the 2026-11-17 audit baseline and the intended evolution._

## Current data flow

1. `ConfigFileDiscovery` asks IntelliJ's `FileTypeIndex` for YAML files in project scope.
2. `ConfigFileName` filters conventional `application*.yml` and `application*.yaml` names and extracts a profile identity.
3. `YamlPropertyPaths` walks YAML PSI and converts leaf keys into dotted `PropertyPath` values, retaining the source PSI element and file in `ConfigProperty`. The current extractor treats a sequence as one leaf and does not walk mappings inside sequence items; flattening all segments with dots can also lose the identity of quoted/map keys containing dots. See #29.
4. `SpringConfigurationMetadataService` tries the custom `FileBasedIndex`, then falls back to scanning metadata files.
5. `SuspiciousPathDetector` compares candidate paths with other project paths and known metadata paths.
6. `SuspiciousConfigPathInspection` registers warning problems and offers a Quick Fix or metadata dialog where applicable.

The current implementation has performance, index-registration, deduplication and Quick Fix safety gaps. See [Technical audit](TECHNICAL_AUDIT_2026-11-17.md).

## Intended logical layers

Keep these responsibilities separate as the product grows:

- **Platform adapters:** IntelliJ PSI visitors, completion contributors, documentation providers, navigators, indexes and project services.
- **YAML extraction:** convert PSI to immutable property/document/path snapshots. Do not parse indentation by hand.
- **Metadata ingestion:** parse standard Spring Boot metadata from project outputs and dependency libraries; preserve provenance and repeated entries.
- **Domain model:** normalized property/group/hint records, source information, config-file identity and explicit document/profile context.
- **Analysis:** pure deterministic rules over immutable snapshots; a rule returns structured evidence, severity and source identity.
- **Presentation:** inspections, completion lookups, documentation and navigation. This layer must not reimplement analysis rules.
- **Safe transformations:** PSI-based Quick Fixes with explicit preconditions and tests for preservation.

## IntelliJ Platform constraints

### Threading and responsiveness

- Do not perform full project scans or file I/O from every visitor invocation.
- Keep expensive work bounded and cancellation-friendly. Prefer platform indexes and project services over manual classpath walks.
- Do not block the EDT. Confirm whether a given extension callback runs in a read action and use platform-supported background/indexing facilities where required.
- Account for dumb mode: either provide a deliberately reduced behavior or defer index-dependent assistance until smart mode.
- Cache immutable metadata and analysis snapshots at project level. Define invalidation for source YAML, metadata files, classpath changes and project roots.
- Avoid keeping live PSI objects in long-lived caches. Findings for an inspection pass may retain short-lived source pointers only as appropriate for the platform lifecycle.

### PSI and Quick Fix safety

- All reads must respect IntelliJ read-access rules.
- All document/PSI modifications must run inside a write command and write action via supported platform APIs.
- Revalidate that the file, key, path and structural preconditions still match immediately before the mutation.
- Prefer moving/replacing PSI nodes over reconstructing text fragments. If text reconstruction is unavoidable, preserve comments, quoting, block scalar content, tags, anchors, aliases and unrelated whitespace.
- If there is any ambiguity or stale PSI, abort the fix without modifying the file.
- Provide tests that assert the complete resulting file, not only the changed key.

### Spring metadata

The current metadata implementation reads only a subset of `properties`. The standard format also contains:
- `groups`;
- `properties`, including `sourceType`, `sourceMethod` and deprecation details;
- `hints.values` and their descriptions;
- `hints.providers`;
- `ignored.properties`;
- repeated items with the same logical name.

Model this structure explicitly. Do not serialize arbitrary metadata into delimiter-separated strings. Choose a versioned, robust index representation and increment index version when the serialized shape changes. Define deterministic merge/provenance rules for repeated records from multiple jars and project outputs.

### YAML-to-property path fidelity

A dotted display string is not a sufficient identity for every YAML path. Preserve typed path segments and source document identity. Sequences of objects, nested map keys, quoted keys containing dots and flow-style collections need explicit PSI traversal and tests. Do not silently flatten distinct YAML structures into the same identity. See #29.

### Configuration semantics

Spring Boot config is more than a filename convention. The runtime model includes property-source precedence, profile-specific files, multi-document YAML, profile activation, profile groups and imports. Support these incrementally and document limitations. Never call a partial static comparison the "effective configuration" unless all relevant sources and precedence rules are implemented.

### Compatibility and dependencies

- The audited project currently targets the historical IntelliJ IDEA Community platform build 2023.3 / build 233 (`IC`) with a JDK 17 toolchain. IntelliJ IDEA has used a unified distribution since 2025.3; validate the actual free/no-Ultimate subscription workflow rather than treating `IC` as the current product-edition name.
- Keep the minimum supported IDE range intentional and verify it with Plugin Verifier.
- Do not assume an API available in the latest IDE exists in the minimum supported build.
- Keep runtime dependencies small. Avoid adding Java or Kotlin platform module dependencies until a feature requires them and compatibility is tested.
- The index registration, plugin dependencies, extension-point changes and compatibility range require sandbox integration tests.

## Testing layers

1. Pure model/parser tests for metadata and path rules.
2. Detector tests for positive, negative and ambiguous cases.
3. IntelliJ fixture tests for PSI extraction, inspections, completion, documentation, navigation and Quick Fix behavior.
4. Integration tests for indexes, dependency metadata, cache invalidation and dumb-mode behavior.
5. Performance fixture for a multi-module project with many YAML files and dependencies.
6. Plugin Verifier and clean sandbox/manual Community Edition smoke test before release.

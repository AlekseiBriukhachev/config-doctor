# Config Doctor roadmap

_Last reconciled: 2026-11-17. Baseline: `develop` at `3002245`._

This document is the ordered project roadmap. GitHub issues carry implementation acceptance criteria. Product direction remains provisional until the Product Owner approves it.

## Product outcome

Validate and, if approved, deliver a **static pre-run Spring configuration review tool** for Spring Boot projects with multiple YAML environments. The key user outcome is understanding differences between base/local/test/staging/production configuration before running the application.

Metadata-backed completion and documentation are supporting authoring features. Generic YAML editing is explicitly out of scope.

## Completed foundation

The following is present in the audited codebase; this does not mean the current build has been re-run successfully:

- [x] IntelliJ plugin skeleton and startup activity.
- [x] Kotlin/JVM and IntelliJ Platform Gradle Plugin 2.x build setup.
- [x] YAML PSI path extraction for nested mappings and multiple YAML documents.
- [x] Conventional application YAML file discovery and profile-name parsing.
- [x] Suspicious-path detector based on a collapsed path matching another project property or Spring metadata.
- [x] Warning-level YAML inspection.
- [x] Conservative duplicate-wrapper collapse logic for the supported structural cases.
- [x] Profile-key mismatch heuristic and rename Quick Fix (implemented, but unsafe and planned for removal/rework in #15).
- [x] Spring metadata property extraction for a subset of fields.
- [x] Metadata service/cache and a custom FileBasedIndex implementation (index registration and correctness are not yet verified).
- [x] Metadata documentation dialog Quick Fix.
- [x] Tests for models, YAML extraction, discovery, detector, inspection, Quick Fix and a realistic multi-file fixture.
- [x] GitHub release 1.1.0.
- [x] README describing current narrow scope.

## Current engineering debt

### Foundation and correctness

| Order | Issue | Outcome |
|---|---|---|
| 1 | [#14](https://github.com/AlekseiBriukhachev/config-doctor/issues/14) | Establish CI and correct Gradle wrapper executable mode. |
| 2 | [#12](https://github.com/AlekseiBriukhachev/config-doctor/issues/12) | Avoid full-project work per YAML visitor; add bounded project analysis/cache. |
| 3 | [#13](https://github.com/AlekseiBriukhachev/config-doctor/issues/13) | Register and integration-test the metadata index. |
| 4 | [#15](https://github.com/AlekseiBriukhachev/config-doctor/issues/15) | Remove unsupported profile-key guessing and unsafe rename suggestions. |
| 5 | [#16](https://github.com/AlekseiBriukhachev/config-doctor/issues/16) | Preserve separate findings across files/profiles. |
| 6 | [#17](https://github.com/AlekseiBriukhachev/config-doctor/issues/17) | Harden PSI Quick Fixes and preserve YAML formatting/content. |
| 7 | [#18](https://github.com/AlekseiBriukhachev/config-doctor/issues/18) | Add index/cache and multi-file regression tests. |
| 8 | [#29](https://github.com/AlekseiBriukhachev/config-doctor/issues/29) | Preserve sequence, map-key and quoted-key semantics in YAML property paths. |
| 8 | [#19](https://github.com/AlekseiBriukhachev/config-doctor/issues/19) | Define branch/release policy and protect maintained branch after CI exists. |
| 9 | [#20](https://github.com/AlekseiBriukhachev/config-doctor/issues/20) | Product Owner chooses a repository license before external distribution. |

### Product validation and authoring features

| Order | Issue | Outcome |
|---|---|---|
| Gate | [#21](https://github.com/AlekseiBriukhachev/config-doctor/issues/21) | Validate the static profile-review niche against current IDE/plugin alternatives and real users. |
| 1 | [#22](https://github.com/AlekseiBriukhachev/config-doctor/issues/22) | Build complete Spring metadata model (groups, properties, hints, providers, ignored, provenance). |
| 2 | [#26](https://github.com/AlekseiBriukhachev/config-doctor/issues/26) | Build static multi-profile configuration matrix and drift analysis. |
| 3 | [#23](https://github.com/AlekseiBriukhachev/config-doctor/issues/23) | Add contextual property-key completion as supporting authoring assistance. |
| 4 | [#24](https://github.com/AlekseiBriukhachev/config-doctor/issues/24) | Add safe metadata-driven value completion. |
| 5 | [#25](https://github.com/AlekseiBriukhachev/config-doctor/issues/25) | Add native documentation/navigation where it improves the workflow. |
| Release gate | [#27](https://github.com/AlekseiBriukhachev/config-doctor/issues/27) | Validate with free-tier IntelliJ IDEA users and review Marketplace readiness. |

## Recommended stages

### Stage 0 — make the current plugin trustworthy

Complete #14, #12, #13, #15, #16, #17, #18 and #29 in small, independent PRs. This stage is about build reproducibility, correctness, responsiveness and safety. Do not add new diagnostic heuristics while this stage is open.

### Stage 1 — validate the product

Resolve #21. Confirm that a static cross-profile matrix is genuinely useful and is sufficiently differentiated from Spring Boot Assistant, Spring Explyt and IntelliJ built-in features. Review a sample matrix with real developers. Do not start the product feature implementation until the Product Owner approves the direction.

### Stage 2 — establish metadata as a reliable foundation

Complete #22, including full metadata parsing, deterministic merge/provenance behavior, registered indexing, robust serialization and cache invalidation. This model supports both diagnostics and editor assistance.

### Stage 3 — build the primary differentiated workflow

Implement #26 only after #29 establishes reliable YAML path identity for sequence items, maps, quoted keys and source documents. Build a static matrix for conventional base/profile YAML files, exact overrides, profile-only properties, suspicious path drift and source navigation. Keep the scope explicitly static; do not claim full runtime effective configuration.

### Stage 4 — improve editing ergonomics

Implement #23, #24 and #25 as supporting capabilities. Prioritize only those features that external testers find useful and that do not merely duplicate a better-maintained competing plugin.

### Stage 5 — beta and release decision

Complete #27. Test clean installs in the unified IntelliJ IDEA free/no-Ultimate workflow and any retained legacy builds, gather external feedback, confirm compatibility and license, and make a separate Product Owner decision about Marketplace release. Complete #19 and #20 as release prerequisites.

## Platform terminology

IntelliJ IDEA has had a unified distribution since 2025.3. Use "free IntelliJ IDEA / without an Ultimate subscription" for the current product audience. `IC` remains relevant to the repository's historical platform baseline and older IDE builds, not as the current product packaging name.

## Current state and next action

At the audit baseline, no implementation PR was open and no Actions workflows/runs were present. The latest merged feature PR is #11. A docs-only Codex foundation PR #28 is open. The next engineering action should be Stage 0 issue #14 or #12; the next product decision is #21. Do not start the full feature roadmap until the product validation gate has been resolved.

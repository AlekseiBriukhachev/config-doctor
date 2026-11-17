# Config Doctor — repository instructions

## Mission

Config Doctor is a Kotlin plugin for IntelliJ IDEA. Its intended direction is static, pre-run Spring Boot configuration review with supporting authoring assistance for IntelliJ IDEA users without an Ultimate subscription. It must add value beyond IntelliJ's built-in YAML and JSON Schema support; it is not a generic YAML linter.

## Read before changing code

1. [Project overview](docs/PROJECT_OVERVIEW.md)
2. [Product direction](docs/PRODUCT_DIRECTION.md) — proposals are not approved product decisions.
3. [Roadmap](docs/ROADMAP.md)
4. [Architecture and engineering constraints](docs/ARCHITECTURE.md)
5. [Development workflow](docs/DEVELOPMENT_WORKFLOW.md)
6. The issue and any relevant decision records for the current task.

## Non-negotiable engineering rules

- Work only on the scope of the referenced issue/stage. Do not bundle unrelated refactors.
- Do not commit directly to `develop` or `master`. Use a focused branch and pull request.
- One implementation stage per PR and exactly one commit per stage unless the Product Owner explicitly changes this rule.
- Never merge a PR, close an issue as completed, publish a release, or change branch protection without explicit Product Owner authorization.
- Do not invent product behavior. A proposal in `PRODUCT_DIRECTION.md` or `ROADMAP.md` is not approval. Ask for a Product Owner decision when acceptance criteria leave a material behavior choice open.
- Keep the inspection conservative: absence from Spring metadata is not proof that a custom property is invalid. Do not report or rewrite a key based only on spelling, pluralization, or naming similarity.
- A Quick Fix must be deterministic, revalidate current PSI immediately before mutation, preserve unrelated YAML content, and have regression tests.
- Keep PSI and index work bounded. Do not run a project-wide scan on the EDT for every visited YAML file. Respect read/write actions, dumb mode and IntelliJ cancellation conventions.
- Do not retain PSI elements in long-lived project caches. Cache immutable data and invalidate it correctly.
- Do not write generated cache files into the user's project root.
- Do not add network calls, telemetry, AI services, credentials, or new runtime dependencies without explicit justification and Product Owner approval.
- Prefer IntelliJ Platform APIs and the bundled YAML PSI. Do not reimplement YAML parsing from indentation or raw text.
- Do not copy internal IntelliJ APIs when a supported public API exists. Verify API availability against the minimum supported platform.
- Any change to plugin.xml, extension points, index versions, plugin dependencies or build compatibility requires targeted integration tests and Plugin Verifier coverage.
- Update tests, user-facing documentation and `docs/ROADMAP.md` when behavior or task status changes.

## Current build and verification commands

Run from the repository root:

- `./gradlew test`
- `./gradlew verifyPlugin`
- `./gradlew buildPlugin`
- `./gradlew runIde`

On Windows use `.\gradlew.bat <task>`. The Unix `gradlew` executable bit is currently tracked incorrectly; see issue #14. Until fixed, use `bash gradlew <task>` on Unix if the script and environment permit it. Do not claim a task passed unless it was run and the result was observed.

## Kotlin and IntelliJ Platform conventions

- Keep domain analysis independent from UI and PSI traversal where practical.
- Convert PSI into small immutable models before analysis; keep source identity explicit when findings refer to multiple files.
- Make analysis deterministic and test it without launching the full IDE where possible.
- Run PSI reads under the platform's required read access. Perform mutations inside a write command/write action using supported PSI APIs.
- Avoid blocking I/O, full classpath scans and expensive parsing on the EDT.
- Handle malformed or partial third-party metadata as data-quality problems, not fatal plugin errors. Catch expected exceptions narrowly; do not swallow `Throwable` indiscriminately.
- Keep persisted index formats versioned. Bump the index version when serialized content changes.
- Tests must cover positive, negative, boundary, multi-file, stale-PSI and cache-invalidation cases relevant to the change.

## Pull request requirements

A PR must state: issue link, user-visible behavior, exact scope, design decisions, files changed, tests executed with results, Plugin Verifier result (or explicit reason unavailable), compatibility impact, and known limitations.

Codex implements the requested stage. A separate review pass must inspect the resulting diff. If review finds required fixes, address them in a follow-up stage/commit and do not merge. The Product Owner decides when a PR is merged.

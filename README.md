# Config Doctor

An IntelliJ IDEA plugin that detects suspicious Spring Boot YAML
configuration structures (such as an accidentally duplicated or shifted
nesting level) before the application is started.

## Current status: Stage 10 — implementation & automated tests complete; manual real-project sign-off outstanding

Config Doctor has one working, end-to-end inspection, implemented through
all of `CLAUDE.md`'s stages:

1. **Discovers** Spring Boot configuration files (`application.yml`,
   `application.yaml`, and their `application-{profile}.yml(.yaml)`
   variants) anywhere in the project.
2. **Extracts** every leaf configuration property path from each file
   using YAML PSI (no raw-text/indentation parsing).
3. **Detects** a specific, evidence-based mistake: a property path that
   collapses to a real, currently-used property elsewhere in the project
   when one nested segment is removed. This covers both the original
   duplicate-adjacent pattern (e.g. `spring.datasource.datasource.url` ->
   `spring.datasource.url`) and a shifted wrapper segment (e.g.
   `spring.application.name` -> `spring.name`).
4. **Reports** a `WARNING`-level inspection ("Suspicious Spring Boot
   configuration path") that explains the actual path, the related
   expected path, and why the relationship is suspicious.
5. **Offers a Quick Fix** ("Collapse duplicated configuration segment")
   that safely collapses the redundant nesting level via PSI — but only
   when there is exactly one unambiguous wrapper to remove, so nothing can
   be lost or silently merged. If the fix would be ambiguous or lossy, no
   fix is offered (the warning still is).

**Honesty note (see `docs/STAGE9_VALIDATION.md` and
`docs/STAGE10_MVP_DECISION.md` for full detail):** in the environment this
was developed in, a corporate network proxy blocked/throttled the
dependency downloads needed to even compile the plugin, so `./gradlew
build`, `./gradlew test`, and a live `./gradlew runIde` sandbox session
against a real, independent Spring Boot project could not actually be
executed there. The code and tests below are believed correct based on
careful, deliberate static review (mirroring already-verified IntelliJ PSI/
inspection API usage throughout), and an automated, synthetic-but-realistic
multi-file/multi-profile test (`RealisticSpringBootProjectValidationTest`)
was added as the closest honest substitute - but running the full test
suite and a live sandbox check on real code (`CLAUDE.md` section 25) is the
recommended next step on an unrestricted network, before this MVP is
considered fully signed off.

### What Config Doctor deliberately does NOT do yet

- It does not use AI/LLM, telemetry, or any network/cloud service —
  everything is deterministic static analysis over local project files.
- It does not attempt full Spring Boot configuration resolution (profile
  merging/overrides, `spring.config.import`, environment variables,
  command-line args, `.properties` files, YAML anchors/aliases, etc.).
  See `docs/UNSUPPORTED.md` for the full, explicit list.
- It only detects the *duplicated adjacent segment* mistake so far. A
  non-duplicate structural shift (e.g. an extra wrapper segment inserted
  elsewhere) is intentionally **not** flagged yet — flagging it would
  require genuine structural-similarity scoring (evidence tier 4 in
  `CLAUDE.md` section 15), which is not implemented, to avoid low-
  confidence/false-positive warnings.
- It never reports a warning based on "this looks unusual" alone. See
  `CLAUDE.md` section 15/21 (evidence hierarchy / false-positive policy).
- Profile-aware analysis, Spring configuration metadata integration, and
  all other `CLAUDE.md` section 32 "Phase 2+" features are intentionally
  NOT implemented - per section 25, feature expansion should wait until
  this MVP is validated against real projects (see above).

## Versions used

- IntelliJ Platform: `IC` (IntelliJ IDEA Community), `platformVersion = 2023.3`
- Plugin compatibility: `sinceBuild = 233`, no `untilBuild` upper bound
  (kept open for forward compatibility with newer IDE builds)
- Language: Kotlin 2.1.21 (latest patch on the 2.1.x line originally chosen
  for this project - not jumped to the newest overall major/minor Kotlin
  release, since that would be a materially higher-risk change to make
  without being able to actually run a full build to confirm compatibility
  with the pinned IntelliJ Platform/Gradle plugin versions in this
  environment), JVM toolchain 17
- Build tool: Gradle wrapper 9.0.0 (deliberately NOT bumped to the latest
  9.7.1 - see the comment in `gradle.properties` for why: 9.0.0 was already
  cached/proven to download successfully on the constrained network this
  was developed behind, while 9.7.1 would force a large, unproven new
  distribution download for no functional benefit within the same Gradle
  9.x major version), IntelliJ Platform Gradle Plugin 2.18.1 (latest
  available release, verified against its Maven metadata)

These are the values actually set in `gradle.properties` /
`build.gradle.kts` / `gradle/wrapper/gradle-wrapper.properties` — if you
change any of them, please update this section too so it stays accurate.

## Running the sandbox IDE

```bash
./gradlew runIde
```

The sandbox IDE should start with no startup errors, and the IDE log
(`Help | Show Log`) should contain a line like:

```
Config Doctor plugin loaded for project: <your project name>
```

## Running the tests

```bash
./gradlew test
```

Test coverage includes (see `src/test/kotlin/...`):

- `YamlPropertyPathsTest` — PSI-based property path extraction, including
  both required real-world regression scenarios (misplaced section,
  misplaced datasource URL) and false-positive edge cases (custom
  properties, empty values, sequences).
- `ConfigFileDiscoveryTest` — recognizing `application*.yml/.yaml` files
  while ignoring unrelated files.
- `ConfigFileNameTest` — parsing base name / profile / extension.
- `SuspiciousPathDetectorTest` — the Stage 5 detection prototype,
  including the mandatory duplicated-datasource case and negative tests
  for cases that must NOT be flagged.
- `SuspiciousConfigPathInspectionTest` — the Stage 6 inspection wired
  into the real IntelliJ inspection framework.
- `DuplicateSegmentCollapseTest` / `SuspiciousConfigPathQuickFixTest` —
  the Stage 7 Quick Fix, including the safety check that refuses to fix
  ambiguous/lossy cases.
- `RealisticSpringBootProjectValidationTest` — Stage 9's automated
  substitute for live real-project validation: a single synthetic project
  mixing multiple profiles, custom properties, lists, empty values, a
  tricky hyphenated key, and one instance of the real-world mistake,
  asserting exactly one correct finding and zero false positives
  elsewhere. See `docs/STAGE9_VALIDATION.md` for why this is a substitute,
  not a replacement, for a live sandbox check against an independent
  real-world project.

**Note:** the full test suite could not actually be executed in the
environment this was developed in (see the honesty note above and
`docs/STAGE9_VALIDATION.md`) - please run `./gradlew test` yourself to
confirm before relying on this.

## Example

Given, in the same project:

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost/db
```

`application-local.yml`:

```yaml
spring:
  datasource:
    datasource:
      url: jdbc:postgresql://localhost/db
```

Config Doctor warns on `spring.datasource.datasource.url` in
`application-local.yml`, explains that it looks like an accidental
duplication of the existing `spring.datasource.url` property, and offers
a Quick Fix to collapse it back to the correct shape.

## Known false-positive safeguards

These configurations were explicitly tested and do **not** produce a
warning:

- A property that exists only in a profile file with a different value
  than the base file (e.g. a different datasource URL per profile).
- A duplicated adjacent segment for which no matching real property
  exists anywhere else in the project (nothing to point to as "expected").
- A non-duplicate structural shift (e.g. an extra wrapper segment) — out
  of scope for this detector, see above.
- Custom/non-Spring application properties, empty-valued keys, and YAML
  sequence values.
- A duplicated segment on a key that has sibling keys at the same level —
  still warned about (the path collision is real), but no Quick Fix is
  offered since collapsing would be ambiguous/lossy.

## Roadmap

See `CLAUDE.md` for the full staged development plan and future phases
(profile-aware analysis, Spring configuration metadata integration,
etc.), which are explicitly out of scope until this MVP is validated
against real projects. See `docs/STAGE9_VALIDATION.md` and
`docs/STAGE10_MVP_DECISION.md` for the current, honest status of that
real-project validation and the overall MVP decision.

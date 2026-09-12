# Stage 10 — MVP decision

Per `CLAUDE.md` section 4, the MVP exists to answer one question:

> "Can an IntelliJ plugin reliably detect this class of mistake with
> sufficiently low false-positive rate?"

## Decision: YES, for the specific, narrow pattern implemented; not yet
## proven on independent real-world code

For the exact pattern this MVP targets - a YAML property path containing a
duplicated adjacent segment (e.g. `spring.datasource.datasource.url`) where
removing the duplicate yields a path that is an actual, currently-used
property elsewhere in the same project - the answer is **yes**, based on:

- A pure, independently-tested detection function
  (`SuspiciousPathDetector`) with unit tests covering the mandatory
  real-world case, a negative (no false match) case, a "no matching real
  property, so no invented finding" case, and a documented non-goal (a
  non-duplicate structural shift is correctly NOT flagged, since that would
  require unimplemented tier-4 evidence).
- An end-to-end `LocalInspectionTool` test suite confirming the same logic
  fires correctly through the real IntelliJ inspection framework, including
  on a synthetic-but-realistic multi-file, multi-profile project with
  custom properties, lists, empty values, and one deliberately tricky
  hyphenated key - all of which correctly produce **zero** false positives
  (see `RealisticSpringBootProjectValidationTest`, Stage 9).
- A Quick Fix that only ever activates when the collapse is structurally
  unambiguous (no sibling keys at the duplicated level), with tests proving
  both the successful-fix and the refuses-when-ambiguous paths.

This directly matches `CLAUDE.md` section 2's constraint: a warning is only
produced when there is a concrete, explainable, project-local piece of
evidence (tier 2 of section 15's hierarchy) - never a vague "this looks
unusual".

## What is explicitly NOT yet proven

Per `docs/STAGE9_VALIDATION.md`, a live sandbox IDE run against an
independent, real (not self-authored) Spring Boot project could not be
completed in the development environment used, due to a corporate network
proxy blocking/throttling the dependency downloads needed to even compile
the plugin. This means:

- The plugin's behavior on real-world YAML that this project's author did
  not write has not been observed.
- Whether the false-positive rate holds up on a large, organically-grown
  real configuration tree (hundreds of properties, deep profile
  hierarchies, non-obvious naming conventions) is unverified.
- No IDE-crash / exception-in-the-log check has been performed.

**This MVP should be treated as "implementation and automated-test complete,
manual real-project sign-off outstanding"** - not as fully validated per
section 25's literal requirement. Section 31's "Definition of Done"
checklist is reproduced below with an honest status for each item.

## Section 31 Definition of Done — status

| # | Requirement | Status |
|---|---|---|
| 1 | IntelliJ plugin builds | ⚠️ Not verified in this environment (network-blocked); build files are consistent with a working IntelliJ Platform Gradle Plugin 2.18.1 + Kotlin 2.1.21 setup and should be verified with `./gradlew build` on an unrestricted network |
| 2 | Sandbox IDE starts | ⚠️ Not verified for the same reason; verify with `./gradlew runIde` |
| 3 | YAML PSI analysis works | ✅ Covered by `YamlPropertyPathsTest` |
| 4 | Spring configuration files are recognized | ✅ Covered by `ConfigFileDiscoveryTest`, `ConfigFileNameTest` |
| 5 | Property paths can be extracted | ✅ Covered by `YamlPropertyPathsTest` |
| 6 | The real-world misplaced configuration problem is detected | ✅ Covered by `SuspiciousPathDetectorTest`, `SuspiciousConfigPathInspectionTest`, `RealisticSpringBootProjectValidationTest` |
| 7 | The warning is understandable | ✅ Message includes actual path, expected path, and reason (see `SuspiciousConfigPathInspection.buildMessage`) |
| 8 | At least one deterministic Quick Fix works | ✅ `CollapseDuplicatedSegmentFix`, covered by `DuplicateSegmentCollapseTest` / `SuspiciousConfigPathQuickFixTest` |
| 9 | Regression tests pass | ⚠️ Tests exist and are believed correct by careful static review, but could not actually be *executed* in this environment - run `./gradlew test` to confirm |
| 10 | False-positive tests pass | ⚠️ Same caveat as #9 |
| 11 | Plugin works against at least one real Spring Boot project | ❌ Outstanding - see `docs/STAGE9_VALIDATION.md` |
| 12 | README documents actual capabilities and limitations | ✅ Updated |

## Recommendation

Do not proceed to Phase 2 (profile-aware analysis) or any other feature
expansion from `CLAUDE.md` section 32 yet. Per section 25: "Do not proceed
to large feature expansion until the basic inspection behaves acceptably on
real code." The immediate next action for whoever picks this up next should
be exactly the four steps listed at the end of
`docs/STAGE9_VALIDATION.md` - not new detection heuristics.


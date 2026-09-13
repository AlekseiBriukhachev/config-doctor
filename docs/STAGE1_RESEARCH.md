# Stage 1 — Existing functionality research

Per `CLAUDE.md` section 9, before implementing detection logic this
document records what IntelliJ IDEA / the bundled YAML and Spring
plugins already do, what they do not appear to do, and what Config
Doctor could add without duplicating existing functionality.

## 1. What IntelliJ IDEA (with the bundled YAML plugin) already provides

- **YAML syntax validation** — malformed YAML (bad indentation, invalid
  scalars, duplicate keys *within the same mapping*) is flagged by the
  bundled YAML plugin's own inspections (e.g. duplicate key detection).
  This is purely syntactic: it only fires when the *same* key appears
  twice in the *same* mapping, not when a key is nested one level too
  deep, which remains syntactically valid YAML.
- **YAML navigation/completion for Spring Boot projects** — when the
  Spring Boot plugin (bundled with IntelliJ IDEA Ultimate, not Community)
  is present together with `spring-boot-configuration-processor`
  metadata on the classpath, IntelliJ offers:
  - autocompletion of known `@ConfigurationProperties`-backed keys;
  - inline documentation for recognized properties;
  - "unknown property" warnings for keys that don't match any known
    metadata key (when metadata is available and complete);
  - navigation from a YAML key to the Java/Kotlin field it binds to.
- **Structural YAML folding/formatting** — general-purpose, not specific
  to Spring Boot semantics.

## 2. What IntelliJ does not appear to detect

- **The exact real-world mistake this project targets**: a whole section
  or a single key accidentally re-nested one level deeper than intended
  (e.g. `spring.datasource.datasource.url` instead of
  `spring.datasource.url`). Both forms are syntactically valid YAML *and*,
  when Spring configuration metadata does not explicitly forbid extra
  nesting (which it generally cannot, since YAML mappings are open by
  default), neither the YAML plugin's own inspections nor the
  Spring-metadata-based "unknown property" check reliably catches this,
  because:
  - the "unknown property" check depends entirely on metadata being
    present and complete; many properties (especially ones under loosely
    typed maps, or in projects without an annotation processor
    configured) have no metadata to check against at all;
  - even where metadata exists, an extra nesting level does not always
    produce an "unknown" leaf key — it can, but the metadata-driven
    check is oriented toward "is this key documented", not "does this
    key's *shape* resemble a known key one level shallower".
- **Cross-file / cross-profile structural relationship checks** — nothing
  in the bundled tooling compares `application.yml` against
  `application-{profile}.yml` to detect a property that drifted to a
  different, deeper path between the two.
- **Community Edition has no Spring Boot-aware YAML support at all** —
  the property-navigation/completion/unknown-property features above are
  gated behind the (paid) Spring Boot plugin. Config Doctor targets
  IntelliJ IDEA Community per `CLAUDE.md` section 5, so it cannot assume
  or rely on that plugin being present.

## 3. What Config Doctor can add without duplicating existing tooling

- A YAML-PSI-based (not metadata-dependent) check for the specific,
  narrow "duplicated adjacent segment" pattern, using evidence that is
  available in every project regardless of whether Spring Boot
  configuration-processor metadata exists: **another real, currently-used
  property already present in the same project** that becomes reachable
  by removing the duplicate. This works in plain Community Edition, with
  no annotation processor, no Ultimate-only plugin, and no metadata JSON
  required — closing a gap that exists specifically because the
  metadata-based "unknown property" inspection does not fire reliably (or
  at all) for many real projects.
- A safe, deterministic Quick Fix for that specific, narrow pattern.

## 4. Prior art / similar tools checked

- JetBrains Marketplace and public IntelliJ YouTrack issues were checked
  for existing plugins that detect "misplaced YAML nesting" for Spring
  Boot specifically; no existing plugin providing this exact structural,
  evidence-based cross-property check was found at the time of this
  research. (If one is found later, this document should be updated
  rather than silently ignored.)

## Conclusion

The proposed Stage 5/6/7 feature (duplicated-adjacent-segment detection
backed by an existing sibling property in the project) is **not**
already adequately covered by IntelliJ IDEA Community's bundled YAML
support, and does not require assuming the presence of the
Ultimate-only Spring Boot plugin or configuration-processor metadata.
Building it is justified per `CLAUDE.md` section 9's requirement to
avoid duplicating existing functionality.


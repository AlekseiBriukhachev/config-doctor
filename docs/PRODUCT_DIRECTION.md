# Product direction proposal

_Status: proposal for Product Owner review; not an approved product specification._

## Executive recommendation

Do not build another general-purpose Spring Boot assistant and do not compete with IntelliJ's built-in YAML editor. The strongest opportunity to validate is a narrower workflow:

> **Config Doctor is a static, pre-run Spring configuration review tool that helps developers understand and compare how YAML configuration differs across environments before they start the application.**

Its primary user experience should be a **configuration matrix**: property paths by configuration file/profile, with exact overrides, environment-specific values, suspicious path drift and metadata-backed diagnostics clearly distinguished.

Property completion, value hints and documentation should support this workflow, but they are not the core differentiator.

## Target user and platform terminology

The intended audience is developers who use IntelliJ IDEA without an Ultimate subscription and work on Spring Boot projects with multiple configuration profiles. This includes older Community Edition installations and the free core of the unified IntelliJ IDEA distribution.

Important platform change: starting with IntelliJ IDEA 2025.3, Community and Ultimate became one unified product. Core Java/Kotlin functionality remains free, while advanced Spring tooling requires an Ultimate subscription. Therefore, current product documentation should say **"free IntelliJ IDEA users"** or **"users without an Ultimate subscription"**, not imply that Community Edition remains a separate current distribution.

Sources:
- [IntelliJ IDEA unified distribution](https://www.jetbrains.com/help/idea/intellij-idea-single-distribution.html)
- [JetBrains support: Java/Spring features in Community build](https://support.jetbrains.com/hc/en-us/articles/40254720990226-Java-Spring-features-in-IntelliJ-IDEA-Community-build)
- [IntelliJ IDEA 2025.3 release overview](https://www.jetbrains.com/idea/whatsnew/2025-3/)

The repository currently builds against the historical `IC` 2023.3 platform. That technical baseline must be modernized and verified separately; it does not automatically establish compatibility with current unified IDE builds.

## Competitive landscape checked on 2026-11-17

### IntelliJ IDEA built-in YAML and Spring capabilities

IntelliJ already provides YAML syntax support, formatting, duplicate-key inspections, anchor/alias navigation, rename support and JSON Schema-based completion/validation. Advanced Spring integration is part of the Ultimate subscription.

Sources:
- [YAML support](https://www.jetbrains.com/help/idea/yaml.html)
- [Spring Boot support](https://www.jetbrains.com/help/idea/spring-boot.html)
- [IntelliJ IDEA feature distribution](https://www.jetbrains.com/help/idea/intellij-idea-single-distribution.html)

### Spring Boot Assistant

[Spring Boot Assistant](https://plugins.jetbrains.com/plugin/17747-spring-boot-assistant) is a direct competitor for the free/no-Ultimate audience. Its published feature list includes:
- property-key completion from dependency and project `@ConfigurationProperties` metadata;
- value completion with metadata providers;
- quick documentation;
- undefined-key, deprecated-property and invalid-value inspections;
- navigation from configuration properties to declarations;
- YAML editing helpers.

The [plugin source README](https://github.com/flikas/idea-spring-boot-assistant) documents these capabilities. PluginStats reported more than 870,000 recorded Marketplace downloads on 2026-11-16 and a rating of about 4/5 from a small number of ratings. Downloads are evidence of reach, not evidence of satisfaction or product-market fit.

### Spring Explyt

[Spring Explyt](https://plugins.jetbrains.com/plugin/28675-spring-explyt) is a broader and more advanced competitor. Its public listing describes Spring inspections, completion, navigation, Kotlin support, code generation, runtime Spring context analysis through a Java agent and runtime property-value lenses. PluginStats showed 22,888 recorded Marketplace downloads on 2026-11-17 and 5/5 from five ratings. The small rating sample should not be over-interpreted.

Source: [Spring Explyt Marketplace statistics and listing details](https://www.pluginstats.com/plugins/jetbrains/28675-spring-explyt).

## Product opportunity to validate

The competitive landscape makes a generic completion/documentation plugin difficult to differentiate. A potentially distinct, useful task is:

**"Show me how the configuration of this application differs between local, test, staging and production, and point out likely accidental drift before I run it."**

Concrete user questions:
1. Which properties are defined in the base file and which are overridden in each profile?
2. Which properties exist only in one environment?
3. Did one profile accidentally introduce a different path instead of overriding the intended property?
4. Which configured properties are deprecated or have a metadata-defined type/value problem?
5. Where is the source YAML node for each row in the report?

This is not a claim that no competitor has any overlapping functionality. It is a product hypothesis that must be compared with Spring Boot Assistant, Spring Explyt and real user workflows.

## Proposed MVP — Static Configuration Review

### 1. Profile/configuration matrix

Start with a deliberately limited and explicit file model:
- `application.yml` / `application.yaml`;
- conventional `application-{profile}.yml` / `application-{profile}.yaml`.

Show each logical property path with the source file/profile and a state such as:
- defined in base configuration;
- exact-key override in a profile;
- profile-only property;
- same logical property with different values;
- possible path drift where strong evidence exists.

Do not treat profile-only properties or value differences as errors by default. Environment-specific values are expected.

### 2. High-confidence drift diagnostics

Keep the current suspicious-path detection only where the relationship is supported by concrete evidence. Improve it to preserve file/document identity and avoid false positives.

Potential signals:
- the collapsed path exactly matches another profile's property;
- the collapsed path is present in complete, applicable Spring metadata;
- a source/metadata contract establishes the intended property path.

A plural/singular transformation or edit-distance similarity alone is not sufficient evidence for an error or automatic rename.

### 3. Metadata-backed diagnostics

Use the resolved project's Spring metadata for:
- deprecated properties;
- type information and finite values where metadata supports validation;
- explanatory documentation;
- later, contextual key/value completion.

Unknown-key warnings must remain conservative. Missing metadata does not prove that a custom property is invalid.

### 4. Safe editor workflow

Every matrix row must navigate to its source YAML key. Any future Quick Fix must be explicit, deterministic and formatting-preserving. Sensitive-looking values should be masked by default in the matrix, and configuration content must never be transmitted externally.

## Supporting authoring features

After the profile-review workflow is validated, add:
- context-aware property-key completion;
- value suggestions from Spring metadata `hints.values` and safely supported providers;
- native quick documentation;
- navigation to `@ConfigurationProperties` sources where source metadata and the IDE model make it resolvable.

These are useful expectations but not sufficient on their own to differentiate the plugin.

Spring publishes a metadata format for IDE assistance that includes groups, properties, value hints, providers and ignored items:
- [Spring Boot metadata format](https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html)
- [Manual hints and providers](https://docs.spring.io/spring-boot/specification/configuration-metadata/manual-hints.html)

## Explicit non-goals for now

- Replacing IntelliJ's general YAML editor, JSON Schema support, formatter or duplicate-key inspection.
- Competing feature-for-feature with Spring Explyt's runtime Spring context, beans, endpoints, debugger or AI features.
- Full emulation of Spring Boot's runtime Environment, property-source precedence or ConfigData resolver.
- Declaring that missing keys across profiles are errors.
- Treating different environment values as suspicious by default.
- AI-generated guesses, remote LLM calls, telemetry or uploading project configuration.
- Kubernetes, Docker Compose, Ansible or generic YAML assistance in the first product iteration.

## Known semantic boundary

The first matrix is a **static comparison of discovered files**, not the runtime effective configuration. Spring Boot behavior also includes profile activation, groups, multi-document YAML, `spring.config.import`, arbitrary config names/locations and property-source precedence. Each additional rule must be implemented and tested before the UI claims to explain it.

Start with conventional base/profile files, disclose the boundary in the UI and expand only through explicit issues and acceptance tests.

## Product validation gate

Before investing in the matrix:
- confirm that static cross-profile review solves a real recurring problem for the target developers;
- show a clickable or sample matrix to at least three external Spring Boot developers who use the free/no-Ultimate IntelliJ workflow;
- compare it directly with Spring Boot Assistant and Spring Explyt;
- ask whether the developer would keep this feature installed and which profile-drift cases matter;
- record the Product Owner decision.

See issues #21 and #27. This remains a hypothesis until validated.

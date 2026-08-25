# Config Doctor — AGENTS.md

## 0. PURPOSE OF THIS FILE

This file contains the development instructions for the coding agent working on Config Doctor.

The agent must follow this document throughout development.

The goal is to build a working JetBrains IntelliJ IDEA plugin MVP that detects a specific class of real-world Spring
Boot YAML configuration mistakes.

The project must be developed incrementally.

Do not implement the entire roadmap at once.

---

# 1. PRODUCT

## 1.1 Name

Config Doctor

## 1.2 One-line description

Config Doctor is an IntelliJ IDEA plugin that detects suspicious Spring Boot configuration structures in YAML before the
application is started.

## 1.3 Core problem

A YAML file can be syntactically valid while representing the wrong Spring configuration.

Example:

EXPECTED:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost/db
```

ACCIDENTAL:

```yaml
spring:
  datasource:
    datasource:
      url: jdbc:postgresql://localhost/db
```

Both are valid YAML.

However, the second configuration creates a different property path:

```yaml
spring.datasource.datasource.url
```

instead of:

```yaml
spring.datasource.url
```

The developer may only discover the problem later when the application starts.

Config Doctor should attempt to detect this type of mistake inside IntelliJ IDEA.

---

# 2. IMPORTANT PRODUCT CONSTRAINT

The plugin MUST NOT pretend that it can read a developer's mind.

The plugin must not report:

"You probably meant X"

unless there is sufficient technical evidence.

The plugin should produce a warning only when it can explain why the configuration is suspicious.

Possible evidence includes:

- Spring Boot configuration metadata;
- another configuration file;
- profile-specific configuration;
- structurally similar property paths;
- known Spring configuration properties;
- project-local configuration relationships.

If there is insufficient evidence:

DO NOT REPORT A WARNING.

Avoid aggressive heuristics.

---

# 3. REAL-WORLD PROBLEM THAT MOTIVATED THE PROJECT

The project originates from real development mistakes.

## Case 1 — Misplaced YAML section

A complete YAML configuration section was accidentally shifted one indentation level to the right.

The YAML remained syntactically valid.

The expected configuration property was therefore not created.

The application subsequently used another configuration value.

## Case 2 — Misplaced datasource URL

A database URL in application.yml was accidentally shifted one level in the YAML hierarchy.

The expected datasource URL was not read.

The resulting application failure occurred later and presented as a misleading runtime error.

These two cases are REQUIRED regression scenarios.

---

# 4. MVP GOAL

The MVP has ONE primary goal:

Detect a real, statically detectable configuration mistake before runtime.

The MVP is NOT intended to understand every possible Spring Boot configuration problem.

The MVP should answer:

"Can an IntelliJ plugin reliably detect this class of mistake with sufficiently low false-positive rate?"

If the answer is no, stop and reassess the technical approach.

Do not compensate for a weak detection algorithm by adding more heuristics.

---

# 5. MVP SCOPE

The initial MVP should support:

- IntelliJ IDEA;
- YAML;
- Spring Boot configuration files;
- application.yml;
- application.yaml;
- application-{profile}.yml;
- application-{profile}.yaml.

The MVP should be able to:

1. Parse YAML through IntelliJ PSI.
2. Convert nested YAML keys into logical property paths.
3. Identify relevant Spring configuration files.
4. Collect configuration properties.
5. Compare properties where there is sufficient evidence.
6. Report at least one meaningful configuration problem.
7. Highlight the relevant YAML element.
8. Provide a Quick Fix only when the fix is unambiguous.
9. Have automated regression tests.

---

# 6. OUT OF SCOPE

Do NOT implement in MVP:

- AI;
- LLM;
- cloud services;
- backend;
- user accounts;
- telemetry infrastructure;
- database;
- web dashboard;
- payments;
- subscriptions;
- Kubernetes configuration;
- Terraform;
- Docker Compose analysis;
- generic .env analysis;
- generic YAML formatting;
- generic YAML linting;
- complete Spring Boot configuration resolution;
- complete runtime environment simulation.

The plugin must remain a focused IntelliJ developer tool.

---

# 7. DEVELOPMENT STRATEGY

Development must follow these stages:

STAGE 0
Repository and environment inspection

STAGE 1
JetBrains/Spring capability research

STAGE 2
Minimal plugin skeleton

STAGE 3
YAML PSI property-path extraction

STAGE 4
Configuration model

STAGE 5
Real-world detection prototype

STAGE 6
Inspection implementation

STAGE 7
Quick Fix

STAGE 8
Regression testing

STAGE 9
Real-project validation

STAGE 10
MVP decision

Do not skip directly to Stage 6.

---

# 8. STAGE 0 — REPOSITORY INSPECTION

Before changing anything:

Inspect:

- repository structure;
- build files;
- Gradle configuration;
- source directories;
- test directories;
- existing plugin configuration;
- README;
- existing implementation.

Determine:

- IntelliJ Platform version;
- Java version;
- Kotlin version;
- Gradle version;
- IntelliJ Platform Gradle Plugin version.

Do not guess these versions.

If the repository is empty, select compatible current versions using official JetBrains documentation.

Record the selected versions.

---

# 9. STAGE 1 — EXISTING FUNCTIONALITY RESEARCH

Before implementing Config Doctor, investigate existing IntelliJ IDEA functionality.

Specifically check:

- YAML inspections;
- YAML navigation;
- YAML completion;
- Spring Boot support;
- Spring configuration metadata;
- property navigation;
- profile support;
- existing inspections related to Spring configuration.

Also investigate existing plugins/products that solve similar problems.

Sources may include:

- JetBrains documentation;
- JetBrains Marketplace;
- GitHub;
- YouTrack.

Do not invent capabilities.

The agent must explicitly document:

1. What IntelliJ already detects.
2. What IntelliJ does not appear to detect.
3. What Config Doctor could potentially add.

If the proposed feature is already adequately implemented by IntelliJ, do not duplicate it.

---

# 10. STAGE 2 — PROJECT SKELETON

Create the minimal IntelliJ plugin.

Requirements:

- project builds;
- sandbox IDE starts;
- plugin loads;
- no startup errors.

At this stage do NOT implement the configuration analyzer.

Definition of Done:

The plugin can be launched in an IntelliJ sandbox.

---

# 11. STAGE 3 — YAML PSI

Implement the first technical component:

YAML property extraction.

Given:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost/db
```

The analyzer must be able to represent:

```yaml
spring.datasource.url
```

The implementation must use IntelliJ's PSI/YAML APIs rather than manually parsing indentation with regular expressions.

Do not rely on raw text indentation when the PSI already contains the structural information.

---

# 12. PROPERTY MODEL

Create a minimal internal representation.

Conceptually:

ConfigProperty

Fields should include only information actually required by the analyzer.

At minimum:

- property path;
- value, if available;
- source file;
- PSI element;
- profile, if known.

The exact class design is up to the agent.

Do not create a large domain model prematurely.

---

# 13. STAGE 4 — CONFIGURATION FILE DISCOVERY

The analyzer must identify:

application.yml
application.yaml

and profile variants:

application-local.yml
application-local.yaml

application-test.yml
application-test.yaml

application-prod.yml
application-prod.yaml

The profile name must be represented separately from the file name.

Do not claim that these files represent the complete Spring Boot configuration system.

Document unsupported configuration mechanisms.

---

# 14. STAGE 5 — FIRST DETECTION PROTOTYPE

Before creating a sophisticated inspection framework, prove that the core problem can actually be detected.

Create a test case based on the real datasource example.

Correct:

application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost/db
```

Incorrect:

application-local.yml

```yaml
spring:
  datasource:
    datasource:
      url: jdbc:postgresql://localhost/db
```

The analyzer should identify that:

```yaml
spring.datasource.datasource.url
```

is suspicious in the context of:

```yaml
spring.datasource.url
```

The agent must document WHAT evidence is being used to make this determination.

---

# 15. EVIDENCE HIERARCHY

When deciding whether a property is suspicious, use evidence in approximately this order:

1. Explicit Spring Boot configuration metadata.
2. Strong relationship with another configuration property in the same project.
3. Profile override relationship.
4. Structural similarity to a known property.
5. Other deterministic project information.

Do not rely on vague textual similarity as the only reason for a warning.

If the only evidence is:

"this property looks unusual"

do not report an error.

---

# 16. FIRST INSPECTION

The first inspection should target the real-world problem.

It should detect a property path that appears to be an accidental structural deviation from a known/expected
configuration property.

Example:

KNOWN:

```yaml
spring.datasource.url
```

FOUND:

```yaml
spring.datasource.datasource.url
```

Possible inspection message:

"Suspicious configuration path."

The message must explain:

- the actual path;
- the related expected path;
- why the plugin considers the relationship suspicious.

Do not use vague messages such as:

"Invalid YAML."

The YAML may be perfectly valid.

---

# 17. INSPECTION IMPLEMENTATION

Use IntelliJ's inspection framework.

The implementation should integrate with:

LocalInspectionTool

or the appropriate current IntelliJ Platform inspection API.

The agent must verify the exact API for the selected IntelliJ Platform version.

The inspection should:

1. inspect the relevant YAML PSI;
2. identify the property;
3. obtain evidence;
4. decide whether the property is suspicious;
5. register a problem only when confidence is sufficient.

---

# 18. WARNING SEVERITY

Do not automatically use ERROR severity.

The initial implementation should prefer a warning-level inspection unless the evidence is strong enough to justify a
stronger severity.

The message should communicate uncertainty when appropriate.

For example:

"Suspicious configuration path"

is preferable to:

"Invalid configuration"

unless invalidity can actually be proven.

---

# 19. QUICK FIX

Implement a Quick Fix only when the intended transformation is deterministic.

Example:

FROM:

```yaml
spring.datasource.datasource.url
```

TO:

```yaml
spring.datasource.url
```

The Quick Fix must operate on PSI safely.

Do not perform unsafe global string replacement.

If there are multiple plausible fixes:

Do not automatically modify the file.

---

# 20. PROFILE ANALYSIS

Profile analysis is a SECONDARY feature.

Do not implement it before the basic real-world case works.

Potential example:

application.yml:

```yaml
server:
  port: 8080
```

application-local.yml:

```yaml
server:
  ports: 8081
```

This MAY be suspicious.

But:

A property existing only in application-local.yml is NOT automatically an error.

Do not implement the simplistic rule:

"If profile property is absent from base file, show warning."

That rule is invalid.

---

# 21. FALSE POSITIVE POLICY

False positives are one of the primary product risks.

The analyzer must prefer:

NO WARNING

over:

INCORRECT WARNING

when evidence is insufficient.

Create explicit negative tests for valid configurations that look unusual.

Examples:

- custom application properties;
- profile-specific properties;
- profile-only properties;
- nested configuration objects;
- lists;
- intentionally different structures.

---

# 22. TEST STRUCTURE

Create test fixtures such as:

testData/
correct/
real-world/
misplaced-section/
misplaced-datasource-url/
profile/
edge-cases/
false-positives/

Each inspection must have:

1. positive test;
2. negative test;
3. edge-case test;
4. regression test where applicable.

---

# 23. REQUIRED REAL-WORLD TESTS

Test 1:

A YAML section is accidentally nested one level deeper.

Expected:

The plugin identifies the suspicious property structure.

Test 2:

A datasource URL is accidentally nested one level deeper.

Expected:

The plugin identifies that the expected datasource property is not represented by the actual property path and
identifies the suspicious structure.

The exact detection mechanism must be based on evidence available to the plugin.

---

# 24. BUILD AND TEST RULE

After every meaningful implementation step:

Run:

- Gradle build;
- relevant tests;
- plugin sandbox/manual verification when necessary.

Do not claim that something works unless it has actually been tested.

When a bug is fixed:

Add a regression test whenever technically practical.

---

# 25. REAL PROJECT VALIDATION

After the first working inspection:

Use the plugin against a real Spring Boot project.

Test:

- application.yml;
- application-local.yml;
- application-test.yml;
- nested configuration;
- datasource configuration;
- custom application properties.

Record:

- detected problems;
- missed problems;
- false positives;
- crashes;
- performance problems.

Do not proceed to large feature expansion until the basic inspection behaves acceptably on real code.

---

# 26. PERFORMANCE

The plugin runs inside the IDE.

Therefore:

Do not perform expensive full-project analysis on every keystroke.

Use IntelliJ's inspection infrastructure and appropriate caching/indexing mechanisms where necessary.

Do not introduce optimization prematurely.

First measure.

If performance becomes a problem:

1. reproduce;
2. measure;
3. identify the expensive operation;
4. optimize that operation.

---

# 27. ERROR HANDLING

The plugin must never crash the IDE because of malformed configuration.

Handle safely:

- invalid YAML;
- incomplete YAML while user is typing;
- missing files;
- missing metadata;
- malformed values;
- unsupported configuration constructs.

The plugin should simply refrain from reporting an inspection when analysis is not reliable.

---

# 28. DOCUMENTATION

README must explain:

- what Config Doctor does;
- what it currently detects;
- supported configuration files;
- supported IntelliJ Platform version;
- installation;
- limitations;
- examples;
- known false-positive cases.

Do not advertise features that are not implemented.

---

# 29. GIT

Use small commits.

Examples:

feat: bootstrap IntelliJ plugin

feat: extract YAML property paths

feat: discover Spring application files

feat: create configuration model

feat: detect suspicious datasource path

test: add misplaced datasource regression

feat: add quick fix

fix: avoid false positive for profile-only property

Do not mix unrelated refactoring with feature development.

---

# 30. FIRST SPRINT

The first sprint is NOT "build the complete plugin."

The first sprint goal is to prove technical feasibility.

### Step 1

Inspect repository and environment.

### Step 2

Verify IntelliJ Platform APIs.

### Step 3

Create/run plugin sandbox.

### Step 4

Extract YAML property paths.

### Step 5

Create the real-world datasource fixture.

### Step 6

Implement the smallest possible detection experiment.

### Step 7

Add positive and negative tests.

### Step 8

Evaluate false positives.

At the end of Sprint 1 the agent MUST produce a short technical report:

- What works.
- What does not work.
- What API is being used.
- What evidence is available.
- How the real-world error is detected.
- What false positives were found.
- What remains technically uncertain.
- Recommended next step.

Do not automatically start building additional inspections.

---

# 31. MVP DEFINITION OF DONE

The MVP is complete when:

1. IntelliJ plugin builds.
2. Sandbox IDE starts.
3. YAML PSI analysis works.
4. Spring configuration files are recognized.
5. Property paths can be extracted.
6. The real-world misplaced configuration problem is detected.
7. The warning is understandable.
8. At least one deterministic Quick Fix works, if applicable.
9. Regression tests pass.
10. False-positive tests pass.
11. Plugin works against at least one real Spring Boot project.
12. README documents actual capabilities and limitations.

The MVP does NOT need:

- many inspections;
- AI;
- cloud;
- marketplace publication;
- monetization.

One reliable inspection is sufficient to validate the core technical hypothesis.

---

# 32. FUTURE FEATURES

Only consider these AFTER MVP validation:

### Phase 2

Profile-aware configuration analysis.

### Phase 3

Spring configuration metadata integration.

### Phase 4

Effective configuration analysis.

### Phase 5

Environment-variable relationships.

### Phase 6

Additional deterministic configuration diagnostics.

Each feature requires evidence that users actually need it.

---

# 33. DO NOT BUILD AI YET

Do not introduce an LLM into Config Doctor during MVP.

The first question is:

Can deterministic static analysis solve enough of the problem?

Only if deterministic analysis reaches a clear limitation should AI be considered.

If AI is considered later, it must not replace deterministic validation.

---

# 34. AGENT BEHAVIOUR

The agent must:

- verify facts;
- verify version-sensitive APIs;
- prefer official documentation;
- state uncertainty;
- avoid invented APIs;
- avoid invented Spring semantics;
- run tests;
- create regression tests;
- keep implementation small;
- stop when the technical hypothesis is disproven.

The agent must NOT:

- invent APIs;
- assume developer intent;
- create warnings without evidence;
- create a large framework before the first use case works;
- add features merely because they are technically interesting.

---

# 35. FINAL PRINCIPLE

Config Doctor is not a YAML formatter.

Config Doctor is not a generic linter.

Config Doctor is not an AI assistant.

Config Doctor is a focused static-analysis tool whose purpose is:

DETECT REAL SPRING BOOT CONFIGURATION MISTAKES BEFORE RUNTIME.

The primary engineering priorities are:

1. Correctness.
2. Low false-positive rate.
3. Useful diagnostics.
4. Safe Quick Fixes.
5. Simple architecture.
6. Real-world validation.
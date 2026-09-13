# Config Doctor

Config Doctor is an IntelliJ IDEA plugin for catching a specific class of Spring Boot YAML configuration mistakes before the application starts.

It is designed for the case where YAML is syntactically valid, but the property path is wrong because a section or key was accidentally moved one level deeper.

## What it does

The plugin scans Spring Boot configuration files in the project and inspects the logical property paths generated from YAML PSI.

It warns when a property path is suspicious because it structurally matches a real property elsewhere in the project after removing one nested segment.

Typical example:

```yaml
spring:
  datasource:
   datasource:
     url: jdbc:postgresql://localhost/db
```

The plugin compares that path against a known existing property such as:

```yaml
spring:
  datasource:
   url: jdbc:postgresql://localhost/db
```

and highlights the likely accidental extra nesting level.

## How it works

1. Discovers likely Spring Boot config files in the project.
2. Reads YAML through IntelliJ PSI instead of raw indentation parsing.
3. Builds dotted property paths like `spring.datasource.url`.
4. Compares those paths across project files and profile variants.
5. Reports a warning only when there is a concrete structural match.
6. Offers a Quick Fix only when the collapse is safe and unambiguous.

This makes it a narrow, deterministic inspection rather than a generic YAML linter.

## What it currently detects

The current implementation focuses on duplicated or shifted path segments such as:

- `spring.datasource.datasource.url`
- `spring.application.name` when `spring.name` already exists

It intentionally avoids guessing when there is not enough evidence.

## Supported files

The plugin recognizes Spring Boot-style configuration files such as:

- `application.yml`
- `application.yaml`
- `application-local.yml`
- `application-local.yaml`
- `application-prod.yml`
- `application-prod.yaml`
- dot-qualified variants like `application.ONLINE.yml`

## How to run

### Run tests

```bash
./gradlew test
```

### Run the sandbox IDE

```bash
./gradlew runIde
```

After startup, the plugin loads in the IntelliJ sandbox and logs a startup message for the active project.

## Author

Author: Aleksei Briukhachev

Repository: `AlekseiBriukhachev/config-doctor`

## Current scope and limitations

This plugin is intentionally narrow.

It does not attempt to be a full Spring Boot configuration resolver and does not use AI or external services. It focuses on a small, reliable warning category with low false-positive risk.

Current limitations include:

- no full Spring Boot property-resolution engine;
- no broad profile-merging simulation;
- no generic YAML linting;
- no warnings based only on vague “this looks unusual” heuristics;
- no AI features.

## Example usage

If a project contains:

`application.yml`
```yaml
spring:
  datasource:
   url: jdbc:postgresql://localhost/db
```

and a profile file contains:

`application-local.yml`
```yaml
spring:
  datasource:
   datasource:
     url: jdbc:postgresql://localhost/db
```

Config Doctor highlights the latter and explains that it likely duplicates the existing `spring.datasource.url` property.

## Validation

The project includes automated tests for:

- YAML path extraction;
- config file discovery;
- suspicious path detection;
- inspection behavior;
- quick-fix safety;
- realistic multi-file project scenarios.

Run the suite with:

```bash
./gradlew test
```

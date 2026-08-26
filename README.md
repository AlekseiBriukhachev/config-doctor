# Config Doctor

An IntelliJ IDEA plugin that will detect suspicious Spring Boot YAML
configuration structures (such as an accidentally duplicated nesting level)
before the application is started.

## Current status: Stage 2 — project skeleton

Nothing is analyzed yet. This build only proves that the plugin project
builds and loads inside an IntelliJ sandbox IDE with no startup errors.

- Target IDE: IntelliJ IDEA Community 2024.2 (`sinceBuild=242`, `untilBuild=251.*`)
- Language: Kotlin (2.0.21)
- Build tool: Gradle with IntelliJ Platform Gradle Plugin 2.18.1

## Running the sandbox IDE

```bash
./gradlew runIde
```

The sandbox IDE should start with no startup errors, and the IDE log
(`Help | Show Log`) should contain a line like:

```
Config Doctor plugin loaded for project: <your project name>
```

## Roadmap

See `AGENTS.md` for the full staged development plan. Nothing beyond the
Stage 2 skeleton is implemented yet.

# Unsupported Spring Boot configuration mechanisms (Stage 4)

Per AGENTS.md section 13: "Do not claim that these files represent the
complete Spring Boot configuration system. Document unsupported
configuration mechanisms." This file is that documentation.

`ConfigFileDiscovery` (Stage 4) only recognizes files named:

- `application.yml` / `application.yaml`
- `application-<profile>.yml` / `application-<profile>.yaml`

located anywhere IntelliJ's YAML file type index sees them in the project.

## What this deliberately does NOT detect

- **Custom `spring.config.name`.** If a project renames its base config
  file (e.g. `spring.config.name=myapp`), those files are invisible to
  this discovery - only the literal "application" base name is matched.
- **`.properties` files.** `application.properties`,
  `application-prod.properties`, etc. are a fully valid Spring Boot config
  format and are not analyzed at all yet.
- **`spring.config.import`.** Additional config files pulled in via this
  key (other classpath resources, config servers, Vault, etc.) are not
  followed or discovered.
- **Spring Cloud Config Server, Vault, or other externalized config
  sources.** Entirely out of scope - this plugin only sees files present
  in the local project.
- **Environment variables and JVM system properties**, which can override
  any YAML-defined value at runtime. The analyzer has no way to know these
  exist or what they contain.
- **Command-line arguments** (e.g. `--server.port=8081`), which also
  override YAML values at runtime.
- **In-file multi-document profile activation.** Spring Boot allows a
  single `application.yml` to contain multiple `---`-separated YAML
  documents, each activated via a `spring.config.activate.on-profile: X`
  key inside that document, instead of using a separate
  `application-X.yml` file. Profile identification here is based purely
  on the **file name** (see `ConfigFileName`) - a profile expressed this
  way inside a single file is not recognized as a profile by this stage.
- **Profile groups** (`spring.profiles.group.*`), which combine several
  profiles under one name.
- **Config files outside standard resource roots**, e.g. an external
  `/config` directory next to the packaged jar, or a location supplied via
  `--spring.config.location` at runtime.
- **YAML anchors, aliases, and merge keys** (`&`, `*`, `<<`), which can
  affect what value a key effectively resolves to.

Any of these may be added in a later stage, but none of them are
implemented, assumed, or silently ignored-as-if-equivalent right now.

package com.aleksei.configdoctor.plugin.model

/**
 * Parsed identity of a Spring Boot application configuration file.
 *
 * Examples include `application.yml`, `application-local.yaml`, and
 * `application.ONLINE.yml`. The object keeps the base name, optional qualifier
 * values and the effective profile separately from the file extension so the
 * analyzer can reason about profile-specific config without string-splitting at
 * every call site.
 */
data class ConfigFileName(
    val baseName: String,
    val step: String?,
    val appType: String?,
    val profile: String?,
    val extension: String
) {
    // Backwards-compatible constructor used in older tests/code that created
    // ConfigFileName(baseName, profile, extension). Delegate to the primary
    // constructor with null step/appType.
    constructor(baseName: String, profile: String?, extension: String) : this(baseName, null, null, profile, extension)

    companion object {
        // Only the conventional Spring Boot default base name is
        // recognized. See UNSUPPORTED.md: a custom spring.config.name is
        // not detected.

        /**
         * Parses a bare file name (no path) against the naming convention.
         *
         * Supported shapes:
         * - application.yml
         * - application-local.yml
         * - application.ONLINE.yml
         * - application.ONLINE.LOCAL.yml
         *
         * The dot-separated qualifiers are treated as a hierarchic override chain,
         * while the final qualifier becomes the effective profile name for
         * compatibility with the existing project model.
         */
        fun parse(fileName: String): ConfigFileName? {
            // Be case-insensitive for both the extension and the base name so files
            // like "application.DEFAULT.YML" or "Application-local.Yaml" are
            // recognized correctly.
            val lc = fileName.lowercase()
            val ext = when {
                lc.endsWith(".yaml") -> "yaml"
                lc.endsWith(".yml") -> "yml"
                else -> return null
            }

            val base = "application"
            // Remove the extension by length instead of relying on removeSuffix with exact case.
            val nameWithoutExt = fileName.substring(0, fileName.length - ext.length - 1)

            if (nameWithoutExt.equals(base, ignoreCase = true)) {
                return ConfigFileName(base, null, null, null, ext)
            }

            if (!nameWithoutExt.regionMatches(0, base, 0, base.length, ignoreCase = true)) {
                return null
            }

            val suffix = nameWithoutExt.substring(base.length)
            val qualifiers = when {
                suffix.startsWith("-") -> {
                    val profile = suffix.removePrefix("-")
                    if (profile.isBlank()) return null
                    listOf(profile.trim())
                }
                suffix.startsWith(".") -> {
                    val profileParts = suffix.removePrefix(".")
                        .split('.')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    if (profileParts.isEmpty()) return null
                    profileParts
                }
                suffix.isEmpty() -> emptyList()
                else -> return null
            }

            if (qualifiers.any { it.isBlank() }) return null

            // Preserve original qualifier casing (tests expect original case like "ONLINE"/"LOCAL").
            val profile = qualifiers.lastOrNull() ?: return null
            val step = qualifiers.dropLast(1).lastOrNull()
            val appType = qualifiers.dropLast(2).lastOrNull()

            return ConfigFileName(base, step?.takeIf { it.isNotBlank() }, appType?.takeIf { it.isNotBlank() }, profile, ext)
        }
    }
}

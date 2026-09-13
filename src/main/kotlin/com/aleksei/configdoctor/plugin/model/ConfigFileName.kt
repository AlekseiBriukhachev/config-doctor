package com.aleksei.configdoctor.plugin.model

/**
 * Parsed identity of a Spring Boot application configuration file name,
 * e.g. "application-prod.yml" -> baseName="application", profile="prod",
 * extension="yml".
 *
 * AGENTS.md section 13 (Stage 4) explicitly requires the profile to be
 * represented separately from the file name string, rather than code
 * elsewhere re-parsing "application-prod.yml" every time it needs to know
 * the profile. This class is that single parsing point.
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
            val ext = when {
                fileName.endsWith(".yaml") -> "yaml"
                fileName.endsWith(".yml") -> "yml"
                else -> return null
            }

            val base = "application"
            val nameWithoutExt = fileName.removeSuffix(".$ext")

            if (nameWithoutExt == base) {
                return ConfigFileName(base, null, null, null, ext)
            }

            if (!nameWithoutExt.startsWith(base)) {
                return null
            }

            val suffix = nameWithoutExt.removePrefix(base)
            val qualifiers = when {
                suffix.startsWith("-") -> {
                    val profile = suffix.removePrefix("-")
                    if (profile.isBlank()) return null
                    listOf(profile)
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

            val profile = qualifiers.lastOrNull() ?: return null
            val step = qualifiers.dropLast(1).lastOrNull()
            val appType = qualifiers.dropLast(2).lastOrNull()

            return ConfigFileName(base, step?.takeIf { it.isNotBlank() }, appType?.takeIf { it.isNotBlank() }, profile, ext)
        }
    }
}

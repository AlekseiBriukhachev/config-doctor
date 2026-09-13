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
        private val PATTERN = Regex("^(application)(?:\\.([A-Za-z0-9]+))?(?:\\.([A-Za-z0-9]+))?(?:-([A-Za-z0-9]+))?\\.(yml|yaml)$")

        /**
         * Parses a bare file name (no path) against the naming convention.
         * Returns null if it doesn't match - e.g. "config.yml",
         * "application.properties", or "application-local.txt".
         */
        fun parse(fileName: String): ConfigFileName? {
            val match = PATTERN.matchEntire(fileName) ?: return null
            val (base, step, appType, profile, ext) = match.destructured
            return ConfigFileName(base, step.ifEmpty { null }, appType.ifEmpty { null }, profile.ifEmpty { null }, ext)
        }
    }
}

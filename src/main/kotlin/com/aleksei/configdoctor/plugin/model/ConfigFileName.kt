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
    val profile: String?,
    val extension: String
) {
    companion object {
        // Only the conventional Spring Boot default base name is
        // recognized. See UNSUPPORTED.md: a custom spring.config.name is
        // not detected.
        private val PATTERN = Regex("^(application)(?:-([A-Za-z0-9]+))?\\.(yml|yaml)$")

        /**
         * Parses a bare file name (no path) against the naming convention.
         * Returns null if it doesn't match - e.g. "config.yml",
         * "application.properties", or "application-local.txt".
         */
        fun parse(fileName: String): ConfigFileName? {
            val match = PATTERN.matchEntire(fileName) ?: return null
            val (base, profile, ext) = match.destructured
            return ConfigFileName(base, profile.ifEmpty { null }, ext)
        }
    }
}

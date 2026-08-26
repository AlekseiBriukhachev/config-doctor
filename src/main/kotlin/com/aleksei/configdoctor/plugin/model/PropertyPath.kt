package com.aleksei.configdoctor.plugin.model

/**
 * A Spring Boot style dotted configuration property path, e.g.
 * "spring.datasource.url".
 *
 * Kept deliberately minimal per AGENTS.md section 12 ("Do not create a
 * large domain model prematurely"): this is Stage 3 material only - the
 * fuller ConfigProperty model (value, source file, PSI element, profile)
 * comes later, in Stage 4.
 */
data class PropertyPath(val segments: List<String>) {

    init {
        require(segments.isNotEmpty()) { "A property path must have at least one segment" }
    }

    override fun toString(): String = segments.joinToString(".")

    /** True if this path is exactly [other] or a parent of it. */
    fun isPrefixOf(other: PropertyPath): Boolean {
        if (segments.size > other.segments.size) return false
        return segments == other.segments.subList(0, segments.size)
    }

    companion object {
        fun of(vararg segments: String): PropertyPath = PropertyPath(segments.toList())
    }
}
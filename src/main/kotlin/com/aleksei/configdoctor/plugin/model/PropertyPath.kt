package com.aleksei.configdoctor.plugin.model

/**
 * Represents a dotted Spring-style configuration key, such as
 * `spring.datasource.url`.
 *
 * The model is intentionally small: it stores only the path segments and
 * helper methods used to compare one property path with another.
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
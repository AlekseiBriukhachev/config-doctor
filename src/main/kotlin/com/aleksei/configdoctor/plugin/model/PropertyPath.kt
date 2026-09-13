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

    companion object {
        fun of(vararg segments: String): PropertyPath = PropertyPath(segments.toList())
    }
}
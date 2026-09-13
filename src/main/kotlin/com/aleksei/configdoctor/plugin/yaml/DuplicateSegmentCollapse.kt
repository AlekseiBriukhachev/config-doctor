package com.aleksei.configdoctor.plugin.yaml

import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Stage 7 (AGENTS.md section 19): decides whether a suspicious path can be
 * safely and unambiguously collapsed by a Quick Fix.
 *
 * The same safety rule is used for both duplicate-segment and shifted-segment
 * mistakes: only a single wrapper layer is considered safe to collapse if the
 * outer key's mapping contains exactly one child. If the ancestor chain has no
 * such position, or more than one candidate position, this returns null and no
 * fix should be offered.
 */
/**
 * Stage 7 (AGENTS.md section 19): decides whether a suspicious path can be
 * safely and unambiguously collapsed by a Quick Fix.
 *
 * The same safety rule is used for both duplicate-segment and shifted-segment
 * mistakes: only a single wrapper layer is considered safe to collapse if the
 * outer key's mapping contains exactly one child. If the ancestor chain has no
 * such position, or more than one candidate position, this returns null and no
 * fix should be offered.
 */
object DuplicateSegmentCollapse {

    /**
     * Given the leaf key value that was flagged as suspicious, walks its
     * ancestor chain (root to leaf) looking for a safely collapsible nesting
     * layer.
     *
     * Returns (outerKeyValue, innerKeyValue): collapsing means replacing
     * outerKeyValue's mapping value with innerKeyValue's value, removing the
     * redundant nesting level entirely.
     */
    fun findSafeCollapse(leaf: YAMLKeyValue): Pair<YAMLKeyValue, YAMLKeyValue>? {
        val chain = YamlPropertyPaths.ancestorChainOf(leaf)
        var found: Pair<YAMLKeyValue, YAMLKeyValue>? = null

        for (i in 0 until chain.size - 1) {
            val outer = chain[i]
            val inner = chain[i + 1]

            if (outer.isDocumentRootWrapper()) continue // ignore the document-root wrapper

            val outerMapping = outer.value as? YAMLMapping ?: continue
            if (outerMapping.keyValues.singleOrNull() != inner) continue // has siblings -> unsafe
            if (inner.value !is YAMLMapping) continue // collapsing a scalar leaf would not remove a wrapper

            if (found != null) return null // a second candidate position -> ambiguous, refuse entirely
            found = outer to inner
        }

        return found
    }

    private fun YAMLKeyValue.isDocumentRootWrapper(): Boolean {
        var current = this.parent
        while (current != null) {
            if (current is YAMLKeyValue) return false
            if (current is YAMLDocument) return true
            current = current.parent
        }
        return true
    }
}
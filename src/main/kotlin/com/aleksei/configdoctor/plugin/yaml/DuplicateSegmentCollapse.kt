package com.aleksei.configdoctor.plugin.yaml

import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Stage 7 (AGENTS.md section 19): decides whether the duplicated-adjacent-
 * segment pattern found by SuspiciousPathDetector (Stage 5) can be safely
 * and unambiguously collapsed by a Quick Fix.
 *
 * A collapse at a given ancestor position is only considered SAFE when
 * the outer key's mapping contains that duplicate key as its ONLY child -
 * i.e. nothing else exists at that level that could be lost or would need
 * merging. If the ancestor chain contains no such position, or more than
 * one candidate position, this returns null and no fix should be offered:
 *
 *   AGENTS.md section 19: "If there are multiple plausible fixes: Do not
 *   automatically modify the file."
 *
 * Note this is deliberately independent of SuspiciousPathDetector: a path
 * can be flagged as suspicious (a real path collision exists) while still
 * having no *safe* fix (e.g. the duplicate level has sibling keys) - the
 * warning and the fix have different, separately-checked safety bars.
 */
object DuplicateSegmentCollapse {

    /**
     * Given the leaf key value that was flagged as suspicious, walks its
     * ancestor chain (root to leaf) looking for a single safely
     * collapsible duplicated segment.
     *
     * Returns (outerKeyValue, duplicateKeyValue): collapsing means
     * replacing outerKeyValue's mapping value with duplicateKeyValue's
     * value, removing the redundant nesting level entirely.
     */
    fun findSafeCollapse(leaf: YAMLKeyValue): Pair<YAMLKeyValue, YAMLKeyValue>? {
        val chain = YamlPropertyPaths.ancestorChainOf(leaf)
        var found: Pair<YAMLKeyValue, YAMLKeyValue>? = null

        for (i in 0 until chain.size - 1) {
            val outer = chain[i]
            val inner = chain[i + 1]
            if (outer.keyText != inner.keyText) continue

            val outerMapping = outer.value as? YAMLMapping ?: continue
            if (outerMapping.keyValues.singleOrNull() != inner) continue // has siblings -> unsafe

            if (found != null) return null // a second candidate position -> ambiguous, refuse entirely
            found = outer to inner
        }

        return found
    }
}
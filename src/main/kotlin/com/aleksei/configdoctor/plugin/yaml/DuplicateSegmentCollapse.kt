package com.aleksei.configdoctor.plugin.yaml

import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Decides whether a suspicious YAML key can be fixed by removing one redundant
 * nesting layer.
 *
 * The fix is only offered when there is exactly one unambiguous wrapper node
 * in the ancestor chain. If the parent mapping has siblings or there are
 * multiple collapse candidates, the method returns null and the inspection does
 * not suggest a rewrite.
 */
object DuplicateSegmentCollapse {

    /**
     * Walks the ancestor chain of a flagged key and returns the single safe
     * collapse point, if any.
     *
     * The returned pair is `(outerKeyValue, innerKeyValue)`: the outer mapping
     * is the redundant wrapper that can be replaced by the inner value.
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
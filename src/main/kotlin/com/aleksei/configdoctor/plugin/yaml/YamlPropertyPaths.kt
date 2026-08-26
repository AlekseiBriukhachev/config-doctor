package com.aleksei.configdoctor.plugin.yaml

import com.aleksei.configdoctor.plugin.model.PropertyPath
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLValue

/**
 * Converts YAML PSI structure into Spring Boot-style dotted property paths.
 *
 * AGENTS.md, section 11 (Stage 3), requires this to use IntelliJ's PSI/YAML
 * API rather than parsing raw text or indentation. This class does exactly
 * that: it only ever reads YAMLKeyValue.keyText and walks PSI parent links
 * (YAMLKeyValue -> YAMLMapping -> YAMLKeyValue -> ...). It never inspects
 * whitespace, column offsets, or the document's raw text.
 *
 * Scope note (see AGENTS.md section 6, OUT OF SCOPE): YAML sequences
 * (lists) are not expanded into indexed paths here. A key whose value is a
 * YAMLSequence is currently treated as a leaf and its path is reported,
 * but the list's contents are not walked. Spring Boot's list-binding rules
 * are more involved than this MVP needs to model yet.
 */
object YamlPropertyPaths {

    /**
     * Builds the full property path for a single YAMLKeyValue by walking up
     * through its enclosing YAMLMapping/YAMLKeyValue ancestors.
     *
     * Example - given:
     * ```
     * spring:
     *   datasource:
     *     url: jdbc:postgresql://localhost/db
     * ```
     * calling this on the "url" YAMLKeyValue returns the path
     * "spring.datasource.url".
     */
    fun pathOf(keyValue: YAMLKeyValue): PropertyPath {
        val segments = ArrayList<String>()
        var current: YAMLKeyValue? = keyValue
        while (current != null) {
            segments.add(current.keyText)
            current = current.enclosingKeyValue()
        }
        segments.reverse()
        return PropertyPath(segments)
    }

    /**
     * Walks an entire YAML file and returns the property path for every
     * "leaf" key: a key whose value is a scalar, a sequence, or missing/
     * empty. Keys whose value is itself a mapping are internal nodes and
     * are not returned individually - only their leaves are, since those
     * are the actual configuration properties Spring Boot binds to.
     *
     * A YAML file may contain multiple documents (separated by `---`);
     * all of them are walked.
     */
    fun leafPathsOf(file: YAMLFile): List<PropertyPath> {
        val result = ArrayList<PropertyPath>()
        for (document in file.documents) {
            val topMapping = document.topLevelValue as? YAMLMapping ?: continue
            collectLeaves(topMapping, result)
        }
        return result
    }

    private fun collectLeaves(mapping: YAMLMapping, out: ArrayList<PropertyPath>) {
        for (keyValue in mapping.keyValues) {
            when (val value: YAMLValue? = keyValue.value) {
                is YAMLMapping -> collectLeaves(value, out)
                else -> out.add(pathOf(keyValue))
            }
        }
    }

    /**
     * The nearest enclosing YAMLKeyValue one level up in the YAML
     * hierarchy - i.e. this key's parent key. Returns null once we reach
     * the document root (no more enclosing key).
     */
    private fun YAMLKeyValue.enclosingKeyValue(): YAMLKeyValue? {
        var candidate = this.parent
        while (candidate != null) {
            if (candidate is YAMLKeyValue) return candidate
            // Stop climbing once we leave the mapping structure entirely.
            if (candidate is YAMLDocument) return null
            candidate = candidate.parent
        }
        return null
    }
}
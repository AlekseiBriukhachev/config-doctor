package com.aleksei.configdoctor.plugin.yaml

import com.aleksei.configdoctor.plugin.model.ConfigFile
import com.aleksei.configdoctor.plugin.model.ConfigProperty
import com.aleksei.configdoctor.plugin.model.PropertyPath
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLValue

/**
 * Builds dotted property paths from IntelliJ YAML PSI objects.
 *
 * Instead of reading indentation or raw text, it walks the actual YAML key
 * hierarchy using `YAMLKeyValue` and `YAMLMapping` parent links. This makes the
 * extracted path match the real structure the IDE is representing.
 */
object YamlPropertyPaths {

    /**
     * Returns the full dotted path for a single YAML key, including all
     * enclosing mapping keys.
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
     * Returns the ancestor chain from the outermost mapping key to this key.
     * This is useful when a quick fix needs to locate the exact PSI node that
     * corresponds to a particular path segment.
     */
    fun ancestorChainOf(keyValue: YAMLKeyValue): List<YAMLKeyValue> {
        val chain = ArrayList<YAMLKeyValue>()
        var current: YAMLKeyValue? = keyValue
        while (current != null) {
            chain.add(current)
            current = current.enclosingKeyValue()
        }
        chain.reverse()
        return chain
    }

    /**
     * Collects the dotted path for every leaf property in a YAML file.
     *
     * A leaf is a key whose value is a scalar, an empty value, or a list; keys
     * containing nested mappings are treated as parent sections instead of being
     * reported as standalone properties.
     */
    fun leafPathsOf(file: YAMLFile): List<PropertyPath> =
        leafKeyValuesOf(file).map { pathOf(it) }

    /**
     * Returns the YAML key/value PSI nodes for each leaf property in the file.
     * Callers can use this to access the original PSI element and the raw value
     * text while also building a logical property path.
     */
    fun leafKeyValuesOf(file: YAMLFile): List<YAMLKeyValue> {
        val result = ArrayList<YAMLKeyValue>()
        for (document in file.documents) {
            val topMapping = document.topLevelValue as? YAMLMapping ?: continue
            collectLeafKeyValues(topMapping, result)
        }
        return result
    }

    private fun collectLeafKeyValues(mapping: YAMLMapping, out: MutableList<YAMLKeyValue>) {
        for (keyValue in mapping.keyValues) {
            when (val value: YAMLValue? = keyValue.value) {
                is YAMLMapping -> collectLeafKeyValues(value, out)
                else -> out.add(keyValue)
            }
        }
    }

    /**
     * Finds the nearest parent YAML key above this key within the same mapping
     * structure. Returns null once the document root is reached.
     */
    private fun YAMLKeyValue.enclosingKeyValue(): YAMLKeyValue? {
        var candidate = this.parent
        while (candidate != null) {
            if (candidate is YAMLKeyValue) return candidate
            if (candidate is YAMLDocument) return null
            candidate = candidate.parent
        }
        return null
    }

    /**
     * Converts every leaf YAML key in a file into a `ConfigProperty` with the
     * source file, profile and PSI element attached.
     */
    fun leafConfigPropertiesOf(
        configFile: ConfigFile,
        yamlFile: YAMLFile
    ): List<ConfigProperty> =
        leafKeyValuesOf(yamlFile).map { keyValue ->
            ConfigProperty(
                path = pathOf(keyValue),
                value = keyValue.valueText.ifEmpty { null },
                sourceFile = configFile.virtualFile,
                psiElement = keyValue,
                profile = configFile.profile
            )
        }
}
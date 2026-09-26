package com.aleksei.configdoctor.plugin.metadata

import com.aleksei.configdoctor.plugin.model.PropertyPath
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Lightweight loader for Spring Boot "spring-configuration-metadata.json" files
 * found in the project. The loader extracts property names from the JSON file
 * using a simple, robust pattern and returns them as PropertyPath instances.
 *
 * This intentionally avoids pulling a full JSON dependency; the metadata file
 * has a well-known shape and extracting the "name" fields is sufficient for
 * evidence-based detection.
 */
data class MetadataEntry(
    val name: String,
    val type: String?,
    val description: String?,
    val defaultValue: String?,
    val deprecatedInfo: String?
)

object SpringConfigurationMetadata {

    private val fileName = "spring-configuration-metadata.json"

    /**
     * Loads a map from property path -> metadata entry (type + description) by
     * scanning spring-configuration-metadata.json files found in project and
     * in dependencies.
     */
    fun loadKnownPropertyMetadata(project: Project): Map<PropertyPath, MetadataEntry> {
        val scope = GlobalSearchScope.everythingScope(project)
        val files = FilenameIndex.getVirtualFilesByName(project, fileName, scope)
        val result = mutableMapOf<PropertyPath, MetadataEntry>()

        // Pattern to capture property objects roughly: use DOTALL via (?s)
        val objRegex = "(?s)\\{.*?\"name\"\\s*:\\s*\"([^\"]+)\".*?\\}".toRegex()
        val typeRegex = "\"type\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val descRegex = "\"description\"\\s*:\\s*\"([^\"]+)\"".toRegex()

        for (vf in files) {
            try {
                val text = VfsUtilCore.loadText(vf)
                val json = com.google.gson.JsonParser.parseString(text)
                val props = json.asJsonObject.getAsJsonArray("properties") ?: continue
                for (el in props) {
                    try {
                        val obj = el.asJsonObject
                        val name = obj.getAsJsonPrimitive("name")?.asString?.trim() ?: continue
                        val type = obj.getAsJsonPrimitive("type")?.asString
                        val desc = obj.getAsJsonPrimitive("description")?.asString
                        val defaultVal = obj.getAsJsonPrimitive("defaultValue")?.asString
                        var deprecatedInfo: String? = null
                        if (obj.has("deprecated")) {
                            try {
                                val deprecatedEl = obj.get("deprecated")
                                if (deprecatedEl.isJsonPrimitive && deprecatedEl.asJsonPrimitive.isBoolean) {
                                    if (deprecatedEl.asBoolean) deprecatedInfo = "deprecated"
                                } else if (deprecatedEl.isJsonObject) {
                                    deprecatedInfo = deprecatedEl.asJsonObject.getAsJsonPrimitive("reason")?.asString
                                }
                            } catch (_: Throwable) {
                            }
                        } else if (obj.has("deprecation")) {
                            try {
                                val depObj = obj.getAsJsonObject("deprecation")
                                deprecatedInfo = depObj.getAsJsonPrimitive("reason")?.asString
                            } catch (_: Throwable) {
                            }
                        }
                        val segments = name.split('.').map { it.trim() }.filter { it.isNotEmpty() }
                        if (segments.isNotEmpty()) {
                            result[PropertyPath(segments)] = MetadataEntry(name, type, desc, defaultVal, deprecatedInfo)
                        }
                    } catch (_: Throwable) {
                        // skip malformed entry
                    }
                }
            } catch (e: Exception) {
                // Ignore malformed files and continue
            }
        }

        return result
    }

    // Backwards-compatible loader that returns just the set of property paths
    fun loadKnownPropertyPaths(project: Project): Set<PropertyPath> = loadKnownPropertyMetadata(project).keys
}

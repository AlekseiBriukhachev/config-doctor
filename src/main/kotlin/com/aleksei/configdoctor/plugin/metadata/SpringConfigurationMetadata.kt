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
object SpringConfigurationMetadata {

    private val fileName = "spring-configuration-metadata.json"

    fun loadKnownPropertyPaths(project: Project): Set<PropertyPath> {
        val scope = GlobalSearchScope.projectScope(project)
        val files = FilenameIndex.getVirtualFilesByName(project, fileName, scope)
        val result = mutableSetOf<PropertyPath>()

        for (vf in files) {
            try {
                val text = VfsUtilCore.loadText(vf)
                // Find occurrences of "name" : "some.property.path"
                val regex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                for (m in regex.findAll(text)) {
                    val name = m.groupValues[1].trim()
                    if (name.isNotEmpty()) {
                        val segments = name.split('.').map { it.trim() }.filter { it.isNotEmpty() }
                        if (segments.isNotEmpty()) result.add(PropertyPath(segments))
                    }
                }
            } catch (e: Exception) {
                // Be conservative: ignore malformed files rather than failing.
            }
        }

        return result
    }
}

package com.aleksei.configdoctor.plugin.yaml

import com.aleksei.configdoctor.plugin.model.ConfigFile
import com.aleksei.configdoctor.plugin.model.ConfigFileName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.YAMLFileType

/**
 * Finds YAML files in the project that look like Spring Boot application
 * configuration files and converts them into `ConfigFile` entries.
 *
 * This is intentionally a lightweight discovery step: it matches file names
 * against the standard `application*.yml` / `application*.yaml` patterns and
 * relies on IntelliJ's indexed file search instead of manually walking the
 * project tree.
 */
object ConfigFileDiscovery {

    fun findConfigFiles(
        project: Project,
        scope: GlobalSearchScope = GlobalSearchScope.projectScope(project)
    ): List<ConfigFile> {
        val yamlFiles = FileTypeIndex.getFiles(YAMLFileType.YML, scope)
        return yamlFiles.mapNotNull { virtualFile ->
            ConfigFileName.parse(virtualFile.name)?.let { name -> ConfigFile(virtualFile, name) }
        }
    }
}

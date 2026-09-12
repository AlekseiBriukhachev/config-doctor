package com.aleksei.configdoctor.plugin.yaml

import com.aleksei.configdoctor.plugin.model.ConfigFile
import com.aleksei.configdoctor.plugin.model.ConfigFileName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.YAMLFileType

/**
 * Locates Spring Boot application configuration files within a project
 * (AGENTS.md section 13, Stage 4).
 *
 * This only *identifies* files by name using IntelliJ's file type index
 * (fast, incremental, no manual directory walking). It does not attempt
 * to resolve Spring Boot's full configuration model - see UNSUPPORTED.md
 * for what is deliberately not covered yet.
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

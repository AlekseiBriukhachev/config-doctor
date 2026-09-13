package com.aleksei.configdoctor.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * A single YAML property extracted from a Spring Boot configuration file.
 *
 * The model keeps only the pieces that the analyzer actually needs while
 * working on a file: the property path, the scalar value, the source file,
 * the PSI node for navigation/highlighting, and the profile name when the file
 * is profile-specific.
 */
data class ConfigProperty(
    val path: PropertyPath,
    val value: String?,
    val sourceFile: VirtualFile,
    val psiElement: PsiElement,
    val profile: String?
)

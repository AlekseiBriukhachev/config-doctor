package com.aleksei.configdoctor.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * Minimal internal representation of a single resolved configuration
 * property (AGENTS.md section 12).
 *
 * Deliberately small - only what the analyzer actually needs right now:
 * - [path]: the dotted property path (Stage 3, YamlPropertyPaths)
 * - [value]: the raw scalar text, if the key has one (null for empty/
 *   section-only keys)
 * - [sourceFile]: which physical file this came from
 * - [psiElement]: the underlying YAMLKeyValue, kept as plain PsiElement so
 *   this model class doesn't need a YAML-plugin import - useful later if
 *   an inspection needs to navigate to / highlight this exact element
 * - [profile]: which Spring profile this property belongs to, if any
 *   (null = the default/base application.yml, not "no profile system
 *   exists" - see ConfigFileName and UNSUPPORTED.md)
 *
 * No value resolution, override/merge logic across profiles, or type
 * coercion is attempted here - that is out of scope for now.
 */
data class ConfigProperty(
    val path: PropertyPath,
    val value: String?,
    val sourceFile: VirtualFile,
    val psiElement: PsiElement,
    val profile: String?
)

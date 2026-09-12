package com.aleksei.configdoctor.plugin.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * A configuration file recognized by Config Doctor, paired with its
 * parsed name identity.
 */
data class ConfigFile(
    val virtualFile: VirtualFile,
    val name: ConfigFileName
) {
    /** Null means this is the base/default file (application.yml), not that profiles are unsupported. */
    val profile: String? get() = name.profile
}

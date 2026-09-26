package com.aleksei.configdoctor.plugin.inspection

import com.aleksei.configdoctor.plugin.metadata.MetadataEntry
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class ShowPropertyMetadataQuickFix(private val metadata: MetadataEntry) : LocalQuickFix {

    override fun getName(): String = "Show property documentation"

    override fun getFamilyName(): String = "Show property documentation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val sb = StringBuilder()
        sb.append("Property: ${metadata.name}")
        metadata.type?.let { sb.append("\nType: $it") }
        metadata.defaultValue?.let { sb.append("\nDefault: $it") }
        metadata.deprecatedInfo?.let { sb.append("\nDeprecated: $it") }
        metadata.description?.let { sb.append("\n\n$it") }

        Messages.showInfoMessage(project, sb.toString(), "Property documentation: ${metadata.name}")
    }
}

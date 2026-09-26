package com.aleksei.configdoctor.plugin.inspection

import com.aleksei.configdoctor.plugin.analysis.EvidenceKind
import com.aleksei.configdoctor.plugin.analysis.SuspiciousPathDetector
import com.aleksei.configdoctor.plugin.analysis.SuspiciousPathFinding
import com.aleksei.configdoctor.plugin.model.ConfigProperty
import com.aleksei.configdoctor.plugin.yaml.ConfigFileDiscovery
import com.aleksei.configdoctor.plugin.yaml.YamlPropertyPaths
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import com.aleksei.configdoctor.plugin.yaml.DuplicateSegmentCollapse

/**
 * Highlights YAML keys that look like an accidental extra nesting level or a
 * duplicated path segment.
 *
 * The inspection gathers all Spring configuration properties in the project,
 * compares their paths with a lightweight suspicious-path detector, and warns
 * only when there is a concrete match that explains the problem.
 */
class SuspiciousConfigPathInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val findings: List<SuspiciousPathFinding> by lazy {
            val known = com.aleksei.configdoctor.plugin.metadata.SpringConfigurationMetadataService
                .getInstance(project)
                .knownPaths()
            SuspiciousPathDetector.findSuspiciousPaths(collectAllConfigProperties(project), known)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val keyValue = element as? YAMLKeyValue ?: return
                // Only leaf keys are treated as properties; a section key's
                // value is itself a mapping and is not a standalone config item.
                if (keyValue.value is YAMLMapping) return

                val finding = findings.firstOrNull { it.actual.psiElement == keyValue } ?: return

                val metadataEntry = if (finding.evidenceKind == EvidenceKind.SPRING_METADATA) {
                    com.aleksei.configdoctor.plugin.metadata.SpringConfigurationMetadataService
                        .getInstance(project)
                        .knownMetadata()[finding.relatedExpected.path]
                } else null

                val primaryFix = when (finding.evidenceKind) {
                    EvidenceKind.PROFILE_OVERRIDE_RELATIONSHIP -> {
                        val expectedLeaf = finding.relatedExpected.path.segments.lastOrNull()
                        if (expectedLeaf != null && expectedLeaf != keyValue.keyText) {
                            ReplaceProfileMismatchKeyFix(keyValue, expectedLeaf)
                        } else {
                            null
                        }
                    }
                    else -> if (DuplicateSegmentCollapse.findSafeCollapse(keyValue) != null) {
                        CollapseDuplicatedSegmentFix()
                    } else {
                        null
                    }
                }

                val docFix = metadataEntry?.let { ShowPropertyMetadataQuickFix(it) }

                holder.registerProblem(
                    keyValue,
                    buildMessage(finding, project),
                    ProblemHighlightType.WARNING,
                    *listOfNotNull(primaryFix, docFix).toTypedArray()
                )
            }
        }
    }

    /**
     * Builds a warning message that names both the suspicious path and the known
     * property it appears to correspond to.
     */
    private fun buildMessage(finding: SuspiciousPathFinding, project: Project): String {
        val metadataEntry = if (finding.evidenceKind == EvidenceKind.SPRING_METADATA) {
            com.aleksei.configdoctor.plugin.metadata.SpringConfigurationMetadataService
                .getInstance(project)
                .knownMetadata()[finding.relatedExpected.path]
        } else null

        return when (finding.evidenceKind) {
            EvidenceKind.PROFILE_OVERRIDE_RELATIONSHIP ->
                "This property does not override '${finding.relatedExpected.path}'. " +
                    "The profile value '${finding.actual.path}' belongs to the same parent path but is a different leaf key. " +
                        finding.evidence
            EvidenceKind.SPRING_METADATA -> {
                val sb = StringBuilder()
                sb.append("Suspicious configuration path '${finding.actual.path}'. ")
                sb.append("Removing the misplaced segment yields '${finding.relatedExpected.path}', which is a known Spring Boot property.")
                metadataEntry?.let {
                    sb.append("\nProperty: ${it.name}")
                    it.type?.let { t -> sb.append(" (type: $t)") }
                    it.defaultValue?.let { dv -> sb.append("\nDefault: $dv") }
                    it.deprecatedInfo?.let { dp -> sb.append("\nDeprecated: $dp") }
                    it.description?.let { d -> sb.append(" \n— $d") }
                }
                sb.append(" ${finding.evidence}")
                sb.toString()
            }
            else ->
                "Suspicious configuration path '${finding.actual.path}'. " +
                    "It may be an accidental misplacement or duplication of the existing property " +
                    "'${finding.relatedExpected.path}'. ${finding.evidence}"
        }
    }

    private fun collectAllConfigProperties(project: Project): List<ConfigProperty> {
        val psiManager = PsiManager.getInstance(project)
        return ConfigFileDiscovery.findConfigFiles(project).flatMap { configFile ->
            val yamlFile = psiManager.findFile(configFile.virtualFile) as? YAMLFile
                ?: return@flatMap emptyList()
            YamlPropertyPaths.leafConfigPropertiesOf(configFile, yamlFile)
        }
    }
}

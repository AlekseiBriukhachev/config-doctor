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
            SuspiciousPathDetector.findSuspiciousPaths(collectAllConfigProperties(project))
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val keyValue = element as? YAMLKeyValue ?: return
                // Only leaf keys are treated as properties; a section key's
                // value is itself a mapping and is not a standalone config item.
                if (keyValue.value is YAMLMapping) return

                val finding = findings.firstOrNull { it.actual.psiElement == keyValue } ?: return

                val fix = when (finding.evidenceKind) {
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

                holder.registerProblem(
                    keyValue,
                    buildMessage(finding),
                    ProblemHighlightType.WARNING,
                    *listOfNotNull(fix).toTypedArray()
                )
            }
        }
    }

    /**
     * Builds a warning message that names both the suspicious path and the known
     * property it appears to correspond to.
     */
    private fun buildMessage(finding: SuspiciousPathFinding): String {
        return when (finding.evidenceKind) {
            EvidenceKind.PROFILE_OVERRIDE_RELATIONSHIP ->
                "This property does not override '${finding.relatedExpected.path}'. " +
                    "The profile value '${finding.actual.path}' belongs to the same parent path but is a different leaf key. " +
                        finding.evidence
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

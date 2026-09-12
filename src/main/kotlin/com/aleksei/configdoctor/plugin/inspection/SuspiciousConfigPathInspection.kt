package com.aleksei.configdoctor.plugin.inspection

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
 * Stage 6 (AGENTS.md section 17): the first real IntelliJ inspection.
 * Wires together Stage 3 (property-path extraction), Stage 4
 * (configuration file discovery) and Stage 5 (the duplicated-segment
 * detector) so problems are actually highlighted in the editor.
 *
 * API note (section 17 requires verifying the exact API for the target
 * platform version): `LocalInspectionTool.buildVisitor(ProblemsHolder,
 * Boolean): PsiElementVisitor` was confirmed against the current
 * JetBrains/intellij-community source for the 2023.2-2024.2 range - the
 * signature has not changed there. This visitor deliberately checks
 * `element is YAMLKeyValue` on a plain PsiElementVisitor rather than
 * extending a YAML-specific visitor base class, since that class's exact
 * name/package was not independently re-verified - a generic
 * PsiElementVisitor is guaranteed to exist regardless of platform
 * version or YAML plugin internals.
 *
 * Follows the 5-step process from section 17 explicitly:
 *   1. inspect the relevant YAML PSI       -> visitElement / is YAMLKeyValue
 *   2. identify the property               -> YamlPropertyPaths.pathOf
 *   3. obtain evidence                     -> SuspiciousPathDetector
 *   4. decide whether it's suspicious      -> match against detector findings
 *   5. register a problem only when confident -> only on an actual finding
 *
 * Severity note (section 18): registers at WARNING, never ERROR - the
 * YAML here is syntactically valid, we are only flagging a suspected
 * accidental structural deviation.
 *
 * Known limitation (not addressed at this stage): findings are
 * recomputed by re-scanning the whole project once per file visited,
 * rather than being cached/shared across a single analysis pass. This is
 * a correctness-first prototype (section 16: "prove the core problem is
 * detectable"); performance tuning belongs to a later regression/
 * real-project-validation stage.
 */
class SuspiciousConfigPathInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val findings: List<SuspiciousPathFinding> by lazy {
            SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllConfigProperties(project))
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val keyValue = element as? YAMLKeyValue ?: return
                // Only leaf keys are properties (same notion as Stage 3);
                // a section key's value is itself a mapping.
                if (keyValue.value is YAMLMapping) return

                val finding = findings.firstOrNull { it.actual.psiElement == keyValue } ?: return

                // Section 19: only offer the fix when a single, unambiguous, lossless
                // collapse position was found - independent from "is it suspicious".
                val fix = DuplicateSegmentCollapse.findSafeCollapse(keyValue)?.let { (outer, duplicate) ->
                    CollapseDuplicatedSegmentFix(outer, duplicate)
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
     * AGENTS.md section 16: the message must explain the actual path, the
     * related expected path, and WHY the relationship is suspicious - not
     * a vague "Invalid YAML." (the YAML here is perfectly valid).
     */
    private fun buildMessage(finding: SuspiciousPathFinding): String {
        return "Suspicious configuration path '${finding.actual.path}'. " +
            "It may be an accidental duplication of the existing property " +
            "'${finding.relatedExpected.path}'. ${finding.evidence}"
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

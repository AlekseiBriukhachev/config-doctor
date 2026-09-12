package com.aleksei.configdoctor.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Stage 7 (AGENTS.md section 19): collapses a duplicated adjacent
 * configuration segment. Turns:
 *
 *   spring:
 *     datasource:
 *       datasource:
 *         url: jdbc:...
 *
 * into:
 *
 *   spring:
 *     datasource:
 *       url: jdbc:...
 *
 * Only ever constructed when DuplicateSegmentCollapse.findSafeCollapse
 * found exactly one unambiguous, lossless collapse position - see that
 * class for the safety condition.
 *
 * Operates on PSI (structural replace + reformat), never on raw document
 * text, per section 19: "must operate on PSI safely... Do not perform
 * unsafe global string replacement."
 *
 * Uses SmartPsiElementPointer rather than holding the YAMLKeyValue
 * references directly, since a Quick Fix can be applied after the
 * document has changed since the inspection ran (e.g. user typed
 * something else first) - the pointer stays valid or safely resolves to
 * null through such changes, whereas a raw PSI reference would not.
 */
class CollapseDuplicatedSegmentFix(
    outerKeyValue: YAMLKeyValue,
    duplicateKeyValue: YAMLKeyValue
) : LocalQuickFix {

    private val outerPointer: SmartPsiElementPointer<YAMLKeyValue> =
        SmartPointerManager.createPointer(outerKeyValue)
    private val duplicatePointer: SmartPsiElementPointer<YAMLKeyValue> =
        SmartPointerManager.createPointer(duplicateKeyValue)

    override fun getFamilyName(): String = "Collapse duplicated configuration segment"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val outer = outerPointer.element ?: return
        val duplicate = duplicatePointer.element ?: return

        // Re-validate at apply time: the file may have changed since the
        // inspection last ran. Section 19 requires this to stay safe and
        // deterministic - never assume stale analysis state is still valid.
        val outerMapping = outer.value as? YAMLMapping ?: return
        if (outerMapping.keyValues.singleOrNull() != duplicate) return

        val outerValue = outer.value ?: return
        val duplicateValue = duplicate.value ?: return

        // .copy() before replace(): duplicateValue is a descendant of the
        // very node being replaced, so it must be detached/copied first
        // rather than transplanted directly.
        val replaced = outerValue.replace(duplicateValue.copy())

        // The collapsed content keeps its original (one-level-too-deep)
        // indentation after a raw PSI replace; reformat fixes that rather
        // than leaving structurally-correct-but-misindented YAML.
        CodeStyleManager.getInstance(project).reformat(replaced)
    }
}
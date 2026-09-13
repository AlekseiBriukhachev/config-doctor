package com.aleksei.configdoctor.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.aleksei.configdoctor.plugin.yaml.DuplicateSegmentCollapse
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Quick fix that removes one redundant nesting level from a duplicated YAML
 * segment.
 *
 * The fix reconstructs the YAML fragment with the correct indentation and
 * replaces the outer key/value node with the collapsed version. It only runs
 * after the code has re-validated that the duplication is still structurally
 * safe to collapse.
 */
class CollapseDuplicatedSegmentFix : LocalQuickFix {

    override fun getFamilyName(): String = "Collapse duplicated configuration segment"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val keyValue = descriptor.psiElement as? YAMLKeyValue ?: return
        val collapse = DuplicateSegmentCollapse.findSafeCollapse(keyValue) ?: return
        val outer = collapse.first
        val duplicate = collapse.second

        // Re-validate at apply time: the file may have changed since the
        // inspection last ran. This keeps the fix safe and deterministic.
        val outerMapping = outer.value as? YAMLMapping ?: return
        if (outerMapping.keyValues.singleOrNull() != duplicate) return

        val duplicateValue = duplicate.value ?: return

        val outerColumn = columnOf(outer)
        val unitIndent = columnOf(duplicate) - outerColumn
        if (unitIndent <= 0) return

        val valueLines = duplicateValue.text.lines()
        val newValueLines = valueLines.mapIndexed { index, line ->
            when {
                line.isBlank() -> line
                index == 0 -> " ".repeat(outerColumn + unitIndent) + line
                else -> {
                    val leading = line.takeWhile { it == ' ' }.length
                    " ".repeat(maxOf(leading - unitIndent, 0)) + line.trimStart(' ')
                }
            }
        }

        val newFragmentText = "${outer.keyText}:\n" + newValueLines.joinToString("\n")

        val generator = YAMLElementGenerator.getInstance(project)
        val dummyFile = generator.createDummyYamlWithText(newFragmentText)
        val newOuterKeyValue = PsiTreeUtil.findChildOfType(dummyFile, YAMLKeyValue::class.java) ?: return

        outer.replace(newOuterKeyValue)
    }

    /** The column (0-based) at which [element] starts on its own source line. */
    private fun columnOf(element: PsiElement): Int {
        val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
            ?: return 0
        val lineNumber = document.getLineNumber(element.textOffset)
        return element.textOffset - document.getLineStartOffset(lineNumber)
    }
}
package com.aleksei.configdoctor.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
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

        val duplicateValue = duplicate.value ?: return

        // How many columns one indent level is in THIS file, measured from
        // the actual two real key lines - not assumed to be 2 or 4 spaces.
        val outerColumn = columnOf(outer)
        val unitIndent = columnOf(duplicate) - outerColumn
        if (unitIndent <= 0) return // can't determine a safe single indent unit - refuse rather than guess

        // duplicateValue.text's own first line never includes its leading
        // indentation (that whitespace belongs to the "duplicate:" key
        // line that precedes it, i.e. it is outside duplicateValue's own
        // PSI range) - it must be added back explicitly, using the real
        // absolute column this content needs once promoted one level
        // above outer. Every subsequent line DOES already carry its own
        // real absolute indentation as literal text, so those only need
        // to be dedented by exactly one indent unit.
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
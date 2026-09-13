package com.aleksei.configdoctor.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLKeyValue

class ReplaceProfileMismatchKeyFix(
    keyValue: YAMLKeyValue,
    private val replacementKey: String
) : LocalQuickFix {

    private val originalKeyText: String = keyValue.keyText

    override fun getName(): String = "Replace '${originalKeyText}' with '${replacementKey}'"

    override fun getFamilyName(): String = "Replace mismatched profile key"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val current = descriptor.psiElement as? YAMLKeyValue ?: return
        val valueText = current.value?.text ?: ""
        val replacementText = "$replacementKey: $valueText".trimEnd()

        val generator = YAMLElementGenerator.getInstance(project)
        val dummyFile = generator.createDummyYamlWithText(replacementText)
        val replacementKeyValue = PsiTreeUtil.findChildOfType(dummyFile, YAMLKeyValue::class.java) ?: return

        current.replace(replacementKeyValue)
    }
}

package com.aleksei.configdoctor

import com.github.weisj.jsvg.e
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.util.*

class GenerateFolderStructureAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val dialog = InputDialog()
        dialog.show()
        ApplicationManager.getApplication().runWriteAction {
            if (dialog.isOK) {
                val featureName = dialog.featureName
                val libDir = event.getData(PlatformDataKeys.VIRTUAL_FILE)
                generateFolderStructure(libDir, featureName)
            }
        }
    }

    private fun generateFolderStructure(libDir: VirtualFile?, featureName: String) {
        if (libDir != null) {
            val featureDir = libDir.createChildDirectory(null, featureName)

            val dataDir = featureDir.createChildDirectory(null, "data")
            dataDir
                .createChildDirectory(null, "data_source")
                .createChildData(null, "${featureName}_data_source.dart")
                .setBinaryContent(getDataSourceContent(featureName).toByteArray())

            showToastMessage("Generated Successfully!")
        }
    }

    private fun getDataSourceContent(featureName: String): String {
        return """
           |final class ${featureName.toCamelCase()}DataSource {
           |  const ${featureName.toCamelCase()}DataSource({required NetworkService service}) : _service = service;
           |
           | final NetworkService _service;
           |
           | }
           |
       """.trimMargin()
    }

    private fun String.toCamelCase(): String = this.split("_")
        .joinToString("") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                else it.toString()
            }
        }

    private fun showToastMessage(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showMessageDialog(message, "Success", Messages.getInformationIcon())
        }
    }
}
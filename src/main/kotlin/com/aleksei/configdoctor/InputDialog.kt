package com.aleksei.configdoctor

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class InputDialog : DialogWrapper(null) {
    private val textField = JTextField(20)
    var featureName = ""

    init {
        init()
        title = "Config Doctor"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(JLabel("Config Doctor:"), BorderLayout.WEST)
        panel.add(textField, BorderLayout.CENTER)
        return panel
    }

    override fun getPreferredSize(): Dimension = Dimension(300, 100)

    override fun doOKAction() {
        featureName = textField.text.trim()
        if (featureName.isEmpty()) {
            Messages.showErrorDialog("Config Doctor cannot be empty.", "Error")
            return
        }
        super.doOKAction()
    }
}
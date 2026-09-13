package com.aleksei.configdoctor.plugin.yaml

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class DuplicateSegmentCollapseTest : BasePlatformTestCase() {

    fun `test finds safe collapse for the mandatory duplicated datasource case`() {
        val file = configureYaml(
            """
            spring:
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val urlKeyValue = findKeyValue(file, "url")
        val result = DuplicateSegmentCollapse.findSafeCollapse(urlKeyValue)

        assertNotNull(result)
        assertEquals("datasource", result!!.first.keyText)
        assertEquals("datasource", result.second.keyText)
        // The "outer" one must be the higher-level duplicate, distinct
        // PSI element from the "inner"/duplicate one.
        assertNotSame(result.first, result.second)
    }

    fun `test refuses when the duplicated level has sibling keys`() {
        // Collapsing here would either lose "username" or require an
        // arbitrary merge decision - must not be offered.
        val file = configureYaml(
            """
            spring:
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost/db
                username: admin
            """.trimIndent()
        )

        val urlKeyValue = findKeyValue(file, "url")

        assertNull(DuplicateSegmentCollapse.findSafeCollapse(urlKeyValue))
    }

    fun `test finds safe collapse for a shifted wrapper segment`() {
        val file = configureYaml(
            """
            spring:
              datasource:
                jdbc:
                  url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val urlKeyValue = findKeyValue(file, "url")
        val result = DuplicateSegmentCollapse.findSafeCollapse(urlKeyValue)

        assertNotNull(result)
        assertEquals("datasource", result!!.first.keyText)
        assertEquals("jdbc", result.second.keyText)
    }

    fun `test returns null when there is no duplicated segment at all`() {
        val file = configureYaml(
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val urlKeyValue = findKeyValue(file, "url")

        assertNull(DuplicateSegmentCollapse.findSafeCollapse(urlKeyValue))
    }

    fun `test refuses when more than one collapsible position exists in the same chain`() {
        val file = configureYaml(
            """
            a:
              a:
                b:
                  b:
                    value: 1
            """.trimIndent()
        )

        val valueKeyValue = findKeyValue(file, "value")

        assertNull(DuplicateSegmentCollapse.findSafeCollapse(valueKeyValue))
    }

    private fun configureYaml(text: String): YAMLFile =
        myFixture.configureByText("application.yml", text) as YAMLFile

    private fun findKeyValue(file: YAMLFile, keyName: String): YAMLKeyValue =
        PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java).first { it.keyText == keyName }
}

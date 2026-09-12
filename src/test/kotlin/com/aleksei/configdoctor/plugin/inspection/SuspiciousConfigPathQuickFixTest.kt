package com.aleksei.configdoctor.plugin.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Separate test class from Stage 6's SuspiciousConfigPathInspectionTest
 * (kept untouched) - this one specifically exercises the Stage 7 Quick
 * Fix end-to-end through the real inspection + intention machinery.
 */
class SuspiciousConfigPathQuickFixTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SuspiciousConfigPathInspection())
    }

    fun `test applying the quick fix collapses the duplicated segment`() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )
        myFixture.configureByText(
            "application-local.yml",
            """
            spring:
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val intention = myFixture.findSingleIntention("Collapse duplicated configuration segment")
        myFixture.launchAction(intention)

        assertEquals(
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent(),
            myFixture.file.text.trim()
        )
    }

    fun `test no quick fix is offered when the duplicated level has sibling keys`() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )
        myFixture.configureByText(
            "application-local.yml",
            """
            spring:
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost/db
                username: admin
            """.trimIndent()
        )

        // The warning must still be present (the path collision is real)...
        val warnings = myFixture.doHighlighting(com.intellij.lang.annotation.HighlightSeverity.WARNING)
        assertEquals(1, warnings.size)

        // ...but no fix should be offered, since collapsing would be
        // ambiguous/lossy (section 19).
        val availableFixes = myFixture.getAllQuickFixes("application-local.yml")
            .filter { it.text == "Collapse duplicated configuration segment" }
        assertTrue(availableFixes.isEmpty())
    }
}

package com.aleksei.configdoctor.plugin.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Verifies that the quick fixes proposed by the inspection actually rewrite the
 * YAML into the expected, non-duplicated structure.
 */
class SuspiciousConfigPathQuickFixTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SuspiciousConfigPathInspection())
    }

    fun `test applying the quick fix collapses the duplicated segment`() {
        myFixture.addFileToProject(
            "src/main/resources/application.ONLINE.yml",
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
                  <caret>url: jdbc:postgresql://localhost/db
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

    fun `test applying the quick fix collapses a shifted wrapper segment`() {
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
                jdbc:
                  <caret>url: jdbc:postgresql://localhost/local-db
            """.trimIndent()
        )

        val intention = myFixture.findSingleIntention("Collapse duplicated configuration segment")
        myFixture.launchAction(intention)

        assertEquals(
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/local-db
            """.trimIndent(),
            myFixture.file.text.trim()
        )
    }

    fun `test applying the profile override fix renames the mismatched leaf key`() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            server:
              port: 8080
            """.trimIndent()
        )
        myFixture.configureByText(
            "application-local.yml",
            """
            server:
              <caret>ports: 8081
            """.trimIndent()
        )

        val intention = myFixture.findSingleIntention("Replace 'ports' with 'port'")
        myFixture.launchAction(intention)

        assertEquals(
            """
            server:
              port: 8081
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

        val warnings = myFixture.doHighlighting(com.intellij.lang.annotation.HighlightSeverity.WARNING)
        assertEquals(1, warnings.size)

        val availableFixes = myFixture.getAllQuickFixes("application-local.yml")
            .filter { it.text == "Collapse duplicated configuration segment" }
        assertTrue(availableFixes.isEmpty())
    }
}
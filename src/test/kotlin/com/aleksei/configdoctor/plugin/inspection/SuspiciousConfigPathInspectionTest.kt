package com.aleksei.configdoctor.plugin.inspection

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SuspiciousConfigPathInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SuspiciousConfigPathInspection())
    }

    fun `test warns on the mandatory real-world duplicated datasource segment`() {
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

        val warnings = myFixture.doHighlighting(HighlightSeverity.WARNING)

        assertEquals(1, warnings.size)
        val message = warnings.single().description
        assertTrue(message.contains("spring.datasource.datasource.url"))
        assertTrue(message.contains("spring.datasource.url"))
        assertTrue(message.contains("Suspicious"))
    }

    fun `test warns on a shifted non-duplicate segment when the collapsed path already exists`() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            spring:
              name: config-doctor-demo
            """.trimIndent()
        )
        myFixture.configureByText(
            "application-local.yml",
            """
            spring:
              application:
                name: config-doctor-demo
            """.trimIndent()
        )

        val warnings = myFixture.doHighlighting(HighlightSeverity.WARNING)

        assertEquals(1, warnings.size)
        val message = warnings.single().description
        assertTrue(message.contains("spring.application.name"))
        assertTrue(message.contains("spring.name"))
        assertTrue(message.contains("Suspicious"))
    }

    fun `test no warning when the property is correctly structured`() {
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
                url: jdbc:postgresql://localhost/local-db
            """.trimIndent()
        )

        assertTrue(myFixture.doHighlighting(HighlightSeverity.WARNING).isEmpty())
    }

    fun `test no warning for a non-config yaml file even with a duplicated segment`() {
        // Not an application*.yml file - out of scope for ConfigFileDiscovery,
        // must not be treated as a Spring Boot config property at all.
        myFixture.configureByText(
            "logback.yml",
            """
            appender:
              appender:
                type: console
            """.trimIndent()
        )

        assertTrue(myFixture.doHighlighting(HighlightSeverity.WARNING).isEmpty())
    }
}

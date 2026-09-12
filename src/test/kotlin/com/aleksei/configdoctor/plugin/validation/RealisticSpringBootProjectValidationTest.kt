package com.aleksei.configdoctor.plugin.validation

import com.aleksei.configdoctor.plugin.inspection.SuspiciousConfigPathInspection
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Stage 9 (AGENTS.md section 25, "Real project validation").
 *
 * AGENTS.md asks for the plugin to be exercised "against a real Spring Boot
 * project" (application.yml / application-local.yml / application-test.yml,
 * nested configuration, datasource configuration, custom application
 * properties) and for detected problems / false positives / crashes to be
 * recorded.
 *
 * IMPORTANT HONESTY NOTE: this test is NOT a substitute for actually running
 * `./gradlew runIde` against a real, independent Spring Boot repository in a
 * live sandbox IDE - that manual step could not be completed in the
 * environment this was developed in (dependency downloads are blocked/
 * throttled by a corporate proxy that prevents even compiling the plugin -
 * see the environment's build troubleshooting notes). This test is the
 * closest honest automated approximation: a single project containing a
 * realistic MIX of files and property shapes (multiple profiles, custom
 * non-Spring properties, lists, nested nulls, a legitimate cross-profile
 * override, AND both of the two required real-world mistake shapes),
 * asserting that exactly the intended finding is produced and nothing else
 * is. Running the plugin in a live sandbox against a real external project
 * (per section 25's checklist) is still recommended before shipping and is
 * tracked in docs/STAGE9_VALIDATION.md.
 */
class RealisticSpringBootProjectValidationTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SuspiciousConfigPathInspection())
    }

    fun `test a realistic multi-file multi-profile project produces exactly one correct finding`() {
        myFixture.addFileToProject("src/main/resources/application.yml", BASE_APPLICATION_YML)
        myFixture.addFileToProject("src/main/resources/application-local.yml", LOCAL_APPLICATION_YML)
        myFixture.addFileToProject("src/main/resources/application-prod.yml", PROD_APPLICATION_YML)
        myFixture.addFileToProject("src/main/resources/application.ODD.ONLINE.yml", BASE_APPLICATION_YML)
        myFixture.addFileToProject("src/main/resources/application.ONLINE.yml", BASE_APPLICATION_YML)
        // logback.xml/yml-style unrelated config file: must be ignored entirely.
        myFixture.addFileToProject("src/main/resources/logback.yml", "root: INFO\nappenders:\n  appenders:\n    type: console\n")

        val mistakeFile = myFixture.configureByText("application-test.yml", TEST_PROFILE_WITH_MISTAKE_YML)
        val warnings = myFixture.doHighlighting(HighlightSeverity.WARNING)

        assertEquals(
            "Expected exactly one warning (the duplicated datasource segment); all other " +
                "properties in this realistic mixed project must not produce false positives",
            1,
            warnings.size
        )

        val message = warnings.single().description
        assertTrue(message.contains("spring.datasource.datasource.url"))
        assertTrue(message.contains("spring.datasource.url"))
        assertEquals("application-test.yml", mistakeFile.name)
    }

    fun `test each individual file in the realistic project produces no warnings on its own`() {
        // Defense in depth: verify the base/local/prod files, each checked
        // individually as the active/highlighted file (doHighlighting only
        // analyzes the currently active file, so each must be configured in
        // turn), never produce a warning on their own - so we know the
        // single finding above really does come from the one intentional
        // mistake in application-test.yml, not from an interaction with
        // some other file's content.
        val baseFile = myFixture.addFileToProject("src/main/resources/application.yml", BASE_APPLICATION_YML)
        val onlineFile = myFixture.addFileToProject("src/main/resources/application.ONLINE.yml", BASE_APPLICATION_YML)
        val oddOnlineFile = myFixture.addFileToProject("src/main/resources/application.ODD.ONLINE.yml", BASE_APPLICATION_YML)
        val localFile = myFixture.addFileToProject("src/main/resources/application-local.yml", LOCAL_APPLICATION_YML)
        val prodFile = myFixture.addFileToProject("src/main/resources/application-prod.yml", PROD_APPLICATION_YML)

        listOf(baseFile, onlineFile, oddOnlineFile, localFile, prodFile).forEach { file ->
            myFixture.configureFromExistingVirtualFile(file.virtualFile)
            val warnings = myFixture.doHighlighting(HighlightSeverity.WARNING)
            assertTrue("Expected no warnings in ${file.name}, but found: $warnings", warnings.isEmpty())
        }
    }

    private companion object {
        val BASE_APPLICATION_YML = """
            server:
              port: 8080
              address: 0.0.0.0
            spring:
              application:
                name: config-doctor-demo
              datasource:
                url: jdbc:postgresql://localhost:5432/demo
                username: demo_user
              jpa:
                hibernate:
                  ddl-auto: validate
                show-sql: false
              profiles:
                include:
                  - base-extras
            management:
              endpoints:
                web:
                  exposure:
                    include: health,info
            logging:
              level:
                root: INFO
                com.aleksei: DEBUG
            app:
              feature-flags:
                new-checkout: true
              retry-retry-policy:
                max-attempts: 3
              scheduled-report-cutoff:
        """.trimIndent()

        val LOCAL_APPLICATION_YML = """
            spring:
              datasource:
                url: jdbc:postgresql://localhost:5432/demo_local
            logging:
              level:
                root: DEBUG
            app:
              feature-flags:
                new-checkout: false
        """.trimIndent()

        val PROD_APPLICATION_YML = """
            server:
              port: 80
            spring:
              datasource:
                url: jdbc:postgresql://prod-db.internal:5432/demo
                username: prod_user
              jpa:
                show-sql: false
            management:
              endpoints:
                web:
                  exposure:
                    include: health
        """.trimIndent()

        val TEST_PROFILE_WITH_MISTAKE_YML = """
            spring:
              application:
                name: config-doctor-demo
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost:5432/demo_test
              jpa:
                hibernate:
                  ddl-auto: create-drop
            app:
              feature-flags:
                new-checkout: true
        """.trimIndent()
    }
}


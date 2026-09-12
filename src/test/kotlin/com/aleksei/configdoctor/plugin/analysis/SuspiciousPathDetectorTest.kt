package com.aleksei.configdoctor.plugin.analysis

import com.aleksei.configdoctor.plugin.model.ConfigProperty
import com.aleksei.configdoctor.plugin.yaml.ConfigFileDiscovery
import com.aleksei.configdoctor.plugin.yaml.YamlPropertyPaths
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile

/**
 * AGENTS.md section 14 (Stage 5): "Create a test case based on the real
 * datasource example." This test builds ConfigProperty the same way real
 * usage would - by running Stage 4 discovery, then Stage 3 extraction,
 * then handing the result to the Stage 5 detector - rather than
 * constructing ConfigProperty by hand, so the whole pipeline is actually
 * exercised together.
 */
class SuspiciousPathDetectorTest : BasePlatformTestCase() {

    fun `test the mandatory real-world datasource case is flagged with correct evidence`() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/main/resources/application-local.yml",
            """
            spring:
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val findings = SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllProperties())

        assertEquals(1, findings.size)
        val finding = findings.single()

        assertEquals("spring.datasource.datasource.url", finding.actual.path.toString())
        assertEquals("local", finding.actual.profile)

        assertEquals("spring.datasource.url", finding.relatedExpected.path.toString())
        assertNull(finding.relatedExpected.profile)

        assertEquals(EvidenceKind.STRONG_PROJECT_RELATIONSHIP, finding.evidenceKind)
        assertTrue(finding.evidence.contains("spring.datasource.datasource.url"))
        assertTrue(finding.evidence.contains("spring.datasource.url"))
    }

    fun `test correctly structured properties across profiles produce no findings`() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/main/resources/application-prod.yml",
            """
            spring:
              datasource:
                url: jdbc:postgresql://prod-host/db
            """.trimIndent()
        )

        assertTrue(SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllProperties()).isEmpty())
    }

    fun `test duplicated segment with no matching real property is not flagged`() {
        // A path with an adjacent duplicate exists, but nothing in the
        // project matches the collapsed form - must not invent a finding.
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            app:
              cache:
                cache:
                  size: 100
            """.trimIndent()
        )

        assertTrue(SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllProperties()).isEmpty())
    }

    fun `test a shifted (non-duplicate) section is intentionally not flagged - documented scope limit`() {
        // Same shape as the "misplaced section" regression case from
        // Stage 3 - an inserted, non-duplicate segment. This detector
        // only targets adjacent duplicate segments (evidence tier 2), not
        // general structural similarity (tier 4), so this must NOT be
        // flagged yet.
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            app:
              feature:
                enabled: true
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/main/resources/application-local.yml",
            """
            app:
              wrapper:
                feature:
                  enabled: true
            """.trimIndent()
        )

        assertTrue(SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllProperties()).isEmpty())
    }

    fun `test a property existing only in a profile file is not itself an error - AGENTS section 20`() {
        // AGENTS.md section 20 explicitly forbids the rule "if a profile
        // property is absent from the base file, show a warning" - a
        // profile-only property (with no duplicated segment at all) must
        // never be flagged, regardless of whether a same-named property
        // exists in the base file.
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            server:
              port: 8080
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/main/resources/application-local.yml",
            """
            server:
              port: 8080
            debug:
              verbose-logging: true
            """.trimIndent()
        )

        assertTrue(SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllProperties()).isEmpty())
    }

    fun `test a duplicated segment that is itself the intended property shape is not flagged`() {
        // A property that legitimately contains a repeated word as a
        // deliberate, single, real property (not two nested keys with the
        // same name) must not be affected - the detector only ever looks at
        // adjacent YAML *key* segments, never substrings of a single key.
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
            app:
              retry-retry-policy:
                max-attempts: 3
            """.trimIndent()
        )

        assertTrue(SuspiciousPathDetector.findSuspiciousDuplicatedSegments(collectAllProperties()).isEmpty())
    }

    private fun collectAllProperties(): List<ConfigProperty> {
        val configFiles = ConfigFileDiscovery.findConfigFiles(project)
        val psiManager = PsiManager.getInstance(project)

        return configFiles.flatMap { configFile ->
            val yamlFile = psiManager.findFile(configFile.virtualFile) as YAMLFile
            YamlPropertyPaths.leafConfigPropertiesOf(configFile, yamlFile)
        }
    }
}

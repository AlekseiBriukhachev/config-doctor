package com.aleksei.configdoctor.plugin.yaml

import com.aleksei.configdoctor.plugin.model.PropertyPath
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Verifies that YAML PSI extraction produces the same dotted paths that Spring
 * configuration expects, even when a section or key is nested one level too
 * deep.
 */
class YamlPropertyPathsTest : BasePlatformTestCase() {

    // --- Section 23, Test 1: misplaced section ------------------------

    fun `test correctly placed section produces expected path`() {
        val file = configureYaml(
            """
            app:
              feature:
                enabled: true
            """.trimIndent()
        )

        assertEquals(
            listOf("app.feature.enabled"),
            YamlPropertyPaths.leafPathsOf(file).map { it.toString() }
        )
    }

    fun `test section shifted one level deeper produces a different path`() {
        // Simulates AGENTS.md Case 1: a whole section accidentally
        // indented one level too far, so the expected property is never
        // created and a different, deeper one exists instead.
        val file = configureYaml(
            """
            app:
              wrapper:
                feature:
                  enabled: true
            """.trimIndent()
        )

        val paths = YamlPropertyPaths.leafPathsOf(file).map { it.toString() }

        assertEquals(listOf("app.wrapper.feature.enabled"), paths)
        assertFalse("app.feature.enabled" in paths)
    }

    // --- Section 23, Test 2: misplaced datasource url ------------------

    fun `test correct datasource url path`() {
        val file = configureYaml(
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        assertEquals(
            listOf("spring.datasource.url"),
            YamlPropertyPaths.leafPathsOf(file).map { it.toString() }
        )
    }

    fun `test accidentally duplicated datasource nesting produces a different path`() {
        val file = configureYaml(
            """
            spring:
              datasource:
                datasource:
                  url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val paths = YamlPropertyPaths.leafPathsOf(file).map { it.toString() }

        assertEquals(listOf("spring.datasource.datasource.url"), paths)
        assertFalse("spring.datasource.url" in paths)
    }

    // --- General structural cases ---------------------------------------

    fun `test multiple sibling leaves at different depths`() {
        val file = configureYaml(
            """
            server:
              port: 8080
              address: 0.0.0.0
            spring:
              application:
                name: config-doctor
            """.trimIndent()
        )

        val paths = YamlPropertyPaths.leafPathsOf(file).map { it.toString() }.toSet()

        assertEquals(
            setOf("server.port", "server.address", "spring.application.name"),
            paths
        )
    }

    fun `test pathOf a single selected key value`() {
        val file = configureYaml(
            """
            spring:
              datasource:
                url: jdbc:postgresql://localhost/db
            """.trimIndent()
        )

        val urlKeyValue = findKeyValue(file, "url")

        assertEquals(
            PropertyPath.of("spring", "datasource", "url"),
            YamlPropertyPaths.pathOf(urlKeyValue)
        )
    }

    // --- False positives (AGENTS.md section 21): valid-but-unusual shapes

    fun `test custom application properties are extracted without error`() {
        // Not a Spring property at all - just an app-specific value. The
        // extractor must not choke on or misinterpret this; it only
        // reports the path, it does not judge it here.
        val file = configureYaml(
            """
            my-app:
              feature-flags:
                new-checkout: true
            """.trimIndent()
        )

        assertEquals(
            listOf("my-app.feature-flags.new-checkout"),
            YamlPropertyPaths.leafPathsOf(file).map { it.toString() }
        )
    }

    fun `test empty value key is still reported as a leaf`() {
        val file = configureYaml(
            """
            spring:
              profiles:
                active:
            """.trimIndent()
        )

        assertEquals(
            listOf("spring.profiles.active"),
            YamlPropertyPaths.leafPathsOf(file).map { it.toString() }
        )
    }

    fun `test sequence value is treated as a single leaf, not expanded`() {
        val file = configureYaml(
            """
            spring:
              profiles:
                include:
                  - local
                  - debug
            """.trimIndent()
        )

        // Documented scope limitation: list contents are not walked.
        assertEquals(
            listOf("spring.profiles.include"),
            YamlPropertyPaths.leafPathsOf(file).map { it.toString() }
        )
    }

    // --- Helpers ---------------------------------------------------------

    private fun configureYaml(text: String): YAMLFile =
        myFixture.configureByText("application.yml", text) as YAMLFile

    private fun findKeyValue(file: YAMLFile, keyName: String): YAMLKeyValue =
        PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .first { it.keyText == keyName }
}

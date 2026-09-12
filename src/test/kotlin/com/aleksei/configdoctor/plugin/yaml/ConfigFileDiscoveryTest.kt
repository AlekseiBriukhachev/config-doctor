package com.aleksei.configdoctor.plugin.yaml

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ConfigFileDiscoveryTest : BasePlatformTestCase() {

    fun `test discovers default and profile variants while ignoring unrelated files`() {
        myFixture.addFileToProject("src/main/resources/application.yml", "server:\n  port: 8080\n")
        myFixture.addFileToProject("src/main/resources/application-local.yml", "server:\n  port: 9090\n")
        myFixture.addFileToProject("src/main/resources/application-prod.yaml", "server:\n  port: 80\n")
        // Must be ignored: not YAML at all.
        myFixture.addFileToProject("src/main/resources/application.properties", "server.port=8080\n")
        // Must be ignored: valid YAML, but not an application config file.
        myFixture.addFileToProject("src/main/resources/logback.yml", "root: INFO\n")

        val found = ConfigFileDiscovery.findConfigFiles(project)
        val nameByProfile = found.associate { it.profile to it.virtualFile.name }

        assertEquals(3, found.size)
        assertEquals("application.yml", nameByProfile[null])
        assertEquals("application-local.yml", nameByProfile["local"])
        assertEquals("application-prod.yaml", nameByProfile["prod"])
    }

    fun `test returns empty list when no config files exist`() {
        myFixture.addFileToProject("src/main/resources/logback.yml", "root: INFO\n")

        assertEquals(emptyList<Any>(), ConfigFileDiscovery.findConfigFiles(project))
    }
}

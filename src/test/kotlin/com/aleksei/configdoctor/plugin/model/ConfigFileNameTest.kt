package com.aleksei.configdoctor.plugin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigFileNameTest {

    @Test
    fun `default application yml has no profile`() {
        assertEquals(ConfigFileName("application", null, "yml"), ConfigFileName.parse("application.yml"))
    }

    @Test
    fun `default application yaml has no profile`() {
        assertEquals(ConfigFileName("application", null, "yaml"), ConfigFileName.parse("application.yaml"))
    }

    @Test
    fun `profile is parsed separately from the file name string`() {
        val name = ConfigFileName.parse("application-prod.yml")
        assertEquals("application", name?.baseName)
        assertEquals("prod", name?.profile)
        assertEquals("yml", name?.extension)
    }

    @Test
    fun `local and test profile variants are recognized`() {
        assertEquals("local", ConfigFileName.parse("application-local.yml")?.profile)
        assertEquals("test", ConfigFileName.parse("application-test.yaml")?.profile)
    }

    @Test
    fun `unrelated or malformed file names are rejected`() {
        assertNull(ConfigFileName.parse("config.yml"))
        assertNull(ConfigFileName.parse("application.properties"))
        assertNull(ConfigFileName.parse("application-local.txt"))
        assertNull(ConfigFileName.parse("myapplication.yml"))
        assertNull(ConfigFileName.parse("application-.yml"))
    }
}

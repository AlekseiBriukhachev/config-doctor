package com.aleksei.configdoctor.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Stage 2 skeleton verification hook (AGENTS.md, section 10).
 *
 * This activity performs no configuration analysis whatsoever. Its only
 * purpose is to prove that the plugin loads correctly inside the IDE
 * sandbox with no startup errors. Once Stage 2's Definition of Done is
 * confirmed (sandbox starts, plugin loads, this log line appears), this
 * class can stay as a harmless diagnostic hook or be removed in a later
 * stage.
 */
class ConfigDoctorStartupActivity : ProjectActivity {

    private val logger = Logger.getInstance(ConfigDoctorStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        logger.info("Config Doctor plugin loaded for project: ${project.name}")
    }
}

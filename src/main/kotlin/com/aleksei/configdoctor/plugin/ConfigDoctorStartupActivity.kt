package com.aleksei.configdoctor.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Emits a startup log entry when the plugin loads in an IDE project.
 *
 * This is a minimal smoke-check for the IDE sandbox and does not perform any
 * configuration analysis itself.
 */
class ConfigDoctorStartupActivity : ProjectActivity {

    private val logger = Logger.getInstance(ConfigDoctorStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        logger.info("Config Doctor plugin loaded for project: ${project.name}")
    }
}

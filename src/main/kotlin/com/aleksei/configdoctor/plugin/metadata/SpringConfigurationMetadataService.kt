package com.aleksei.configdoctor.plugin.metadata

import com.aleksei.configdoctor.plugin.model.PropertyPath
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import java.util.concurrent.atomic.AtomicReference

/**
 * Project-level service that caches known property paths extracted from
 * spring-configuration-metadata.json files found in the project and
 * dependencies. The cache is invalidated when a metadata file is created,
 * deleted or changed.
 */
@Service(Service.Level.PROJECT)
class SpringConfigurationMetadataService(private val project: Project) : Disposable {

    private val cache = AtomicReference<Set<PropertyPath>?>(null)
    private val fileName = "spring-configuration-metadata.json"

    init {
        val listener = object : VirtualFileListener {
            override fun contentsChanged(event: VirtualFileEvent) {
                if (event.file?.name == fileName) clearCache()
            }

            override fun fileCreated(event: VirtualFileEvent) {
                if (event.file?.name == fileName) clearCache()
            }

            override fun fileDeleted(event: VirtualFileEvent) {
                if (event.file?.name == fileName) clearCache()
            }
        }
        VirtualFileManager.getInstance().addVirtualFileListener(listener, this)
    }

    fun knownPaths(): Set<PropertyPath> {
        cache.get()?.let { return it }
        val loaded = SpringConfigurationMetadata.loadKnownPropertyPaths(project)
        cache.set(loaded)
        return loaded
    }

    private fun clearCache() {
        cache.set(null)
    }

    override fun dispose() {
        // Nothing to dispose explicitly - listener is removed automatically.
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): SpringConfigurationMetadataService = project.getService(SpringConfigurationMetadataService::class.java)
    }
}

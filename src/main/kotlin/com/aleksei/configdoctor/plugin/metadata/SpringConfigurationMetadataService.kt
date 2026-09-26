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
 * deleted or changed. File-based disk caching has been removed: the
 * FileBasedIndex is now authoritative and the service falls back to
 * scanning metadata JSON files at runtime only when indexing is unavailable.
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

        // Prefer indexed data via FileBasedIndex if available
        try {
            val index = com.intellij.util.indexing.FileBasedIndex.getInstance()
            val keys = index.getAllKeys(com.aleksei.configdoctor.plugin.metadata.SpringConfigurationMetadataIndex.NAME, project)
            if (keys.isNotEmpty()) {
                val set = keys.mapNotNull { name ->
                    val segments = name.split('.').map { it.trim() }.filter { it.isNotEmpty() }
                    if (segments.isEmpty()) null else PropertyPath(segments)
                }.toSet()
                cache.set(set)
                return set
            }
        } catch (_: Exception) {
            // fall through to scanner
        }

        // Fall back to runtime scan of metadata files when index is not available
        val loaded = SpringConfigurationMetadata.loadKnownPropertyPaths(project)
        cache.set(loaded)
        return loaded
    }

    /**
     * Returns a map of PropertyPath -> MetadataEntry containing type/description
     * when available. Uses FileBasedIndex primarily, falling back to runtime
     * scanning via SpringConfigurationMetadata when indexing is not available.
     */
    fun knownMetadata(): Map<PropertyPath, com.aleksei.configdoctor.plugin.metadata.MetadataEntry> {
        // Try FileBasedIndex first
        try {
            val index = com.intellij.util.indexing.FileBasedIndex.getInstance()
            val keys = index.getAllKeys(com.aleksei.configdoctor.plugin.metadata.SpringConfigurationMetadataIndex.NAME, project)
            if (keys.isNotEmpty()) {
                val map = mutableMapOf<PropertyPath, com.aleksei.configdoctor.plugin.metadata.MetadataEntry>()
                val scope = com.intellij.psi.search.GlobalSearchScope.everythingScope(project)
                for (k in keys) {
                    try {
                        val values = index.getValues(com.aleksei.configdoctor.plugin.metadata.SpringConfigurationMetadataIndex.NAME, k, scope)
                        val first = values.firstOrNull()
                        val parts = first?.split('\t') ?: listOf()
                        val type = parts.getOrNull(0)?.takeIf { it.isNotEmpty() }
                        val desc = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        val defaultVal = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
                        val deprecatedInfo = parts.getOrNull(3)?.takeIf { it.isNotEmpty() }
                        val segments = k.split('.').map { it.trim() }.filter { it.isNotEmpty() }
                        if (segments.isNotEmpty()) {
                            map[PropertyPath(segments)] = com.aleksei.configdoctor.plugin.metadata.MetadataEntry(k, type, desc, defaultVal, deprecatedInfo)
                        }
                    } catch (_: Exception) {
                        // ignore value-level failures
                    }
                }
                // populate fast key cache
                cache.set(map.keys)
                return map
            }
        } catch (_: Exception) {
            // fall through to runtime scan
        }

        // Runtime scan fallback
        val loaded = SpringConfigurationMetadata.loadKnownPropertyMetadata(project)
        cache.set(loaded.keys)
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

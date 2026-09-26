package com.aleksei.configdoctor.plugin.metadata

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import java.io.DataInput
import java.io.DataOutput

/**
 * FileBasedIndex extension that indexes spring-configuration-metadata.json files.
 * Key: fully-qualified property name (String)
 * Value: a compact serialized string containing type and description separated by tab
 */
class SpringConfigurationMetadataIndex : FileBasedIndexExtension<String, String>() {
    companion object {
        val NAME: ID<String, String> = ID.create("spring.configuration.metadata.index")
        private const val METADATA_FILE_NAME = "spring-configuration-metadata.json"
    }

    override fun getName(): ID<String, String> = NAME

    override fun getIndexer(): DataIndexer<String, String, FileContent> = DataIndexer { inputData ->
        val text = try {
            inputData.contentAsText.toString()
        } catch (t: Throwable) {
            try { String(inputData.content) } catch (_: Throwable) { "" }
        }
        val map = HashMap<String, String>()

        try {
            val json = JsonParser.parseString(text)
            // standard metadata layout: { "properties": [ {"name":"...","type":"...","description":"..."}, ... ] }
            val props = json.asJsonObject.getAsJsonArray("properties")
            for (el in props) {
                try {
                    val obj = el.asJsonObject
                    val name = obj.getAsJsonPrimitive("name")?.asString?.trim() ?: continue
                    val type = obj.getAsJsonPrimitive("type")?.asString
                    val desc = obj.getAsJsonPrimitive("description")?.asString?.replace('\n', ' ')
                    val defaultVal = obj.getAsJsonPrimitive("defaultValue")?.asString
                    // deprecation may be a boolean or an object with reason
                    var deprecatedInfo: String? = null
                    if (obj.has("deprecated")) {
                        try {
                            val deprecatedEl = obj.get("deprecated")
                            if (deprecatedEl.isJsonPrimitive && deprecatedEl.asJsonPrimitive.isBoolean) {
                                if (deprecatedEl.asBoolean) deprecatedInfo = "deprecated"
                            } else if (deprecatedEl.isJsonObject) {
                                deprecatedInfo = deprecatedEl.asJsonObject.getAsJsonPrimitive("reason")?.asString
                            }
                        } catch (_: Throwable) {
                        }
                    } else if (obj.has("deprecation")) {
                        try {
                            val depObj = obj.getAsJsonObject("deprecation")
                            deprecatedInfo = depObj.getAsJsonPrimitive("reason")?.asString
                        } catch (_: Throwable) {
                        }
                    }
                    val value = listOf(type ?: "", desc ?: "", defaultVal ?: "", deprecatedInfo ?: "").joinToString("\t")
                    if (name.isNotEmpty()) map[name] = value
                } catch (_: Throwable) {
                    // skip malformed entry
                }
            }
        } catch (_: Throwable) {
            // ignore parse errors
        }

        map
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<String> = object : DataExternalizer<String> {
        override fun save(out: DataOutput, value: String) {
            out.writeUTF(value)
        }

        override fun read(input: DataInput): String = input.readUTF()
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.name.equals(METADATA_FILE_NAME, ignoreCase = true)
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 3 // bumped because value format now includes default + deprecation

    override fun getCacheSize(): Int = 3
}

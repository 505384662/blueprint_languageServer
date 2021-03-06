package com.tang.vscode

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.psi.PsiElement
import com.tang.lsp.FileURI
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Range

enum class AnnotatorType {
    Param,
    Global,
    DocName,
    Upvalue,
    NotUse,
}

data class AnnotatorParams(val uri: String)

data class RenderRange(val range: Range, var hint: String?, val data: String? = null)

data class Annotator(val uri: String, val ranges: List<RenderRange>, val type: AnnotatorType)

data class ProgressReport(val text: String, val percent: Float)

enum class UpdateType {
    Created,
    Changed,
    Deleted
}

data class BluePrintConfigurationSource(val uri: String, val workspace: String) {
    companion object {
        fun parse(arr: JsonArray): Array<BluePrintConfigurationSource> {
            val list = mutableListOf<BluePrintConfigurationSource>()
            for (element in arr) {
                if (element is JsonObject) {
                    val cfg = parse(element)
                    list.add(cfg)
                }
            }
            return list.toTypedArray()
        }

        private fun parse(json: JsonObject): BluePrintConfigurationSource {
            val uri = json["uri"].asString
            val workspace = json["workspace"].asString
            return BluePrintConfigurationSource(uri, workspace)
        }
    }

    val fileURI: FileURI get() {
        return FileURI.uri(uri, false)
    }

    override fun equals(other: Any?): Boolean {
        return other is BluePrintConfigurationSource && other.uri == uri
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + workspace.hashCode()
        return result
    }
}

data class UpdateConfigParams(val type: UpdateType, val source: BluePrintConfigurationSource)
@file:Suppress("unused")

package com.tang.vscode

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.services.LanguageClient

interface LuaLanguageClient : LanguageClient {
    @JsonNotification("bluePrint/annotator")
    fun annotator(ann: AnnotatorParams)

    @JsonNotification("bluePrint/updateConfig")
    fun updateConfig(params: UpdateConfigParams)

    @JsonNotification("bluePrint/progressReport")
    fun progressReport(report: ProgressReport)
}
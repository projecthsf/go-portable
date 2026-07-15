package io.genai.go.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

/**
 * Registers the Go language server (gopls) with LSP4IJ. Declared via the LSP4IJ `server` +
 * `languageMapping` extension points in META-INF/lsp.xml (an optional module that loads only
 * when LSP4IJ is installed).
 */
class GoLanguageServerFactory : LanguageServerFactory {

    override fun createConnectionProvider(project: Project): StreamConnectionProvider =
        GoplsConnectionProvider(project)

    override fun createClientFeatures(): LSPClientFeatures = GoClientFeatures()
}

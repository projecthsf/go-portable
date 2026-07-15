package io.genai.go.lsp

import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import io.genai.go.settings.GoSettings

/**
 * Gates when the Go language server is active. LSP4IJ calls [isEnabled] before starting the
 * server for a file, so this enforces the "Code intelligence" toggle and the prerequisites
 * (a toolchain is configured, gopls is installed). Returning false keeps the server dormant.
 *
 * gopls is robust, so unlike the PHP setup we keep all sub-features (code actions such as
 * organize-imports and quick-fixes are genuinely useful here).
 */
class GoClientFeatures : LSPClientFeatures() {

    override fun isEnabled(file: VirtualFile): Boolean {
        val settings = GoSettings.getInstance()
        if (!settings.codeIntelligenceEnabled) return false
        return GoplsManager.defaultGo() != null && GoplsManager.isInstalled()
    }
}

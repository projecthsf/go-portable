package io.genai.go.lsp

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.ThrowableComputable

/**
 * One-click setup for Go code intelligence. Two prerequisites the user would otherwise have to
 * piece together:
 *   1. gopls — installed with the portable toolchain (`go install …gopls@latest`).
 *   2. LSP4IJ — a JetBrains Marketplace plugin. We open the Plugins screen so the user installs
 *      it with one click, then restarts.
 *
 * Programmatic plugin install is intentionally avoided: the APIs for it are in the frontend-split
 * `app-client` module or marked `@ApiStatus.Internal` — both fail the JetBrains Plugin Verifier.
 * Opening the Plugins screen via its public configurable id is the supported, verifier-clean path.
 */
object CodeIntelligenceSetup {

    const val LSP4IJ_ID = "com.redhat.devtools.lsp4ij"
    private const val PLUGINS_CONFIGURABLE_ID = "preferences.pluginManager"

    fun isLsp4ijInstalled(): Boolean =
        PluginManager.isPluginInstalled(PluginId.getId(LSP4IJ_ID))

    /** Both prerequisites present — code intelligence can actually run. */
    fun isFullySetUp(): Boolean = isLsp4ijInstalled() && GoplsManager.isInstalled()

    /**
     * Install gopls (if missing), then — if LSP4IJ isn't installed — open the Plugins screen.
     * [onChanged] runs after the gopls step so callers can refresh banners. Must run on the EDT.
     */
    fun enable(project: Project, onChanged: () -> Unit) {
        if (!GoplsManager.isInstalled()) {
            val go = GoplsManager.defaultGo()
            if (go == null) {
                Messages.showErrorDialog(
                    project,
                    "No Go toolchain configured yet. Download one in Settings ▸ Go Portable first.",
                    "Go Code Intelligence",
                )
                return
            }
            try {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    ThrowableComputable { GoplsManager.install(go) },
                    "Installing gopls…",
                    true,
                    project,
                )
                onChanged()
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Failed to install gopls: ${e.message}", "Go Code Intelligence")
                return
            }
        }

        if (!isLsp4ijInstalled()) {
            Messages.showInfoMessage(
                project,
                "gopls is ready. One step left: in the Plugins window that opens, go to " +
                    "<b>Marketplace</b>, search <b>LSP4IJ</b>, click <b>Install</b>, then restart the IDE.",
                "Enable Go Code Intelligence",
            )
            ShowSettingsUtil.getInstance().showSettingsDialog(project, PLUGINS_CONFIGURABLE_ID)
        } else {
            onChanged()
        }
    }
}

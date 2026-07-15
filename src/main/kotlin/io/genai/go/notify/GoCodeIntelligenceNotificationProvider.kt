package io.genai.go.notify

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import io.genai.go.lang.GoFileType
import io.genai.go.lang.GoFiles
import io.genai.go.lsp.CodeIntelligenceSetup
import io.genai.go.settings.GoConfigurable
import io.genai.go.settings.GoSettings
import io.genai.go.sdk.GoSdkType
import java.util.function.Function
import javax.swing.JComponent

/**
 * On a `.go` file where our language layer is active (IDEs without native Go support), offer a
 * single click to turn on code intelligence (install gopls + LSP4IJ). Disappears once both are
 * present. On GoLand / Ultimate the native Go support handles this, so we stay quiet.
 */
class GoCodeIntelligenceNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension?.lowercase() !in GoFiles.EXTENSIONS) return null
        if (FileTypeManager.getInstance().getFileTypeByExtension("go") != GoFileType) return null

        val settings = GoSettings.getInstance()
        if (!settings.codeIntelligenceEnabled) return null
        val hasToolchain = settings.defaultSdk()?.homePath
            ?.let { GoSdkType.findGoExecutable(it) } != null
        if (!hasToolchain) return null

        if (CodeIntelligenceSetup.isFullySetUp()) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                text("Turn on Go code intelligence — completion, go-to-definition and error highlighting.")
                createActionLabel("Enable code intelligence") {
                    CodeIntelligenceSetup.enable(project) {
                        EditorNotifications.getInstance(project).updateAllNotifications()
                    }
                }
                createActionLabel("Settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, GoConfigurable::class.java)
                }
            }
        }
    }
}

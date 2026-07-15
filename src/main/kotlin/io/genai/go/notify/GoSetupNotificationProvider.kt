package io.genai.go.notify

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import io.genai.go.lang.GoFiles
import io.genai.go.sdk.GoInterpreterActions
import io.genai.go.settings.GoSettings
import io.genai.go.settings.GoConfigurable
import java.util.function.Function
import javax.swing.JComponent

/**
 * On a .go file, if no Go toolchain is configured yet, show a banner offering to download or add
 * one. Disappears automatically once a toolchain exists.
 */
class GoSetupNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension?.lowercase() !in GoFiles.EXTENSIONS) return null
        if (GoSettings.getInstance().defaultSdk() != null) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                text("No Go toolchain configured — set one up to run this file.")
                createActionLabel("Download Go…") {
                    GoInterpreterActions.downloadInteractively(project) { refresh(project) }
                }
                createActionLabel("Add from Disk…") {
                    GoInterpreterActions.addFromDisk { refresh(project) }
                }
                createActionLabel("Settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, GoConfigurable::class.java)
                }
            }
        }
    }

    private fun refresh(project: Project) {
        EditorNotifications.getInstance(project).updateAllNotifications()
    }
}

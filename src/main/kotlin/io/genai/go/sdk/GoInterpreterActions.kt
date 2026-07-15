package io.genai.go.sdk

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files

/**
 * Interactive install flows shared by the settings panel and the editor banner.
 * All entry points run on the EDT; [onComplete] fires on the EDT after registration.
 */
object GoInterpreterActions {

    fun downloadInteractively(project: Project?, onComplete: (Sdk?) -> Unit) {
        val releases = GoDownloads.fetchAvailableWithProgress(project)
        if (releases.isEmpty()) {
            Messages.showInfoMessage("No portable Go builds are listed for this OS.", "Download Go")
            return
        }
        val dialog = GoDownloadDialog(releases)
        if (!dialog.showAndGet()) return
        val release = dialog.selected ?: return
        val home = GoSdkManager.plannedHome(release.version)

        ProgressManager.getInstance().run(object : Task.Modal(project, "Downloading Go ${release.version}", true) {
            override fun run(indicator: ProgressIndicator) {
                GoSdkDownloadTask(release, home).doDownload(indicator)
            }

            override fun onSuccess() {
                val sdk = GoSdkManager.registerFromHome(home.toString())
                if (sdk == null) {
                    Messages.showErrorDialog(
                        "Download finished but no go executable was found under\n$home",
                        "Download Go",
                    )
                }
                onComplete(sdk)
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(error.message ?: error.toString(), "Download Go Failed")
            }
        })
    }

    fun addFromDisk(onComplete: (Sdk?) -> Unit) {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Go Home Directory")
            .withDescription("Pick a Go toolchain folder (contains bin/go, or a go/ with it).")
        val root = GoSdkManager.downloadRoot()
        Files.createDirectories(root)
        val toSelect = LocalFileSystem.getInstance().findFileByNioFile(root)
        val chosen = FileChooser.chooseFile(descriptor, null, toSelect) ?: return
        val home = chosen.path
        if (GoSdkType.findGoExecutable(home) == null) {
            Messages.showErrorDialog("No go executable found under\n$home", "Add Go Toolchain")
            return
        }
        onComplete(GoSdkManager.registerFromHome(home))
    }
}

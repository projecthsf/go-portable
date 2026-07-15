package io.genai.go.tools

import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import io.genai.go.lsp.GoplsManager
import kotlin.io.path.Path

/**
 * Runs a `go` command (mod / get / build / test / …) with the portable Go toolchain in a Run
 * console, using the project directory as the working dir. No system Go needed. Environment is
 * the isolated portable setup (GOROOT + ~/.go-portable GOPATH/GOCACHE, toolchain on PATH).
 */
class RunGoToolAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val base = project.basePath ?: run {
            Messages.showErrorDialog(project, "No project directory.", "Run Go Tool")
            return
        }

        val go = GoplsManager.defaultGo()
        if (go == null) {
            Messages.showErrorDialog(
                project,
                "No Go toolchain configured. Set one up in Settings ▸ Go Portable.",
                "Run Go Tool",
            )
            return
        }

        val dialog = RunGoToolDialog(project)
        if (!dialog.showAndGet()) return
        val args = dialog.commandArgs
        if (args.isEmpty()) return

        val cmd = GeneralCommandLine()
            .withExePath(go.absolutePath)
            .withParameters(args)
            .withWorkDirectory(base)
            .withEnvironment(GoplsManager.environment(go))
        val handler = OSProcessHandler(cmd)
        // go writes files (go.sum, vendor/, build output) via an external process; refresh the
        // project dir when it finishes so new files show up without a manual "Reload from Disk".
        handler.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                LocalFileSystem.getInstance().findFileByNioFile(Path(base))?.let {
                    VfsUtil.markDirtyAndRefresh(true, true, true, it)
                }
            }
        })
        RunContentExecutor(project, handler)
            .withTitle("go ${args.joinToString(" ")}")
            .withActivateToolWindow(true)
            .run()
    }
}

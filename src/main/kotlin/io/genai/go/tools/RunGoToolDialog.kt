package io.genai.go.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Prompts for a `go` command: a dropdown of common commands, plus a package field that only
 * appears for `get` (the one that takes a package argument).
 */
class RunGoToolDialog(project: Project) : DialogWrapper(project) {

    private val commandCombo = ComboBox(COMMANDS)
    private val packageField = JBTextField()
    private lateinit var packageRow: Row

    init {
        title = "Run Go Tool"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row("Command:") { cell(commandCombo) }
            row("Package:") {
                cell(packageField).align(AlignX.FILL)
                    .comment("e.g. github.com/spf13/cobra@latest")
            }.also { packageRow = it }
        }
        commandCombo.addActionListener { syncPackageRow() }
        syncPackageRow()
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = commandCombo

    private fun syncPackageRow() = packageRow.visible(needsPackage())

    private fun needsPackage(): Boolean = (commandCombo.selectedItem as? String) == "get"

    /** The go argv, e.g. ["mod","tidy"] or ["get","github.com/x/y@latest"]. */
    val commandArgs: List<String>
        get() {
            val cmd = commandCombo.selectedItem as? String ?: return emptyList()
            val parts = cmd.split(" ")
            val pkg = packageField.text.trim()
            return if (cmd == "get" && pkg.isNotEmpty()) parts + pkg else parts
        }

    companion object {
        private val COMMANDS = arrayOf(
            "mod tidy", "mod download", "get", "build ./...", "test ./...",
            "vet ./...", "fmt ./...", "run .", "version",
        )
    }
}

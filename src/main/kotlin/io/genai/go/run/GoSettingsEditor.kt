package io.genai.go.run

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import io.genai.go.sdk.GoSdkType
import javax.swing.JComponent

class GoSettingsEditor(project: Project) : SettingsEditor<GoRunConfiguration>() {

    private val sdkCombo = ComboBox<String>()
    private val goField = TextFieldWithBrowseButton()
    private val scriptField = TextFieldWithBrowseButton()
    private val envComponent = EnvironmentVariablesComponent()

    init {
        sdkCombo.addItem(NONE)
        ProjectJdkTable.getInstance().getSdksOfType(GoSdkType.getInstance()).forEach {
            sdkCombo.addItem(it.name)
        }
        scriptField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select Go File"),
                project,
            ),
        )
        goField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select go Executable"),
                project,
            ),
        )
    }

    override fun createEditor(): JComponent = panel {
        row("Go SDK:") {
            cell(sdkCombo).align(AlignX.FILL)
        }
        row("Or go executable:") {
            cell(goField).align(AlignX.FILL)
        }.rowComment("Overrides the SDK above when set.")
        row("Go file:") {
            cell(scriptField).align(AlignX.FILL)
        }
        row {
            cell(envComponent).align(AlignX.FILL)
        }
    }

    override fun resetEditorFrom(s: GoRunConfiguration) {
        sdkCombo.selectedItem = s.sdkName?.takeIf { it.isNotBlank() } ?: NONE
        goField.text = s.goPath.orEmpty()
        scriptField.text = s.scriptPath.orEmpty()
        envComponent.envData = EnvironmentVariablesData.create(s.envs, s.passParentEnvs)
    }

    override fun applyEditorTo(s: GoRunConfiguration) {
        val selected = sdkCombo.selectedItem as? String
        s.sdkName = if (selected == null || selected == NONE) "" else selected
        s.goPath = goField.text.trim()
        s.scriptPath = scriptField.text.trim()
        val data = envComponent.envData
        s.envs = HashMap(data.envs)
        s.passParentEnvs = data.isPassParentEnvs
    }

    companion object {
        private const val NONE = "<none>"
    }
}

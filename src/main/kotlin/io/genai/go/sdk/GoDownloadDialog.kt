package io.genai.go.sdk

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** Lets the user pick which portable Go build to download. */
class GoDownloadDialog(releases: List<GoRelease>) : DialogWrapper(true) {
    private val combo = ComboBox(releases.toTypedArray())

    var selected: GoRelease? = null
        private set

    init {
        title = "Download Go Toolchain"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Go version:") {
            cell(combo).align(AlignX.FILL)
        }
        row {
            comment("Downloaded to ~/.go-portable and registered as a Go SDK.")
        }
    }

    override fun doOKAction() {
        selected = combo.selectedItem as? GoRelease
        super.doOKAction()
    }
}

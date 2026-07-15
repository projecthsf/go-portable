package io.genai.go.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * `.go` file type. Referenced by plugin.xml via fieldName="INSTANCE" — a Kotlin `object`
 * already exposes a static `INSTANCE` field, so no extra declaration needed.
 */
object GoFileType : LanguageFileType(GoLanguage) {
    private val ICON: Icon = IconLoader.getIcon("/icons/go.svg", GoFileType::class.java.classLoader)

    override fun getName(): String = "Go File"
    override fun getDescription(): String = "Go source file"
    override fun getDefaultExtension(): String = "go"
    override fun getIcon(): Icon = ICON
}

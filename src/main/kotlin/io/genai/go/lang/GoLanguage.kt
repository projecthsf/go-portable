package io.genai.go.lang

import com.intellij.lang.Language

/**
 * The Go language for this plugin.
 *
 * The ID is deliberately **"GoPortable"**, NOT "go": IntelliJ requires language IDs to be
 * globally unique, and the official JetBrains Go plugin (org.jetbrains.plugins.go, on GoLand /
 * IDEA Ultimate) already registers ID "go". A distinct ID lets us coexist quietly; the display
 * name is still "Go". The ID must match the `language=` attributes in plugin.xml.
 */
object GoLanguage : Language("GoPortable") {
    override fun getDisplayName(): String = "Go"
}

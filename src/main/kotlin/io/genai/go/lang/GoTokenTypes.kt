package io.genai.go.lang

import com.intellij.psi.tree.IElementType

object GoTokenTypes {
    @JvmField val KEYWORD = IElementType("GO_KEYWORD", GoLanguage)
    @JvmField val IDENTIFIER = IElementType("GO_IDENTIFIER", GoLanguage)
    @JvmField val STRING = IElementType("GO_STRING", GoLanguage)
    @JvmField val NUMBER = IElementType("GO_NUMBER", GoLanguage)
    @JvmField val LINE_COMMENT = IElementType("GO_LINE_COMMENT", GoLanguage)
    @JvmField val BLOCK_COMMENT = IElementType("GO_BLOCK_COMMENT", GoLanguage)
    @JvmField val OPERATOR = IElementType("GO_OPERATOR", GoLanguage)

    /** Go's 25 keywords (case-sensitive). */
    val KEYWORDS: Set<String> = setOf(
        "break", "case", "chan", "const", "continue", "default", "defer", "else",
        "fallthrough", "for", "func", "go", "goto", "if", "import", "interface",
        "map", "package", "range", "return", "select", "struct", "switch", "type", "var",
    )
}

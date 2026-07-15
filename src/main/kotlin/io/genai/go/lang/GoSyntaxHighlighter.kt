package io.genai.go.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Colors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey as key
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class GoSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = GoLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            GoTokenTypes.KEYWORD -> pack(KEYWORD)
            GoTokenTypes.STRING -> pack(STRING)
            GoTokenTypes.NUMBER -> pack(NUMBER)
            GoTokenTypes.LINE_COMMENT -> pack(LINE_COMMENT)
            GoTokenTypes.BLOCK_COMMENT -> pack(BLOCK_COMMENT)
            GoTokenTypes.IDENTIFIER -> pack(IDENTIFIER)
            GoTokenTypes.OPERATOR -> pack(OPERATOR)
            else -> EMPTY
        }

    companion object {
        val KEYWORD: TextAttributesKey = key("GO_KEYWORD", Colors.KEYWORD)
        val STRING: TextAttributesKey = key("GO_STRING", Colors.STRING)
        val NUMBER: TextAttributesKey = key("GO_NUMBER", Colors.NUMBER)
        val LINE_COMMENT: TextAttributesKey = key("GO_LINE_COMMENT", Colors.LINE_COMMENT)
        val BLOCK_COMMENT: TextAttributesKey = key("GO_BLOCK_COMMENT", Colors.BLOCK_COMMENT)
        val IDENTIFIER: TextAttributesKey = key("GO_IDENTIFIER", Colors.IDENTIFIER)
        val OPERATOR: TextAttributesKey = key("GO_OPERATOR", Colors.OPERATION_SIGN)
        private val EMPTY = emptyArray<TextAttributesKey>()
    }
}

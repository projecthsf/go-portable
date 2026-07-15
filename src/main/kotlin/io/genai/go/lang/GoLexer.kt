package io.genai.go.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * A small hand-written lexer for basic Go highlighting. It is intentionally not a full Go
 * grammar — it covers comments, strings (interpreted "…", raw `…`, and rune '…'), numbers,
 * keywords and identifiers. Semantic understanding comes from gopls via LSP; this is just the
 * local highlighting/PSI skeleton the platform expects.
 */
class GoLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.tokenStart = startOffset
        locateToken()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        tokenStart = tokenEnd
        locateToken()
    }

    private fun locateToken() {
        if (tokenStart >= endOffset) {
            tokenType = null
            tokenEnd = tokenStart
            return
        }
        val c = buffer[tokenStart]
        when {
            c.isWhitespace() -> whitespace()
            c == '/' && peek(tokenStart + 1) == '/' -> lineComment()
            c == '/' && peek(tokenStart + 1) == '*' -> blockComment()
            c == '"' || c == '\'' -> quoted(c)
            c == '`' -> rawString()
            c.isDigit() -> number()
            c == '_' || c.isLetter() -> word()
            else -> {
                tokenType = GoTokenTypes.OPERATOR
                tokenEnd = tokenStart + 1
            }
        }
    }

    private fun whitespace() {
        var i = tokenStart
        while (i < endOffset && buffer[i].isWhitespace()) i++
        tokenType = TokenType.WHITE_SPACE
        tokenEnd = i
    }

    private fun lineComment() {
        var i = tokenStart
        while (i < endOffset && buffer[i] != '\n') i++
        tokenType = GoTokenTypes.LINE_COMMENT
        tokenEnd = i
    }

    private fun blockComment() {
        var i = tokenStart + 2
        while (i < endOffset) {
            if (buffer[i] == '*' && peek(i + 1) == '/') { i += 2; break }
            i++
        }
        tokenType = GoTokenTypes.BLOCK_COMMENT
        tokenEnd = i.coerceAtMost(endOffset)
    }

    /** Interpreted string ("…") or rune ('…') — both honour backslash escapes. */
    private fun quoted(quote: Char) {
        var i = tokenStart + 1
        while (i < endOffset) {
            val ch = buffer[i]
            if (ch == '\\') { i += 2; continue }
            if (ch == quote || ch == '\n') { if (ch == quote) i++; break }
            i++
        }
        tokenType = GoTokenTypes.STRING
        tokenEnd = i.coerceAtMost(endOffset)
    }

    /** Raw string literal `…` — no escapes, may span lines. */
    private fun rawString() {
        var i = tokenStart + 1
        while (i < endOffset && buffer[i] != '`') i++
        if (i < endOffset) i++ // consume closing backtick
        tokenType = GoTokenTypes.STRING
        tokenEnd = i.coerceAtMost(endOffset)
    }

    private fun number() {
        var i = tokenStart
        while (i < endOffset) {
            val ch = buffer[i]
            if (ch.isDigit() || ch == '.' || ch == '_' || ch == 'x' || ch == 'X' ||
                ch == 'e' || ch == 'E' || ch == 'p' || ch == 'P' || ch == '+' || ch == '-' ||
                ch in 'a'..'f' || ch in 'A'..'F'
            ) {
                // + / - only continue an exponent (…e+, …p-); otherwise stop.
                if ((ch == '+' || ch == '-')) {
                    val prev = buffer[i - 1]
                    if (prev != 'e' && prev != 'E' && prev != 'p' && prev != 'P') break
                }
                i++
            } else break
        }
        tokenType = GoTokenTypes.NUMBER
        tokenEnd = i
    }

    private fun word() {
        var i = tokenStart
        while (i < endOffset && (buffer[i] == '_' || buffer[i].isLetterOrDigit())) i++
        val text = buffer.subSequence(tokenStart, i).toString()
        tokenType = if (text in GoTokenTypes.KEYWORDS) GoTokenTypes.KEYWORD else GoTokenTypes.IDENTIFIER
        tokenEnd = i
    }

    private fun peek(index: Int): Char = if (index < endOffset) buffer[index] else ' '
}

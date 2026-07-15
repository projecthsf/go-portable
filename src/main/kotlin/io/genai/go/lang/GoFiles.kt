package io.genai.go.lang

/** Single source of truth for what counts as a Go file, by extension. */
object GoFiles {
    /** Extensions we treat as Go. Used both to bind our file type (Community-only, see
     *  [GoLanguageActivation]) and by the tooling, which keys off the extension directly. */
    val EXTENSIONS: Set<String> = setOf("go")
}

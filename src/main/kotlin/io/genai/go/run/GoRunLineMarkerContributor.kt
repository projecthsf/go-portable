package io.genai.go.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.genai.go.lang.GoTokenTypes

/**
 * Puts a green ▶ Run marker in the gutter next to `func main()` in a `package main` file, so a
 * Go program launches with one click (like GoLand). Clicking it runs the [GoRunConfiguration]
 * for the file (`go run .` on the package).
 *
 * Our PSI is flat (one leaf per lexer token), so we anchor on the `main` identifier leaf that
 * directly follows the `func` keyword — exactly one marker per main function.
 */
class GoRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.firstChild != null) return null // leaves only
        if (element.node?.elementType != GoTokenTypes.IDENTIFIER) return null
        if (element.text != "main") return null
        if (PsiTreeUtil.prevVisibleLeaf(element)?.text != "func") return null // `func main`
        if (!isPackageMain(element)) return null

        val actions = ExecutorAction.getActions(0)
        if (actions.isEmpty()) return null
        return Info(AllIcons.RunConfigurations.TestState.Run, actions) { "Run go program" }
    }

    /** True if the file starts with a `package main` clause. */
    private fun isPackageMain(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        var leaf: PsiElement? = PsiTreeUtil.getDeepestFirst(file)
        while (leaf != null) {
            if (leaf.node?.elementType == GoTokenTypes.KEYWORD) {
                // The first keyword in a Go file is the package clause.
                return leaf.text == "package" && PsiTreeUtil.nextVisibleLeaf(leaf)?.text == "main"
            }
            leaf = PsiTreeUtil.nextLeaf(leaf)
        }
        return false
    }
}

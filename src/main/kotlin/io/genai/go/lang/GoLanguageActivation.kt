package io.genai.go.lang

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Binds our lightweight Go file type to `.go` files — but ONLY in an IDE that has no official
 * Go support (i.e. IntelliJ IDEA Community, the whole reason this plugin exists).
 *
 * On GoLand / IDEA Ultimate the official Go plugin owns `.go`, so we stay dormant: our
 * `<fileType>` is declared with no extensions (see plugin.xml), so we never statically claim
 * `.go` and there is nothing to clash. We only ever *add* the association, at runtime, when the
 * official plugin is absent — using the supported [FileTypeManager.associate] API.
 */
class GoLanguageActivation : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (!activated.compareAndSet(false, true)) return
        if (goExtensionAlreadyOwned()) return

        val fileTypeManager = FileTypeManager.getInstance()
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                for (ext in GoFiles.EXTENSIONS) {
                    fileTypeManager.associate(GoFileType, ExtensionFileNameMatcher(ext))
                }
            }
        }
    }

    /**
     * Is `.go` already claimed by some other file type? On GoLand / IDEA Ultimate the official
     * Go plugin registers it at load — before this runs — so we defer to whoever owns it.
     * `UnknownFileType` means nobody owns it (Community); our own [GoFileType] means we bound it
     * in a prior session (re-associating is harmless).
     */
    private fun goExtensionAlreadyOwned(): Boolean {
        val existing = FileTypeManager.getInstance().getFileTypeByExtension("go")
        return existing != UnknownFileType.INSTANCE && existing != GoFileType
    }

    companion object {
        private val activated = AtomicBoolean(false)
    }
}

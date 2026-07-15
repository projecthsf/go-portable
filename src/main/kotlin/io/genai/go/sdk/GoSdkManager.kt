package io.genai.go.sdk

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.EditorNotifications
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Create/list/remove portable Go SDKs. All mutating calls must run on the EDT
 * (button handlers and background-task success callbacks already are).
 */
object GoSdkManager {

    /** Root under which downloaded toolchains live; only these are deleted from disk. */
    fun downloadRoot(): Path = Paths.get(System.getProperty("user.home"), ".go-portable")

    fun plannedHome(version: String): Path = downloadRoot().resolve("go$version")

    fun listSdks(): List<Sdk> =
        ProjectJdkTable.getInstance().getSdksOfType(GoSdkType.getInstance())

    /**
     * Register the toolchain at [home] as a Go SDK.
     * @return the created SDK, or null if no go executable was found there.
     */
    fun registerFromHome(home: String): Sdk? {
        val type = GoSdkType.getInstance()
        if (GoSdkType.findGoExecutable(home) == null) return null

        val table = ProjectJdkTable.getInstance()
        val name = uniqueName(type.suggestSdkName(null, home))
        val sdk = table.createSdk(name, type)
        val modificator = sdk.sdkModificator
        modificator.homePath = home
        modificator.versionString = type.getVersionString(home)
        ApplicationManager.getApplication().runWriteAction {
            modificator.commitChanges()
            table.addJdk(sdk)
        }
        refreshBanners()
        return sdk
    }

    /** Remove [sdk] from the table, optionally deleting its files (only under [downloadRoot]). */
    fun remove(sdk: Sdk, deleteFiles: Boolean) {
        ApplicationManager.getApplication().runWriteAction {
            ProjectJdkTable.getInstance().removeJdk(sdk)
        }
        if (deleteFiles) {
            val home = sdk.homePath?.let { File(it) }
            val root = downloadRoot().toFile()
            if (home != null && home.absolutePath.startsWith(root.absolutePath + File.separator)) {
                home.deleteRecursively()
            }
        }
        refreshBanners()
    }

    private fun refreshBanners() {
        ProjectManager.getInstance().openProjects.forEach {
            EditorNotifications.getInstance(it).updateAllNotifications()
        }
    }

    data class CleanupResult(val removed: Int, val added: Int)

    /**
     * Remove registered toolchains whose files are gone, and register any install sitting
     * under [downloadRoot] that isn't registered yet (e.g. an interrupted download).
     */
    fun cleanUp(): CleanupResult {
        var removed = 0
        listSdks().forEach { sdk ->
            if (GoSdkType.findGoExecutable(sdk.homePath) == null) {
                remove(sdk, deleteFiles = false)
                removed++
            }
        }

        var added = 0
        val registered = listSdks().mapNotNull { it.homePath?.let { p -> File(p).absolutePath } }.toSet()
        downloadRoot().toFile().listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            if (dir.absolutePath !in registered && GoSdkType.findGoExecutable(dir.absolutePath) != null) {
                if (registerFromHome(dir.absolutePath) != null) added++
            }
        }
        return CleanupResult(removed, added)
    }

    private fun uniqueName(base: String): String {
        val table = ProjectJdkTable.getInstance()
        if (table.findJdk(base) == null) return base
        var i = 2
        while (table.findJdk("$base ($i)") != null) i++
        return "$base ($i)"
    }
}

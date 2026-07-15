package io.genai.go.sdk

import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.util.IconLoader
import org.jdom.Element
import java.io.File
import javax.swing.Icon

/**
 * The "Go Portable" SDK type. Downloading/switching Go is handled by our own UI
 * (Settings ▸ Go Portable), so we keep this type out of the platform's Java-oriented SDK
 * combos: `allowCreationByUser() = false` removes both the "Add" and "Download" actions there.
 */
class GoSdkType : SdkType("Go Portable") {

    override fun suggestHomePath(): String? = null

    override fun isValidSdkHome(path: String): Boolean = findGoExecutable(path) != null

    override fun getVersionString(sdkHome: String): String? {
        val go = findGoExecutable(sdkHome) ?: return null
        return try {
            val process = ProcessBuilder(go.absolutePath, "version")
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            // "go version go1.22.5 darwin/arm64" -> "1.22.5"
            Regex("""go(\d+(?:\.\d+)+)""").find(out)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        val version = getVersionString(sdkHome)
        return if (version != null) "Go $version" else "Go"
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator,
    ): AdditionalDataConfigurable? = null

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}

    override fun getPresentableName(): String = "Go Portable"

    override fun getIcon(): Icon = ICON

    override fun allowCreationByUser(): Boolean = false

    companion object {
        private val ICON: Icon = IconLoader.getIcon("/icons/go.svg", GoSdkType::class.java.classLoader)

        fun getInstance(): GoSdkType = SdkType.findInstance(GoSdkType::class.java)

        /**
         * Locate the `go` binary within an SDK home. Go archives expand into a top-level
         * `go/` folder (home/go/bin/go), so we check the root, home/bin, and one level of
         * nesting (home/<sub>/bin/go).
         */
        fun findGoExecutable(home: String?): File? {
            if (home.isNullOrBlank()) return null
            val root = File(home)
            if (!root.exists()) return null
            val names = listOf("go", "go.exe")
            val candidates = mutableListOf<File>()
            for (n in names) {
                candidates += File(File(root, "bin"), n)
                candidates += File(root, n)
            }
            root.listFiles()?.filter { it.isDirectory }?.forEach { sub ->
                for (n in names) {
                    candidates += File(File(sub, "bin"), n)
                    candidates += File(sub, n)
                }
            }
            return candidates.firstOrNull { it.isFile }
        }

        /**
         * GOROOT for an SDK home: the directory containing `bin/go`. Derived from the located
         * executable (bin/go -> its grandparent), so it works for both flat and nested layouts.
         */
        fun goRoot(home: String?): File? {
            val go = findGoExecutable(home) ?: return null
            // <root>/bin/go -> parentFile = bin, parentFile.parentFile = <root>
            return go.parentFile?.parentFile
        }
    }
}

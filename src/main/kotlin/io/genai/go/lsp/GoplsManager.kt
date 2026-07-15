package io.genai.go.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.util.SystemInfo
import io.genai.go.sdk.GoSdkManager
import io.genai.go.sdk.GoSdkType
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Locates (and, on request, installs) gopls — the official Go language server that powers code
 * intelligence. Unlike a single downloadable binary, gopls is installed with the portable Go
 * toolchain (`go install golang.org/x/tools/gopls@latest`) into ~/.go-portable/bin, so it runs
 * on the exact toolchain the user already has. One-time (~30–60s, needs network).
 */
object GoplsManager {

    private fun binDir(): Path = GoSdkManager.downloadRoot().resolve("bin")
    private fun goPath(): Path = GoSdkManager.downloadRoot().resolve("gopath")
    private fun goCache(): Path = GoSdkManager.downloadRoot().resolve("cache")

    fun goplsBin(): Path = binDir().resolve(if (SystemInfo.isWindows) "gopls.exe" else "gopls")

    fun isInstalled(): Boolean = Files.isRegularFile(goplsBin())

    /**
     * Environment for running `go` / `gopls` against the portable toolchain: GOROOT + isolated
     * GOPATH/GOCACHE under ~/.go-portable, and the toolchain's bin (plus our bin) prepended to
     * PATH so gopls can find `go`.
     */
    fun environment(go: File): Map<String, String> {
        val goRoot = go.parentFile?.parentFile // <root>/bin/go -> <root>
        val goBinDir = go.parentFile
        val existingPath = System.getenv("PATH").orEmpty()
        val path = listOfNotNull(goBinDir?.absolutePath, binDir().toString(), existingPath.ifBlank { null })
            .joinToString(File.pathSeparator)
        val env = mutableMapOf(
            "GOPATH" to goPath().toString(),
            "GOCACHE" to goCache().toString(),
            "GOMODCACHE" to goPath().resolve("pkg").resolve("mod").toString(),
            "PATH" to path,
        )
        goRoot?.let { env["GOROOT"] = it.absolutePath }
        return env
    }

    /**
     * Install gopls with the given portable go. Blocking (~30–60s) — call off the EDT.
     * Throws on failure with the command output.
     */
    fun install(go: File) {
        Files.createDirectories(binDir())
        Files.createDirectories(goPath())
        Files.createDirectories(goCache())
        val cmd = GeneralCommandLine(go.absolutePath, "install", "golang.org/x/tools/gopls@latest")
            .withEnvironment(environment(go) + mapOf("GOBIN" to binDir().toString(), "GO111MODULE" to "on"))
        val output = ExecUtil.execAndGetOutput(cmd)
        if (output.exitCode != 0 || !isInstalled()) {
            throw RuntimeException(
                "go install gopls failed (exit ${output.exitCode}).\n" +
                    output.stderr.ifBlank { output.stdout }.take(1000),
            )
        }
    }

    /** Alias used by the settings "Reinstall gopls" path. */
    fun download(go: File): Path {
        install(go)
        return goplsBin()
    }

    /** The current default toolchain's go binary, or null if none configured. */
    fun defaultGo(): File? =
        io.genai.go.settings.GoSettings.getInstance().defaultSdk()?.homePath
            ?.let { GoSdkType.findGoExecutable(it) }
}

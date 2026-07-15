package io.genai.go.sdk

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch

/**
 * Go toolchain catalog, fetched live from the official downloads feed so the version list is
 * always current: https://go.dev/dl/?mode=json lists the current stable releases and their
 * files. We pick the `archive` build for the current OS/arch (a self-contained tarball/zip —
 * extract and go, no installer). Falls back to a pinned version if the network is unavailable.
 */
object GoDownloads {

    private const val DL_JSON = "https://go.dev/dl/?mode=json"
    private const val DL_BASE = "https://go.dev/dl/"

    fun currentOs(): OsFamily = when {
        SystemInfo.isWindows -> OsFamily.WINDOWS
        SystemInfo.isMac -> OsFamily.MAC
        else -> OsFamily.LINUX
    }

    /** Go's arch naming (amd64 / arm64), not the platform's (x86_64 / aarch64). */
    private fun currentArch(): String = if (CpuArch.isArm64()) "arm64" else "amd64"

    private fun osTag(os: OsFamily): String = when (os) {
        OsFamily.MAC -> "darwin"
        OsFamily.LINUX -> "linux"
        OsFamily.WINDOWS -> "windows"
    }

    private fun ext(os: OsFamily): Pair<String, ArchiveKind> =
        if (os == OsFamily.WINDOWS) "zip" to ArchiveKind.ZIP else "tar.gz" to ArchiveKind.TAR_GZ

    fun fetchAvailableWithProgress(project: Project?): List<GoRelease> =
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable { fetchAvailable() },
            "Fetching Available Go Versions…",
            true,
            project,
        )

    /** Blocking fetch — must be called off the EDT. Returns newest-first. */
    fun fetchAvailable(): List<GoRelease> = try {
        val os = currentOs()
        val arch = currentArch()
        val tag = osTag(os)
        val (extension, kind) = ext(os)
        val json = HttpRequests.request(DL_JSON).readString()
        // Match e.g. "go1.22.5.darwin-arm64.tar.gz" straight out of the JSON text.
        val regex = Regex("""go(\d+(?:\.\d+)+)\.$tag-$arch\.${Regex.escape(extension)}""")
        regex.findAll(json).map { it.groupValues[1] }.distinct().toList()
            .sortedWith(VERSION_DESC)
            .map { v -> GoRelease(v, os, arch, "$DL_BASE" + "go$v.$tag-$arch.$extension", kind) }
            .ifEmpty { fallback() }
    } catch (e: Exception) {
        fallback()
    }

    private fun fallback(): List<GoRelease> {
        val os = currentOs()
        val arch = currentArch()
        val tag = osTag(os)
        val (extension, kind) = ext(os)
        val v = "1.22.5"
        return listOf(GoRelease(v, os, arch, "$DL_BASE" + "go$v.$tag-$arch.$extension", kind))
    }

    /** Descending semantic-ish version order (1.23.0 before 1.22.5). */
    private val VERSION_DESC: Comparator<String> = Comparator<String> { a, b ->
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (d != 0) return@Comparator d
        }
        0
    }.reversed()
}

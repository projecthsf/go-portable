package io.genai.go.sdk

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import java.nio.file.Files
import java.nio.file.Path

/**
 * Downloads a portable Go build into [homeDir] and extracts it. The archive expands into a
 * top-level `go/` folder, so the toolchain ends up at [homeDir]/go; [GoSdkType.findGoExecutable]
 * then locates `go/bin/go` within it.
 */
class GoSdkDownloadTask(
    private val release: GoRelease,
    private val homeDir: Path,
) : SdkDownloadTask {

    override fun getSuggestedSdkName(): String = "Go ${release.version}"
    override fun getPlannedHomeDir(): String = homeDir.toString()
    override fun getPlannedVersion(): String = release.version

    override fun doDownload(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        indicator.text = "Downloading Go ${release.version}…"
        Files.createDirectories(homeDir)

        when (release.kind) {
            ArchiveKind.ZIP -> extractArchive(indicator, ".zip") { tmp ->
                Decompressor.Zip(tmp).extract(homeDir)
            }
            ArchiveKind.TAR_GZ -> extractArchive(indicator, ".tar.gz") { tmp ->
                Decompressor.Tar(tmp).extract(homeDir)
            }
        }
    }

    private fun extractArchive(indicator: ProgressIndicator, suffix: String, extract: (Path) -> Unit) {
        val tmp = Files.createTempFile("go-", suffix)
        try {
            HttpRequests.request(release.url).saveToFile(tmp.toFile(), indicator)
            indicator.text = "Extracting Go ${release.version}…"
            extract(tmp)
        } finally {
            Files.deleteIfExists(tmp)
        }
    }
}

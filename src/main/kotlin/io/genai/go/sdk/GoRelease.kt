package io.genai.go.sdk

enum class OsFamily { WINDOWS, MAC, LINUX }

enum class ArchiveKind { ZIP, TAR_GZ }

/** A single downloadable portable Go toolchain build. */
data class GoRelease(
    val version: String,   // e.g. "1.22.5" (without the "go" prefix)
    val os: OsFamily,
    val arch: String,      // Go arch: amd64 / arm64
    val url: String,
    val kind: ArchiveKind,
) {
    val label: String get() = "Go $version  ·  ${os.name.lowercase()}/$arch"
    override fun toString(): String = label
}

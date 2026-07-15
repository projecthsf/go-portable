package io.genai.go.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider

/**
 * Launches gopls (`~/.go-portable/bin/gopls`) talking LSP over stdio, with an environment that
 * points at the portable Go toolchain (GOROOT + isolated GOPATH/GOCACHE, and the toolchain's
 * bin on PATH so gopls can invoke `go`). [GoClientFeatures.isEnabled] gates this up front, so if
 * no toolchain or gopls is available we never get here.
 */
class GoplsConnectionProvider(project: Project) : ProcessStreamConnectionProvider() {
    init {
        val go = GoplsManager.defaultGo()
        if (go != null && GoplsManager.isInstalled()) {
            setCommands(listOf(GoplsManager.goplsBin().toString()))
            project.basePath?.let { setWorkingDirectory(it) }
            setIncludeSystemEnvironmentVariables(true)
            setUserEnvironmentVariables(GoplsManager.environment(go))
        }
    }
}

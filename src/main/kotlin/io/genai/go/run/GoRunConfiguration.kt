package io.genai.go.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import io.genai.go.sdk.GoSdkType
import io.genai.go.settings.GoSettings
import java.io.File

class GoRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String?,
) : RunConfigurationBase<GoRunConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): GoRunConfigurationOptions =
        super.getOptions() as GoRunConfigurationOptions

    var scriptPath: String?
        get() = options.scriptPath
        set(value) { options.scriptPath = value }

    var sdkName: String?
        get() = options.sdkName
        set(value) { options.sdkName = value }

    var goPath: String?
        get() = options.goPath
        set(value) { options.goPath = value }

    var envs: MutableMap<String, String>
        get() = options.envs
        set(value) { options.envs = value }

    var passParentEnvs: Boolean
        get() = options.passParentEnvs
        set(value) { options.passParentEnvs = value }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        GoSettingsEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val script = scriptPath?.takeIf { it.isNotBlank() }
                    ?: throw ExecutionException("No Go file specified")
                val go = resolveGo()
                    ?: throw ExecutionException(
                        "No Go toolchain configured — pick a Go SDK or set a go executable path",
                    )

                val scriptFile = File(script)
                val dir = scriptFile.parentFile
                val moduleRoot = dir?.let { findModuleRoot(it) }

                val cmd = GeneralCommandLine()
                cmd.exePath = go
                cmd.addParameter("run")
                if (moduleRoot != null) {
                    // Run the file's PACKAGE (not just the one file) so symbols from sibling files
                    // resolve — `go run <file>` compiles only that file and reports the rest as
                    // "undefined". Crucially, run it from the MODULE ROOT (working dir), not the
                    // file's directory: a program's relative file lookups (e.g. loading a .env)
                    // resolve against the process CWD, and GoLand's default working dir is the
                    // project root. Running from cmd/server/ instead made the app miss the repo
                    // .env and fall back to localhost. So: cwd = module root, package = ./cmd/server.
                    cmd.addParameter(packagePath(moduleRoot, dir))
                    cmd.setWorkDirectory(moduleRoot)
                } else {
                    // No module: a loose single file — the only thing that works is `go run file`.
                    cmd.addParameter(script)
                    dir?.let { cmd.setWorkDirectory(it) }
                }
                // GOROOT is auto-detected by the go binary from its own location; GOPATH/GOCACHE
                // default sensibly. Apply the user's environment variables on top (e.g. ENV_FILE),
                // optionally inheriting the parent environment — same as any run configuration.
                cmd.withEnvironment(options.envs)
                cmd.withParentEnvironmentType(
                    if (options.passParentEnvs) GeneralCommandLine.ParentEnvironmentType.CONSOLE
                    else GeneralCommandLine.ParentEnvironmentType.NONE,
                )

                val handler = OSProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }

    /** The nearest ancestor of [dir] (inclusive) that contains a go.mod, or null. */
    private fun findModuleRoot(dir: File): File? =
        generateSequence(dir) { it.parentFile }.firstOrNull { File(it, "go.mod").isFile }

    /** Import path of [dir] relative to [moduleRoot], e.g. "./cmd/server" (or "." at the root). */
    private fun packagePath(moduleRoot: File, dir: File): String {
        val rel = moduleRoot.toPath().relativize(dir.toPath()).toString()
        return if (rel.isEmpty()) "." else "./" + rel.replace(File.separatorChar, '/')
    }

    /** An explicit go path wins; then the SDK pinned on this config; otherwise the current default. */
    private fun resolveGo(): String? {
        goPath?.takeIf { it.isNotBlank() }?.let { return it }

        val name = sdkName?.takeIf { it.isNotBlank() }
        val pinned = name?.let { ProjectJdkTable.getInstance().findJdk(it) }
        pinned?.homePath?.let { GoSdkType.findGoExecutable(it) }?.let { return it.absolutePath }

        val default = GoSettings.getInstance().defaultSdk()
        return default?.homePath?.let { GoSdkType.findGoExecutable(it)?.absolutePath }
    }
}

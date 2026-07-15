package io.genai.go.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import io.genai.go.lang.GoFiles
import java.io.File

/**
 * Makes a `.go` file runnable directly: right-click ▸ Run, and a gutter ▶ marker.
 * Auto-fills the file path; the run follows the current default toolchain unless pinned.
 */
class GoRunConfigurationProducer : LazyRunConfigurationProducer<GoRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(GoRunConfigurationType::class.java)
            .configurationFactories[0]

    override fun setupConfigurationFromContext(
        configuration: GoRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val file = goFile(context) ?: return false
        configuration.scriptPath = file.path
        configuration.name = file.name
        // Convenience matching GoLand: if the module has a root .env, default ENV_FILE=.env, so
        // apps that gate config loading on ENV_FILE (a common pattern) pick it up automatically.
        // The run's working dir is the module root, so a relative ".env" resolves. User-editable.
        if (defaultEnvFile(file) != null && !configuration.envs.containsKey("ENV_FILE")) {
            configuration.envs["ENV_FILE"] = ".env"
        }
        return true
    }

    /** ".env" if the file's module has a go.mod-root .env, else null. */
    private fun defaultEnvFile(vf: VirtualFile): String? {
        val dir = File(vf.path).parentFile ?: return null
        val moduleRoot = generateSequence(dir) { it.parentFile }
            .firstOrNull { File(it, "go.mod").isFile } ?: return null
        return if (File(moduleRoot, ".env").isFile) ".env" else null
    }

    override fun isConfigurationFromContext(
        configuration: GoRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val file = goFile(context) ?: return false
        val script = configuration.scriptPath ?: return false
        return FileUtil.pathsEqual(script, file.path)
    }

    private fun goFile(context: ConfigurationContext): VirtualFile? {
        val vf = CommonDataKeys.VIRTUAL_FILE.getData(context.dataContext)
            ?: context.psiLocation?.containingFile?.virtualFile
        if (vf == null || vf.isDirectory) return null
        return if (vf.extension?.lowercase() in GoFiles.EXTENSIONS) vf else null
    }
}

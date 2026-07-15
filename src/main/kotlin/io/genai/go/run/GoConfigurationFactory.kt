package io.genai.go.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.project.Project

class GoConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "GoPortableRun"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        GoRunConfiguration(project, this, "Go")

    override fun getOptionsClass(): Class<out RunConfigurationOptions> =
        GoRunConfigurationOptions::class.java
}

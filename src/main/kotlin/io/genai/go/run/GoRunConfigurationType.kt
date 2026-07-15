package io.genai.go.run

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullLazyValue

class GoRunConfigurationType : ConfigurationTypeBase(
    "GoPortableRunConfiguration",
    // "(Portable)" so it's distinct from the official Go plugin's run type on GoLand / Ultimate.
    "Go File (Portable)",
    "Run a Go file with a portable Go toolchain",
    NotNullLazyValue.createValue { AllIcons.Actions.Execute },
) {
    init {
        addFactory(GoConfigurationFactory(this))
    }
}

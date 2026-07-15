package io.genai.go.run

import com.intellij.execution.configurations.RunConfigurationOptions

/** Persisted state for a Go run configuration. */
class GoRunConfigurationOptions : RunConfigurationOptions() {
    private val scriptPathProp = string("").provideDelegate(this, "scriptPath")
    private val sdkNameProp = string("").provideDelegate(this, "sdkName")
    private val goPathProp = string("").provideDelegate(this, "goPath")

    var scriptPath: String?
        get() = scriptPathProp.getValue(this)
        set(value) = scriptPathProp.setValue(this, value)

    var sdkName: String?
        get() = sdkNameProp.getValue(this)
        set(value) = sdkNameProp.setValue(this, value)

    var goPath: String?
        get() = goPathProp.getValue(this)
        set(value) = goPathProp.setValue(this, value)

    /** User environment variables for the run (e.g. ENV_FILE=.env). */
    var envs by map<String, String>()

    /** Whether to inherit the system/parent environment on top of [envs]. */
    var passParentEnvs by property(true)
}

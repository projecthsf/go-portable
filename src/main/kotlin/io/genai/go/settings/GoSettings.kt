package io.genai.go.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.projectRoots.Sdk
import io.genai.go.sdk.GoSdkManager
import io.genai.go.sdk.GoSdkType

/**
 * Remembers which Go toolchain is the "current" one. Application-level, since the toolchains
 * (SDKs) are application-level. Run configs with no explicit toolchain fall back to this.
 */
@Service(Service.Level.APP)
@State(name = "GoPortable", storages = [Storage("go-portable.xml")])
class GoSettings : PersistentStateComponent<GoSettings.State> {

    class State {
        var defaultSdkName: String? = null
        // Code intelligence (gopls completion, navigation, errors). Default ON — the headline
        // feature beyond "run a file". Gated via GoClientFeatures.isEnabled.
        var codeIntelligenceEnabled: Boolean = true
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    var defaultSdkName: String?
        get() = myState.defaultSdkName
        set(value) { myState.defaultSdkName = value }

    var codeIntelligenceEnabled: Boolean
        get() = myState.codeIntelligenceEnabled
        set(value) { myState.codeIntelligenceEnabled = value }

    /** The selected toolchain, restricted to ones that still exist on disk, falling back to
     *  the first usable install. */
    fun defaultSdk(): Sdk? {
        val usable = GoSdkManager.listSdks().filter { GoSdkType.findGoExecutable(it.homePath) != null }
        return usable.firstOrNull { it.name == myState.defaultSdkName } ?: usable.firstOrNull()
    }

    companion object {
        fun getInstance(): GoSettings = service()
    }
}

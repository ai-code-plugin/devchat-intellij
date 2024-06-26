package ai.devchat.storage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.*

/**
 * Supports storing the DevChat settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent DevChat settings are stored.
 */
@State(name = "org.intellij.sdk.settings.DevChatSettingsState", storages = [Storage("DevChatSettings.xml")])
class DevChatSettingsState : PersistentStateComponent<DevChatSettingsState?> {
    var apiBase = "https://api.devchat.ai/v1"
    var apiKey = ""
    var defaultModel = "gpt-3.5-turbo"
    var maxLogCount = 20
    var language = Locale.getDefault().language.takeIf { it == "zh" } ?: "en"
    var pythonForChat = ""
    var pythonForCommands = ""
    override fun getState(): DevChatSettingsState {
        return this
    }

    override fun loadState(state: DevChatSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: DevChatSettingsState
            get() = ApplicationManager.getApplication().getService(
                DevChatSettingsState::class.java
            )
    }
}

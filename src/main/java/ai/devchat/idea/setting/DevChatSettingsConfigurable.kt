package ai.devchat.idea.setting

import ai.devchat.idea.storage.SensitiveDataStorage
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Provides controller functionality for DevChat settings.
 */
class DevChatSettingsConfigurable : Configurable {
    private var devChatSettingsComponent: DevChatSettingsComponent? = null

    // A default constructor with no arguments is required because this implementation
    // is registered in an applicationConfigurable EP
    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String? {
        return "DevChat"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return devChatSettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent? {
        devChatSettingsComponent = DevChatSettingsComponent()
        return devChatSettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings: DevChatSettingsState = DevChatSettingsState.instance
        return devChatSettingsComponent!!.apiBase != settings.apiBase ||
                devChatSettingsComponent!!.apiKey != settings.apiKey ||
                devChatSettingsComponent!!.defaultModel != settings.defaultModel
    }

    override fun apply() {
        val settings: DevChatSettingsState = DevChatSettingsState.instance
        settings.apiBase = devChatSettingsComponent!!.apiBase
        settings.apiKey = devChatSettingsComponent!!.apiKey
        settings.defaultModel = devChatSettingsComponent!!.defaultModel
        SensitiveDataStorage.setKey(settings.apiKey)
    }

    override fun reset() {
        val settings: DevChatSettingsState = DevChatSettingsState.instance
        devChatSettingsComponent!!.apiBase = settings.apiBase
        devChatSettingsComponent!!.apiKey = settings.apiKey
        devChatSettingsComponent!!.defaultModel = settings.defaultModel
        SensitiveDataStorage.setKey(settings.apiKey)
    }

    override fun disposeUIResources() {
        devChatSettingsComponent = null
    }
}

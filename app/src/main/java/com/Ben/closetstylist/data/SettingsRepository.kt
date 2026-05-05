package com.Ben.closetstylist.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.Ben.closetstylist.domain.StylistPersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = createPrefs(context)

    private val _claudeApiKey = MutableStateFlow(prefs.getString(KEY_CLAUDE, "") ?: "")
    val claudeApiKey: StateFlow<String> = _claudeApiKey.asStateFlow()

    private val _weatherApiKey = MutableStateFlow(prefs.getString(KEY_WEATHER, "") ?: "")
    val weatherApiKey: StateFlow<String> = _weatherApiKey.asStateFlow()

    private val _selectedPersona = MutableStateFlow(
        prefs.getString(KEY_PERSONA, null)
            ?.let { runCatching { StylistPersona.valueOf(it) }.getOrNull() }
            ?: StylistPersona.MINIMALIST,
    )
    val selectedPersona: StateFlow<StylistPersona> = _selectedPersona.asStateFlow()

    fun saveClaudeApiKey(key: String) {
        prefs.edit().putString(KEY_CLAUDE, key).apply()
        _claudeApiKey.value = key
    }

    fun saveWeatherApiKey(key: String) {
        prefs.edit().putString(KEY_WEATHER, key).apply()
        _weatherApiKey.value = key
    }

    fun savePersona(persona: StylistPersona) {
        prefs.edit().putString(KEY_PERSONA, persona.name).apply()
        _selectedPersona.value = persona
    }

    companion object {
        private const val PREFS_FILE = "closet_stylist_settings"
        private const val KEY_CLAUDE = "claude_api_key"
        private const val KEY_WEATHER = "weather_api_key"
        private const val KEY_PERSONA = "stylist_persona"

        private fun createPrefs(context: Context) = try {
            openPrefs(context)
        } catch (e: Exception) {
            // Keystore can become corrupted after a backup/restore or factory reset on some
            // devices; wiping the file and re-creating is the only safe recovery path.
            context.deleteSharedPreferences(PREFS_FILE)
            openPrefs(context)
        }

        private fun openPrefs(context: Context) = EncryptedSharedPreferences.create(
            PREFS_FILE,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}

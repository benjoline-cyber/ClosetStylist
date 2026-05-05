package com.Ben.closetstylist.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.Ben.closetstylist.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val claudeKeyInput: String = "",
    val weatherKeyInput: String = "",
    val claudeKeySaved: Boolean = false,
    val weatherKeySaved: Boolean = false,
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        repository.claudeApiKey
            .onEach { key -> _uiState.update { it.copy(claudeKeySaved = key.isNotEmpty()) } }
            .launchIn(viewModelScope)

        repository.weatherApiKey
            .onEach { key -> _uiState.update { it.copy(weatherKeySaved = key.isNotEmpty()) } }
            .launchIn(viewModelScope)
    }

    fun onClaudeKeyInput(value: String) = _uiState.update { it.copy(claudeKeyInput = value) }

    fun onWeatherKeyInput(value: String) = _uiState.update { it.copy(weatherKeyInput = value) }

    fun saveClaudeKey() {
        repository.saveClaudeApiKey(_uiState.value.claudeKeyInput.trim())
        _uiState.update { it.copy(claudeKeyInput = "") }
    }

    fun saveWeatherKey() {
        repository.saveWeatherApiKey(_uiState.value.weatherKeyInput.trim())
        _uiState.update { it.copy(weatherKeyInput = "") }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repository) as T
    }
}

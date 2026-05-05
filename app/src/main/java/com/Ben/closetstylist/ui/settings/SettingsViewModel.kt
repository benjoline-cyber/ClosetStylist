package com.Ben.closetstylist.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.Ben.closetstylist.data.SettingsRepository
import com.Ben.closetstylist.network.ClaudeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class TestConnectionState {
    data object Idle : TestConnectionState()
    data object Testing : TestConnectionState()
    data object Success : TestConnectionState()
    data class Failure(val message: String) : TestConnectionState()
}

data class SettingsUiState(
    val claudeKeyInput: String = "",
    val weatherKeyInput: String = "",
    val claudeKeySaved: Boolean = false,
    val weatherKeySaved: Boolean = false,
    val claudeKeyTail: String = "",
    val weatherKeyTail: String = "",
    val editingClaude: Boolean = false,
    val editingWeather: Boolean = false,
    val testState: TestConnectionState = TestConnectionState.Idle,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val claudeRepository: ClaudeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        repository.claudeApiKey
            .onEach { key ->
                _uiState.update {
                    it.copy(
                        claudeKeySaved = key.isNotEmpty(),
                        claudeKeyTail = if (key.length >= 4) key.takeLast(4) else key,
                        editingClaude = if (key.isEmpty()) true else it.editingClaude,
                    )
                }
            }
            .launchIn(viewModelScope)

        repository.weatherApiKey
            .onEach { key ->
                _uiState.update {
                    it.copy(
                        weatherKeySaved = key.isNotEmpty(),
                        weatherKeyTail = if (key.length >= 4) key.takeLast(4) else key,
                        editingWeather = if (key.isEmpty()) true else it.editingWeather,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onClaudeKeyInput(value: String) = _uiState.update { it.copy(claudeKeyInput = value) }
    fun onWeatherKeyInput(value: String) = _uiState.update { it.copy(weatherKeyInput = value) }

    fun startEditClaude() = _uiState.update { it.copy(editingClaude = true, claudeKeyInput = "") }
    fun startEditWeather() = _uiState.update { it.copy(editingWeather = true, weatherKeyInput = "") }

    fun saveClaudeKey() {
        repository.saveClaudeApiKey(_uiState.value.claudeKeyInput.trim())
        _uiState.update { it.copy(claudeKeyInput = "", editingClaude = false, testState = TestConnectionState.Idle) }
    }

    fun saveWeatherKey() {
        repository.saveWeatherApiKey(_uiState.value.weatherKeyInput.trim())
        _uiState.update { it.copy(weatherKeyInput = "", editingWeather = false) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(testState = TestConnectionState.Testing) }
            val success = runCatching { claudeRepository.testConnection() }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    testState = if (success) TestConnectionState.Success
                    else TestConnectionState.Failure("Connection failed — check your Claude API key"),
                )
            }
        }
    }

    class Factory(
        private val repository: SettingsRepository,
        private val claudeRepository: ClaudeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repository, claudeRepository) as T
    }
}

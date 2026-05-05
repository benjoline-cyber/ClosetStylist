package com.Ben.closetstylist.ui.suggest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.Ben.closetstylist.R
import com.Ben.closetstylist.data.ClothingItem
import com.Ben.closetstylist.data.ClothingRepository
import com.Ben.closetstylist.data.FeedbackType
import com.Ben.closetstylist.data.InspirationRepository
import com.Ben.closetstylist.data.OutfitFeedback
import com.Ben.closetstylist.data.OutfitFeedbackRepository
import com.Ben.closetstylist.data.SettingsRepository
import com.Ben.closetstylist.data.WeatherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import com.Ben.closetstylist.domain.StylistPersona
import com.Ben.closetstylist.domain.OutfitJsonParser
import com.Ben.closetstylist.domain.ParsedOutfit
import com.Ben.closetstylist.domain.PromptBuilder
import com.Ben.closetstylist.domain.WeatherInfo
import com.Ben.closetstylist.network.ClaudeRepository
import com.Ben.closetstylist.util.getCurrentLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OutfitSuggestion(
    val items: List<ClothingItem>,
    val rationale: String,
) {
    val cardKey: String get() = items.map { it.id }.sorted().joinToString(",")
}

data class SuggestUiState(
    val isLoading: Boolean = false,
    val outfits: List<OutfitSuggestion> = emptyList(),
    val error: String? = null,
    val collapsedKeys: Set<String> = emptySet(),
    val loadingCaption: String = "",
    val weatherStale: Boolean = false,
    val awaitingWeatherChoice: Boolean = false,
)

sealed class FeedbackResult {
    abstract val cardKey: String

    data class Wore(
        val outfit: OutfitSuggestion,
        override val cardKey: String,
        val previousDates: Map<String, LocalDate?>,
    ) : FeedbackResult()

    data class Dismissed(override val cardKey: String) : FeedbackResult()

    data class RejectedCombo(
        val feedback: OutfitFeedback,
        override val cardKey: String,
    ) : FeedbackResult()
}

class SuggestViewModel(
    application: Application,
    private val clothingRepository: ClothingRepository,
    private val inspirationRepository: InspirationRepository,
    private val weatherRepository: WeatherRepository,
    private val claudeRepository: ClaudeRepository,
    private val settingsRepository: SettingsRepository,
    private val outfitFeedbackRepository: OutfitFeedbackRepository,
) : AndroidViewModel(application) {

    val selectedPersona: StateFlow<StylistPersona> = settingsRepository.selectedPersona

    fun selectPersona(persona: StylistPersona) = settingsRepository.savePersona(persona)

    val hasInspiration: StateFlow<Boolean> = inspirationRepository.getAllPhotos()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val itemCount: StateFlow<Int> = clothingRepository.getAllItems()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _uiState = MutableStateFlow(SuggestUiState())
    val uiState: StateFlow<SuggestUiState> = _uiState.asStateFlow()

    private val _weatherSummary = MutableStateFlow("")
    val weatherSummary: StateFlow<String> = _weatherSummary.asStateFlow()

    private val _feedbackEvent = MutableSharedFlow<FeedbackResult>()
    val feedbackEvent: SharedFlow<FeedbackResult> = _feedbackEvent.asSharedFlow()

    private val _debugWeatherOverride = MutableStateFlow<WeatherInfo?>(null)
    val debugWeatherOverride: StateFlow<WeatherInfo?> = _debugWeatherOverride.asStateFlow()

    private val _manualWeatherPreset = MutableStateFlow<WeatherInfo?>(null)

    private val _refreshReason = MutableStateFlow("")
    val refreshReason: StateFlow<String> = _refreshReason.asStateFlow()

    fun onRefreshReasonChange(value: String) { _refreshReason.value = value }

    fun selectWeatherPreset(weather: WeatherInfo) {
        _manualWeatherPreset.value = weather
        _uiState.value = _uiState.value.copy(awaitingWeatherChoice = false)
        generateSuggestions()
    }

    private var cachedWeather: WeatherInfo? = null
    private var cacheTimestamp: Long = 0L
    private val cacheMaxAgeMs = 2 * 60 * 60 * 1000L

    fun setDebugWeather(tempCelsius: Double, condition: String) {
        _debugWeatherOverride.value = WeatherInfo(tempCelsius, condition, "Debug")
    }

    fun clearDebugWeather() {
        _debugWeatherOverride.value = null
    }

    fun generateSuggestions() {
        viewModelScope.launch {
            _uiState.value = SuggestUiState(isLoading = true)
            val app = getApplication<Application>()
            try {
                val weather = _debugWeatherOverride.value
                    ?: _manualWeatherPreset.value
                    ?: fetchWeatherWithCache(app)
                    ?: return@launch

                _weatherSummary.value = weather.summary()

                val cutoff = LocalDate.now().minusDays(3)
                val available = clothingRepository.getAvailableItems(cutoff).first()

                if (available.isEmpty()) {
                    _uiState.value = SuggestUiState(
                        error = app.getString(R.string.error_suggest_empty_closet),
                    )
                    return@launch
                }

                val persona = settingsRepository.selectedPersona.value
                _uiState.value = SuggestUiState(
                    isLoading = true,
                    loadingCaption = app.getString(
                        R.string.suggest_loading_caption,
                        available.size,
                        weather.summary(),
                        persona.displayName(),
                    ),
                    weatherStale = _uiState.value.weatherStale,
                )

                val inspirationBytes = inspirationRepository.getAllPhotos().first()
                    .take(3)
                    .mapNotNull { photo ->
                        runCatching {
                            File(photo.imagePath.removePrefix("file://")).readBytes()
                        }.getOrNull()
                    }

                val rejections = outfitFeedbackRepository.getRecentRejections(persona)
                val idToItem = available.associateBy { it.id }
                val rejectionDescriptions = rejections.mapNotNull { feedback ->
                    val desc = feedback.itemIds.split(",")
                        .mapNotNull { id -> idToItem[id]?.description?.ifBlank { null } }
                        .joinToString(" + ")
                    desc.ifBlank { null }
                }

                val reason = _refreshReason.value
                val prompt = PromptBuilder.buildOutfitPrompt(available, weather, persona, rejectionDescriptions, reason)
                val availableIds = available.map { it.id }.toSet()
                val parsed = callWithRetry(prompt, inspirationBytes, availableIds)

                val suggestions = parsed.mapNotNull { p ->
                    val items = p.itemIds.mapNotNull { idToItem[it] }
                    if (items.isNotEmpty()) OutfitSuggestion(items, p.rationale) else null
                }

                _refreshReason.value = ""

                if (suggestions.isEmpty()) {
                    _uiState.value = SuggestUiState(error = app.getString(R.string.error_suggest_parse_failed))
                } else {
                    _uiState.value = SuggestUiState(outfits = suggestions, weatherStale = _uiState.value.weatherStale)
                }
            } catch (e: Exception) {
                val msg = errorMessage(app, e)
                _uiState.value = SuggestUiState(error = msg)
            }
        }
    }

    private suspend fun fetchWeatherWithCache(app: Application): WeatherInfo? {
        val isCacheFresh = cachedWeather != null &&
            System.currentTimeMillis() - cacheTimestamp < cacheMaxAgeMs
        if (isCacheFresh) {
            _uiState.value = _uiState.value.copy(weatherStale = true)
            return cachedWeather
        }
        return try {
            val (lat, lon) = getCurrentLocation(app)
            val w = weatherRepository.fetchWeather(lat, lon)
            cachedWeather = w
            cacheTimestamp = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(weatherStale = false)
            w
        } catch (e: Exception) {
            val stale = cachedWeather
            if (stale != null) {
                _uiState.value = _uiState.value.copy(weatherStale = true)
                stale
            } else {
                _uiState.value = SuggestUiState(awaitingWeatherChoice = true)
                null
            }
        }
    }

    private fun errorMessage(app: Application, e: Exception): String = when {
        e.message?.contains("API key") == true ->
            app.getString(R.string.error_claude_key_invalid)
        e.message?.contains("location", ignoreCase = true) == true ->
            app.getString(R.string.error_suggest_no_location)
        e.message?.contains("overloaded", ignoreCase = true) == true ||
            e.message?.contains("529") == true ->
            app.getString(R.string.error_claude_overloaded)
        e is java.net.UnknownHostException || e is java.net.ConnectException ->
            app.getString(R.string.error_no_internet)
        else -> app.getString(R.string.error_suggest_failed)
    }

    fun onWore(outfit: OutfitSuggestion) {
        val cardKey = outfit.cardKey
        val previousDates = outfit.items.associate { it.id to it.lastWornDate }
        val today = LocalDate.now()
        collapse(cardKey)
        viewModelScope.launch {
            outfit.items.forEach { item -> clothingRepository.recordWorn(item.id, today) }
            _feedbackEvent.emit(FeedbackResult.Wore(outfit, cardKey, previousDates))
        }
    }

    fun onDismissed(outfit: OutfitSuggestion) {
        val cardKey = outfit.cardKey
        collapse(cardKey)
        viewModelScope.launch {
            _feedbackEvent.emit(FeedbackResult.Dismissed(cardKey))
        }
    }

    fun onRejectedCombo(outfit: OutfitSuggestion) {
        val cardKey = outfit.cardKey
        val feedback = OutfitFeedback(
            id = UUID.randomUUID().toString(),
            itemIds = cardKey,
            feedbackType = FeedbackType.REJECTED_COMBO,
            createdAt = Instant.now().toEpochMilli(),
            persona = settingsRepository.selectedPersona.value.name,
        )
        collapse(cardKey)
        viewModelScope.launch {
            outfitFeedbackRepository.save(feedback)
            _feedbackEvent.emit(FeedbackResult.RejectedCombo(feedback, cardKey))
        }
    }

    fun undoFeedback(result: FeedbackResult) {
        _uiState.value = _uiState.value.copy(
            collapsedKeys = _uiState.value.collapsedKeys - result.cardKey,
        )
        viewModelScope.launch {
            when (result) {
                is FeedbackResult.Wore -> {
                    result.previousDates.forEach { (itemId, prevDate) ->
                        clothingRepository.undoWorn(itemId, prevDate)
                    }
                }
                is FeedbackResult.Dismissed -> Unit
                is FeedbackResult.RejectedCombo -> outfitFeedbackRepository.delete(result.feedback)
            }
        }
    }

    private fun collapse(cardKey: String) {
        _uiState.value = _uiState.value.copy(
            collapsedKeys = _uiState.value.collapsedKeys + cardKey,
        )
    }

    private suspend fun callWithRetry(
        prompt: String,
        inspirationPhotos: List<ByteArray>,
        availableIds: Set<String>,
    ): List<ParsedOutfit> {
        val first = claudeRepository.suggestOutfits(prompt, inspirationPhotos)
        return runCatching { OutfitJsonParser.parse(first, availableIds) }
            .getOrElse {
                val strictPrompt = "$prompt\n\nIMPORTANT: Respond with ONLY valid JSON. No extra text."
                val retry = claudeRepository.suggestOutfits(strictPrompt, inspirationPhotos)
                OutfitJsonParser.parse(retry, availableIds)
            }
    }

    class Factory(
        private val application: Application,
        private val clothingRepository: ClothingRepository,
        private val inspirationRepository: InspirationRepository,
        private val weatherRepository: WeatherRepository,
        private val claudeRepository: ClaudeRepository,
        private val settingsRepository: SettingsRepository,
        private val outfitFeedbackRepository: OutfitFeedbackRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            SuggestViewModel(
                application,
                clothingRepository,
                inspirationRepository,
                weatherRepository,
                claudeRepository,
                settingsRepository,
                outfitFeedbackRepository,
            ) as T
    }
}

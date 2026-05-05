package com.Ben.closetstylist.ui.closet

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.Ben.closetstylist.ClosetStylistApplication
import com.Ben.closetstylist.R
import com.Ben.closetstylist.data.ClothingCategory
import com.Ben.closetstylist.data.ClothingItem
import com.Ben.closetstylist.data.ClothingRepository
import com.Ben.closetstylist.data.Season
import com.Ben.closetstylist.network.ClaudeRepository
import com.Ben.closetstylist.util.compressImageFromUri
import com.Ben.closetstylist.util.saveToClosetDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class AddItemUiState(
    val imageUri: Uri? = null,
    val category: ClothingCategory = ClothingCategory.TOP,
    val colorTags: List<String> = emptyList(),
    val colorInput: String = "",
    val seasonTags: Set<Season> = emptySet(),
    val isSaving: Boolean = false,
    val showPickerOnLoad: Boolean = true,
)

sealed class AddItemEvent {
    data object Saved : AddItemEvent()
    data class ShowMessage(val message: String) : AddItemEvent()
}

class AddItemViewModel(
    private val app: Application,
    private val clothingRepository: ClothingRepository,
    private val claudeRepository: ClaudeRepository,
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddItemEvent>()
    val events: SharedFlow<AddItemEvent> = _events.asSharedFlow()

    // Held separately — ByteArray breaks data-class equality; UI only needs the URI.
    private var compressedBytes: ByteArray? = null

    fun pickerShown() = _uiState.update { it.copy(showPickerOnLoad = false) }

    fun setImageUri(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { compressImageFromUri(app, uri, maxDimension = 1024) }.getOrNull()
            }
            if (bytes == null) {
                _events.emit(AddItemEvent.ShowMessage(app.getString(R.string.error_load_image)))
            } else {
                compressedBytes = bytes
                _uiState.update { it.copy(imageUri = uri) }
            }
        }
    }

    fun setCategory(category: ClothingCategory) = _uiState.update { it.copy(category = category) }

    fun onColorInput(value: String) = _uiState.update { it.copy(colorInput = value) }

    fun addColorTag() {
        val tag = _uiState.value.colorInput.trim()
        if (tag.isNotEmpty() && tag !in _uiState.value.colorTags) {
            _uiState.update { it.copy(colorTags = it.colorTags + tag, colorInput = "") }
        }
    }

    fun removeColorTag(tag: String) = _uiState.update { it.copy(colorTags = it.colorTags - tag) }

    fun toggleSeason(season: Season) = _uiState.update {
        it.copy(seasonTags = if (season in it.seasonTags) it.seasonTags - season else it.seasonTags + season)
    }

    fun save() {
        val bytes = compressedBytes ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val id = UUID.randomUUID().toString()
                val filePath = withContext(Dispatchers.IO) {
                    saveToClosetDir(app, bytes, "$id.jpg")
                }
                // Save immediately with blank description so the user can navigate back right away.
                clothingRepository.addItem(
                    ClothingItem(
                        id = id,
                        imagePath = filePath,
                        category = state.category,
                        colorTags = state.colorTags,
                        seasonTags = state.seasonTags.toList(),
                        description = "",
                        lastWornDate = null,
                    ),
                )
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(AddItemEvent.Saved)
                // Generate description in app-scoped coroutine — survives navigation.
                val bytesSnapshot = bytes
                (app as ClosetStylistApplication).applicationScope.launch {
                    runCatching {
                        val description = claudeRepository.describeItem(bytesSnapshot)
                        if (description.isNotBlank()) {
                            val item = ClothingItem(
                                id = id,
                                imagePath = filePath,
                                category = state.category,
                                colorTags = state.colorTags,
                                seasonTags = state.seasonTags.toList(),
                                description = description,
                                lastWornDate = null,
                            )
                            clothingRepository.updateItem(item)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(AddItemEvent.ShowMessage(app.getString(R.string.error_save_item)))
            }
        }
    }

    class Factory(
        private val app: Application,
        private val clothingRepository: ClothingRepository,
        private val claudeRepository: ClaudeRepository,
    ) : ViewModelProvider.AndroidViewModelFactory(app) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AddItemViewModel(app, clothingRepository, claudeRepository) as T
    }
}

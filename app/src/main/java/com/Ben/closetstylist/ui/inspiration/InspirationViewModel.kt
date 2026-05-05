package com.Ben.closetstylist.ui.inspiration

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.Ben.closetstylist.R
import com.Ben.closetstylist.data.InspirationPhoto
import com.Ben.closetstylist.data.InspirationRepository
import com.Ben.closetstylist.util.compressImageFromUri
import com.Ben.closetstylist.util.saveToInspirationDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID

class InspirationViewModel(
    private val app: Application,
    private val repository: InspirationRepository,
) : AndroidViewModel(app) {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val photos: StateFlow<List<InspirationPhoto>> = repository.getAllPhotos()
        .onEach { _isLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun savePhoto(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { compressImageFromUri(app, uri, maxDimension = 1024) }.getOrNull()
            }
            if (bytes == null) {
                _events.emit(app.getString(R.string.error_save_photo))
                return@launch
            }
            val id = UUID.randomUUID().toString()
            val filePath = withContext(Dispatchers.IO) {
                saveToInspirationDir(app, bytes, "$id.jpg")
            }
            repository.addPhoto(InspirationPhoto(id = id, imagePath = filePath, addedAt = Instant.now()))
        }
    }

    fun deletePhoto(photo: InspirationPhoto) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File(photo.imagePath.removePrefix("file://")).delete()
            }
            repository.deletePhoto(photo)
        }
    }

    class Factory(
        private val app: Application,
        private val repository: InspirationRepository,
    ) : ViewModelProvider.AndroidViewModelFactory(app) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            InspirationViewModel(app, repository) as T
    }
}

package com.Ben.closetstylist.ui.closet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.Ben.closetstylist.data.ClothingCategory
import com.Ben.closetstylist.data.ClothingItem
import com.Ben.closetstylist.data.ClothingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClosetUiState(
    val items: List<ClothingItem> = emptyList(),
    val activeFilter: ClothingCategory? = null,
    val isLoaded: Boolean = false,
)

class ClosetViewModel(private val repository: ClothingRepository) : ViewModel() {

    private val activeFilter = MutableStateFlow<ClothingCategory?>(null)

    val uiState: StateFlow<ClosetUiState> = combine(
        repository.getAllItems(),
        activeFilter,
    ) { items, filter ->
        val filtered = if (filter == null) items else items.filter { it.category == filter }
        ClosetUiState(items = filtered, activeFilter = filter, isLoaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClosetUiState(isLoaded = false))

    fun setFilter(category: ClothingCategory?) {
        activeFilter.value = category
    }

    fun deleteItem(item: ClothingItem) = viewModelScope.launch {
        repository.deleteItem(item)
    }

    class Factory(private val repository: ClothingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ClosetViewModel(repository) as T
    }
}

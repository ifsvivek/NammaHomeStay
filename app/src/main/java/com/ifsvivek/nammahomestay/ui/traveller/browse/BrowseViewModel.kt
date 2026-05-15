package com.ifsvivek.nammahomestay.ui.traveller.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.repository.HomestayBrowseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val loading: Boolean = true,
    val homestays: List<Homestay> = emptyList(),
    val query: String = "",
) {
    val filtered: List<Homestay>
        get() = if (query.isBlank()) homestays else homestays.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
        }
}

/** Drives the "Browse LIVE homestays" screen on the traveller side. */
class BrowseViewModel(
    private val repo: HomestayBrowseRepository = HomestayBrowseRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeLiveHomestays().collect { list ->
                // Newest-first by name fallback; Firestore doesn't order arbitrary timestamps
                // on homestays so we sort by name for a stable visual order.
                _state.update { it.copy(loading = false, homestays = list.sortedBy { h -> h.name }) }
            }
        }
    }

    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
}

package com.ifsvivek.nammahomestay.ui.traveller.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.AggregateRating
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.repository.HomestayBrowseRepository
import com.ifsvivek.nammahomestay.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val loading: Boolean = true,
    val homestays: List<Homestay> = emptyList(),
    /** Today's menu keyed by hostId — surfaced on the card so guests see what's cooking. */
    val menusByHostId: Map<String, DailyMenu> = emptyMap(),
    val ratingsByHostId: Map<String, AggregateRating> = emptyMap(),
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
    private val browseRepo: HomestayBrowseRepository = HomestayBrowseRepository(),
    private val reviewRepo: ReviewRepository = ReviewRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                browseRepo.observeLiveHomestays(),
                browseRepo.observeAllTodaysMenus(),
                reviewRepo.observeAggregatesByHostId(),
            ) { homes, menus, ratings -> Triple(homes, menus, ratings) }
                .collect { (homes, menus, ratings) ->
                    _state.update {
                        it.copy(
                            loading = false,
                            homestays = homes.sortedBy { h -> h.name.lowercase() },
                            menusByHostId = menus,
                            ratingsByHostId = ratings,
                        )
                    }
                }
        }
    }

    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
}

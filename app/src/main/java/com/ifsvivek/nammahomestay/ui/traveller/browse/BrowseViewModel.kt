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

/** How the browse list is ordered. */
enum class BrowseSort(val label: String) {
    /** A → Z by name (default). */
    NAME("A → Z"),

    /** Highest aggregate star rating first; un-reviewed at the bottom. */
    TOP_RATED("Top-rated"),

    /** Hosts who have published today's menu first — likely cooking right now. */
    NEWEST_MENU("Has today's menu"),
}

data class BrowseUiState(
    val loading: Boolean = true,
    val homestays: List<Homestay> = emptyList(),
    val menusByHostId: Map<String, DailyMenu> = emptyMap(),
    val ratingsByHostId: Map<String, AggregateRating> = emptyMap(),
    val query: String = "",
    val sort: BrowseSort = BrowseSort.NAME,
) {
    /** Total number of LIVE homestays in the system (before filtering). */
    val liveCount: Int get() = homestays.size

    val filtered: List<Homestay>
        get() {
            val q = query.trim()
            val matched = if (q.isBlank()) homestays else homestays.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.location.contains(q, ignoreCase = true)
            }
            return when (sort) {
                BrowseSort.NAME -> matched.sortedBy { it.name.lowercase() }
                BrowseSort.TOP_RATED -> matched.sortedByDescending {
                    ratingsByHostId[it.id]?.averageStars ?: -1f
                }
                BrowseSort.NEWEST_MENU -> matched.sortedWith(
                    compareByDescending<Homestay> { menusByHostId[it.id] != null }
                        .thenByDescending { menusByHostId[it.id]?.dateTimestamp?.time ?: 0L }
                        .thenBy { it.name.lowercase() }
                )
            }
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
                            homestays = homes,
                            menusByHostId = menus,
                            ratingsByHostId = ratings,
                        )
                    }
                }
        }
    }

    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
    fun onSortChange(sort: BrowseSort) = _state.update { it.copy(sort = sort) }
}

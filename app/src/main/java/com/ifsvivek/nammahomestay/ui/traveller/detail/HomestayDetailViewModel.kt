package com.ifsvivek.nammahomestay.ui.traveller.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.AggregateRating
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.model.Review
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.HomestayBrowseRepository
import com.ifsvivek.nammahomestay.data.repository.InquiryRepository
import com.ifsvivek.nammahomestay.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomestayDetailUiState(
    val loading: Boolean = true,
    val homestay: Homestay? = null,
    val todaysMenu: DailyMenu? = null,
    val reviews: List<Review> = emptyList(),
    val aggregate: AggregateRating? = null,
    val myTravellerId: String = "",
    val sending: Boolean = false,
    val justSent: Boolean = false,
    val submittingReview: Boolean = false,
    val justReviewed: Boolean = false,
    val error: String? = null,
) {
    /** True if this traveller has already left a review for this homestay (we then hide the form). */
    val travellerAlreadyReviewed: Boolean
        get() = myTravellerId.isNotBlank() && reviews.any { it.travellerId == myTravellerId }
}

/**
 * Drives the homestay detail screen + the "I'm interested" action + reviews
 * (read + submit). [hostId] is read from the nav argument (route `detail/{hostId}`)
 * via [SavedStateHandle].
 */
class HomestayDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val browseRepo = HomestayBrowseRepository()
    private val inquiryRepo = InquiryRepository()
    private val reviewRepo = ReviewRepository()
    private val authRepo = AuthRepository()
    private val hostId: String = savedStateHandle.get<String>("hostId").orEmpty()

    private val _state = MutableStateFlow(HomestayDetailUiState())
    val state: StateFlow<HomestayDetailUiState> = _state.asStateFlow()

    init {
        if (hostId.isBlank()) {
            _state.update { it.copy(loading = false, error = "Missing host id.") }
        } else {
            val myId = authRepo.currentUid.orEmpty()
            _state.update { it.copy(myTravellerId = myId) }
            viewModelScope.launch {
                combine(
                    browseRepo.observeHomestay(hostId),
                    browseRepo.observeTodaysMenu(hostId),
                    reviewRepo.observeReviewsFor(hostId),
                ) { home, menu, reviews ->
                    Triple(home, menu, reviews)
                }.collect { (home, menu, reviews) ->
                    _state.update {
                        it.copy(
                            loading = false,
                            homestay = home,
                            todaysMenu = menu,
                            reviews = reviews,
                            aggregate = aggregate(reviews),
                        )
                    }
                }
            }
        }
    }

    private fun aggregate(reviews: List<Review>): AggregateRating? {
        val valid = reviews.filter { it.rating in 1..5 }
        if (valid.isEmpty()) return null
        return AggregateRating(
            homestayId = hostId,
            count = valid.size,
            averageStars = valid.sumOf { it.rating }.toFloat() / valid.size,
        )
    }

    fun sendInquiry() {
        val travellerId = authRepo.currentUid ?: return
        val phone = authRepo.currentPhone.orEmpty()
        val name = authRepo.currentDisplayName.orEmpty()
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            runCatching {
                inquiryRepo.sendInquiry(
                    hostId = hostId,
                    travellerId = travellerId,
                    guestName = name,
                    guestPhone = phone,
                )
            }
                .onSuccess { _state.update { it.copy(sending = false, justSent = true) } }
                .onFailure { e ->
                    Log.e(TAG, "sendInquiry failed", e)
                    _state.update {
                        it.copy(
                            sending = false,
                            error = "Could not send: ${e.message ?: "unknown error"}",
                        )
                    }
                }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        val travellerId = authRepo.currentUid ?: return
        if (_state.value.submittingReview) return
        _state.update { it.copy(submittingReview = true, error = null) }
        viewModelScope.launch {
            runCatching {
                reviewRepo.submit(
                    homestayId = hostId,
                    travellerId = travellerId,
                    travellerName = authRepo.currentDisplayName.orEmpty(),
                    rating = rating,
                    comment = comment,
                )
            }
                .onSuccess { _state.update { it.copy(submittingReview = false, justReviewed = true) } }
                .onFailure { e ->
                    Log.e(TAG, "submitReview failed", e)
                    _state.update {
                        it.copy(
                            submittingReview = false,
                            error = "Could not save review: ${e.message ?: "unknown error"}",
                        )
                    }
                }
        }
    }

    fun consumeSent() = _state.update { it.copy(justSent = false) }
    fun consumeReviewed() = _state.update { it.copy(justReviewed = false) }
    fun consumeError() = _state.update { it.copy(error = null) }

    private companion object {
        const val TAG = "HomestayDetailVM"
    }
}

package com.ifsvivek.nammahomestay.ui.traveller.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.HomestayBrowseRepository
import com.ifsvivek.nammahomestay.data.repository.InquiryRepository
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
    val sending: Boolean = false,
    val justSent: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the homestay detail screen + the "I'm interested" action. [hostId] is
 * read from the nav argument (route `detail/{hostId}`) via [SavedStateHandle].
 */
class HomestayDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val browseRepo = HomestayBrowseRepository()
    private val inquiryRepo = InquiryRepository()
    private val authRepo = AuthRepository()
    private val hostId: String = savedStateHandle.get<String>("hostId").orEmpty()

    private val _state = MutableStateFlow(HomestayDetailUiState())
    val state: StateFlow<HomestayDetailUiState> = _state.asStateFlow()

    init {
        if (hostId.isBlank()) {
            _state.update { it.copy(loading = false, error = "Missing host id.") }
        } else {
            viewModelScope.launch {
                combine(
                    browseRepo.observeHomestay(hostId),
                    browseRepo.observeTodaysMenu(hostId),
                ) { home, menu -> home to menu }
                    .collect { (home, menu) ->
                        _state.update { it.copy(loading = false, homestay = home, todaysMenu = menu) }
                    }
            }
        }
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

    fun consumeSent() = _state.update { it.copy(justSent = false) }
    fun consumeError() = _state.update { it.copy(error = null) }

    private companion object {
        const val TAG = "HomestayDetailVM"
    }
}

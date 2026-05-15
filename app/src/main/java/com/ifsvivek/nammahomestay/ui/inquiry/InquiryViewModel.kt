package com.ifsvivek.nammahomestay.ui.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.Inquiry
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.InquiryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InquiryUiState(
    val loading: Boolean = true,
    val inquiries: List<Inquiry> = emptyList(),
    val message: String? = null,
) {
    val newCount: Int get() = inquiries.count { it.status == "pending" }
}

class InquiryViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val inquiryRepo: InquiryRepository = InquiryRepository(),
) : ViewModel() {

    private val uid: String? = authRepo.currentUid
    private val _state = MutableStateFlow(InquiryUiState())
    val state: StateFlow<InquiryUiState> = _state.asStateFlow()

    init {
        val id = uid
        if (id == null) {
            _state.update { it.copy(loading = false, message = "Please sign in again.") }
        } else {
            viewModelScope.launch {
                inquiryRepo.observeInquiriesForHost(id).collect { list ->
                    _state.update { it.copy(loading = false, inquiries = list) }
                }
            }
        }
    }

    /** Call when the host taps the green "Call Guest" button. */
    fun markCalled(inquiryId: String) {
        viewModelScope.launch { runCatching { inquiryRepo.markCalled(inquiryId) } }
    }

    fun markClosed(inquiryId: String) {
        viewModelScope.launch { runCatching { inquiryRepo.markClosed(inquiryId) } }
    }

    /** Dev/demo helper until the traveller-facing app exists. */
    fun addSampleInquiry() {
        val id = uid ?: return
        viewModelScope.launch {
            runCatching { inquiryRepo.addSampleInquiry(id) }
                .onFailure { _state.update { it.copy(message = "Could not add sample. Check internet.") } }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

package com.ifsvivek.nammahomestay.ui.traveller.myinterests

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

data class MyInterestsUiState(
    val loading: Boolean = true,
    val inquiries: List<Inquiry> = emptyList(),
)

/** Drives the traveller's "My Interests" tab — the inquiries they've sent. */
class MyInterestsViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val inquiryRepo: InquiryRepository = InquiryRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(MyInterestsUiState())
    val state: StateFlow<MyInterestsUiState> = _state.asStateFlow()

    init {
        val uid = authRepo.currentUid
        if (uid == null) {
            _state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                inquiryRepo.observeInquiriesForTraveller(uid).collect { list ->
                    _state.update { it.copy(loading = false, inquiries = list) }
                }
            }
        }
    }
}

package com.ifsvivek.nammahomestay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.InquiryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shell-level state for the host's bottom nav. Today this just tracks the count
 * of pending inquiries so the "Interests" tab can show a badge — when more host
 * shell metrics arrive (e.g. unread review notifications) they go here too.
 */
class HostShellViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val inquiryRepo = InquiryRepository()

    private val _pendingCount = MutableStateFlow(0)
    val pendingInquiryCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        val uid = authRepo.currentUid
        if (uid != null) {
            viewModelScope.launch {
                inquiryRepo.observeInquiriesForHost(uid).collect { list ->
                    _pendingCount.value = list.count { it.status == "pending" }
                }
            }
        }
    }
}

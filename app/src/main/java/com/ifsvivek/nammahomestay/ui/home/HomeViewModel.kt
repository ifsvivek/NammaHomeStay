package com.ifsvivek.nammahomestay.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.model.VerificationChecklist
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.HostRepository
import com.ifsvivek.nammahomestay.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One actionable thing the host still needs to do to go "Live". */
data class SetupStep(val label: String, val done: Boolean)

data class HomeUiState(
    val loading: Boolean = true,
    val homestay: Homestay? = null,
    val savingPhoto: Boolean = false,
    val message: String? = null,
) {
    private val home: Homestay get() = homestay ?: Homestay()

    val steps: List<SetupStep> = listOf(
        SetupStep("Add your home's name", home.name.isNotBlank()),
        SetupStep("Add at least one photo", home.images.isNotEmpty()),
        SetupStep("Clean bedding ready", home.checklist.cleanBedding),
        SetupStep("Working washroom", home.checklist.functionalWashroom),
        SetupStep("Drinking water available", home.checklist.drinkingWater),
    )
    val doneCount: Int get() = steps.count { it.done }
    val totalCount: Int get() = steps.size
    val progress: Float get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
    val isLive: Boolean get() = home.live || home.canGoLive
}

class HomeViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val hostRepo: HostRepository = HostRepository(),
) : ViewModel() {

    private val uid: String? = authRepo.currentUid
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        val id = uid
        if (id == null) {
            _state.update { it.copy(loading = false, message = "Please sign in again.") }
        } else {
            viewModelScope.launch {
                hostRepo.observeHomestay(id).collect { home ->
                    _state.update { it.copy(loading = false, homestay = home) }
                }
            }
        }
    }

    fun saveBasics(name: String, location: String) {
        val id = uid ?: return
        viewModelScope.launch {
            runCatching { hostRepo.updateBasics(id, name, location) }
                .onSuccess { _state.update { it.copy(message = "Saved ✓") } }
                .onFailure { _state.update { it.copy(message = "Could not save. Check internet.") } }
        }
    }

    fun setChecklist(cleanBedding: Boolean, functionalWashroom: Boolean, drinkingWater: Boolean) {
        val id = uid ?: return
        viewModelScope.launch {
            runCatching {
                hostRepo.updateChecklist(
                    id,
                    VerificationChecklist(cleanBedding, functionalWashroom, drinkingWater),
                )
            }.onFailure { _state.update { it.copy(message = "Could not save. Check internet.") } }
        }
    }

    fun addPhoto(context: Context, uri: Uri) {
        val id = uid ?: return
        if (_state.value.savingPhoto) return
        _state.update { it.copy(savingPhoto = true) }
        viewModelScope.launch {
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) { ImageCompressor.compress(context, uri) }
                    ?: error("Could not read that photo")
                hostRepo.addPhoto(id, bytes)
            }
            _state.update {
                it.copy(
                    savingPhoto = false,
                    message = if (result.isSuccess) "Photo added ✓" else "Photo upload failed. Try again.",
                )
            }
        }
    }

    fun removePhoto(url: String) {
        val id = uid ?: return
        viewModelScope.launch { runCatching { hostRepo.removePhoto(id, url) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

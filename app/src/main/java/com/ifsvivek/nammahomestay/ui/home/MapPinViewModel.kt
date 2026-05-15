package com.ifsvivek.nammahomestay.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.HostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapPinUiState(
    val loading: Boolean = true,
    val savedLat: Double? = null,
    val savedLng: Double? = null,
    val draftLat: Double? = null,
    val draftLng: Double? = null,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val error: String? = null,
) {
    val draftIsSet: Boolean get() = draftLat != null && draftLng != null
    val draftDiffersFromSaved: Boolean
        get() = draftLat != savedLat || draftLng != savedLng
    val canSave: Boolean get() = draftIsSet && draftDiffersFromSaved && !saving
}

/** Drives the host's "pin your home on the map" picker. */
class MapPinViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val hostRepo: HostRepository = HostRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(MapPinUiState())
    val state: StateFlow<MapPinUiState> = _state.asStateFlow()

    init {
        val uid = authRepo.currentUid
        if (uid != null) {
            viewModelScope.launch {
                hostRepo.observeHomestay(uid).collect { home ->
                    _state.update {
                        it.copy(
                            loading = false,
                            savedLat = home?.latitude,
                            savedLng = home?.longitude,
                            // Initialise the draft from the saved pin the first time we see it.
                            draftLat = it.draftLat ?: home?.latitude,
                            draftLng = it.draftLng ?: home?.longitude,
                        )
                    }
                }
            }
        }
    }

    fun onMapTap(lat: Double, lng: Double) {
        _state.update { it.copy(draftLat = lat, draftLng = lng, error = null) }
    }

    fun save() {
        val uid = authRepo.currentUid ?: return
        val st = _state.value
        if (!st.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { hostRepo.updatePin(uid, st.draftLat, st.draftLng) }
                .onSuccess { _state.update { it.copy(saving = false, justSaved = true) } }
                .onFailure { e ->
                    Log.e(TAG, "updatePin failed", e)
                    _state.update {
                        it.copy(
                            saving = false,
                            error = "Could not save: ${e.message ?: "unknown error"}",
                        )
                    }
                }
        }
    }

    fun clearPin() {
        val uid = authRepo.currentUid ?: return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { hostRepo.updatePin(uid, null, null) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            saving = false,
                            draftLat = null,
                            draftLng = null,
                            justSaved = true,
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "clearPin failed", e)
                    _state.update {
                        it.copy(
                            saving = false,
                            error = "Could not save: ${e.message ?: "unknown error"}",
                        )
                    }
                }
        }
    }

    fun consumeSaved() = _state.update { it.copy(justSaved = false) }
    fun consumeError() = _state.update { it.copy(error = null) }

    private companion object {
        const val TAG = "MapPinViewModel"
    }
}

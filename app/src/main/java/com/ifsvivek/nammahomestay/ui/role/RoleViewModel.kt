package com.ifsvivek.nammahomestay.ui.role

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.role.UserMode
import com.ifsvivek.nammahomestay.data.role.UserModeStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What's known about the current user's mode at any moment. */
sealed interface ModeState {
    /** Still reading DataStore. Show a splash / spinner. */
    data object Loading : ModeState

    /** First sign-in (or after sign-out). Route to [ModePickerScreen]. */
    data object NotChosen : ModeState

    /** Show the corresponding shell. */
    data class Chosen(val mode: UserMode) : ModeState
}

/**
 * Owns the host/traveller mode and the user's display name. Uber-style: a single
 * uid can flip freely between modes, and the choice is per-device (DataStore).
 */
class RoleViewModel(app: Application) : AndroidViewModel(app) {

    private val store = UserModeStore(app)
    private val authRepo = AuthRepository()

    val state: StateFlow<ModeState> = store.mode
        .map { if (it == null) ModeState.NotChosen else ModeState.Chosen(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ModeState.Loading)

    /** What Firebase Auth currently has as the user's display name (used to pre-fill the picker). */
    val currentDisplayName: String get() = authRepo.currentDisplayName.orEmpty()

    /** Called by the picker — saves the chosen mode and (if provided) the display name. */
    fun choose(mode: UserMode, name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) runCatching { authRepo.updateDisplayName(name) }
            store.set(mode)
        }
    }

    /** Called by the in-app mode-switch icon — just flips the mode. */
    fun switchTo(mode: UserMode) {
        viewModelScope.launch { store.set(mode) }
    }

    /** Called from app-root on sign-out so the next user sees the picker. */
    fun clearOnSignOut() {
        viewModelScope.launch { store.clear() }
    }
}

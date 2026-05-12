package com.ifsvivek.nammahomestay.ui.menu

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.MenuRepository
import com.ifsvivek.nammahomestay.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MenuUiState(
    val loading: Boolean = true,
    /** The menu currently published (if any). */
    val published: DailyMenu? = null,
    // ── the "draft" the host is editing ──────────────────────────────────────
    val dishName: String = "",
    val priceText: String = "",
    /** A photo just picked from gallery/camera, not yet uploaded. */
    val pickedImage: Uri? = null,
    // ── transient ────────────────────────────────────────────────────────────
    val saving: Boolean = false,
    val justPublished: Boolean = false,   // drives the success animation/snackbar
    val error: String? = null,
) {
    val price: Long? get() = priceText.trim().toLongOrNull()
    val hasPhoto: Boolean get() = pickedImage != null || !published?.imageUrl.isNullOrBlank()
    val canPublish: Boolean
        get() = dishName.isNotBlank() && (price ?: 0L) > 0L && hasPhoto && !saving
    val hasPublishedMenu: Boolean get() = published?.isEmpty == false
}

/**
 * Drives the "60-Second Menu". The host fills one photo + one name + one price
 * and [publish] writes it in a single Firestore `set()`.
 */
class MenuViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val menuRepo: MenuRepository = MenuRepository(),
) : ViewModel() {

    private val uid: String? = authRepo.currentUid
    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    private var prefilledFromPublished = false

    init {
        val id = uid
        if (id == null) {
            _state.update { it.copy(loading = false, error = "Please sign in again.") }
        } else {
            viewModelScope.launch {
                menuRepo.observeTodaysMenu(id).collect { menu ->
                    _state.update { s ->
                        // Pre-fill the draft once, so editing today's menu is one tap away.
                        if (!prefilledFromPublished && menu != null && !menu.isEmpty) {
                            prefilledFromPublished = true
                            s.copy(
                                loading = false,
                                published = menu,
                                dishName = menu.dishName,
                                priceText = menu.price.toString(),
                            )
                        } else {
                            s.copy(loading = false, published = menu)
                        }
                    }
                }
            }
        }
    }

    fun onDishNameChange(v: String) = _state.update { it.copy(dishName = v.take(40), error = null) }
    fun onPriceChange(v: String) = _state.update {
        it.copy(priceText = v.filter(Char::isDigit).take(5), error = null)
    }
    fun onImagePicked(uri: Uri?) = _state.update { it.copy(pickedImage = uri) }

    fun publish(context: Context) {
        val id = uid ?: return
        val st = _state.value
        val price = st.price
        if (!st.canPublish || price == null) return
        _state.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            val result = runCatching {
                val imageUrl = when (val picked = st.pickedImage) {
                    null -> st.published?.imageUrl.orEmpty()
                    else -> {
                        val bytes = withContext(Dispatchers.IO) { ImageCompressor.compress(context, picked) }
                            ?: error("Could not read that photo")
                        menuRepo.uploadMenuPhoto(id, bytes)
                    }
                }
                menuRepo.save(id, st.dishName, price, imageUrl)
            }
            _state.update {
                if (result.isSuccess) {
                    it.copy(saving = false, justPublished = true, pickedImage = null)
                } else {
                    it.copy(saving = false, error = "Could not post. Check your internet and try again.")
                }
            }
        }
    }

    fun clearMenu() {
        val id = uid ?: return
        viewModelScope.launch {
            runCatching { menuRepo.clear(id) }
            _state.update { it.copy(dishName = "", priceText = "", pickedImage = null) }
            prefilledFromPublished = false
        }
    }

    fun consumePublished() = _state.update { it.copy(justPublished = false) }
    fun consumeError() = _state.update { it.copy(error = null) }
}

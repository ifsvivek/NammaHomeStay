package com.ifsvivek.nammahomestay.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.ifsvivek.nammahomestay.data.repository.AuthRepository
import com.ifsvivek.nammahomestay.data.repository.HostRepository
import com.ifsvivek.nammahomestay.data.repository.OtpEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginStep { ENTER_PHONE, ENTER_CODE }

data class LoginUiState(
    val step: LoginStep = LoginStep.ENTER_PHONE,
    val phoneDigits: String = "",   // 10 local digits, country code is fixed to +91
    val code: String = "",          // the 6-digit SMS code
    val sending: Boolean = false,   // requesting / resending the SMS
    val verifying: Boolean = false, // checking the typed code
    val error: String? = null,
    val isLoggedIn: Boolean = false,
) {
    val e164Phone: String get() = "+91$phoneDigits"
    val phoneValid: Boolean get() = phoneDigits.length == 10
    val codeValid: Boolean get() = code.length == 6
    val busy: Boolean get() = sending || verifying
}

/**
 * Owns the one-screen phone-login flow and the global "am I signed in?" signal
 * that [com.ifsvivek.nammahomestay.MainActivity] uses to pick the start screen.
 */
class AuthViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val hostRepo: HostRepository = HostRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(isLoggedIn = authRepo.isLoggedIn))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verifyJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { fb ->
        _state.update { it.copy(isLoggedIn = fb.currentUser != null) }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun onPhoneChange(raw: String) = _state.update {
        it.copy(phoneDigits = raw.filter(Char::isDigit).take(10), error = null)
    }

    fun onCodeChange(raw: String) = _state.update {
        it.copy(code = raw.filter(Char::isDigit).take(6), error = null)
    }

    fun backToPhoneStep() = _state.update {
        it.copy(step = LoginStep.ENTER_PHONE, code = "", error = null)
    }

    /** Ask Firebase to text a code to the entered number. [activity] is required by Play Services. */
    fun requestCode(activity: Activity) {
        val st = _state.value
        if (!st.phoneValid || st.busy) return
        _state.update { it.copy(sending = true, error = null) }

        verifyJob?.cancel()
        verifyJob = viewModelScope.launch {
            authRepo.startPhoneVerification(activity, st.e164Phone, resendToken).collect { event ->
                when (event) {
                    is OtpEvent.CodeSent -> {
                        verificationId = event.verificationId
                        resendToken = event.resendToken
                        _state.update { it.copy(sending = false, step = LoginStep.ENTER_CODE) }
                    }

                    is OtpEvent.AutoRetrieved ->
                        finishSignIn(authRepo.signInWith(event.credential))

                    is OtpEvent.Failed ->
                        _state.update { it.copy(sending = false, error = event.message) }
                }
            }
        }
    }

    /** Verify the code the host typed in. */
    fun submitCode() {
        val st = _state.value
        val vid = verificationId
        if (vid == null || !st.codeValid || st.busy) return
        _state.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch { finishSignIn(authRepo.confirmOtp(vid, st.code)) }
    }

    private suspend fun finishSignIn(result: Result<Unit>) {
        result.fold(
            onSuccess = {
                val uid = authRepo.currentUid
                val phone = authRepo.currentPhone ?: _state.value.e164Phone
                if (uid != null) runCatching { hostRepo.ensureHostProfile(uid, phone) }
                _state.update { it.copy(sending = false, verifying = false) }
                // isLoggedIn flips through authListener -> navigation reacts.
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        sending = false,
                        verifying = false,
                        error = e.localizedMessage ?: "That code did not work. Please try again.",
                    )
                }
            },
        )
    }

    fun signOut() {
        verifyJob?.cancel()
        verificationId = null
        resendToken = null
        _state.value = LoginUiState(isLoggedIn = false)
        authRepo.signOut()
    }
}

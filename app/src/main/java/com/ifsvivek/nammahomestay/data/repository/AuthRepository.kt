package com.ifsvivek.nammahomestay.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** What the OTP screen needs to react to while a verification is in flight. */
sealed interface OtpEvent {
    /** SMS sent — show the 6-box code field. */
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken,
    ) : OtpEvent

    /** Play Services auto-read the SMS / instant-verified — just sign in with this. */
    data class AutoRetrieved(val credential: PhoneAuthCredential) : OtpEvent

    /** Something went wrong (bad number, quota, no network…). */
    data class Failed(val message: String) : OtpEvent
}

/**
 * Wraps Firebase Phone Auth. No passwords, ever — the rural host signs in with a
 * phone number and a 6-digit code, the same mental model as a missed-call OTP.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUid: String? get() = auth.currentUser?.uid
    val currentPhone: String? get() = auth.currentUser?.phoneNumber
    val currentDisplayName: String? get() = auth.currentUser?.displayName
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /** Sets the user's display name on the Firebase Auth profile. Used for the traveller's [Inquiry.guestName]. */
    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        user.reload().await()
        Unit
    }

    /**
     * Kicks off verification for an E.164 number (e.g. "+91XXXXXXXXXX") and streams
     * back [OtpEvent]s. Cancelling the collector cancels the verification.
     */
    fun startPhoneVerification(
        activity: Activity,
        e164Phone: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    ): Flow<OtpEvent> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                trySend(OtpEvent.AutoRetrieved(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(OtpEvent.Failed(e.localizedMessage ?: "Could not send the code. Try again."))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                trySend(OtpEvent.CodeSent(verificationId, token))
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply { if (resendToken != null) setForceResendingToken(resendToken) }
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        awaitClose { /* nothing to unregister; verification times out on its own */ }
    }

    /** Signs in with the code the host typed. */
    suspend fun confirmOtp(verificationId: String, smsCode: String): Result<Unit> = runCatching {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        auth.signInWithCredential(credential).await()
        Unit
    }

    /** Signs in with a credential that arrived via auto-retrieval / instant verification. */
    suspend fun signInWith(credential: PhoneAuthCredential): Result<Unit> = runCatching {
        auth.signInWithCredential(credential).await()
        Unit
    }

    fun signOut() = auth.signOut()
}

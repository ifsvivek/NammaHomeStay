package com.ifsvivek.nammahomestay.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Opens the phone dialer with [phoneNumber] pre-filled. Uses ACTION_DIAL (not
 * ACTION_CALL) on purpose: no CALL_PHONE permission, and the host always sees the
 * number before the call goes out — fewer surprises for a first-time user.
 */
fun Context.dialPhoneNumber(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:${phoneNumber.trim()}".toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No phone app found on this device", Toast.LENGTH_LONG).show()
    }
}

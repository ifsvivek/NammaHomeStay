package com.ifsvivek.nammahomestay

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * App entry point. Firebase auto-initialises from `google-services.json` via the
 * gms plugin, but we touch it here so any mis-configuration fails fast on launch,
 * and we turn on Firestore's on-device cache — rural connections drop often and
 * the host should still see their last menu / inquiries offline.
 */
class NammaHomeStayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
    }
}

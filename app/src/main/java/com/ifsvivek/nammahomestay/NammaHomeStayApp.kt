package com.ifsvivek.nammahomestay

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import org.osmdroid.config.Configuration

/**
 * App entry point. Firebase auto-initialises from `google-services.json` via the
 * gms plugin, but we touch it here so any mis-configuration fails fast on launch,
 * and we turn on Firestore's on-device cache — rural connections drop often and
 * the host should still see their last menu / inquiries offline.
 *
 * We also configure osmdroid here: its tile servers require a unique User-Agent
 * (a polite OSM-TOS request), and we point its tile cache at our internal cache
 * dir so we don't need any external-storage permission.
 */
class NammaHomeStayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles")
        }
    }
}

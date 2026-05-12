package com.ifsvivek.nammahomestay.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.StoragePaths
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.model.Host
import com.ifsvivek.nammahomestay.data.model.VerificationChecklist
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Reads / writes the host's profile ([Host]) and their "digital shopfront"
 * ([Homestay]). For the MVP there is exactly one homestay per host, so the
 * homestay document id is the host's uid.
 */
class HostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {
    private fun hostDoc(uid: String) = db.collection(FirestoreCollections.HOSTS).document(uid)
    private fun homestayDoc(uid: String) = db.collection(FirestoreCollections.HOMESTAYS).document(uid)

    /** Creates the host (and an empty shopfront) on first login if they don't exist yet. */
    suspend fun ensureHostProfile(uid: String, phone: String) {
        val snap = hostDoc(uid).get().await()
        if (!snap.exists()) {
            hostDoc(uid).set(Host(uid = uid, phone = phone, verifiedStatus = "new")).await()
        }
        val homeSnap = homestayDoc(uid).get().await()
        if (!homeSnap.exists()) {
            homestayDoc(uid).set(Homestay(id = uid, hostId = uid)).await()
        }
    }

    /** Live view of the shopfront — drives the "Setup your Home" progress everywhere. */
    fun observeHomestay(uid: String): Flow<Homestay?> = callbackFlow {
        val reg = homestayDoc(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(Homestay::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun getHost(uid: String): Host? =
        hostDoc(uid).get().await().toObject(Host::class.java)

    /** Saves the name + location the host typed (merges, leaves photos/checklist alone). */
    suspend fun updateBasics(uid: String, name: String, location: String) {
        homestayDoc(uid).set(
            mapOf("hostId" to uid, "name" to name.trim(), "location" to location.trim()),
            SetOptions.merge(),
        ).await()
        recomputeLive(uid)
    }

    suspend fun updateChecklist(uid: String, checklist: VerificationChecklist) {
        homestayDoc(uid).set(mapOf("checklist" to checklist), SetOptions.merge()).await()
        recomputeLive(uid)
    }

    /** Compresses-then-uploads happens in the ViewModel; here we just push bytes + link it. */
    suspend fun addPhoto(uid: String, jpegBytes: ByteArray): String {
        val ref = storage.reference
            .child("${StoragePaths.HOMESTAY_PHOTOS}/$uid/${UUID.randomUUID()}.jpg")
        ref.putBytes(jpegBytes).await()
        val url = ref.downloadUrl.await().toString()
        val current = homestayDoc(uid).get().await().toObject(Homestay::class.java) ?: Homestay(id = uid, hostId = uid)
        homestayDoc(uid).set(
            mapOf("hostId" to uid, "images" to current.images + url),
            SetOptions.merge(),
        ).await()
        recomputeLive(uid)
        return url
    }

    suspend fun removePhoto(uid: String, url: String) {
        val current = homestayDoc(uid).get().await().toObject(Homestay::class.java) ?: return
        homestayDoc(uid).set(mapOf("images" to current.images - url), SetOptions.merge()).await()
        recomputeLive(uid)
    }

    /** Recomputes the `live` flag from the current state and mirrors it onto the host doc. */
    private suspend fun recomputeLive(uid: String) {
        val home = homestayDoc(uid).get().await().toObject(Homestay::class.java) ?: return
        val live = home.copy().canGoLive
        homestayDoc(uid).set(mapOf("live" to live), SetOptions.merge()).await()
        hostDoc(uid).set(
            mapOf("verifiedStatus" to if (live) "verified" else "new"),
            SetOptions.merge(),
        ).await()
    }
}

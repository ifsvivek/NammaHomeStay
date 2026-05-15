package com.ifsvivek.nammahomestay.data.repository

import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.model.VerificationChecklist
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reads / writes the host's "digital shopfront" ([Homestay]). For the MVP there
 * is exactly one homestay per host, so the homestay document id is the host's
 * uid. Photos live as JPEG [Blob]s on the homestay document (no Cloud Storage
 * on the free plan).
 */
class HostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun hostDoc(uid: String) = db.collection(FirestoreCollections.HOSTS).document(uid)
    private fun homestayDoc(uid: String) = db.collection(FirestoreCollections.HOMESTAYS).document(uid)

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

    /** Appends one compressed JPEG to the homestay's photo list. */
    suspend fun addPhoto(uid: String, jpegBytes: ByteArray) {
        val current = currentHomestay(uid)
        require(current.images.size < Homestay.MAX_PHOTOS) {
            "You can add up to ${Homestay.MAX_PHOTOS} photos. Remove one first."
        }
        homestayDoc(uid).set(
            mapOf("hostId" to uid, "images" to current.images + Blob.fromBytes(jpegBytes)),
            SetOptions.merge(),
        ).await()
        recomputeLive(uid)
    }

    /** Removes the photo at [index] in the current list (the order the UI shows). */
    suspend fun removePhotoAt(uid: String, index: Int) {
        val current = currentHomestay(uid)
        if (index !in current.images.indices) return
        val updated = current.images.toMutableList().apply { removeAt(index) }
        homestayDoc(uid).set(mapOf("images" to updated), SetOptions.merge()).await()
        recomputeLive(uid)
    }

    private suspend fun currentHomestay(uid: String): Homestay =
        homestayDoc(uid).get().await().toObject(Homestay::class.java) ?: Homestay(id = uid, hostId = uid)

    /** Recomputes the `live` flag from the current state and mirrors it onto the host doc. */
    private suspend fun recomputeLive(uid: String) {
        val live = currentHomestay(uid).canGoLive
        homestayDoc(uid).set(mapOf("live" to live), SetOptions.merge()).await()
        hostDoc(uid).set(
            mapOf("verifiedStatus" to if (live) "verified" else "new"),
            SetOptions.merge(),
        ).await()
    }
}

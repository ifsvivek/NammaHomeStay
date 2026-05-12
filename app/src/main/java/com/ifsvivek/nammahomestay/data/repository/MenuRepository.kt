package com.ifsvivek.nammahomestay.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Backs the "60-Second Menu" feature. There is one menu document per host
 * (`daily_menus/{hostId}`); publishing today's dish is a single [save] which is
 * one Firestore `set()` — that's the whole point: fast enough to feel like
 * posting a status. The dish photo rides along inside the document as a [Blob]
 * (no Cloud Storage on the free plan).
 */
class MenuRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun menuDoc(hostId: String) =
        db.collection(FirestoreCollections.DAILY_MENUS).document(hostId)

    fun observeTodaysMenu(hostId: String): Flow<DailyMenu?> = callbackFlow {
        val reg = menuDoc(hostId).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(DailyMenu::class.java))
        }
        awaitClose { reg.remove() }
    }

    /**
     * Publishes today's menu in one write. Pass [image] as a fresh [Blob] of the
     * new photo, or the previously stored one to keep it, or null for no photo.
     * Overwrites yesterday's entry by design.
     */
    suspend fun save(hostId: String, dishName: String, price: Long, image: Blob?) {
        val menu = DailyMenu(
            id = hostId,
            hostId = hostId,
            dishName = dishName.trim(),
            price = price,
            image = image,
            // Set client-side so the UI shows a time immediately; @ServerTimestamp
            // only fills this in when it's null on write.
            dateTimestamp = Timestamp.now().toDate(),
        )
        menuDoc(hostId).set(menu).await()
    }

    suspend fun clear(hostId: String) {
        menuDoc(hostId).delete().await()
    }
}

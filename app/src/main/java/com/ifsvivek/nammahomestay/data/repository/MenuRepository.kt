package com.ifsvivek.nammahomestay.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.StoragePaths
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Backs the "60-Second Menu" feature. There is one menu document per host
 * (`daily_menus/{hostId}`); publishing today's dish is a single [save] which is
 * one Firestore `set()` — that's the whole point: fast enough to feel like
 * posting a status.
 */
class MenuRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
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

    /** Uploads the (already compressed) dish photo and returns its download URL. */
    suspend fun uploadMenuPhoto(hostId: String, jpegBytes: ByteArray): String {
        val ref = storage.reference
            .child("${StoragePaths.MENU_PHOTOS}/$hostId/${UUID.randomUUID()}.jpg")
        ref.putBytes(jpegBytes).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Publishes today's menu in one write. Pass [imageUrl] from [uploadMenuPhoto]
     * (or keep the previous one). This overwrites yesterday's entry by design.
     */
    suspend fun save(hostId: String, dishName: String, price: Long, imageUrl: String) {
        val menu = DailyMenu(
            id = hostId,
            hostId = hostId,
            dishName = dishName.trim(),
            price = price,
            imageUrl = imageUrl,
            // Set client-side so the UI shows a time immediately even before the
            // server timestamp resolves; @ServerTimestamp would null this on write.
            dateTimestamp = Timestamp.now().toDate(),
        )
        menuDoc(hostId).set(menu).await()
    }

    suspend fun clear(hostId: String) {
        menuDoc(hostId).delete().await()
    }
}

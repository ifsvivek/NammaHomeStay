package com.ifsvivek.nammahomestay.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.model.Homestay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Traveller-side reads of the public marketplace: every homestay flagged LIVE,
 * plus a way to fetch the homestay + today's menu for a detail screen.
 */
class HomestayBrowseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val homestays = db.collection(FirestoreCollections.HOMESTAYS)
    private val dailyMenus = db.collection(FirestoreCollections.DAILY_MENUS)

    /** Live stream of every homestay currently flagged `live == true`. */
    fun observeLiveHomestays(): Flow<List<Homestay>> = callbackFlow {
        val reg = homestays
            .whereEqualTo("live", true)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeLiveHomestays failed", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(Homestay::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    /** Live stream of every host's today's-menu doc, keyed by hostId. */
    fun observeAllTodaysMenus(): Flow<Map<String, DailyMenu>> = callbackFlow {
        val reg = dailyMenus.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeAllTodaysMenus failed", err)
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val map = snap?.toObjects(DailyMenu::class.java).orEmpty()
                .filter { !it.isEmpty }
                .associateBy { it.hostId }
            trySend(map)
        }
        awaitClose { reg.remove() }
    }

    fun observeHomestay(hostId: String): Flow<Homestay?> = callbackFlow {
        val reg = homestays.document(hostId).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeHomestay($hostId) failed", err)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(Homestay::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun observeTodaysMenu(hostId: String): Flow<DailyMenu?> = callbackFlow {
        val reg = dailyMenus.document(hostId).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeTodaysMenu($hostId) failed", err)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(DailyMenu::class.java))
        }
        awaitClose { reg.remove() }
    }

    private companion object {
        const val TAG = "HomestayBrowseRepo"
    }
}

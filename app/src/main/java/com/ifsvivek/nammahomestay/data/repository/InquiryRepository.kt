package com.ifsvivek.nammahomestay.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.model.Inquiry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * The "Inquiry Box". A traveller hits "I'm interested" on a homestay detail and
 * a doc is written here; the host sees it newest-first and taps to call back.
 * The same collection is queried from both sides (filtered by [Inquiry.hostId]
 * for the host's tab, or by [Inquiry.travellerId] for the traveller's tab).
 */
class InquiryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val col = db.collection(FirestoreCollections.INQUIRIES)

    /** Host-side stream: inquiries addressed to [hostId]. Sorted newest-first client-side. */
    fun observeInquiriesForHost(hostId: String): Flow<List<Inquiry>> =
        observeBy(field = "hostId", value = hostId, tag = "ForHost")

    /** Traveller-side stream: inquiries this user has sent. Sorted newest-first client-side. */
    fun observeInquiriesForTraveller(travellerId: String): Flow<List<Inquiry>> =
        observeBy(field = "travellerId", value = travellerId, tag = "ForTraveller")

    /**
     * The two streams are structurally identical — a single `whereEqualTo` with
     * an in-memory newest-first sort. We intentionally don't `orderBy` on the
     * server because that would force Firestore to demand a composite index
     * (hostId|travellerId asc + timestamp desc) before the listener works at
     * all, and we'd rather get every inquiry and sort 1–100 in Kotlin.
     */
    private fun observeBy(field: String, value: String, tag: String): Flow<List<Inquiry>> = callbackFlow {
        val reg = col
            .whereEqualTo(field, value)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "snapshot[$tag, $field=$value] failed", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(Inquiry::class.java).orEmpty()
                    .sortedByDescending { it.timestamp?.time ?: 0L }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Traveller-side: send an "I'm interested" to the given host. */
    suspend fun sendInquiry(
        hostId: String,
        travellerId: String,
        guestName: String,
        guestPhone: String,
    ): String {
        val ref = col.add(
            Inquiry(
                hostId = hostId,
                travellerId = travellerId,
                guestName = guestName.ifBlank { "A traveller" },
                guestPhone = guestPhone,
                status = "pending",
                timestamp = Timestamp.now().toDate(),
            ),
        ).await()
        return ref.id
    }

    suspend fun markCalled(inquiryId: String) {
        col.document(inquiryId).update("status", "called").await()
    }

    suspend fun markClosed(inquiryId: String) {
        col.document(inquiryId).update("status", "closed").await()
    }

    /**
     * Host-side dev helper: seeds one fake inquiry "from a traveller" so the
     * Inquiry Box has something in it. Leaves [Inquiry.travellerId] blank so
     * the rules accept it without a real traveller uid.
     */
    suspend fun addSampleInquiry(hostId: String) {
        val samples = listOf(
            "Anita & family" to "+919900112233",
            "Ramesh" to "+919812345678",
            "Priya (Bengaluru)" to "+919845098450",
        )
        val (name, phone) = samples.random()
        col.add(
            Inquiry(
                hostId = hostId,
                travellerId = "",
                guestName = name,
                guestPhone = phone,
                status = "pending",
                timestamp = Timestamp.now().toDate(),
            ),
        ).await()
    }

    private companion object {
        const val TAG = "InquiryRepository"
    }
}

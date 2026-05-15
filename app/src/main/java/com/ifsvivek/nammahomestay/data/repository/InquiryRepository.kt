package com.ifsvivek.nammahomestay.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    /** Host-side stream: inquiries addressed to [hostId]. */
    fun observeInquiriesForHost(hostId: String): Flow<List<Inquiry>> = callbackFlow {
        val reg = col
            .whereEqualTo("hostId", hostId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(Inquiry::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    /** Traveller-side stream: inquiries this user has sent. */
    fun observeInquiriesForTraveller(travellerId: String): Flow<List<Inquiry>> = callbackFlow {
        val reg = col
            .whereEqualTo("travellerId", travellerId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(Inquiry::class.java).orEmpty())
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
}

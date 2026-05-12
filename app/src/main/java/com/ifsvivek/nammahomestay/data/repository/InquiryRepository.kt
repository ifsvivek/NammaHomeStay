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
 * The "Inquiry Box": travellers (eventually, the traveller-facing app) drop an
 * interest into `inquiries`; the host sees newest-first and taps to call back.
 */
class InquiryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val col = db.collection(FirestoreCollections.INQUIRIES)

    fun observeInquiries(hostId: String): Flow<List<Inquiry>> = callbackFlow {
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

    suspend fun markCalled(inquiryId: String) {
        col.document(inquiryId).update("status", "called").await()
    }

    suspend fun markClosed(inquiryId: String) {
        col.document(inquiryId).update("status", "closed").await()
    }

    /**
     * Seeds one sample inquiry. Until the traveller app exists this is the only
     * way to populate the Inquiry Box on a test device — wired to a small button
     * on the (otherwise empty) Inquiries screen.
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
                guestName = name,
                guestPhone = phone,
                status = "pending",
                timestamp = Timestamp.now().toDate(),
            ),
        ).await()
    }
}

package com.ifsvivek.nammahomestay.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.model.AggregateRating
import com.ifsvivek.nammahomestay.data.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reviews collection. Travellers can leave 1–5 star reviews on any homestay;
 * the browse list shows an aggregate rating per home, the detail screen shows
 * the per-review list. Aggregates are computed client-side (no Cloud Functions
 * on the free plan), which works fine for the volumes an MVP sees.
 */
class ReviewRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val col = db.collection(FirestoreCollections.REVIEWS)

    /** Newest-first reviews for one homestay. */
    fun observeReviewsFor(homestayId: String): Flow<List<Review>> = callbackFlow {
        val reg = col.whereEqualTo("homestayId", homestayId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeReviewsFor($homestayId) failed", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(Review::class.java).orEmpty()
                    .sortedByDescending { it.timestamp?.time ?: 0L }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Aggregate rating per homestay, for the browse list. */
    fun observeAggregatesByHostId(): Flow<Map<String, AggregateRating>> = callbackFlow {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeAggregatesByHostId failed", err)
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val grouped = snap?.toObjects(Review::class.java).orEmpty()
                .filter { it.rating in 1..5 && it.homestayId.isNotBlank() }
                .groupBy { it.homestayId }
            val agg = grouped.mapValues { (hostId, reviews) ->
                AggregateRating(
                    homestayId = hostId,
                    count = reviews.size,
                    averageStars = reviews.sumOf { it.rating }.toFloat() / reviews.size,
                )
            }
            trySend(agg)
        }
        awaitClose { reg.remove() }
    }

    suspend fun submit(
        homestayId: String,
        travellerId: String,
        travellerName: String,
        rating: Int,
        comment: String,
    ) {
        require(rating in 1..5) { "Rating must be 1..5, was $rating" }
        col.add(
            Review(
                homestayId = homestayId,
                travellerId = travellerId,
                travellerName = travellerName.ifBlank { "A traveller" },
                rating = rating,
                comment = comment.trim().take(500),
                timestamp = Timestamp.now().toDate(),
            ),
        ).await()
    }

    private companion object {
        const val TAG = "ReviewRepository"
    }
}

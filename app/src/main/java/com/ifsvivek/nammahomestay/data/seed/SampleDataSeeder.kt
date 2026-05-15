package com.ifsvivek.nammahomestay.data.seed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ifsvivek.nammahomestay.data.FirestoreCollections
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.model.Review
import com.ifsvivek.nammahomestay.data.model.VerificationChecklist
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Writes a handful of fake homestays — with reviews, today's menus, map pins —
 * into Firestore, so a fresh install / fresh project has something realistic
 * to browse. Wired to a "Add demo data" button on the host Guide screen.
 *
 * Idempotent: every fake homestay has a stable doc id like `sample-coorg-01`,
 * so calling the seeder twice just over-writes the same docs; reviews are
 * keyed by sample doc ids too to avoid duplicates.
 */
class SampleDataSeeder(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun seed(): SeedResult {
        var homestaysWritten = 0
        var menusWritten = 0
        var reviewsWritten = 0

        SAMPLES.forEach { sample ->
            db.collection(FirestoreCollections.HOMESTAYS).document(sample.hostId).set(
                Homestay(
                    id = sample.hostId,
                    hostId = sample.hostId,
                    name = sample.name,
                    location = sample.location,
                    images = sample.photoColors.map { (label, color) ->
                        Blob.fromBytes(placeholderJpeg(label, color))
                    },
                    checklist = VerificationChecklist(
                        cleanBedding = true,
                        functionalWashroom = true,
                        drinkingWater = true,
                    ),
                    live = true,
                    latitude = sample.lat,
                    longitude = sample.lng,
                ),
                SetOptions.merge(),
            ).await()
            homestaysWritten++

            if (sample.todayDish != null && sample.todayPrice != null) {
                db.collection(FirestoreCollections.DAILY_MENUS).document(sample.hostId).set(
                    DailyMenu(
                        id = sample.hostId,
                        hostId = sample.hostId,
                        dishName = sample.todayDish,
                        price = sample.todayPrice,
                        image = Blob.fromBytes(
                            placeholderJpeg(sample.todayDish, sample.dishColor ?: 0xFFB28704.toInt()),
                        ),
                        dateTimestamp = Timestamp.now().toDate(),
                    ),
                ).await()
                menusWritten++
            }

            sample.reviews.forEachIndexed { i, r ->
                // Stable id so reseeding doesn't duplicate.
                val reviewId = "${sample.hostId}-r${i + 1}"
                db.collection(FirestoreCollections.REVIEWS).document(reviewId).set(
                    Review(
                        id = reviewId,
                        homestayId = sample.hostId,
                        travellerId = "sample-traveller-$i",
                        travellerName = r.name,
                        rating = r.rating,
                        comment = r.comment,
                        timestamp = Timestamp.now().toDate(),
                    ),
                ).await()
                reviewsWritten++
            }
        }

        Log.i(TAG, "Seeded $homestaysWritten homestays, $menusWritten menus, $reviewsWritten reviews.")
        return SeedResult(homestaysWritten, menusWritten, reviewsWritten)
    }

    data class SeedResult(val homestays: Int, val menus: Int, val reviews: Int)

    private companion object {
        const val TAG = "SampleDataSeeder"

        // ── Sample homestays ────────────────────────────────────────────────
        private val SAMPLES = listOf(
            Sample(
                hostId = "sample-sakleshpur-01",
                name = "Lakshmi Farm Stay",
                location = "Sakleshpur, Hassan",
                lat = 12.9416, lng = 75.7825,
                photoColors = listOf("Farm view" to 0xFF2E7D32.toInt(), "Verandah" to 0xFF795548.toInt()),
                todayDish = "Ragi mudde & saaru", todayPrice = 120L, dishColor = 0xFF6D4C41.toInt(),
                reviews = listOf(
                    SampleReview("Anita & family", 5, "Lakshmi-amma's coffee is the best part of any morning. Spotless room, plenty of hot water."),
                    SampleReview("Ramesh", 5, "Quiet, clean, and the saaru reminded me of my own grandmother's. Will be back."),
                    SampleReview("Priya (Bengaluru)", 4, "Loved the verandah view. WiFi can be patchy in heavy rain."),
                    SampleReview("Karthik", 5, "Booked on short notice, host picked up the phone and arranged everything."),
                ),
            ),
            Sample(
                hostId = "sample-coorg-02",
                name = "Coorg Coffee Estate Home",
                location = "Madikeri, Kodagu",
                lat = 12.4244, lng = 75.7382,
                photoColors = listOf("Coffee bushes" to 0xFF1B5E20.toInt(), "Cottage" to 0xFFD7CCC8.toInt(), "Fireplace" to 0xFFF9A825.toInt()),
                todayDish = "Pandi curry & akki roti", todayPrice = 250L, dishColor = 0xFF8D2A0E.toInt(),
                reviews = listOf(
                    SampleReview("Suresh & Geetha", 4, "Misty mornings, real coffee, friendly host. Cottage was a little chilly at night, ask for an extra blanket."),
                    SampleReview("Arjun", 5, "Best pandi curry I've eaten outside of a friend's house. Definitely coming back in monsoon."),
                    SampleReview("Meera (Mumbai)", 4, "Lovely walk through the plantation with the host. Power went out once briefly."),
                ),
            ),
            Sample(
                hostId = "sample-chikmagalur-03",
                name = "Western Ghats Retreat",
                location = "Mudigere, Chikmagalur",
                lat = 13.1310, lng = 75.6432,
                photoColors = listOf("Hill view" to 0xFF4CAF50.toInt(), "Garden swing" to 0xFFCDDC39.toInt()),
                todayDish = null, todayPrice = null, dishColor = null,
                reviews = listOf(
                    SampleReview("Divya", 5, "Stunning sunrises. Host left a hand-drawn map of nearby waterfalls — magical touch."),
                    SampleReview("Vishal & Neha", 5, "Came for a long weekend, stayed five days. Everything just works."),
                ),
            ),
            Sample(
                hostId = "sample-mysuru-04",
                name = "Mysuru Heritage House",
                location = "Lakshmipuram, Mysuru",
                lat = 12.3052, lng = 76.6552,
                photoColors = listOf("Courtyard" to 0xFFB28704.toInt(), "Old door" to 0xFF6D4C41.toInt(), "Room" to 0xFFEEE0C8.toInt(), "Garden" to 0xFF388E3C.toInt()),
                todayDish = "Mysore masala dosa", todayPrice = 80L, dishColor = 0xFFFFB300.toInt(),
                reviews = listOf(
                    SampleReview("Anjali", 5, "Walking distance to the palace; the family treats you like an old friend."),
                    SampleReview("Naveen", 5, "Filter coffee + dosa breakfast — exactly the Mysuru experience I came for."),
                    SampleReview("Sangeeta (Kolkata)", 4, "Beautiful old house. The taps creak charmingly. Bring earplugs for the morning birds."),
                    SampleReview("Ravi", 5, "Host's daughter walked me to the market and back. Above-and-beyond kind."),
                    SampleReview("Tina & Mark (US)", 5, "First homestay in India and we couldn't have asked for better. Thank you, aunty!"),
                ),
            ),
            Sample(
                hostId = "sample-wayanad-05",
                name = "Wayanad Wild Stay",
                location = "Kalpetta, Wayanad",
                lat = 11.6086, lng = 76.0840,
                photoColors = listOf("Forest path" to 0xFF2E5F2D.toInt(), "Bamboo hut" to 0xFF8D6E63.toInt()),
                todayDish = "Kerala thali", todayPrice = 150L, dishColor = 0xFF558B2F.toInt(),
                reviews = listOf(
                    SampleReview("Hari", 4, "Heard wild peacocks at dawn. The hut is basic but very clean."),
                    SampleReview("Sneha & Rohit", 5, "Trekking guide was excellent. Will recommend to friends."),
                ),
            ),
        )
    }
}

private data class Sample(
    val hostId: String,
    val name: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val photoColors: List<Pair<String, Int>>,
    val todayDish: String?,
    val todayPrice: Long?,
    val dishColor: Int?,
    val reviews: List<SampleReview>,
)

private data class SampleReview(val name: String, val rating: Int, val comment: String)

/**
 * Generates a small JPEG (about 4-10 KB) with a solid colour background + a
 * caption — used as the placeholder photo for sample homestays so the browse
 * cards look "real" without bundling actual images in the APK.
 */
private fun placeholderJpeg(label: String, color: Int): ByteArray {
    val width = 800
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
        textSize = 64f
        textAlign = Paint.Align.CENTER
    }
    val bounds = Rect()
    paint.getTextBounds(label, 0, label.length, bounds)
    canvas.drawText(label, width / 2f, height / 2f + bounds.height() / 2f, paint)
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
    bitmap.recycle()
    return out.toByteArray()
}

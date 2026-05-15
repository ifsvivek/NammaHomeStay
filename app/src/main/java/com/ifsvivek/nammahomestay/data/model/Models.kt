package com.ifsvivek.nammahomestay.data.model

import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Domain models. These double as Firestore POJOs, so every field has a default
 * value and a no-arg constructor is available (data class with all defaults).
 *
 * Firestore layout (see also [com.ifsvivek.nammahomestay.data.FirestoreCollections]):
 *   hosts/{uid}                -> Host
 *   homestays/{uid}            -> Homestay      (one shopfront per host, for the MVP)
 *   daily_menus/{uid}          -> DailyMenu     (overwritten each day with one set())
 *   inquiries/{autoId}         -> Inquiry
 *
 * Photos are stored as JPEG [Blob]s directly inside the documents, not in Cloud
 * Storage — the free Spark plan doesn't include Storage. [ImageCompressor] keeps
 * each image small enough to stay well under Firestore's ~1 MB per-document cap.
 */

data class Host(
    @DocumentId val uid: String = "",
    val name: String = "",
    val phone: String = "",
    /** "new" | "verified" — the host is "verified" once the checklist is complete. */
    val verifiedStatus: String = "new",
)

data class VerificationChecklist(
    val cleanBedding: Boolean = false,
    val functionalWashroom: Boolean = false,
    val drinkingWater: Boolean = false,
) {
    /** The shopfront can only go Live when every basic promise is ticked. */
    val isComplete: Boolean get() = cleanBedding && functionalWashroom && drinkingWater
}

data class Homestay(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val location: String = "",
    /** Up to a handful of small JPEGs. Capped so the doc stays under 1 MB. */
    val images: List<Blob> = emptyList(),
    val checklist: VerificationChecklist = VerificationChecklist(),
    /** Visible to travellers. Only true when [checklist].isComplete and there is ≥1 photo. */
    val live: Boolean = false,
) {
    val canGoLive: Boolean get() = checklist.isComplete && images.isNotEmpty() && name.isNotBlank()

    companion object {
        const val MAX_PHOTOS = 6
    }
}

data class DailyMenu(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val dishName: String = "",
    val price: Long = 0L,
    /** The dish photo as a small JPEG, or null if none. */
    val image: Blob? = null,
    @ServerTimestamp val dateTimestamp: Date? = null,
) {
    val isEmpty: Boolean get() = dishName.isBlank() && image == null
}

data class Inquiry(
    @DocumentId val id: String = "",
    val hostId: String = "",
    /** Firebase uid of the traveller who sent this inquiry — empty for the host-side seed/sample button. */
    val travellerId: String = "",
    val guestName: String = "",
    val guestPhone: String = "",
    /** "pending" | "called" | "closed" */
    val status: String = "pending",
    @ServerTimestamp val timestamp: Date? = null,
)

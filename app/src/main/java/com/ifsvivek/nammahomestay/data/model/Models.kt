package com.ifsvivek.nammahomestay.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Domain models. These double as Firestore POJOs, so every field has a default
 * value and a no-arg constructor is available (data class with all defaults).
 *
 * Firestore layout (see also [FirestoreCollections]):
 *   hosts/{uid}                -> Host
 *   homestays/{uid}            -> Homestay      (one shopfront per host, for the MVP)
 *   daily_menus/{uid}          -> DailyMenu     (overwritten each day with one set())
 *   inquiries/{autoId}         -> Inquiry
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
    val images: List<String> = emptyList(),
    val checklist: VerificationChecklist = VerificationChecklist(),
    /** Visible to travellers. Only true when [checklist].isComplete and there is ≥1 photo. */
    val live: Boolean = false,
) {
    val canGoLive: Boolean get() = checklist.isComplete && images.isNotEmpty() && name.isNotBlank()
}

data class DailyMenu(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val dishName: String = "",
    val price: Long = 0L,
    val imageUrl: String = "",
    @ServerTimestamp val dateTimestamp: Date? = null,
) {
    val isEmpty: Boolean get() = dishName.isBlank() && imageUrl.isBlank()
}

data class Inquiry(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val guestName: String = "",
    val guestPhone: String = "",
    /** "pending" | "called" | "closed" */
    val status: String = "pending",
    @ServerTimestamp val timestamp: Date? = null,
)

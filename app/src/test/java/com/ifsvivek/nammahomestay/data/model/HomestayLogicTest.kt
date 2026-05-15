package com.ifsvivek.nammahomestay.data.model

import com.google.firebase.firestore.Blob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit tests for the rules that decide when a host's "shopfront" is
 * allowed to go LIVE. These are the only pieces of domain logic that aren't
 * pure plumbing, so they're worth pinning down with tests.
 */
class HomestayLogicTest {

    @Test
    fun `checklist is incomplete by default`() {
        assertFalse(VerificationChecklist().isComplete)
    }

    @Test
    fun `checklist needs all three promises to be complete`() {
        assertFalse(VerificationChecklist(cleanBedding = true).isComplete)
        assertFalse(VerificationChecklist(cleanBedding = true, functionalWashroom = true).isComplete)
        assertTrue(
            VerificationChecklist(
                cleanBedding = true,
                functionalWashroom = true,
                drinkingWater = true,
            ).isComplete,
        )
    }

    @Test
    fun `homestay canGoLive requires name, a photo, and a full checklist`() {
        val readyHome = Homestay(
            id = "u",
            hostId = "u",
            name = "Lakshmi Farm Stay",
            checklist = VerificationChecklist(true, true, true),
            images = listOf(Blob.fromBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))),
        )

        assertTrue("ready home should go live", readyHome.canGoLive)
        assertFalse("no name", readyHome.copy(name = "").canGoLive)
        assertFalse("no photo", readyHome.copy(images = emptyList()).canGoLive)
        assertFalse(
            "incomplete checklist",
            readyHome.copy(checklist = VerificationChecklist(true, true, false)).canGoLive,
        )
    }

    @Test
    fun `MAX_PHOTOS cap matches what the UI shows`() {
        assertEquals(6, Homestay.MAX_PHOTOS)
    }

    @Test
    fun `dailyMenu isEmpty distinguishes blank from filled`() {
        assertTrue(DailyMenu().isEmpty)
        assertFalse(DailyMenu(dishName = "Ragi mudde").isEmpty)
        assertFalse(DailyMenu(image = Blob.fromBytes(byteArrayOf(1, 2, 3))).isEmpty)
    }
}

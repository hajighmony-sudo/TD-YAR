package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TxAiReviewRulesTest {
    @Test
    fun reviewUpdatesAreUniqueAndAccountScoped() {
        val first = TxAiEpisodeReview("A", "episode-1", TxAiReviewStatus.CONFIRMED, 10L)
        val updated = first.copy(status = TxAiReviewStatus.NOT_REVENGE, updatedAtMillis = 20L)
        val rows = TxAiReviewRules.upsertReview(listOf(first), updated)

        assertEquals(1, rows.size)
        assertEquals(TxAiReviewStatus.NOT_REVENGE, TxAiReviewRules.reviewFor(rows, "A", "episode-1"))
        assertNull(TxAiReviewRules.reviewFor(rows, "B", "episode-1"))
    }

    @Test
    fun insightPinsDoNotLeakAcrossAccountsAndCanBeRemoved() {
        val rows = TxAiReviewRules.setPinned(emptyList(), "A", "insight-1", true)
        assertTrue(TxAiReviewRules.isPinned(rows, "A", "insight-1"))
        assertFalse(TxAiReviewRules.isPinned(rows, "B", "insight-1"))

        val cleared = TxAiReviewRules.setPinned(rows, "A", "insight-1", false)
        assertFalse(TxAiReviewRules.isPinned(cleared, "A", "insight-1"))
        assertEquals(1, cleared.size)
    }

    @Test
    fun unknownReviewStorageValuesAreIgnored() {
        assertNull(TxAiReviewStatus.fromStorageKey("future-value"))
        assertEquals(TxAiReviewStatus.UNSURE, TxAiReviewStatus.fromStorageKey("unsure"))
    }
}

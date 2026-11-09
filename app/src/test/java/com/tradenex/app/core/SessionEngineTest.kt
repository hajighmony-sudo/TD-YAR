package com.tradenex.app.core

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {
    @Test
    fun londonSessionMovesWithBritishDaylightSavingTime() {
        val winter = SessionEngine.statesAt(
            Instant.parse("2026-01-15T08:30:00Z"),
            ZoneOffset.UTC,
        ).first { it.def.name == "London" }
        val summer = SessionEngine.statesAt(
            Instant.parse("2026-07-15T07:30:00Z"),
            ZoneOffset.UTC,
        ).first { it.def.name == "London" }

        assertTrue(winter.open)
        assertTrue(summer.open)
        assertEquals(8 * 60, winter.localStart)
        assertEquals(7 * 60, summer.localStart)
    }

    @Test
    fun tokyoUsesItsOwnTimezoneWithoutDstShift() {
        val state = SessionEngine.statesAt(
            Instant.parse("2026-07-15T01:00:00Z"),
            ZoneOffset.UTC,
        ).first { it.def.name == "Tokyo" }

        assertTrue(state.open)
        assertEquals(0, state.localStart)
    }
}

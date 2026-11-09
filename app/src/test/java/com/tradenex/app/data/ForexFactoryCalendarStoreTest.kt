package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ForexFactoryCalendarStoreTest {
    @Test
    fun parsesForexFactoryFieldsAndConvertsOffsetToUtc() {
        val events = ForexFactoryCalendarStore.parseEvents(
            """[
                {
                    "title": "US CPI m/m",
                    "country": "USD",
                    "date": "2026-11-10T08:30:00-05:00",
                    "impact": "High",
                    "actual": "3.1%",
                    "forecast": "3.0%",
                    "previous": "2.9%"
                }
            ]""",
        )

        assertEquals(1, events.size)
        val event = events.single()
        assertEquals("US CPI m/m", event.title)
        assertEquals("USD", event.currency)
        assertEquals(2, event.impact)
        assertEquals("3.1%", event.actual)
        assertEquals("3.0%", event.forecast)
        assertEquals("2.9%", event.previous)
        assertEquals(Instant.parse("2026-11-10T13:30:00Z").toEpochMilli(), event.timestampMillis)
    }

    @Test
    fun skipsRowsWithoutValidDateOrTitle() {
        val events = ForexFactoryCalendarStore.parseEvents(
            """[
                {"title": "Missing date", "country": "USD", "impact": "High"},
                {"title": "", "country": "EUR", "date": "2026-11-10T10:00:00Z", "impact": "Low"},
                {"title": "Valid event", "country": "EUR", "date": "2026-11-10T10:00:00Z", "impact": "Medium"}
            ]""",
        )

        assertEquals(1, events.size)
        assertEquals("Valid event", events.single().title)
        assertTrue(events.single().timestampMillis > 0L)
    }
}

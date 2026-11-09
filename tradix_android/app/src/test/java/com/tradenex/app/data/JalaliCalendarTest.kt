package com.tradenex.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JalaliCalendarTest {
    @Test
    fun convertsKnownNowruzDates() {
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.fromGregorian(LocalDate.of(2026, 3, 21)))
        assertEquals(LocalDate.of(2026, 3, 21), JalaliCalendar.toGregorian(1405, 1, 1))
        assertEquals(JalaliDate(1403, 1, 1), JalaliCalendar.fromGregorian(LocalDate.of(2024, 3, 20)))
    }

    @Test
    fun roundTripsMonthBoundariesAcrossRecentYears() {
        for (year in 1390..1410) {
            for (month in 1..12) {
                val lastDay = JalaliCalendar.daysInMonth(year, month)
                val firstGregorian = JalaliCalendar.toGregorian(year, month, 1)
                val lastGregorian = JalaliCalendar.toGregorian(year, month, lastDay)
                assertEquals(JalaliDate(year, month, 1), JalaliCalendar.fromGregorian(firstGregorian))
                assertEquals(JalaliDate(year, month, lastDay), JalaliCalendar.fromGregorian(lastGregorian))
            }
        }
    }

    @Test
    fun parsesGregorianAndJalaliDatesIncludingPersianDigits() {
        val expected = LocalDate.of(2026, 3, 21)
        assertEquals(expected, JalaliCalendar.parseDateInput("2026-03-21"))
        assertEquals(expected, JalaliCalendar.parseDateInput("1405/01/01"))
        assertEquals(expected, JalaliCalendar.parseDateInput("۱۴۰۵/۰۱/۰۱"))
        assertEquals(null, JalaliCalendar.parseDateInput("۱۴۰۵/۱۳/۰۱"))
        assertEquals(null, JalaliCalendar.parseDateInput("not-a-date"))
    }

    @Test
    fun rejectsInvalidMonthAndDay() {
        assertThrows(IllegalArgumentException::class.java) {
            JalaliCalendar.toGregorian(1405, 13, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            JalaliCalendar.toGregorian(1405, 12, 30)
        }
    }
}

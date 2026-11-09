package com.tradenex.app.data

import java.time.LocalDate

data class JalaliDate(val year: Int, val month: Int, val day: Int)

object JalaliCalendar {
    /** Accepts ISO Gregorian dates or Jalali dates (1405/08/17), including Persian digits. */
    fun parseDateInput(value: String): LocalDate? {
        val normalized = buildString {
            value.trim().forEach { char ->
                val digit = "۰۱۲۳۴۵۶۷۸۹".indexOf(char)
                append(if (digit >= 0) ('0' + digit) else char)
            }
        }.replace('/', '-')
        val parts = normalized.split('-')
        val year = parts.getOrNull(0)?.toIntOrNull()
        if (parts.size == 3 && year != null && year in 1200..1600) {
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null
            return runCatching { toGregorian(year, month, day) }.getOrNull()
        }
        return runCatching { LocalDate.parse(normalized) }.getOrNull()
    }

    fun fromGregorian(date: LocalDate): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return JalaliDate(jy, jm, jd)
    }

    fun toGregorian(year: Int, month: Int, day: Int): LocalDate {
        require(month in 1..12) { "Jalali month must be 1..12" }
        require(day in 1..daysInMonth(year, month)) { "Invalid Jalali day" }
        val jy = year + 1595
        var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + day
        days += if (month < 7) (month - 1) * 31 else (month - 7) * 30 + 186

        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * ((--days) / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val dayOfYear = days + 1
        return LocalDate.of(gy, 1, 1).plusDays((dayOfYear - 1).toLong())
    }

    fun daysInMonth(year: Int, month: Int): Int {
        require(month in 1..12) { "Jalali month must be 1..12" }
        if (month <= 6) return 31
        if (month <= 11) return 30
        val first = toGregorianUnchecked(year, 12, 1)
        val next = toGregorianUnchecked(year + 1, 1, 1)
        return java.time.temporal.ChronoUnit.DAYS.between(first, next).toInt()
    }

    private fun toGregorianUnchecked(year: Int, month: Int, day: Int): LocalDate {
        val jy = year + 1595
        var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + day
        days += if (month < 7) (month - 1) * 31 else (month - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * ((--days) / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val ordinal = days + 1
        return LocalDate.of(gy, 1, 1).plusDays((ordinal - 1).toLong())
    }
}

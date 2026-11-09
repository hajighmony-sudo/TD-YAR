package com.tradenex.app.core

import androidx.compose.ui.graphics.Color
import com.tradenex.app.ui.theme.Tx
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Duration
import java.util.TimeZone

data class SessionDef(
    val name: String,
    val startUtc: Int,
    val endUtc: Int,
    val color: Color,
    val zoneId: ZoneId,
    val localOpen: LocalTime,
    val localClose: LocalTime,
)

data class SessionState(
    val def: SessionDef,
    val open: Boolean,
    val minutesToChange: Int,
    val localStart: Int,
    val length: Int,
    val nextChange: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
)

data class OverlapInfo(val a: String, val b: String, val minutes: Int, val percent: Int, val activeNow: Boolean)

object SessionEngine {
    val defs: List<SessionDef>
        get() = listOf(
        SessionDef("Sydney", 1320, 420, Tx.Blue, ZoneId.of("Australia/Sydney"), LocalTime.of(8, 0), LocalTime.of(17, 0)),
        SessionDef("Tokyo", 0, 540, Tx.Purple, ZoneId.of("Asia/Tokyo"), LocalTime.of(9, 0), LocalTime.of(18, 0)),
        SessionDef("London", 480, 1020, Tx.Gold, ZoneId.of("Europe/London"), LocalTime.of(8, 0), LocalTime.of(17, 0)),
        SessionDef("New York", 780, 1320, Tx.Red, ZoneId.of("America/New_York"), LocalTime.of(8, 0), LocalTime.of(17, 0)),
    )


    /** User display timezone: 0 Tehran, 1 UTC, 2 device. */
    fun displayZone(mode: Int): ZoneId = when (mode) {
        0 -> ZoneId.of("Asia/Tehran")
        1 -> ZoneOffset.UTC
        else -> ZoneId.systemDefault()
    }

    /**
     * DST-aware session state. Each market follows its own IANA timezone; the returned
     * timeline coordinates are converted into the user's selected display timezone.
     */
    fun statesAt(now: Instant, displayZone: ZoneId): List<SessionState> = defs.map { def ->
        val marketNow = now.atZone(def.zoneId)
        val date = marketNow.toLocalDate()
        val opens = date.atTime(def.localOpen).atZone(def.zoneId)
        val closes = (if (def.localClose <= def.localOpen) date.plusDays(1) else date)
            .atTime(def.localClose).atZone(def.zoneId)
        val weekday = marketNow.dayOfWeek.value in 1..5
        val isOpen = weekday && !marketNow.isBefore(opens) && marketNow.isBefore(closes)
        val nextBoundary = if (isOpen) closes else {
            val nextOpenDate = if (weekday && marketNow.isBefore(opens)) date else {
                generateSequence(date.plusDays(1)) { it.plusDays(1) }.first { it.dayOfWeek.value in 1..5 }
            }
            nextOpenDate.atTime(def.localOpen).atZone(def.zoneId)
        }
        val displayStart = opens.withZoneSameInstant(displayZone)
        val duration = Duration.between(opens.toInstant(), closes.toInstant()).toMinutes().toInt()
        SessionState(
            def = def,
            open = isOpen,
            minutesToChange = Duration.between(now, nextBoundary.toInstant()).toMinutes().toInt().coerceAtLeast(0),
            localStart = displayStart.hour * 60 + displayStart.minute,
            length = duration,
            nextChange = nextBoundary,
        )
    }

    private fun isOpenAt(def: SessionDef, instant: Instant): Boolean {
        val local = instant.atZone(def.zoneId)
        val time = local.toLocalTime()
        if (local.dayOfWeek.value !in 1..5) return false
        return if (def.localOpen < def.localClose) {
            !time.isBefore(def.localOpen) && time.isBefore(def.localClose)
        } else {
            !time.isBefore(def.localOpen) || time.isBefore(def.localClose)
        }
    }

    /** Calculate today's overlap in UTC minutes so DST transitions are reflected. */
    fun overlapsAt(now: Instant): List<OverlapInfo> {
        val utcDate = now.atZone(ZoneOffset.UTC).toLocalDate()
        val start = utcDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = utcDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val pairs = listOf(0 to 1, 1 to 2, 2 to 3)
        val counts = IntArray(pairs.size)
        val active = BooleanArray(pairs.size)
        var cursor = start
        while (cursor.isBefore(end)) {
            pairs.forEachIndexed { index, (a, b) ->
                val both = isOpenAt(defs[a], cursor) && isOpenAt(defs[b], cursor)
                if (both) counts[index]++
                if (cursor <= now && now < cursor.plusSeconds(60) && both) active[index] = true
            }
            cursor = cursor.plusSeconds(60)
        }
        return pairs.mapIndexed { index, (a, b) ->
            val baseline = minOf(
                Duration.between(start, end).toMinutes().toInt(),
                540,
            ).coerceAtLeast(1)
            OverlapInfo(
                a = defs[a].name,
                b = defs[b].name,
                minutes = counts[index],
                percent = (counts[index] * 100 / baseline).coerceIn(0, 100),
                activeNow = active[index],
            )
        }
    }

    fun currentUtcMinute(): Int {
        val z = ZonedDateTime.now(ZoneOffset.UTC)
        return z.hour * 60 + z.minute
    }

    /** 0 = Tehran (UTC+3:30), 1 = UTC, 2 = device zone. */
    fun tzOffsetMinutes(mode: Int): Int = when (mode) {
        0 -> 210
        1 -> 0
        else -> TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
    }

    fun states(utcMinute: Int, tzOffset: Int): List<SessionState> {
        val mask = NativeCore.sessionMask(utcMinute)
        return defs.mapIndexed { i, d ->
            SessionState(
                def = d,
                open = (mask shr i) and 1 == 1,
                minutesToChange = NativeCore.minutesToChange(utcMinute, i),
                localStart = (((d.startUtc + tzOffset) % 1440) + 1440) % 1440,
                length = ((d.endUtc - d.startUtc) + 1440) % 1440,
            )
        }
    }

    fun overlaps(utcMinute: Int): List<OverlapInfo> {
        val mask = NativeCore.sessionMask(utcMinute)
        return listOf(0 to 1, 1 to 2, 2 to 3).map { (a, b) ->
            val m = NativeCore.overlapMinutes(a, b)
            OverlapInfo(
                a = defs[a].name,
                b = defs[b].name,
                minutes = m,
                percent = m * 100 / 540,
                activeNow = (mask shr a) and 1 == 1 && (mask shr b) and 1 == 1,
            )
        }
    }

    fun clock(minute: Int): String {
        val m = ((minute % 1440) + 1440) % 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }

    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m دقیقه"
            m == 0 -> "$h ساعت"
            else -> "$h ساعت و $m دقیقه"
        }
    }
}

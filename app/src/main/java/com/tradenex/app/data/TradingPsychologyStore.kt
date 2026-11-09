package com.tradenex.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class PsychologyDay(
    val date: String,
    val confidence: Int = 3,
    val focus: Int = 3,
    val stress: Int = 3,
    val fomo: Int = 1,
    val revengeUrge: Int = 1,
    val discipline: Int = 3,
    val notes: String = "",
)

object TradingPsychologyStore {
    val days = mutableStateMapOf<String, PsychologyDay>()
    private var prefs: android.content.SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("tradix_psychology_v1", Context.MODE_PRIVATE)
        val raw = prefs?.getString("days", null) ?: return
        runCatching {
            val rows = JSONArray(raw)
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val date = row.optString("date")
                if (date.isBlank()) continue
                days[date] = PsychologyDay(
                    date = date,
                    confidence = row.optInt("confidence", 3).coerceIn(1, 5),
                    focus = row.optInt("focus", 3).coerceIn(1, 5),
                    stress = row.optInt("stress", 3).coerceIn(1, 5),
                    fomo = row.optInt("fomo", 1).coerceIn(1, 5),
                    revengeUrge = row.optInt("revengeUrge", 1).coerceIn(1, 5),
                    discipline = row.optInt("discipline", 3).coerceIn(1, 5),
                    notes = row.optString("notes"),
                )
            }
        }
    }

    fun day(date: LocalDate = LocalDate.now()): PsychologyDay = days[date.toString()] ?: PsychologyDay(date.toString())

    fun save(day: PsychologyDay) {
        days[day.date] = day.copy(
            confidence = day.confidence.coerceIn(1, 5),
            focus = day.focus.coerceIn(1, 5),
            stress = day.stress.coerceIn(1, 5),
            fomo = day.fomo.coerceIn(1, 5),
            revengeUrge = day.revengeUrge.coerceIn(1, 5),
            discipline = day.discipline.coerceIn(1, 5),
        )
        persist()
    }

    fun persist() {
        val rows = JSONArray()
        days.values.sortedBy { it.date }.forEach { d ->
            rows.put(JSONObject().apply {
                put("date", d.date)
                put("confidence", d.confidence)
                put("focus", d.focus)
                put("stress", d.stress)
                put("fomo", d.fomo)
                put("revengeUrge", d.revengeUrge)
                put("discipline", d.discipline)
                put("notes", d.notes)
            })
        }
        prefs?.edit()?.putString("days", rows.toString())?.apply()
    }
}

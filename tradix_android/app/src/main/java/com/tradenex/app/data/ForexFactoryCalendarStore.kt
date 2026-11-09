package com.tradenex.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class ForexFactoryEvent(
    val id: Int,
    val title: String,
    val currency: String,
    val impact: Int,
    val timestampMillis: Long,
    val actual: String,
    val forecast: String,
    val previous: String,
)

data class ForexFactoryCalendarResult(
    val events: List<ForexFactoryEvent>,
    val fetchedAtMillis: Long,
    val fromCache: Boolean,
    val message: String,
)

/**
 * Reads the public Forex Factory weekly JSON export.
 *
 * The feed is rate-limited, so successful responses are cached locally and normal
 * refreshes are limited to once per hour. Manual refreshes are additionally guarded
 * by a five-minute cooldown. This is a third-party export endpoint, not a guaranteed API.
 */
object ForexFactoryCalendarStore {
    private const val FEED_URL = "https://nfs.faireconomy.media/ff_calendar_thisweek.json"
    private const val PREFS = "tradenex_forex_factory_calendar"
    private const val PAYLOAD_KEY = "payload"
    private const val FETCHED_AT_KEY = "fetched_at"
    private const val ATTEMPTED_AT_KEY = "attempted_at"
    private const val HOUR_MS = 60L * 60L * 1000L
    private const val COOLDOWN_MS = 5L * 60L * 1000L

    suspend fun load(context: Context, forceRefresh: Boolean = false): ForexFactoryCalendarResult =
        withContext(Dispatchers.IO) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val cachedPayload = prefs.getString(PAYLOAD_KEY, null)
            val cachedAt = prefs.getLong(FETCHED_AT_KEY, 0L)
            val attemptedAt = prefs.getLong(ATTEMPTED_AT_KEY, 0L)
            val cachedEvents = cachedPayload?.let(::parseEvents).orEmpty()
            val now = System.currentTimeMillis()
            val age = if (cachedAt > 0L) now - cachedAt else Long.MAX_VALUE
            val attemptAge = if (attemptedAt > 0L) now - attemptedAt else Long.MAX_VALUE

            if (cachedEvents.isNotEmpty() && !forceRefresh && age < HOUR_MS) {
                return@withContext ForexFactoryCalendarResult(
                    cachedEvents, cachedAt, true, "دادهٔ ذخیره‌شده · دریافت خودکار هر یک ساعت",
                )
            }
            if (attemptAge < COOLDOWN_MS) {
                return@withContext ForexFactoryCalendarResult(
                    cachedEvents,
                    cachedAt,
                    cachedEvents.isNotEmpty(),
                    "برای رعایت محدودیت Forex Factory، تلاش بعدی پس از ۵ دقیقه ممکن است",
                )
            }

            prefs.edit().putLong(ATTEMPTED_AT_KEY, now).apply()
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12_000
                    readTimeout = 12_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "TRADIX-Android/0.1")
                }
                val status = connection.responseCode
                if (status !in 200..299) error("HTTP $status")
                val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val events = parseEvents(payload)
                require(events.isNotEmpty()) { "پاسخ منبع خالی یا نامعتبر بود." }

                val fetchedAt = System.currentTimeMillis()
                prefs.edit()
                    .putString(PAYLOAD_KEY, payload)
                    .putLong(FETCHED_AT_KEY, fetchedAt)
                    .apply()

                ForexFactoryCalendarResult(events, fetchedAt, false, "اتصال موفق به تقویم Forex Factory")
            } catch (error: Exception) {
                if (cachedEvents.isNotEmpty()) {
                    ForexFactoryCalendarResult(
                        cachedEvents, cachedAt, true,
                        "اتصال برقرار نشد؛ آخرین دادهٔ ذخیره‌شده نمایش داده می‌شود",
                    )
                } else {
                    ForexFactoryCalendarResult(
                        emptyList(), 0L, false,
                        "دریافت تقویم ناموفق بود: ${error.message ?: "خطای شبکه"}",
                    )
                }
            } finally {
                connection?.disconnect()
            }
        }

    internal fun parseEvents(payload: String): List<ForexFactoryEvent> {
        val array = JSONArray(payload)
        val events = ArrayList<ForexFactoryEvent>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val currency = item.optString("country").trim().uppercase()
            val rawDate = item.optString("date").trim()
            val timestamp = parseTimestamp(rawDate) ?: continue
            if (title.isBlank() || currency.isBlank()) continue

            val impact = when (item.optString("impact").trim().lowercase()) {
                "high" -> 2
                "medium" -> 1
                "low" -> 0
                "holiday" -> -1
                else -> 0
            }
            val id = "${currency}|${timestamp}|${title}".hashCode()
            events += ForexFactoryEvent(
                id = id,
                title = title,
                currency = currency,
                impact = impact,
                timestampMillis = timestamp,
                actual = item.optString("actual").trim(),
                forecast = item.optString("forecast").trim(),
                previous = item.optString("previous").trim(),
            )
        }
        return events.distinctBy { it.id }.sortedBy { it.timestampMillis }
    }

    private fun parseTimestamp(value: String): Long? {
        if (value.isBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value)
                    .atZone(ZoneId.of("America/New_York"))
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
    }
}

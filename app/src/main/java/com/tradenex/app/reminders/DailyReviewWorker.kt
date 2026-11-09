package com.tradenex.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tradenex.app.MainActivity
import com.tradenex.app.R
import com.tradenex.app.notifications.NexNotify
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

class DailyReviewWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("tradix_weekly_planner", Context.MODE_PRIVATE)
        val settings = applicationContext.getSharedPreferences("tradix_local_settings", Context.MODE_PRIVATE)
        val reminderOn = settings.getBoolean("reminderOn", true)
        val hour = settings.getInt("reminderHour", 22).coerceIn(0, 23)
        val today = LocalDate.now()
        if (reminderOn && today.dayOfWeek.value in 1..5) {
            val raw = prefs.getString("plans", null)
            val plan = runCatching {
                val rows = JSONArray(raw ?: "[]")
                (0 until rows.length()).map { rows.getJSONObject(it) }.firstOrNull { it.optString("date") == today.toString() }
            }.getOrNull()
            val plannedCount = plan?.optInt("tradeCount", 0) ?: 0
            val journalCount = journalTradesOn(today)
            val count = maxOf(plannedCount, journalCount)
            val reason = plan?.optString("noTradeReason").orEmpty()
            val confirmed = plan?.optBoolean("confirmed", false) ?: false
            if (count == 0 && (!confirmed || reason.isBlank())) notifyReview()
        }
        if (today.dayOfWeek.value == 5) {
            val weeklyPrefs = applicationContext.getSharedPreferences("nex_notify_weekly", Context.MODE_PRIVATE)
            val weekKey = "${today.get(WeekFields.ISO.weekBasedYear())}-${today.get(WeekFields.ISO.weekOfWeekBasedYear())}"
            if (weeklyPrefs.getString("sent_week", null) != weekKey) {
                NexNotify.weeklyReport(applicationContext)
                weeklyPrefs.edit().putString("sent_week", weekKey).apply()
            }
        }
        ReminderScheduler.schedule(applicationContext)
        return Result.success()
    }

    private fun journalTradesOn(date: LocalDate): Int {
        val dbFile = applicationContext.getDatabasePath("tradix_local.db")
        if (!dbFile.exists()) return 0
        return runCatching {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT payload FROM app_snapshot WHERE id = 1", null).use { cursor ->
                    if (!cursor.moveToFirst()) return@use 0
                    val snapshot = JSONObject(cursor.getString(0))
                    val trades = snapshot.optJSONArray("trades") ?: JSONArray()
                    (0 until trades.length()).count { index ->
                        trades.optJSONObject(index)?.optLong("epochDay", Long.MIN_VALUE) == date.toEpochDay()
                    }
                }
            }
        }.getOrDefault(0)
    }

    private fun notifyReview() {
        NexNotify.journalReminder(applicationContext)
    }

    private fun todayNotificationId() = LocalDate.now().dayOfYear + 7000
}

object ReminderScheduler {
    const val CHANNEL = "tradix_daily_review"

    fun schedule(context: Context, reminderOn: Boolean? = null, reminderHour: Int? = null) {
        val manager = WorkManager.getInstance(context)
        val settings = context.getSharedPreferences("tradix_local_settings", Context.MODE_PRIVATE)
        if (reminderOn != null || reminderHour != null) {
            settings.edit()
                .putBoolean("reminderOn", reminderOn ?: settings.getBoolean("reminderOn", true))
                .putInt("reminderHour", (reminderHour ?: settings.getInt("reminderHour", 22)).coerceIn(0, 23))
                .apply()
        }
        if (!settings.getBoolean("reminderOn", true)) {
            manager.cancelUniqueWork("tradix_daily_review")
            return
        }
        val hour = settings.getInt("reminderHour", 22).coerceIn(0, 23)
        val now = LocalTime.now()
        var target = LocalDate.now().atTime(hour, 0)
        if (!target.isAfter(java.time.LocalDateTime.now())) target = target.plusDays(1)
        val delay = Duration.between(java.time.LocalDateTime.now(), target).toMillis().coerceAtLeast(60_000)
        val request = OneTimeWorkRequestBuilder<DailyReviewWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        manager.enqueueUniqueWork("tradix_daily_review", ExistingWorkPolicy.REPLACE, request)
    }
}

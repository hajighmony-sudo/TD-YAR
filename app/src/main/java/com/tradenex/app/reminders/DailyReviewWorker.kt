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
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
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
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(ReminderScheduler.CHANNEL, "مرور روزانه معاملات", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val intent = PendingIntent.getActivity(applicationContext, 1201, Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = android.net.Uri.parse("tradix://weekly_reminder")
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("امروز معامله‌ای نداشتی؟")
            .setContentText("وارد TRADIX شو و دلیل نداشتن معامله را در برنامه هفتگی ثبت کن.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("اگر بازار ستاپ مناسبی نداشت یا تصمیم گرفتی معامله نکنی، دلیلش را ثبت کن تا گزارش هفتگی دقیق‌تری داشته باشی."))
            .setContentIntent(intent)
            .setColor(0xFFD4AF37.toInt())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "ثبت معامله", deepLink(1202, "add"))
            .addAction(0, "بدون معامله؛ ثبت دلیل", deepLink(1203, "weekly_reminder"))
            .addAction(0, "بینش‌ها", deepLink(1204, "insights"))
            .setAutoCancel(true)
            .build()
        manager.notify(todayNotificationId(), notification)
    }

    private fun deepLink(code: Int, host: String): PendingIntent = PendingIntent.getActivity(
        applicationContext, code,
        Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = android.net.Uri.parse("tradix://$host")
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun todayNotificationId() = LocalDate.now().dayOfYear + 7000
}

object ReminderScheduler {
    const val CHANNEL = "tradix_daily_review"
    const val NEWS_CHANNEL = "tradix_news_alert"

    /** Schedules (or cancels) a notification [leadMinutes] before a news event. Returns false if the time already passed. */
    fun scheduleNews(context: Context, id: Int, title: String, currency: String, atMillis: Long, enable: Boolean, leadMinutes: Int = 15): Boolean {
        val manager = WorkManager.getInstance(context)
        val name = "tradix_news_$id"
        if (!enable) { manager.cancelUniqueWork(name); return true }
        val delay = atMillis - leadMinutes * 60_000L - System.currentTimeMillis()
        if (delay <= 0) return false
        val data = androidx.work.Data.Builder().putString("title", title).putString("currency", currency).putLong("at", atMillis).putInt("id", id).build()
        manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<NewsAlertWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(data).build())
        return true
    }

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

/** Fires a heads-up notification shortly before a calendar news event. */
class NewsAlertWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title").orEmpty()
        val currency = inputData.getString("currency").orEmpty()
        val at = inputData.getLong("at", 0L)
        val id = inputData.getInt("id", 0)
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(ReminderScheduler.NEWS_CHANNEL, "هشدار اخبار اقتصادی", NotificationManager.IMPORTANCE_HIGH),
        )
        fun link(code: Int, host: String) = PendingIntent.getActivity(
            applicationContext, code,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data = android.net.Uri.parse("tradix://$host")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val time = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val n = NotificationCompat.Builder(applicationContext, ReminderScheduler.NEWS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("خبر $currency ساعت $time")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\nقبل از خبر پوزیشن تازه باز نکن یا ریسک را کم کن."))
            .setColor(0xFFD4AF37.toInt())
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(link(1300 + id % 100, "calendar"))
            .addAction(0, "ماشین حساب ریسک", link(1301, "calculator"))
            .setAutoCancel(true)
            .build()
        manager.notify(9000 + id % 1000, n)
        return Result.success()
    }
}

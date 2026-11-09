package com.tradenex.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.tradenex.app.MainActivity
import com.tradenex.app.R
import java.util.concurrent.atomic.AtomicInteger

object NexNotify {
    const val CHANNEL_GENERAL = "nex_general"
    const val CHANNEL_REMINDERS = "nex_reminders"
    const val CHANNEL_REPORTS = "nex_reports"
    private val ids = AtomicInteger((System.currentTimeMillis() % 100000).toInt())

    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_GENERAL, "اعلان‌های ژورنال", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "ثبت و مرور ژورنال" },
            NotificationChannel(CHANNEL_REMINDERS, "یادآوری‌ها", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "یادآوری‌های قابل تنظیم ژورنال" },
            NotificationChannel(CHANNEL_REPORTS, "گزارش‌ها", NotificationManager.IMPORTANCE_LOW).apply { description = "گزارش‌های دوره‌ای" }
        ))
    }

    fun tradeLogged(context: Context, symbol: String) =
        show(context, NotifyCategory.JOURNAL, "ثبت ژورنال انجام شد", "یادداشت معامله ${symbol.ifBlank { "جدید" }} به ژورنالت اضافه شد.", "trades",
            listOf("مشاهده ژورنال" to "OPEN"))

    fun journalReminder(context: Context) =
        show(context, NotifyCategory.REMINDER, "مرور روزانه ژورنال", "اگر امروز فعالیتی داشتی، ثبت یادداشت‌ها و مرور روند روزت رو فراموش نکن.", "trades",
            listOf("انجام شد" to "DONE", "۳۰ دقیقه بعد" to "SNOOZE"))

    fun weeklyReport(context: Context) =
        show(context, NotifyCategory.REPORT, "گزارش هفتگی آماده است", "گزارش ثبت‌ها، نظم ژورنال و روند عملکردت آماده مرور است.", "insights",
            listOf("مشاهده گزارش" to "OPEN"))

    fun sessionReminder(context: Context, sessionName: String) =
        show(context, NotifyCategory.SESSION, "یادآوری سشن", "سشن $sessionName طبق ساعت انتخابی تو شروع شده است. وضعیت و یادداشت‌ها را بررسی کن.", "sessions",
            listOf("مشاهده سشن‌ها" to "OPEN"))

    fun sessionState(context: Context, sessionName: String, isOpen: Boolean) {
        val title = if (isOpen) "سشن $sessionName باز شد" else "سشن $sessionName بسته شد"
        val body = if (isOpen) "وضعیت سشن‌ها و یادداشت‌های مرتبط را در ژورنال مرور کن." else "زمان سشن $sessionName به پایان رسید؛ اگر لازم است یادداشت جلسه را تکمیل کن."
        show(context, NotifyCategory.SESSION, title, body, "sessions", listOf("مشاهده سشن‌ها" to "OPEN"))
    }

    fun show(context: Context, category: NotifyCategory, title: String, body: String, route: String,
             actions: List<Pair<String, String>> = emptyList()) {
        val prefs = context.getSharedPreferences("nex_notify_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled_global", true) || !prefs.getBoolean("enabled_${category.key}", true)) return
        val id = ids.incrementAndGet()
        NotificationInbox.add(context, InboxNotification(id, category, title, body, System.currentTimeMillis(), route))
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val channel = when (category) {
            NotifyCategory.REMINDER -> CHANNEL_REMINDERS
            NotifyCategory.REPORT -> CHANNEL_REPORTS
            else -> CHANNEL_GENERAL
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nex_route", route); putExtra("nex_notification_id", id)
        }
        val contentPending = PendingIntent.getActivity(context, id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body)).setContentIntent(contentPending)
            .setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
        actions.take(2).forEachIndexed { index, action ->
            val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                putExtra("notification_id", id); putExtra("action", action.second); putExtra("route", route)
            }
            val pending = PendingIntent.getBroadcast(context, id * 10 + index, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null, action.first, pending).build())
        }
        runCatching { context.getSystemService(NotificationManager::class.java).notify(id, builder.build()) }
    }
}

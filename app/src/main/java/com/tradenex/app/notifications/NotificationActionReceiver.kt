package com.tradenex.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tradenex.app.MainActivity

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("notification_id", -1)
        when (intent.getStringExtra("action")) {
            "DONE" -> {
                NotificationInbox.markDone(context, id)
                context.getSystemService(android.app.NotificationManager::class.java).cancel(id)
            }
            "SNOOZE" -> {
                NotificationInbox.markRead(context, id)
                context.getSystemService(android.app.NotificationManager::class.java).cancel(id)
                val later = Intent(context, NotificationActionReceiver::class.java).apply {
                    putExtra("notification_id", id); putExtra("action", "SHOW_SNOOZED")
                }
                val pending = PendingIntent.getBroadcast(context, id + 700000, later,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30 * 60 * 1000L, pending)
            }
            "SHOW_SNOOZED" -> NexNotify.journalReminder(context)
            "NEWS_REMINDER" -> {
                val reminderPrefs = context.getSharedPreferences("nex_calendar_reminders", Context.MODE_PRIVATE)
                val savedIds = reminderPrefs.getStringSet("ids", emptySet()).orEmpty().toMutableSet()
                savedIds.remove(id.toString())
                reminderPrefs.edit().putStringSet("ids", savedIds).apply()
                val title = intent.getStringExtra("title").orEmpty()
                val currency = intent.getStringExtra("currency").orEmpty()
                NexNotify.show(
                    context,
                    NotifyCategory.NEWS,
                    "یادآوری خبر اقتصادی",
                    listOf(currency, title).filter { it.isNotBlank() }.joinToString(" · "),
                    "calendar",
                    listOf("مشاهده تقویم" to "OPEN"),
                )
            }
            "OPEN" -> {
                NotificationInbox.markRead(context, id)
                val open = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("nex_route", intent.getStringExtra("route") ?: "home")
                }
                context.startActivity(open)
            }
        }
    }
}

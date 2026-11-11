package com.tradenex.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.tradenex.app.data.AppState
import com.tradenex.app.data.WeeklyPlannerStore
import com.tradenex.app.data.EducationStore
import com.tradenex.app.nav.AppNav
import com.tradenex.app.notifications.NexNotify
import com.tradenex.app.notifications.NotificationInbox
import com.tradenex.app.reminders.ReminderScheduler
import com.tradenex.app.reminders.SessionAlertScheduler
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.TradenexTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.init(this)
        NexNotify.initialize(applicationContext)
        intent?.getIntExtra("nex_notification_id", -1)?.takeIf { it > 0 }?.let { NotificationInbox.markRead(applicationContext, it) }
        enableEdgeToEdge()
        AppState.initialize(applicationContext)
        WeeklyPlannerStore.initialize(applicationContext)
        EducationStore.initialize(applicationContext)
        EducationStore.migrateLegacy(AppState.lessons.toList())
        ReminderScheduler.schedule(applicationContext, AppState.settings.reminderOn, AppState.settings.reminderHour)
        SessionAlertScheduler.schedule(applicationContext)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            TradenexTheme {
                val destination = intent?.getStringExtra("nex_route") ?: if (intent?.data?.host == "weekly_reminder") "weekly_reminder" else "home"
                AppNav(startDestination = destination)
            }
        }
    }
    override fun onPause() {
        AppState.persist()
        WeeklyPlannerStore.persist()
        ReminderScheduler.schedule(applicationContext, AppState.settings.reminderOn, AppState.settings.reminderHour)
        super.onPause()
    }
}

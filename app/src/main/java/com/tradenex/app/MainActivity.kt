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
import com.tradenex.app.reminders.ReminderScheduler
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.TradenexTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.init(this)
        enableEdgeToEdge()
        AppState.initialize(applicationContext)
        WeeklyPlannerStore.initialize(applicationContext)
        EducationStore.initialize(applicationContext)
        EducationStore.migrateLegacy(AppState.lessons.toList())
        ReminderScheduler.schedule(applicationContext, AppState.settings.reminderOn, AppState.settings.reminderHour)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            TradenexTheme {
                AppNav(startDestination = if (intent?.data?.host == "weekly_reminder") "weekly_reminder" else "home")
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

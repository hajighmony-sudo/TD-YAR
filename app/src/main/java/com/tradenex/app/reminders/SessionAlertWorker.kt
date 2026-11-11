package com.tradenex.app.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tradenex.app.core.SessionEngine
import com.tradenex.app.data.AppState
import com.tradenex.app.notifications.NexNotify
import java.time.Instant
import java.util.concurrent.TimeUnit

class SessionAlertWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return runCatching {
            AppState.initialize(applicationContext)
            val now = Instant.now()
            val states = SessionEngine.statesAt(now, SessionEngine.displayZone(AppState.settings.tzMode))
            val prefs = applicationContext.getSharedPreferences("nex_session_alert_state", Context.MODE_PRIVATE)
            val initialized = prefs.getBoolean("initialized", false)
            val editor = prefs.edit()
            states.forEach { state ->
                val key = "open_${state.def.name}"
                val previous = prefs.getBoolean(key, state.open)
                if (initialized && previous != state.open && AppState.settings.sessionAlerts) {
                    NexNotify.sessionState(applicationContext, state.def.name, state.open)
                }
                editor.putBoolean(key, state.open)
            }
            editor.putBoolean("initialized", true).apply()
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

object SessionAlertScheduler {
    private const val UNIQUE_WORK = "nex_session_state_alerts"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SessionAlertWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

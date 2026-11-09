package com.tradenex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tradenex.app.notifications.NotifyCategory
import com.tradenex.app.data.AppState
import com.tradenex.app.notifications.NexNotify
import androidx.compose.material3.Button

@Composable
fun NotificationSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nex_notify_settings", android.content.Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled_global", true)) }
    val categoryStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            NotifyCategory.entries.forEach { put(it.key, prefs.getBoolean("enabled_${it.key}", true)) }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات اعلان‌ها", style = MaterialTheme.typography.headlineSmall)
        Text("اعلان‌ها را متناسب با نیازت مدیریت کن. تنظیمات کانال‌های سیستمی اندروید هم از تنظیمات گوشی قابل تغییر است.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("فعال‌بودن اعلان‌ها", style = MaterialTheme.typography.titleMedium)
                    Text("خاموش‌کردن کلی اعلان‌های TD-YAR", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    prefs.edit().putBoolean("enabled_global", it).apply()
                })
            }
        }
        Button(onClick = { NexNotify.show(context, NotifyCategory.JOURNAL, "اعلان آزمایشی NEX", "مرکز اعلان‌ها و دکمه‌های تعاملی آماده استفاده هستند.", "notifications", listOf("بازکردن مرکز" to "OPEN")) }, modifier = Modifier.fillMaxWidth()) { Text("ارسال اعلان آزمایشی") }
        NotifyCategory.entries.forEach { category ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(category.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(checked = categoryStates[category.key] ?: true, enabled = enabled, onCheckedChange = {
                        categoryStates[category.key] = it
                        prefs.edit().putBoolean("enabled_${category.key}", it).apply()
                    })
                }
            }
        }
    }
}

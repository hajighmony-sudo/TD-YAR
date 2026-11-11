package com.tradenex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.theme.Tx
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NEX NOTIFY / CONTROL", color = Tx.Gold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text("تنظیمات اعلان‌ها", color = Tx.Text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("مرکز فرمان اعلان‌ها؛ کانال‌ها را دقیق و جداگانه کنترل کن.", color = Tx.Muted, fontSize = 11.sp)
        }
        GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold.copy(alpha = 0.35f)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اعلان‌های TRADIX", color = Tx.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(if (enabled) "سیستم اعلان فعال است" else "همه اعلان‌ها موقتاً خاموش‌اند", color = if (enabled) Tx.Green else Tx.Red, fontSize = 11.sp)
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    prefs.edit().putBoolean("enabled_global", it).apply()
                })
            }
        }
        Button(onClick = { NexNotify.show(context, NotifyCategory.JOURNAL, "اعلان آزمایشی NEX", "مرکز اعلان‌ها و دکمه‌های تعاملی آماده استفاده هستند.", "notifications", listOf("بازکردن مرکز" to "OPEN")) }, modifier = Modifier.fillMaxWidth()) { Text("ارسال اعلان آزمایشی") }
        Text("کانال‌های اختصاصی", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        NotifyCategory.entries.forEach { category ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(category.label, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (categoryStates[category.key] == true) "دریافت اعلان این بخش فعال است" else "این کانال بی‌صدا شده است", color = Tx.Muted, fontSize = 10.sp)
                    }
                    Switch(checked = categoryStates[category.key] ?: true, enabled = enabled, onCheckedChange = {
                        categoryStates[category.key] = it
                        prefs.edit().putBoolean("enabled_${category.key}", it).apply()
                        when (category) {
                            NotifyCategory.NEWS -> AppState.settings = AppState.settings.copy(newsAlerts = it)
                            NotifyCategory.SESSION -> AppState.settings = AppState.settings.copy(sessionAlerts = it)
                            NotifyCategory.REMINDER -> AppState.settings = AppState.settings.copy(reminderOn = it)
                            else -> Unit
                        }
                        AppState.persist()
                    })
                }
            }
        }
    }
}

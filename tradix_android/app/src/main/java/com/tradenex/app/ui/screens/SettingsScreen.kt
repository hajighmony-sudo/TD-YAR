package com.tradenex.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.AppSettings
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.Tx
import java.util.Locale
import kotlin.math.round

private fun update(block: (AppSettings) -> AppSettings) {
    AppState.settings = block(AppState.settings)
    AppState.persist()
}

private fun updateSelectedBalance(delta: Double) {
    val account = AppState.selectedAccount
    val nextBalance = (account.startingBalance + delta).coerceAtLeast(100.0)
    val index = AppState.accounts.indexOfFirst { it.id == account.id }
    if (index >= 0) {
        AppState.accounts[index] = account.copy(startingBalance = nextBalance)
        AppState.settings = AppState.settings.copy(startBalance = nextBalance)
        AppState.persist()
    }
}

@Composable
fun SettingsScreen(nav: NavController) {
    val s = AppState.settings
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(AppState.exportSnapshotJson()) }
                    ?: error("open failed")
            }.onSuccess {
                Toast.makeText(context, "پشتیبان ذخیره شد", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "خطا در ذخیره پشتیبان", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("read failed")
                if (!AppState.importSnapshotJson(raw)) error("invalid snapshot")
            }.onSuccess {
                Toast.makeText(context, "بازیابی موفق بود", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "فایل پشتیبان نامعتبر است", Toast.LENGTH_LONG).show()
            }
        }
    }

    Page(title = "تنظیمات", onBack = { nav.popBackStack() }) {
        Reveal(0) { ThemesGroup() }
        Reveal(0) {
            SettingGroup("ظاهر و زبان") {
                Text("زبان برنامه", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("فارسی", "English"), s.language) { v -> update { it.copy(language = v) } }
                Spacer(Modifier.height(10.dp))
                Text("شدت انیمیشن‌ها", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("کم", "عادی", "فوق‌العاده"), s.animations) { v -> update { it.copy(animations = v) } }
                SwitchRow("لرزش لمسی", "بازخورد هنگام ثبت معامله", s.haptics) { v -> update { it.copy(haptics = v) } }
                SwitchRow("پنهان‌کردن مبالغ", "اعداد پول در خانه و گزارش‌ها نقطه‌چین می‌شود", s.hideBalance) { v -> update { it.copy(hideBalance = v) } }
            }
        }
        Reveal(1) {
            SettingGroup("سشن‌ها") {
                SwitchRow("نمایش کره زمین", "خاموش = نوار زمانی", s.showGlobe) { v -> update { it.copy(showGlobe = v) } }
                Text("منطقه زمانی نمایش", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("تهران", "UTC", "ساعت دستگاه"), s.tzMode) { v -> update { it.copy(tzMode = v) } }
                SwitchRow("هشدار باز/بسته‌شدن سشن", null, s.sessionAlerts) { v -> update { it.copy(sessionAlerts = v) } }
            }
        }
        Reveal(2) {
            SettingGroup("معامله و ریسک") {
                StepRow("بالانس اولیه", AppState.selectedAccount.baseCurrency + " " + String.format(Locale.US, "%,.0f", AppState.selectedAccount.startingBalance),
                    { updateSelectedBalance(-500.0) },
                    { updateSelectedBalance(500.0) })
                StepRow("لات پیش‌فرض", String.format(Locale.US, "%.2f", s.defaultLot),
                    { update { it.copy(defaultLot = (round((it.defaultLot - 0.05) * 100) / 100).coerceAtLeast(0.01)) } },
                    { update { it.copy(defaultLot = round((it.defaultLot + 0.05) * 100) / 100) } })
                StepRow("ریسک هر معامله", String.format(Locale.US, "%.2f%%", s.riskPercent),
                    { update { it.copy(riskPercent = (it.riskPercent - 0.25).coerceAtLeast(0.25)) } },
                    { update { it.copy(riskPercent = (it.riskPercent + 0.25).coerceAtMost(10.0)) } })
                SwitchRow("شنبه و یکشنبه تعطیل", "در نقشه حرارتی و تقویم خط‌خورده می‌شود", s.weekendHoliday) { v -> update { it.copy(weekendHoliday = v) } }
            }
        }
        Reveal(3) {
            SettingGroup("یادآوری و اخبار") {
                SwitchRow("یادآور روز بدون معامله", "می‌پرسد چرا امروز معامله نکردی", s.reminderOn) { v -> update { it.copy(reminderOn = v) } }
                StepRow("ساعت یادآور", "%02d:00".format(s.reminderHour),
                    { update { it.copy(reminderHour = (it.reminderHour - 1).coerceAtLeast(0)) } },
                    { update { it.copy(reminderHour = (it.reminderHour + 1).coerceAtMost(23)) } })
                SwitchRow("هشدار اخبار مهم", null, s.newsAlerts) { v -> update { it.copy(newsAlerts = v) } }
                Text("حداقل اهمیت خبر", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("همه", "متوسط به بالا", "فقط High"), s.newsImpact) { v -> update { it.copy(newsImpact = v) } }
            }
        }
        Reveal(4) { AppIconsSection() }
        Reveal(4) {
            SettingGroup("داده و پشتیبان") {
                Text("خروجی کامل ژورنال (JSON) همه معاملات، حساب‌ها، استراتژی‌ها و تنظیمات را ذخیره می‌کند. CSV فقط از صفحه ژورنال در دسترس است.", color = Tx.Muted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton("خروجی پشتیبان JSON") { exportLauncher.launch("tradix-backup.json") }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton("بازیابی از پشتیبان") { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("موتور محاسبات", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (NativeCore.isNative) "Rust (JNI)" else "Kotlin", color = if (NativeCore.isNative) Tx.Green else Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("نسخه", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("0.1.0", color = Tx.Text, fontSize = 12.sp)
                }
                Text("${AppState.trades.size} معامله · ${AppState.accounts.count { !it.archived }} حساب فعال", color = Tx.Muted, fontSize = 11.sp)
            }
        }
        Reveal(5) {
            GoldButton("بازنشانی همه تنظیمات") { AppState.settings = AppSettings(); AppState.persist(); ThemeController.reset() }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Stepper(value, onMinus, onPlus)
    }
}

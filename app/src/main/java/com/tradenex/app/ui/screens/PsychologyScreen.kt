package com.tradenex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.AppState
import com.tradenex.app.data.PsychologyDay
import com.tradenex.app.data.TradingPsychologyStore
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.theme.Tx
import java.time.LocalDate
import java.util.Locale

@Composable
fun PsychologyScreen(nav: NavController) {
    val context = LocalContext.current
    TradingPsychologyStore.initialize(context)
    val date = LocalDate.now()
    val initial = TradingPsychologyStore.day(date)
    var confidence by remember(date) { mutableStateOf(initial.confidence.toFloat()) }
    var focus by remember(date) { mutableStateOf(initial.focus.toFloat()) }
    var stress by remember(date) { mutableStateOf(initial.stress.toFloat()) }
    var fomo by remember(date) { mutableStateOf(initial.fomo.toFloat()) }
    var revenge by remember(date) { mutableStateOf(initial.revengeUrge.toFloat()) }
    var discipline by remember(date) { mutableStateOf(initial.discipline.toFloat()) }
    var notes by remember(date) { mutableStateOf(initial.notes) }
    val emotionGroups = AppState.visibleTrades
        .groupBy { it.emotion.trim().ifBlank { "ثبت‌نشده" } }
        .map { (emotion, rows) ->
            Triple(emotion, rows.size, rows.sumOf { it.pnl }) to
                (if (rows.isEmpty()) 0 else rows.count { it.pnl > 0 } * 100 / rows.size)
        }.sortedByDescending { it.first.third }

    Page(title = "روان‌شناسی معامله‌گری", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("چک‌این ذهنی امروز", date.toString())
                    Text("این دفتر برای شناخت الگوهای رفتاری است؛ امتیازها تشخیص پزشکی یا پیش‌بینی نتیجه معامله نیستند.", color = Tx.Muted, fontSize = 10.sp)
                    PsychologySlider("اعتمادبه‌نفس", confidence, { confidence = it })
                    PsychologySlider("تمرکز", focus, { focus = it })
                    PsychologySlider("فشار و استرس", stress, { stress = it })
                    PsychologySlider("میل به ورود از جا ماندن (FOMO)", fomo, { fomo = it })
                    PsychologySlider("میل به جبران عجولانه ضرر", revenge, { revenge = it })
                    PsychologySlider("پایبندی به قوانین شخصی", discipline, { discipline = it })
                    Text("یادداشت ذهنی و اتفاقات مهم امروز", color = Tx.Muted, fontSize = 11.sp)
                    TxField(notes, { notes = it }, "چه چیزی روی تصمیم‌هایت اثر گذاشت؟")
                    TextButton(onClick = {
                        TradingPsychologyStore.save(
                            PsychologyDay(
                                date = date.toString(),
                                confidence = confidence.toInt(),
                                focus = focus.toInt(),
                                stress = stress.toInt(),
                                fomo = fomo.toInt(),
                                revengeUrge = revenge.toInt(),
                                discipline = discipline.toInt(),
                                notes = notes.trim(),
                            ),
                        )
                    }) { Text("ذخیره چک‌این امروز", color = Tx.Gold, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("نتیجه معاملات بر اساس احساس", "فقط داده‌های ثبت‌شده در ژورنال")
                    if (emotionGroups.isEmpty()) {
                        Text("برای مقایسه، هنگام ثبت معاملات احساس خود را مشخص کن.", color = Tx.Muted, fontSize = 11.sp)
                    } else {
                        emotionGroups.forEach { (group, winRate) ->
                            val (emotion, count, pnl) = group
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(emotion, color = Tx.Text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("$count معامله · نرخ برد $winRate٪", color = Tx.Muted, fontSize = 10.sp)
                                }
                                Text(String.format(Locale.US, "%+.2f", pnl), color = if (pnl >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PsychologySlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Tx.Text, fontSize = 11.sp)
            Text("${value.toInt()} از ۵", color = Tx.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 1f..5f, steps = 3)
    }
}

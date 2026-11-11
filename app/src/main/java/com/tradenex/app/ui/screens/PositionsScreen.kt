package com.tradenex.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import com.tradenex.app.data.JournalPosition
import com.tradenex.app.data.PositionFill
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.util.Locale

@Composable
fun PositionsScreen(nav: NavController) {
    val context = LocalContext.current
    var symbol by remember { mutableStateOf("XAUUSD") }
    var buy by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(AppState.settings.defaultLot.toString()) }
    var entryPrice by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var entryTarget by remember { mutableStateOf<JournalPosition?>(null) }
    var exitTarget by remember { mutableStateOf<JournalPosition?>(null) }
    var fillVolume by remember { mutableStateOf("") }
    var fillPrice by remember { mutableStateOf("") }
    var fillPnl by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<JournalPosition?>(null) }

    Page(title = "پوزیشن‌های باز و خروج جزئی", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("ثبت پوزیشن دستی", "فقط ژورنال؛ بدون ارسال سفارش")
                    TxField(symbol, { symbol = it.uppercase(Locale.ROOT).filter { ch -> ch.isLetterOrDigit() || ch in "._/-" }.take(24) }, "نماد، مثلاً XAUUSD")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TxChip("خرید (BUY)", buy, { buy = true })
                        TxChip("فروش (SELL)", !buy, { buy = false })
                    }
                    TxField(volume, { volume = it }, "حجم ورود")
                    TxField(entryPrice, { entryPrice = it }, "قیمت ورود")
                    TxField(strategy, { strategy = it }, "استراتژی (اختیاری)")
                    TxField(notes, { notes = it }, "یادداشت (اختیاری)")
                    GoldButton("ثبت پوزیشن") {
                        val v = volume.toDoubleOrNull()
                        val p = entryPrice.toDoubleOrNull()
                        if (symbol.isNotBlank() && v != null && v > 0.0 && v.isFinite() && p != null && p > 0.0 && p.isFinite()) {
                            AppState.addPosition(
                                JournalPosition(
                                    id = System.currentTimeMillis(),
                                    accountId = AppState.selectedAccountId,
                                    symbol = symbol,
                                    buy = buy,
                                    strategy = strategy.trim(),
                                    entryFills = listOf(PositionFill(System.nanoTime(), v, p)),
                                    notes = notes.trim(),
                                ),
                            )
                            symbol = "XAUUSD"; volume = AppState.settings.defaultLot.toString()
                            entryPrice = ""; strategy = ""; notes = ""
                        } else {
                            Toast.makeText(context, "نماد، حجم و قیمت ورود را درست وارد کن.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        val rows = AppState.positions.filter { it.accountId == AppState.selectedAccountId }.sortedByDescending { it.openedAtMillis }
        if (rows.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("هنوز پوزیشنی ثبت نشده است. پوزیشن‌های باز را اینجا ثبت کن و ورود یا خروج جزئی را جداگانه اضافه کن.", color = Tx.Muted, modifier = Modifier.padding(14.dp), fontSize = 11.sp)
            }
        }
        rows.forEachIndexed { index, position ->
            Reveal(index + 1) {
                GlassCard(Modifier.fillMaxWidth(), border = if (position.isClosed) Tx.Muted.copy(alpha = 0.25f) else Tx.Gold.copy(alpha = 0.45f)) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(position.symbol + if (position.buy) " · BUY" else " · SELL", color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text(position.strategy.ifBlank { "بدون استراتژی" }, color = Tx.Muted, fontSize = 10.sp)
                            }
                            Text(if (position.isClosed) "بسته‌شده" else if (position.exitedVolume > 0.0) "خروج جزئی" else "باز", color = if (position.isClosed) Tx.Muted else Tx.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        PositionStat("میانگین ورود", String.format(Locale.US, "%.5f", position.averageEntry))
                        PositionStat("حجم اولیه / خارج‌شده / باز", String.format(Locale.US, "%.2f / %.2f / %.2f", position.enteredVolume, position.exitedVolume, position.openVolume))
                        PositionStat("میانگین خروج", if (position.exitedVolume > 0) String.format(Locale.US, "%.5f", position.averageExit) else "—")
                        PositionStat("سود/زیان تحقق‌یافته", String.format(Locale.US, "%+.2f", position.realizedPnl), if (position.realizedPnl >= 0) Tx.Green else Tx.Red)
                        if (position.notes.isNotBlank()) Text(position.notes, color = Tx.Muted, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!position.isClosed) {
                                TxChip("افزودن ورود", false, {
                                    entryTarget = position
                                    fillVolume = ""
                                    fillPrice = ""
                                })
                                TxChip("ثبت خروج", false, {
                                    exitTarget = position
                                    fillVolume = ""
                                    fillPrice = ""
                                    fillPnl = ""
                                })
                            }
                            TxChip("حذف", false, { deleteTarget = position })
                        }
                    }
                }
            }
        }
    }

    entryTarget?.let { position ->
        AlertDialog(
            onDismissRequest = { entryTarget = null },
            containerColor = Tx.Dialog,
            title = { Text("افزودن ورود به ${position.symbol}", color = Tx.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxField(fillVolume, { fillVolume = it }, "حجم ورود جدید")
                    TxField(fillPrice, { fillPrice = it }, "قیمت ورود جدید")
                    Text("میانگین ورود و حجم باز به‌صورت خودکار به‌روزرسانی می‌شوند.", color = Tx.Muted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = fillVolume.toDoubleOrNull()
                    val p = fillPrice.toDoubleOrNull()
                    if (v != null && v > 0.0 && v.isFinite() && p != null && p > 0.0 && p.isFinite()) {
                        runCatching { AppState.addPositionEntry(position.id, PositionFill(System.nanoTime(), v, p)) }
                            .onFailure { Toast.makeText(context, it.message ?: "ثبت ورود ناموفق بود", Toast.LENGTH_SHORT).show() }
                        entryTarget = null
                    }
                }) { Text("ثبت ورود", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { entryTarget = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }

    exitTarget?.let { position ->
        AlertDialog(
            onDismissRequest = { exitTarget = null },
            containerColor = Tx.Dialog,
            title = { Text("ثبت خروج جزئی / کامل", color = Tx.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حجم باز فعلی: ${String.format(Locale.US, "%.2f", position.openVolume)}", color = Tx.Muted, fontSize = 11.sp)
                    TxField(fillVolume, { fillVolume = it }, "حجم خروج")
                    TxField(fillPrice, { fillPrice = it }, "قیمت خروج")
                    TxField(fillPnl, { fillPnl = it }, "سود/زیان تحقق‌یافته این خروج (خالص)")
                    Text("برای چند خروج، هر بخش را جداگانه ثبت کن. سود/زیان هر خروج را از گزارش حساب خودت وارد کن.", color = Tx.Muted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = fillVolume.toDoubleOrNull()
                    val p = fillPrice.toDoubleOrNull()
                    val pnl = fillPnl.toDoubleOrNull()
                    if (v != null && v > 0.0 && v <= position.openVolume + 1e-8 && v.isFinite() &&
                        p != null && p > 0.0 && p.isFinite() && pnl != null && pnl.isFinite()
                    ) {
                        runCatching { AppState.addPositionExit(position.id, PositionFill(System.nanoTime(), v, p, pnl)) }
                            .onSuccess { exitTarget = null }
                            .onFailure { Toast.makeText(context, it.message ?: "ثبت خروج ناموفق بود", Toast.LENGTH_SHORT).show() }
                    } else {
                        Toast.makeText(context, "حجم خروج نباید از حجم باز بیشتر باشد و همه مقادیر باید معتبر باشند.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("ثبت خروج", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { exitTarget = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }

    deleteTarget?.let { position ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Tx.Dialog,
            title = { Text("حذف پوزیشن؟", color = Tx.Text) },
            text = { Text("پوزیشن ${position.symbol} و جزئیات ورود و خروج آن حذف شود؟", color = Tx.Muted) },
            confirmButton = {
                TextButton(onClick = { AppState.deletePosition(position.id); deleteTarget = null }) { Text("حذف", color = Tx.Red) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
}

@Composable
private fun PositionStat(label: String, value: String, color: androidx.compose.ui.graphics.Color = Tx.Text) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Tx.Muted, fontSize = 10.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

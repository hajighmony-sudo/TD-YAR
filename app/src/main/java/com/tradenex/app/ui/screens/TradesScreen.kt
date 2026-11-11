package com.tradenex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Trade
import java.time.LocalDate
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx

@Composable
fun TradesScreen(nav: NavController) {
    var filter by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var range by remember { mutableStateOf("همه") }
    var editing by remember { mutableStateOf<Trade?>(null) }
    var deleting by remember { mutableStateOf<Trade?>(null) }
    var pnlText by remember { mutableStateOf("") }
    var lotText by remember { mutableStateOf("") }
    var entryText by remember { mutableStateOf("") }
    var exitText by remember { mutableStateOf("") }
    var slText by remember { mutableStateOf("") }
    var tpText by remember { mutableStateOf("") }
    var commissionText by remember { mutableStateOf("") }
    var swapText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var tradeNameText by remember { mutableStateOf("") }
    var complianceEdit by remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val msg = runCatching {
                val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                val parsed = com.tradenex.app.data.TradeCsv.parse(text, AppState.selectedAccount.id)
                val added = AppState.addTrades(parsed.trades)
                "$added معامله وارد شد" + if (parsed.skipped > 0) " (${parsed.skipped} ردیف نامعتبر یا غیرمعامله رد شد)" else ""
            }.getOrElse { "ورود CSV ممکن نشد: ${it.message}" }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.appendLine("id,trade_name,date,symbol,side,lots,net_pnl,currency,strategy,strategy_version,strategy_followed,emotion,timeframe,market_condition,planned_risk,entry,exit,stop_loss,take_profit,commission,swap,fees,notes")
                    AppState.visibleTrades.sortedBy { it.epochDay }.forEach { t ->
                        val values = listOf(
                            t.id.toString(), t.tradeName, t.epochDay.toString(), t.symbol, if (t.buy) "BUY" else "SELL",
                            t.lot.toString(), t.pnl.toString(), AppState.selectedAccount.baseCurrency,
                            t.strategy, t.strategyVersion.toString(), t.strategyFollowed?.let { if (it) "YES" else "NO" }.orEmpty(), t.emotion,
                            t.timeframe, t.marketCondition, t.plannedRisk?.toString().orEmpty(),
                            t.entryPrice?.toString().orEmpty(), t.exitPrice?.toString().orEmpty(),
                            t.stopLossPrice?.toString().orEmpty(), t.takeProfitPrice?.toString().orEmpty(),
                            t.commission.toString(), t.swap.toString(), t.otherFees.toString(), t.notes,
                        )
                        writer.appendLine(values.joinToString(",") { csvCell(it) })
                    }
                } ?: error("Unable to open the selected file.")
            }.onSuccess {
                Toast.makeText(context, "CSV export completed", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "CSV export failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    val all = AppState.visibleTrades.sortedByDescending { it.epochDay }
    val today = AppState.today()
    val rangeDays = when (range) { "۷ روز" -> 7L; "۳۰ روز" -> 30L; "۹۰ روز" -> 90L; else -> null }
    val shown = all.filter { trade ->
        val matchesFilter = when (filter) {
            "BUY" -> trade.buy
            "SELL" -> !trade.buy
            "WIN" -> trade.pnl > 0
            "LOSS" -> trade.pnl < 0
            else -> true
        }
        val matchesSearch = search.isBlank() ||
            trade.symbol.contains(search, ignoreCase = true) ||
            trade.tradeName.contains(search, ignoreCase = true) ||
            trade.strategy.contains(search, ignoreCase = true) ||
            trade.notes.contains(search, ignoreCase = true)
        val matchesRange = rangeDays == null || trade.epochDay >= today - rangeDays + 1
        matchesFilter && matchesSearch && matchesRange
    }
    val shownNet = shown.sumOf { it.pnl }
    val wins = shown.count { it.pnl > 0 }
    val losses = shown.count { it.pnl < 0 }

    Page(title = "ژورنال معاملات", onBack = { nav.popBackStack() }) {
        SectionHero("TRADE JOURNAL / HISTORY", "مرکز فرمان معاملات", "معاملات را جست‌وجو، فیلتر، ویرایش و به‌صورت CSV وارد یا صادر کن.", Tx.Green)
        TxField(search, { search = it }, "جست‌وجوی نماد، نام، استراتژی یا یادداشت")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("همه", "۷ روز", "۳۰ روز", "۹۰ روز").forEach { TxChip(it, range == it, { range = it }) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ALL", "BUY", "SELL", "WIN", "LOSS").forEach {
                TxChip(it, filter == it, { filter = it })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${shown.size} معامله · $wins برد · $losses باخت", color = Tx.Muted, fontSize = 11.sp)
                Text(Analytics.money(shownNet), color = if (shownNet >= 0) Tx.Green else Tx.Red, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { importLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel", "*/*")) }) { Text("ورود CSV", color = Tx.Gold) }
                TextButton(onClick = { exportLauncher.launch("tradix-trades.csv") }) { Text("خروجی CSV", color = Tx.Gold) }
                GoldButton("ثبت معامله جدید") { nav.navigate("add") }
            }
        }
        if (shown.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    if (all.isEmpty()) "هنوز معامله‌ای برای این حساب ثبت نشده است."
                    else "با این جست‌وجو یا فیلتر، معامله‌ای پیدا نشد.",
                    color = Tx.Muted,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }
        shown.forEach { trade ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(trade.tradeName.ifBlank { trade.symbol }, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text(if (trade.buy) "  BUY" else "  SELL", color = if (trade.buy) Tx.Green else Tx.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("لات ${trade.lot} · ${trade.strategy.ifBlank { "بدون استراتژی" }} · ${jalaliText(LocalDate.ofEpochDay(trade.epochDay))}", color = Tx.Muted, fontSize = 10.sp)
                        Text("پایبندی: " + when (trade.strategyFollowed) { true -> "طبق پلن"; false -> "خارج از پلن"; null -> "ثبت نشده" }, color = when (trade.strategyFollowed) { true -> Tx.Green; false -> Tx.Red; null -> Tx.Muted }, fontSize = 10.sp)
                        if (trade.notes.isNotBlank()) Text(trade.notes, color = Tx.Muted, fontSize = 11.sp, maxLines = 2)
                    }
                    Text(Analytics.money(trade.pnl), color = if (trade.pnl >= 0) Tx.Green else Tx.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Edit, "ویرایش معامله", tint = Tx.Gold, modifier = Modifier.clickable {
                            editing = trade
                            pnlText = trade.pnl.toString()
                            lotText = trade.lot.toString()
                            entryText = trade.entryPrice?.toString().orEmpty()
                            exitText = trade.exitPrice?.toString().orEmpty()
                            slText = trade.stopLossPrice?.toString().orEmpty()
                            tpText = trade.takeProfitPrice?.toString().orEmpty()
                            commissionText = trade.commission.toString()
                            swapText = trade.swap.toString()
                            notesText = trade.notes
                            tradeNameText = trade.tradeName
                            complianceEdit = trade.strategyFollowed
                        })
                        Icon(Icons.Rounded.Delete, "حذف معامله", tint = Tx.Muted, modifier = Modifier.clickable { deleting = trade })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    editing?.let { original ->
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = Tx.Dialog,
            title = { Text("ویرایش ${original.symbol}", color = Tx.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    TxField(pnlText, { pnlText = it }, "Net P&L (${AppState.selectedAccount.baseCurrency})")
                    TxField(lotText, { lotText = it }, "Lots")
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TxField(entryText, { entryText = it }, "Entry", Modifier.weight(1f))
                        TxField(exitText, { exitText = it }, "Exit", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TxField(slText, { slText = it }, "Stop Loss", Modifier.weight(1f))
                        TxField(tpText, { tpText = it }, "Take Profit", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TxField(commissionText, { commissionText = it }, "Commission", Modifier.weight(1f))
                        TxField(swapText, { swapText = it }, "Swap", Modifier.weight(1f))
                    }
                    TxField(tradeNameText, { tradeNameText = it }, "Trade name / setup")
                    TxField(notesText, { notesText = it }, "Notes")
                    Text("پایبندی به استراتژی", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TxChip("طبق پلن", complianceEdit == true, { complianceEdit = true })
                        TxChip("خارج از پلن", complianceEdit == false, { complianceEdit = false })
                        TxChip("ثبت نشده", complianceEdit == null, { complianceEdit = null })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pnl = pnlText.toDoubleOrNull()
                    val lot = lotText.toDoubleOrNull()
                    if (pnl != null && pnl.isFinite() && lot != null && lot > 0.0 && lot.isFinite()) {
                        AppState.updateTrade(original.id) {
                            it.copy(
                                pnl = pnl,
                                lot = lot,
                                entryPrice = entryText.toDoubleOrNull(),
                                exitPrice = exitText.toDoubleOrNull(),
                                stopLossPrice = slText.toDoubleOrNull(),
                                takeProfitPrice = tpText.toDoubleOrNull(),
                                commission = commissionText.toDoubleOrNull() ?: 0.0,
                                swap = swapText.toDoubleOrNull() ?: 0.0,
                                notes = notesText.trim(),
                                tradeName = tradeNameText.trim(),
                                strategyFollowed = complianceEdit,
                            )
                        }
                        editing = null
                    }
                }) { Text("ذخیره تغییرات", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }

    deleting?.let { trade ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Tx.Dialog,
            title = { Text("حذف معامله؟", color = Tx.Text) },
            text = { Text("معامله ${trade.symbol} از ژورنال این حساب حذف شود؟", color = Tx.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    AppState.deleteTrade(trade.id)
                    deleting = null
                }) { Text("حذف", color = Tx.Red) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
}

private fun csvCell(value: String): String =
    "\"" + value.replace("\"", "\"\"") + "\""

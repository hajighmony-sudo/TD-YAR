package com.tradenex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Archive
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
import com.tradenex.app.data.AppState
import com.tradenex.app.data.TradingAccount
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.util.Locale

@Composable
fun AccountsScreen(nav: NavController) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var broker by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var type by remember { mutableStateOf("REAL") }
    var balance by remember { mutableStateOf("10000") }

    Page(title = "حساب‌های معاملاتی", onBack = { nav.popBackStack() }) {
        AppState.accounts.filterNot { it.archived }.forEach { account ->
            val selected = account.id == AppState.selectedAccountId
            val accountTrades = AppState.trades.filter { it.accountId == account.id }
            val accountNet = accountTrades.sumOf { it.pnl }
            val accountWins = accountTrades.count { it.pnl > 0 }
            val accountWinRate = if (accountTrades.isEmpty()) 0 else accountWins * 100 / accountTrades.size
            GlassCard(Modifier.fillMaxWidth(), border = if (selected) Tx.Gold else Color.White.copy(alpha = 0.12f)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalance, null, tint = if (selected) Tx.Gold else Tx.Muted)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(account.name, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text(listOf(account.broker, account.type, account.baseCurrency).filter(String::isNotBlank).joinToString(" · "), color = Tx.Muted, fontSize = 11.sp)
                        }
                        if (selected) Text("فعال", color = Tx.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("بالانس اولیه", color = Tx.Muted, fontSize = 10.sp)
                            Text("${account.baseCurrency} " + String.format(Locale.US, "%,.2f", account.startingBalance), color = Tx.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!selected) {
                            TextButton(onClick = {
                                AppState.selectedAccountId = account.id
                                AppState.settings = AppState.settings.copy(startBalance = account.startingBalance)
                                AppState.persist()
                            }) { Text("انتخاب حساب", color = Tx.Gold) }
                        }
                        TextButton(onClick = {
                            val index = AppState.accounts.indexOfFirst { it.id == account.id }
                            if (index >= 0 && AppState.accounts.count { !it.archived } > 1) {
                                AppState.accounts[index] = account.copy(archived = true)
                                if (AppState.selectedAccountId == account.id) {
                                    AppState.selectedAccountId = AppState.accounts.first { !it.archived }.id
                                }
                                AppState.persist()
                            }
                        }) {
                            Icon(Icons.Rounded.Archive, "بایگانی حساب", tint = Tx.Muted)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("معاملات ثبت‌شده", color = Tx.Muted, fontSize = 10.sp)
                            Text(accountTrades.size.toString(), color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("نرخ برد", color = Tx.Muted, fontSize = 10.sp)
                            Text("$accountWinRate٪", color = Tx.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("سود/زیان ثبت‌شده", color = Tx.Muted, fontSize = 10.sp)
                            Text(
                                if (AppState.settings.hideBalance) "•••" else String.format(Locale.US, "%+,.2f", accountNet),
                                color = if (accountNet >= 0) Tx.Green else Tx.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        GoldButton("افزودن حساب جدید") {
            name = ""; broker = ""; currency = "USD"; type = "REAL"; balance = "10000"
            creating = true
        }
        Spacer(Modifier.height(12.dp))
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            containerColor = Tx.Dialog,
            title = { Text("حساب جدید", color = Tx.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxField(name, { name = it }, "نام حساب")
                    TxField(broker, { broker = it }, "بروکر (اختیاری)")
                    Text("نوع حساب", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("REAL", "DEMO", "PROP").forEach { TxChip(it, type == it, { type = it }) }
                    }
                    Text("ارز پایه حساب", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("USD", "EUR", "GBP", "JPY").forEach { TxChip(it, currency == it, { currency = it }) }
                    }
                    TxField(balance, { balance = it }, "بالانس اولیه")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = balance.trim().toDoubleOrNull()
                    if (name.isNotBlank() && amount != null && amount > 0.0 && amount.isFinite()) {
                        val id = "account-${System.currentTimeMillis()}"
                        AppState.accounts.add(TradingAccount(id, name.trim(), broker.trim(), type, currency, amount))
                        AppState.selectedAccountId = id
                        AppState.settings = AppState.settings.copy(startBalance = amount)
                        AppState.persist()
                        creating = false
                    }
                }) { Text("ایجاد حساب", color = Tx.Gold) }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text("انصراف", color = Tx.Muted) }
            },
        )
    }
}

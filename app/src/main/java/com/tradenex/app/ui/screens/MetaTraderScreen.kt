package com.tradenex.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.AppState
import com.tradenex.app.data.MetaTrader
import com.tradenex.app.data.MtBridge
import com.tradenex.app.data.TradeCsv
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.PulseDot
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom

private fun prefs(c: Context) = c.getSharedPreferences("tradix_mt", Context.MODE_PRIVATE)

private fun newToken(): String {
    val b = ByteArray(9).also { SecureRandom().nextBytes(it) }
    return b.joinToString("") { "%02x".format(it) }
}

/** MetaTrader hub: direct Wi-Fi bridge (Expert Advisor), statement import and MetaApi cloud. */
@Composable
fun MetaTraderScreen(nav: NavController) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val account = AppState.selectedAccount
    val importedCount = AppState.trades.count { "metatrader" in it.tags }

    Page(title = "مرکز اتصال متاتریدر", onBack = { nav.popBackStack() }) {
        SectionHero("MT4 / MT5 / READ ONLY", "اتصال امن به متاتریدر", "تاریخچه و اطلاعات حساب را فقط‌خواندنی وارد کن؛ بدون دریافت رمز اصلی یا اجرای معامله.", Tx.Blue)
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("TRADIX CONNECT  /  MT4 • MT5", color = Tx.Gold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Text("اتصال هوشمند به ژورنال", color = Tx.Text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text("همگام‌سازی تاریخچه؛ بدون اجرای معامله", color = Tx.Muted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            PulseDot(if (tab == 0 && MtBridge.running) Tx.Green else Tx.Gold)
                            Text(if (tab == 0 && MtBridge.running) "LIVE" else "READY", color = if (tab == 0 && MtBridge.running) Tx.Green else Tx.Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("ورودی‌های MT", importedCount.toString(), Tx.Gold, Modifier.weight(1f), "ثبت‌شده در ژورنال")
                        StatTile("حساب مقصد", account.name, Tx.Text, Modifier.weight(1f), "حساب انتخاب‌شده")
                    }
                }
            }
        }
        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    TxChip("ورود با حساب", tab == 3, { tab = 3 })
                    TxChip("پل مستقیم", tab == 0, { tab = 0 })
                    TxChip("گزارش MT4/MT5", tab == 1, { tab = 1 })
                    TxChip("اتصال ابری", tab == 2, { tab = 2 })
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("حالت فقط‌خواندنی فعال است", color = Tx.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("فقط تاریخچه و معاملات بسته‌شده به ژورنال منتقل می‌شوند. این بخش سفارش جدید ثبت نمی‌کند و سفارش‌های موجود را تغییر نمی‌دهد.", color = Tx.Muted, fontSize = 11.sp)
                        Text("مقصد ورود: «${account.name}»  •  جلوگیری از ثبت تکراری با Ticket ID", color = Tx.Gold, fontSize = 10.sp)
                    }
                }
            }
        }
        when (tab) {
            0 -> BridgeTab(context)
            1 -> StatementTab(context)
            3 -> LoginTab(context)
            else -> MetaApiTab(context)
        }
    }
}

@Composable
private fun BridgeTab(context: Context) {
    val p = remember { prefs(context) }
    var token by remember { mutableStateOf(p.getString("bridge_token", null) ?: newToken().also { p.edit().putString("bridge_token", it).apply() }) }
    val ip = remember { MtBridge.localIp() }
    val running = MtBridge.running
    MtBridge.token = token
    DisposableEffect(Unit) { onDispose { MtBridge.stop() } }
    val saveMq5 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> copyAsset(context, "metatrader/TradixBridge.mq5", uri) }
    val saveMq4 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> copyAsset(context, "metatrader/TradixBridge.mq4", uri) }

    Reveal(1) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("LOCAL BRIDGE  •  WI-FI", color = Tx.Gold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        SectionTitle("پل مستقیم گوشی و ترمینال", if (running) "متصل" else "آماده")
                    }
                    PulseDot(if (running) Tx.Green else Tx.Muted)
                }
                Text("Expert Advisor در ترمینال دسکتاپ، تاریخچه‌ی معاملات بسته‌شده را از طریق شبکه‌ی محلی به گوشی می‌فرستد. لازم نیست رمز اصلی حساب متاتریدر را داخل این اپ وارد کنی.", color = Tx.Muted, fontSize = 11.sp)
                Text("نشانی دریافت", color = Tx.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("http://${ip ?: "<IP گوشی>"}:${MtBridge.port}/deals", color = Tx.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("کلید جفت‌سازی:  $token", color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (ip == null) Text("به Wi-Fi وصل نیستی؛ آدرس IP پیدا نشد.", color = Tx.Red, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxChip(if (running) "توقف سرور" else "شروع سرور", running, { if (running) MtBridge.stop() else MtBridge.start() })
                    TxChip("توکن جدید", false, { token = newToken(); p.edit().putString("bridge_token", token).apply() })
                }
                if (MtBridge.lastMessage.isNotBlank()) Text(MtBridge.lastMessage + "  ·  مجموع دریافت‌شده: ${MtBridge.received}", color = Tx.Green, fontSize = 12.sp)
            }
        }
    }
    Reveal(2) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("راه‌اندازی در متاتریدر", "یک‌بار")
                listOf(
                    "۱. فایل EA را ذخیره کن و در پوشه‌ی MQL5/Experts (یا MQL4/Experts) بگذار.",
                    "۲. در MetaEditor کامپایل کن.",
                    "۳. متاتریدر: Tools ← Options ← Expert Advisors ← «Allow WebRequest for listed URL» و آدرس بالا را (بدون /deals) اضافه کن.",
                    "۴. EA را روی یک چارت بکش و آدرس و توکن را در ورودی‌هایش بگذار.",
                    "۵. هر چند ثانیه معامله‌های بسته‌شده‌ی جدید به این برنامه می‌رسند.",
                ).forEach { Text(it, color = Tx.Muted, fontSize = 11.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxChip("ذخیره‌ی EA برای MT5", false, { saveMq5.launch("TradixBridge.mq5") })
                    TxChip("ذخیره‌ی EA برای MT4", false, { saveMq4.launch("TradixBridge.mq4") })
                }
            }
        }
    }
}

private fun copyAsset(context: Context, asset: String, uri: android.net.Uri?) {
    if (uri == null) return
    val msg = runCatching {
        context.assets.open(asset).use { i -> context.contentResolver.openOutputStream(uri)!!.use { o -> i.copyTo(o) } }
        "فایل ذخیره شد"
    }.getOrElse { "ذخیره ممکن نشد: ${it.message}" }
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

@Composable
private fun StatementTab(context: Context) {
    var result by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            result = runCatching {
                val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }.let { b ->
                    // MT5 reports are often UTF-16
                    if (b.size > 2 && ((b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte()) || (b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte()))) String(b, Charsets.UTF_16) else String(b, Charsets.UTF_8)
                }
                if (text.contains("<table", ignoreCase = true) || text.contains("<tr", ignoreCase = true)) {
                    val deals = MetaTrader.parseHtmlReport(text)
                    val r = MetaTrader.importDeals(deals)
                    if (deals.isEmpty()) "معامله‌ای در این گزارش پیدا نشد. از Account History ← Save as Report (یا Detailed Report) استفاده کن."
                    else "${r.added} معامله وارد شد، ${r.duplicates} تکراری"
                } else {
                    val parsed = TradeCsv.parse(text, AppState.selectedAccount.id)
                    "${AppState.addTrades(parsed.trades)} معامله از CSV وارد شد (${parsed.skipped} ردیف رد شد)"
                }
            }.getOrElse { "ورود ممکن نشد: ${it.message}" }
        }
    }
    Reveal(1) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("ورود گزارش متاتریدر", "HTML یا CSV")
                Text("MT4: تب Account History ← راست‌کلیک ← Save as Detailed Report.\nMT5: تب History ← راست‌کلیک ← Report ← HTML (یا Open XML/CSV).\nفایل را به گوشی بفرست و اینجا انتخاب کن. قالب‌های MT4 و MT5 خودکار تشخیص داده می‌شوند.", color = Tx.Muted, fontSize = 11.sp)
                GoldButton("انتخاب فایل گزارش", Modifier.fillMaxWidth()) { picker.launch(arrayOf("text/html", "text/csv", "text/*", "*/*")) }
                if (result.isNotBlank()) Text(result, color = Tx.Gold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MetaApiTab(context: Context) {
    val p = remember { prefs(context) }
    var token by remember { mutableStateOf("") }
    remember { p.edit().remove("api_token").apply(); Unit }
    var accountId by remember { mutableStateOf(p.getString("api_account", "") ?: "") }
    var region by remember { mutableStateOf(p.getString("api_region", "new-york") ?: "new-york") }
    var days by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val dayOptions = listOf(7, 30, 90, 365)

    Reveal(1) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("اتصال ابری MetaApi", "سرویس شخص ثالث")
                Text("این مسیر از توکن API و شناسه‌ی حساب MetaApi استفاده می‌کند؛ این توکن با رمز اصلی ورود به متاتریدر فرق دارد. اطلاعات ورود حساب متاتریدر را در این صفحه وارد نکن. MetaApi سرویس شخص ثالث است و ممکن است هزینه یا محدودیت داشته باشد.", color = Tx.Muted, fontSize = 11.sp)
                TxField(token, { token = it }, "توکن API MetaApi", secure = true)
                TxField(accountId, { accountId = it }, "شناسه‌ی حساب (Account ID)")
                TxField(region, { region = it }, "منطقه (مثلاً new-york یا london)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayOptions.forEachIndexed { i, d -> TxChip("$d روز", days == i, { days = i }) }
                }
                GoldButton(if (busy) "در حال دریافت…" else "دریافت معامله‌ها", Modifier.fillMaxWidth()) {
                    if (busy) return@GoldButton
                    p.edit().putString("api_account", accountId).putString("api_region", region).apply()
                    busy = true; result = ""
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val r = withContext(Dispatchers.IO) {
                            runCatching { MetaTrader.fetchMetaApi(region, token, accountId, now - dayOptions[days] * 86_400_000L, now) }
                        }
                        busy = false
                        result = r.fold(
                            { deals -> MetaTrader.importDeals(deals).let { "${it.added} معامله وارد شد، ${it.duplicates} تکراری (از ${deals.size} معامله‌ی بسته‌شده)" } },
                            { "خطا: ${it.message}" },
                        )
                    }
                }
                if (result.isNotBlank()) Text(result, color = Tx.Gold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LoginTab(context: Context) {
    val p = remember { prefs(context) }
    var apiToken by remember { mutableStateOf(p.getString("api_token", "") ?: "") }
    var login by remember { mutableStateOf(p.getString("mt_login", "") ?: "") }
    var server by remember { mutableStateOf(p.getString("mt_server", "") ?: "") }
    var password by remember { mutableStateOf("") }
    var mt5 by remember { mutableStateOf(p.getBoolean("mt_is5", true)) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Reveal(2) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("ورود با نام کاربری و رمز", "از طریق MetaApi")
                Text("شمارهٔ حساب، رمز و سرور بروکر را وارد کن؛ حساب در MetaApi ثبت و وصل می‌شود و بعد معامله‌ها همین‌جا وارد می‌شوند. متاتریدر پروتکل بازی برای اتصال مستقیم برنامه‌ی ثالث ندارد، پس این اتصال از یک سرویس ابری رد می‌شود.", color = Tx.Muted, fontSize = 11.sp)
                Text("هشدار امنیتی: فقط «رمز سرمایه‌گذار (Investor / فقط‌خواندنی)» را بگذار، نه رمز معاملاتی. رمز به MetaApi فرستاده می‌شود و روی گوشی ذخیره نمی‌شود.", color = Tx.Red, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TxChip("MT5", mt5, { mt5 = true }); TxChip("MT4", !mt5, { mt5 = false })
                }
                TxField(apiToken, { apiToken = it }, "توکن MetaApi")
                TxField(login, { login = it }, "شمارهٔ حساب (Login)")
                TxField(password, { password = it }, "رمز سرمایه‌گذار (Investor)")
                TxField(server, { server = it }, "سرور بروکر (مثلاً Broker-Live)")
                GoldButton(if (busy) "در حال اتصال…" else "اتصال حساب", Modifier.fillMaxWidth()) {
                    if (busy) return@GoldButton
                    p.edit().putString("api_token", apiToken).putString("mt_login", login).putString("mt_server", server).putBoolean("mt_is5", mt5).apply()
                    busy = true; result = ""
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                val id = MetaTrader.connectWithLogin(apiToken, "TRADIX $login", login, password, server, mt5)
                                p.edit().putString("api_account", id).apply()
                                id to MetaTrader.accountStatus(apiToken, id)
                            }
                        }
                        busy = false
                        result = r.fold(
                            { "حساب ثبت شد (${it.second}). چند دقیقه صبر کن تا وصل شود، بعد در تب «اتصال ابری» دریافت معامله‌ها را بزن." },
                            { "خطا: ${it.message}" },
                        )
                    }
                }
                if (result.isNotBlank()) Text(result, color = Tx.Gold, fontSize = 12.sp)
            }
        }
    }
}

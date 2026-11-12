package com.tradenex.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.AppState
import com.tradenex.app.data.MtBridge
import com.tradenex.app.data.TvShare
import com.tradenex.app.data.TvDiscovery
import com.tradenex.app.ui.components.QrCode
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx

/** "مشترک": show the whole journal on an Android TV (or any screen with a browser) over the local network. */
@Composable
fun TvShareScreen(nav: NavController) {
    val context: Context = LocalContext.current
    val ip = MtBridge.localIp()
    val running = TvShare.running
    // keep the served snapshot fresh while this screen is open
    LaunchedEffect(AppState.trades.size, AppState.accounts.size, AppState.selectedAccountId, running) {
        TvShare.publish(AppState.accounts, AppState.trades, AppState.selectedAccountId)
    }
    LaunchedEffect(running) { if (running) TvDiscovery.advertise(context, TvShare.port, "TRADIX-" + android.os.Build.MODEL) else TvDiscovery.unadvertise() }
    DisposableEffect(Unit) { onDispose { TvShare.stop(); TvDiscovery.stop(); TvDiscovery.unadvertise() } }

    Page(title = "مشترک", onBack = { nav.popBackStack() }) {
        SectionHero("TV SHARE / ANDROID TV", "نمایش ژورنال روی تلویزیون", "داشبورد کامل (سود خالص، منحنی سرمایه، معامله‌ها، همه حساب‌ها) روی صفحهٔ بزرگ؛ فقط‌خواندنی و فقط روی Wi-Fi خودت.", Tx.Gold)
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("روش ۱ — مرورگر تلویزیون", if (running) "فعال" else "خاموش")
                    Text("گوشی و تلویزیون باید به یک Wi-Fi وصل باشند. سرور را روشن کن، در مرورگر تلویزیون (یا Chrome داخل تلویزیون) آدرس زیر را باز کن و کد ۶ رقمی را بزن. صفحه هر ۵ ثانیه خودش به‌روز می‌شود.", color = Tx.Muted, fontSize = 11.sp)
                    Text("آدرس:  http://${ip ?: "<IP گوشی>"}:${TvShare.port}", color = Tx.Gold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("کد ورود:  ${TvShare.pin}", color = Tx.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (ip == null) Text("به Wi-Fi وصل نیستی؛ آدرس IP پیدا نشد.", color = Tx.Red, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxChip(if (running) "توقف اشتراک" else "شروع اشتراک", running, { if (running) TvShare.stop() else TvShare.start() })
                        TxChip("کد جدید", false, { if (!running) TvShare.pin = TvShare.newPin() })
                    }
                    if (running) Text("درخواست‌های دریافت‌شده: ${TvShare.requests}  ·  برای امنیت، بعد از تمام شدن اشتراک را متوقف کن (با بستن این صفحه هم خودکار متوقف می‌شود).", color = Tx.Green, fontSize = 11.sp)
                }
            }
        }
        if (running && ip != null) {
            Reveal(1) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        SectionTitle("اتصال با QR", "مثل یوتیوب")
                        QrCode("http://$ip:${TvShare.port}/?pin=${TvShare.pin}", 220.dp)
                        Text("با دوربین تلویزیون/گوشی دوم اسکن کن؛ بدون تایپ کد وارد می‌شود. یا آدرس و کد ۶ رقمی را دستی بزن.", color = Tx.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("جست‌وجوی دستگاه", if (TvDiscovery.searching) "در حال جست‌وجو…" else "آماده")
                    Text("تلویزیون‌ها و دستگاه‌های Cast روی همین Wi-Fi (یا هات‌اسپات گوشی) پیدا می‌شوند. اتصال با بلوتوث هنوز پشتیبانی نمی‌شود؛ برای نقطه‌ی اتصال، هات‌اسپات گوشی را روشن کن و تلویزیون را به آن وصل کن.", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxChip(if (TvDiscovery.searching) "توقف جست‌وجو" else "جست‌وجوی دستگاه", TvDiscovery.searching, { if (TvDiscovery.searching) TvDiscovery.stop() else TvDiscovery.start(context) })
                    }
                    if (TvDiscovery.searching && TvDiscovery.devices.isEmpty()) Text("هنوز دستگاهی پیدا نشده…", color = Tx.Muted, fontSize = 11.sp)
                    TvDiscovery.devices.forEach { d ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.name, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(if (d.tradix) "TRADIX TV · ${d.host}" else "Cast · ${d.host}", color = if (d.tradix) Tx.Green else Tx.Muted, fontSize = 11.sp)
                        }
                    }
                    if (TvDiscovery.devices.any { !it.tradix }) Text("برای دستگاه‌های Cast: «روش ۲» (کست صفحه) را بزن، یا در مرورگر آن تلویزیون آدرس بالا را باز کن. اتصال خودکار فقط با اپ اختصاصی TRADIX TV ممکن است.", color = Tx.Gold, fontSize = 10.sp)
                }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("روش ۲ — کست صفحه", "بدون تنظیم")
                    Text("اگر تلویزیونت Chromecast / Google TV دارد، صفحهٔ خود برنامه را مستقیم کست کن و هر بخش را روی تلویزیون ببین.", color = Tx.Muted, fontSize = 11.sp)
                    GoldButton("باز کردن تنظیمات کست گوشی", Modifier.fillMaxWidth()) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { android.widget.Toast.makeText(context, "این گوشی صفحه‌ی کست جداگانه ندارد؛ از پنل بالا «Smart View / Cast / Screen mirroring» را بزن.", android.widget.Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
        Reveal(2) {
            Text("چه چیزی ارسال می‌شود: حساب‌ها و معامله‌ها (تا ۱۵۰۰ مورد آخر). یادداشت‌ها، عکس‌ها، آموزش‌ها و رمزها ارسال نمی‌شوند. هر کسی در همان Wi-Fi که کد را بداند می‌تواند ببیند؛ در Wi-Fi عمومی استفاده نکن. اپ اختصاصی Android TV هنوز ساخته نشده است.", color = Tx.Muted, fontSize = 11.sp)
        }
    }
}

package com.tradenex.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.theme.Tx

private data class MenuItem(val title: String, val sub: String, val icon: ImageVector, val route: String, val color: Color)

@Composable
fun MoreScreen(nav: NavController) {
    var about by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf(AppState.settings.profileFirstName) }
    var lastName by remember { mutableStateOf(AppState.settings.profileLastName) }
    var photoUri by remember { mutableStateOf(AppState.settings.profilePhotoUri) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoUri = uri.toString()
        }
    }
    val profileBitmap = remember(photoUri) {
        if (photoUri.isBlank()) null else runCatching {
            context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
        }.getOrNull()
    }
    val items = listOf(
        MenuItem("ژورنال معاملات", "تاریخچه، ویرایش و فیلتر", Icons.Rounded.ReceiptLong, "trades", Tx.Green),
        MenuItem("برنامه هفتگی", "هدف‌ها، نظم و گزارش هفتگی", Icons.Rounded.EventNote, "weekly", Tx.Gold),
        MenuItem("حساب‌ها", "دمو، ریل و پراپ", Icons.Rounded.AccountBalance, "accounts", Tx.Blue),
        MenuItem("سشن‌ها", "کره زمین و همپوشانی", Icons.Rounded.Public, "sessions", Tx.Blue),
        MenuItem("استراتژی‌ها", "ثبت، حذف و مقایسه", Icons.Rounded.TrackChanges, "strategies", Tx.Gold),
        MenuItem("تقویم و اخبار", "معاملات + فارکس فکتوری", Icons.Rounded.CalendarMonth, "calendar", Tx.Green),
        MenuItem("ماشین حساب", "محاسبات عمومی، لات و ریسک فارکس", Icons.Rounded.Calculate, "calculator", Tx.Blue),
        MenuItem("متاترید", "اتصال مستقیم، گزارش و MetaApi", Icons.Rounded.SwapHoriz, "metatrader", Tx.Blue),
        MenuItem("بینش‌ها", "نماد، روز، ساعت، احساس، استریک", Icons.Rounded.Insights, "insights", Tx.Green),
        MenuItem("آزمایشگاه ریسک", "کِلی، مونت‌کارلو، افت سرمایه، R", Icons.Rounded.Science, "risk_lab", Tx.Red),
        MenuItem("# هشتگ‌ها", "برچسب‌گذاری معامله‌ها", Icons.Rounded.Tag, "tags", Tx.Purple),
        MenuItem("TX AI", "شخصیت معاملاتی و مربی عملکرد", Icons.Rounded.AutoAwesome, "tx_ai", Tx.Gold),
        MenuItem("روان‌شناسی", "خلق‌وخو، تمرکز، FOMO و نظم ذهنی", Icons.Rounded.Insights, "psychology", Tx.Purple),
        MenuItem("گزارش‌ها", "کندل، نقشه حرارتی", Icons.Rounded.BarChart, "perf", Tx.Gold),
        MenuItem("آموزش", "متن، عکس، ویدیو", Icons.Rounded.MenuBook, "edu", Tx.Green),
        MenuItem("تم‌ها", "Default · Gold", Icons.Rounded.Palette, "settings", Tx.Gold),
        MenuItem("تنظیمات", "همه جزئیات برنامه", Icons.Rounded.Settings, "settings", Tx.Muted),
        MenuItem("درباره", "نسخه و هسته", Icons.Rounded.Info, "about", Tx.Blue),
    )

    Page(title = "بیشتر") {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp).clickable { profileOpen = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).border(2.dp, Tx.Gold, CircleShape).background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) { if (profileBitmap != null) Image(profileBitmap, contentDescription = "تصویر پروفایل", modifier = Modifier.size(52.dp).clip(CircleShape)) else Icon(Icons.Rounded.Person, null, tint = Tx.Gold, modifier = Modifier.size(30.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "پروفایل معاملاتی من" }, color = Tx.Text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            AppState.selectedAccount.name + " · " + AppState.visibleTrades.size + " معامله",
                            color = Tx.Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val netPnl = AppState.visibleTrades.sumOf { it.pnl }
                        Text("خالص: " + Analytics.money(netPnl), color = if (netPnl >= 0) Tx.Green else Tx.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Tx.Gold, modifier = Modifier.size(30.dp))
                }
            }
        }
        val groups = listOf(
            "معاملات و حساب‌ها" to items.take(7),
            "تحلیل و پیشرفت" to items.drop(7).take(4),
            "شخصی‌سازی و برنامه" to items.drop(11),
        )
        groups.forEachIndexed { groupIndex, group ->
            Text(group.first, color = Tx.Gold, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            group.second.chunked(2).forEachIndexed { rowIndex, row ->
                Reveal(groupIndex * 4 + rowIndex + 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            MenuTile(item, Modifier.weight(1f)) { if (item.route == "about") about = true else nav.navigate(item.route) }
                        }
                        if (row.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Reveal(12) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("خلاصه فضای کاری", color = Tx.Gold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("حساب‌ها", color = Tx.Muted, fontSize = 10.sp)
                            Text(AppState.accounts.count { !it.archived }.toString(), color = Tx.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("استراتژی‌های فعال", color = Tx.Muted, fontSize = 10.sp)
                            Text(AppState.strategies.count { it.active }.toString(), color = Tx.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("هشتگ‌ها", color = Tx.Muted, fontSize = 10.sp)
                            Text(AppState.hashtags.size.toString(), color = Tx.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text(if (NativeCore.isNative) "هسته محاسباتی Rust فعال است" else "هسته محاسباتی Kotlin فعال است", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (profileOpen) {
        AlertDialog(
            onDismissRequest = { profileOpen = false },
            containerColor = Tx.Dialog,
            title = { Text("پروفایل شخصی", color = Tx.Text, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("اطلاعات فقط روی همین دستگاه ذخیره می‌شود.", color = Tx.Muted, fontSize = 10.sp)
                    TxField(firstName, { firstName = it }, "نام")
                    TxField(lastName, { lastName = it }, "نام خانوادگی")
                    Text(if (photoUri.isBlank()) "هنوز عکسی انتخاب نشده" else "تصویر پروفایل انتخاب شده است", color = Tx.Muted, fontSize = 10.sp)
                    Text("انتخاب تصویر", color = Tx.Gold, modifier = Modifier.clickable { photoPicker.launch(arrayOf("image/*")) }.padding(vertical = 6.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    AppState.settings = AppState.settings.copy(
                        profileFirstName = firstName.trim(),
                        profileLastName = lastName.trim(),
                        profilePhotoUri = photoUri,
                    )
                    AppState.persist()
                    profileOpen = false
                }) { Text("ذخیره پروفایل", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { profileOpen = false }) { Text("انصراف", color = Tx.Muted) } },
        )
    }

    if (about) {
        AlertDialog(
            onDismissRequest = { about = false },
            containerColor = Tx.Dialog,
            title = { Text("TRADIX 0.1.0", color = Tx.Text) },
            text = {
                Text(
                    "دفترچه معاملات و آموزش فارکس. این برنامه سیگنال نمی‌دهد و معامله انجام نمی‌دهد.\nهسته محاسبات: " +
                        if (NativeCore.isNative) "Rust (JNI)" else "Kotlin (کتابخانه Rust بسته‌بندی نشده)",
                    color = Tx.Muted,
                )
            },
            confirmButton = { TextButton(onClick = { about = false }) { Text("باشه", color = Tx.Gold) } },
        )
    }
}

@Composable
private fun MenuTile(m: MenuItem, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.height(104.dp).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(m.color.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(m.icon, null, tint = m.color, modifier = Modifier.size(20.dp))
            }
            Text(m.title, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(m.sub, color = Tx.Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TagsScreen(nav: NavController) {
    var input by remember { mutableStateOf("") }
    val n = AppState.visibleTrades.size

    Page(title = "# هشتگ‌ها", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            Text(
                "هر معامله را با هشتگ برچسب بزن تا ببینی کدام موقعیت‌ها سود یا ضرر می‌آورند.",
                color = Tx.Muted,
                fontSize = 12.sp,
            )
        }
        Reveal(1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxField(input, { input = it }, "هشتگ جدید (مثلاً asia_range)", Modifier.weight(1f))
                IconBox(Icons.Rounded.Add, "افزودن هشتگ") {
                    val t = input.trim().removePrefix("#").lowercase().replace(" ", "_")
                    if (t.isNotEmpty()) AppState.addHashtag("#" + t)
                    input = ""
                }
            }
        }
        AppState.hashtags
            .map { tag -> Triple(tag, AppState.visibleTrades.filter { tag in it.tags }, 0) }
            .sortedByDescending { it.second.size }
            .forEachIndexed { i, (tag, used, _) ->
                Reveal(i + 2) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tag, color = Tx.Purple, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${used.size} معامله از $n", color = Tx.Muted, fontSize = 11.sp)
                            }
                            val net = used.sumOf { it.pnl }
                            Text(Analytics.money(net), color = if (net >= 0) Tx.Green else Tx.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Rounded.Delete,
                                "حذف هشتگ",
                                tint = Tx.Muted,
                                modifier = Modifier.padding(start = 12.dp).size(22.dp).clickable { AppState.removeHashtag(tag) },
                            )
                        }
                    }
                }
            }
        Spacer(Modifier.height(12.dp))
    }
}

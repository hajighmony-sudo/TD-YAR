package com.tradenex.app.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.R
import com.tradenex.app.ui.theme.Tx

/** One launcher icon the user can pick. [id] matches the manifest alias `.icon.Alias_<id>`. */
data class AppIcon(val id: String, val title: String, @DrawableRes val preview: Int)

object AppIcons {
    const val DEFAULT_ID = "bull"

    val all = listOf(
        AppIcon("bull", "گاو طلایی (اصلی)", R.drawable.icon_img_bull),
        AppIcon("bullbear", "نبرد گاو و خرس", R.drawable.icon_img_bullbear),
        AppIcon("stone", "کندل سنگی", R.drawable.icon_img_stone),
        AppIcon("gold_t", "تی طلایی", R.drawable.icon_img_gold_t),
        AppIcon("twin_t", "تی دوقطبی", R.drawable.icon_img_twin_t),
        AppIcon("orbit_t", "مدار طلایی", R.drawable.icon_img_orbit_t),
        AppIcon("tehran_flag", "تهران و پرچم", R.drawable.icon_img_tehran_flag),
        AppIcon("tehran_gold", "تهران طلایی", R.drawable.icon_img_tehran_gold),
        AppIcon("milad", "برج میلاد", R.drawable.icon_img_milad),
        AppIcon("nex_orbit", "نکس مداری", R.drawable.icon_img_nex_orbit),
        AppIcon("nex_globe", "نکس جهانی", R.drawable.icon_img_nex_globe),
        AppIcon("nex_star", "نکس ستاره‌ای", R.drawable.icon_img_nex_star),
    )

    private fun component(context: Context, id: String) = ComponentName(context.packageName, "com.tradenex.app.icon.Alias_$id")

    /** Reads the real state from the system; only the default alias counts as enabled when nothing was changed yet. */
    fun current(context: Context): String {
        val pm = context.packageManager
        val enabled = all.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon.id))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon.id == DEFAULT_ID
                else -> false
            }
        }
        return enabled?.id ?: DEFAULT_ID
    }

    /** Enables the new alias first, then disables the others, so the launcher never loses the app. */
    fun apply(context: Context, id: String) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, id), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        all.filter { it.id != id }.forEach {
            pm.setComponentEnabledSetting(component(context, it.id), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}

/** Settings group "آیکون‌ها": preview grid, tap to switch the launcher icon. */
@Composable
fun AppIconsSection() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppIcons.current(context)) }
    val prefs = remember { context.getSharedPreferences("tradix_icons", Context.MODE_PRIVATE) }
    var hidden by remember { mutableStateOf(prefs.getStringSet("hidden", emptySet()).orEmpty().toSet()) }
    var note by remember { mutableStateOf("") }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) note = runCatching { pinCustomIcon(context, uri) }.getOrElse { "ساخت آیکون دلخواه ممکن نشد: ${it.message}" }
    }
    SettingGroup("آیکون‌ها") {
        Text(
            "آیکون برنامه را عوض کن. بسته به لانچر گوشی، تغییر آیکون روی صفحه‌ی اصلی چند ثانیه طول می‌کشد و ممکن است برنامه یک بار بسته شود.",
            color = Tx.Muted, fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.tradenex.app.ui.components.TxChip("＋ آیکون دلخواه", false, { picker.launch("image/*") })
            if (hidden.isNotEmpty()) com.tradenex.app.ui.components.TxChip("نمایش همه (${hidden.size} پنهان)", false, { hidden = emptySet(); prefs.edit().putStringSet("hidden", emptySet()).apply() })
        }
        Text("برای پنهان‌کردن یک آیکون از این فهرست، روی آن نگه دار. آیکون فعال و پیش‌فرض پنهان نمی‌شوند.", color = Tx.Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        if (note.isNotBlank()) Text(note, color = Tx.Gold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        AppIcons.all.filter { it.id !in hidden || it.id == selected || it.id == AppIcons.DEFAULT_ID }.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { icon ->
                    val on = icon.id == selected
                    val shape = RoundedCornerShape(percent = 22)
                    Column(
                        Modifier.weight(1f).pointerInput(icon.id, selected) {
                            detectTapGestures(onLongPress = {
                                if (icon.id != selected && icon.id != AppIcons.DEFAULT_ID) {
                                    hidden = hidden + icon.id
                                    prefs.edit().putStringSet("hidden", hidden).apply()
                                }
                            })
                        }.clickable {
                            if (!on) {
                                runCatching { AppIcons.apply(context, icon.id) }
                                    .onSuccess {
                                        selected = icon.id
                                        Toast.makeText(context, "آیکون «${icon.title}» فعال شد", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure { Toast.makeText(context, "تغییر آیکون ممکن نشد", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(shape)
                                .then(if (on) Modifier.border(2.dp, Tx.Gold, shape) else Modifier),
                        ) {
                            Image(painterResource(icon.preview), icon.title, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        }
                        Text(
                            icon.title, color = if (on) Tx.Gold else Tx.Text, fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                        )
                        if (on) Text("فعال", color = Tx.Green, fontSize = 10.sp)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Pins a home-screen shortcut that opens TRADIX with the chosen picture as its icon (a launcher icon can't be changed at runtime, a shortcut can). */
private fun pinCustomIcon(context: Context, uri: android.net.Uri): String {
    val sm = context.getSystemService(android.content.pm.ShortcutManager::class.java) ?: return "لانچر از میان‌بر پشتیبانی نمی‌کند."
    if (!sm.isRequestPinShortcutSupported) return "لانچر گوشی افزودن میان‌بر را پشتیبانی نمی‌کند."
    val src = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return "تصویر خوانده نشد."
    val side = minOf(src.width, src.height)
    val square = android.graphics.Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    val bmp = android.graphics.Bitmap.createScaledBitmap(square, 192, 192, true)
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return "راه‌اندازی برنامه ممکن نشد."
    val info = android.content.pm.ShortcutInfo.Builder(context, "custom_" + System.currentTimeMillis())
        .setShortLabel("TRADIX")
        .setIcon(android.graphics.drawable.Icon.createWithAdaptiveBitmap(bmp))
        .setIntent(intent.setAction(android.content.Intent.ACTION_MAIN))
        .build()
    sm.requestPinShortcut(info, null)
    return "درخواست افزودن میان‌بر به صفحه‌ی اصلی فرستاده شد؛ آن را تأیید کن. حذفش هم مثل هر میان‌بر دیگری از صفحه‌ی اصلی است."
}

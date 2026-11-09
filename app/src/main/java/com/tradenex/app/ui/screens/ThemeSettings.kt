package com.tradenex.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Palette
import com.tradenex.app.ui.theme.PanelStyle
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.ThemePresetStore
import com.tradenex.app.ui.theme.UserThemePreset
import com.tradenex.app.ui.theme.ThemeId
import com.tradenex.app.ui.theme.Tx
import com.tradenex.app.ui.theme.paletteOf
import kotlin.math.abs
import kotlin.math.sin

/** Settings > تم‌ها: pick Default or Gold, accent colour, glow strength and the Gold-only effects. */
@Composable
fun ThemesGroup() {
    val context = LocalContext.current
    var presetName by remember { mutableStateOf("") }
    var customPresets by remember { mutableStateOf(ThemePresetStore.load(context)) }
    var presetMessage by remember { mutableStateOf("") }
    val selected = ThemeController.theme
    SettingGroup("تم‌ها") {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeId.values().filter { it == ThemeId.DEFAULT || ThemeController.goldAvailable }.forEach { id ->
                ThemeCard(id, id == selected, Modifier.weight(1f)) { ThemeController.select(id) }
            }
        }
        if (selected == ThemeId.GOLD) {
            Text("حذف تم Gold", color = Tx.Red, fontSize = 11.sp, modifier = Modifier.clickable { ThemeController.removeGoldTheme() }.padding(vertical = 8.dp))
        }

        Spacer(Modifier.height(12.dp))
        SmallLabel("تم‌های شخصی")
        Text("ترکیب فعلی رنگ، درخشش و افکت‌ها را با نام دلخواه ذخیره کن. تم پیش‌فرض قابل حذف نیست؛ تم‌های شخصی را هر زمان خواستی می‌توانی پاک کنی.", color = Tx.Muted, fontSize = 10.sp)
        OutlinedTextField(value = presetName, onValueChange = { presetName = it.take(28) }, label = { Text("نام تم جدید") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TxChip("ذخیره ترکیب فعلی", false, {
            if (presetName.isBlank()) {
                presetMessage = "برای تم شخصی یک نام وارد کن."
            } else {
                val preset = UserThemePreset(
                    id = System.currentTimeMillis().toString(),
                    name = presetName.trim(),
                    themeKey = ThemeController.theme.key,
                    accent = ThemeController.accent,
                    glow = ThemeController.glow,
                    particles = ThemeController.particles,
                    shimmer = ThemeController.shimmer,
                    crown = ThemeController.crown,
                )
                customPresets = ThemePresetStore.save(context, preset)
                presetName = ""
                presetMessage = "تم ذخیره شد."
            }
        })
        if (presetMessage.isNotBlank()) Text(presetMessage, color = Tx.Gold, fontSize = 10.sp)
        customPresets.forEach { preset ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { ThemePresetStore.apply(preset) }, modifier = Modifier.weight(1f)) {
                    Text(preset.name, color = Tx.Text, textAlign = TextAlign.Start)
                }
                TextButton(onClick = { customPresets = ThemePresetStore.delete(context, preset.id) }) {
                    Text("حذف", color = Tx.Red, fontSize = 11.sp)
                }
            }
        }
        if (selected == ThemeId.DEFAULT) {
            SmallLabel("رنگ تأکید")
            ChipsRow(listOf("طلایی", "فیروزه‌ای", "بنفش"), ThemeController.accent) { ThemeController.setAccent(it) }
            Spacer(Modifier.height(8.dp))
        }
        SmallLabel("شدت درخشش")
        ChipsRow(listOf("خاموش", "ملایم", "عادی", "شدید"), ThemeController.glow) { ThemeController.setGlow(it) }
        Spacer(Modifier.height(4.dp))
        if (selected == ThemeId.GOLD) {
            SwitchRow("غبار طلایی", "ذرات شناور؛ حرکتشان را هسته Rust حساب می‌کند", ThemeController.particles) { ThemeController.setParticles(it) }
            SwitchRow("برق لبه‌ها", "نور طلایی که روی لبه کارت‌ها می‌چرخد", ThemeController.shimmer) { ThemeController.setShimmer(it) }
            SwitchRow("تاج روی لوگو", null, ThemeController.crown) { ThemeController.setCrown(it) }
        } else {
            Text(
                "غبار طلایی، برق لبه‌ها و تاج فقط در تم Gold فعال می‌شوند.",
                color = Tx.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SmallLabel(text: String) {
    Text(text, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun ThemeCard(id: ThemeId, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = paletteOf(id, if (id == ThemeId.DEFAULT) ThemeController.accent else 0)
    val border by animateColorAsState(if (selected) Tx.Gold else Color.White.copy(alpha = 0.14f), label = "themeBorder")
    val scale by animateFloatAsState(if (selected) 1f else 0.96f, tween(250), label = "themeScale")
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .scale(scale)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThemePreview(p, Modifier.fillMaxWidth().height(132.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(id.title, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            if (selected) Icon(Icons.Rounded.CheckCircle, "انتخاب‌شده", tint = Tx.Green, modifier = Modifier.size(16.dp))
        }
        Text(id.tagline, color = Tx.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

/** Mini home screen drawn with the given palette, independent of the active theme. */
@Composable
private fun ThemePreview(p: Palette, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "preview")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "phase")
    Canvas(modifier.clip(RoundedCornerShape(12.dp))) {
        val gilded = p.style == PanelStyle.GILDED
        drawRect(p.bg)
        drawRect(
            Brush.radialGradient(
                listOf(p.gold.copy(alpha = if (gilded) 0.38f else 0.22f), Color.Transparent),
                center = Offset(size.width * 0.9f, 0f),
                radius = size.width,
            ),
        )
        val pad = 8.dp.toPx()
        val cardW = size.width - 2 * pad
        val cardH = size.height * 0.32f
        val rr = CornerRadius(10.dp.toPx())
        drawRoundRect(Brush.linearGradient(listOf(p.panelTop, p.panelBottom)), Offset(pad, pad), Size(cardW, cardH), rr)
        drawRoundRect(p.gold.copy(alpha = p.borderAlpha), Offset(pad, pad), Size(cardW, cardH), rr, style = Stroke(1.dp.toPx()))
        if (gilded) {
            val x = -size.width + 2.4f * size.width * phase
            drawRoundRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, p.goldHi, Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + size.width * 0.9f, size.height),
                ),
                Offset(pad, pad), Size(cardW, cardH), rr, style = Stroke(1.6.dp.toPx()),
            )
        }
        drawRoundRect(p.green, Offset(pad * 2.2f, pad * 2.4f), Size(cardW * 0.4f, 7.dp.toPx()), CornerRadius(3.dp.toPx()))
        drawRoundRect(p.muted.copy(alpha = 0.55f), Offset(pad * 2.2f, pad * 2.4f + 12.dp.toPx()), Size(cardW * 0.25f, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawCircle(p.gold.copy(alpha = 0.9f), 9.dp.toPx(), Offset(size.width - pad * 3.2f, pad + cardH / 2f))

        val baseY = size.height * 0.80f
        val slot = (size.width - 3 * pad) / 9f
        for (i in 0 until 9) {
            val x = pad * 1.5f + slot * i + slot / 2f
            val h = (10f + 22f * abs(sin(i * 1.35f))).dp.toPx()
            val col = if (i % 3 == 1) p.red else p.green
            drawLine(col, Offset(x, baseY - h - 4.dp.toPx()), Offset(x, baseY + 3.dp.toPx()), 1.dp.toPx())
            drawRoundRect(col, Offset(x - 2.5.dp.toPx(), baseY - h), Size(5.dp.toPx(), h * 0.7f), CornerRadius(1.dp.toPx()))
        }

        val barH = 20.dp.toPx()
        val barTop = size.height - pad - barH
        drawRoundRect(Brush.verticalGradient(listOf(p.barTop, p.barBottom)), Offset(pad, barTop), Size(cardW, barH), CornerRadius(10.dp.toPx()))
        drawRoundRect(p.gold.copy(alpha = p.borderAlpha), Offset(pad, barTop), Size(cardW, barH), CornerRadius(10.dp.toPx()), style = Stroke(1.dp.toPx()))
        for (i in 0 until 4) {
            val cx = pad + cardW * (if (i < 2) 0.14f + i * 0.17f else 0.62f + (i - 2) * 0.17f)
            drawCircle(if (i == 0) p.gold else p.muted.copy(alpha = 0.6f), 2.dp.toPx(), Offset(cx, barTop + barH / 2f))
        }
        val fab = Offset(size.width / 2f, barTop)
        drawCircle(Brush.radialGradient(listOf(p.fabCore, p.fabEdge), center = fab, radius = 11.dp.toPx()), 10.dp.toPx(), fab)
        drawCircle(p.goldPale, 10.dp.toPx(), fab, style = Stroke(1.2.dp.toPx()))
        drawLine(p.fabIcon, Offset(fab.x - 4.dp.toPx(), fab.y), Offset(fab.x + 4.dp.toPx(), fab.y), 1.6.dp.toPx())
        drawLine(p.fabIcon, Offset(fab.x, fab.y - 4.dp.toPx()), Offset(fab.x, fab.y + 4.dp.toPx()), 1.6.dp.toPx())
    }
}

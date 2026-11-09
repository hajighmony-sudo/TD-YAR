package com.tradenex.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.AppState

/** The themes the user can pick in Settings > تم‌ها. */
enum class ThemeId(val key: String, val title: String, val tagline: String) {
    DEFAULT("default", "Default", "شیشه‌ای و آرام؛ تم فعلی برنامه"),
    GOLD("gold", "Gold", "مشکی عمیق، لبه‌های طلایی و درخشش زنده"),
}

/** GLASS = translucent cards (Default). GILDED = solid dark panels with gold edge, glow and shimmer (Gold). */
enum class PanelStyle { GLASS, GILDED }

/**
 * Every colour and trait a theme controls. Screens never read this directly: they use [Tx],
 * so a theme switch re-colours the whole app, including the animated in-between frames.
 */
data class Palette(
    val bg: Color,
    val gold: Color,
    val goldHi: Color,
    val goldDeep: Color,
    val goldPale: Color,
    val green: Color,
    val greenDeep: Color,
    val red: Color,
    val redDeep: Color,
    val blue: Color,
    val purple: Color,
    val text: Color,
    val muted: Color,
    val ink: Color,
    val panelTop: Color,
    val panelBottom: Color,
    val barTop: Color,
    val barBottom: Color,
    val dialog: Color,
    val fabCore: Color,
    val fabEdge: Color,
    val fabIcon: Color,
    val borderAlpha: Float,
    val glow: Float,
    val radius: Float,
    val style: PanelStyle,
) {
    internal fun colorsArgb(): IntArray = intArrayOf(
        bg.toArgb(), gold.toArgb(), goldHi.toArgb(), goldDeep.toArgb(), goldPale.toArgb(),
        green.toArgb(), greenDeep.toArgb(), red.toArgb(), redDeep.toArgb(), blue.toArgb(), purple.toArgb(),
        text.toArgb(), muted.toArgb(), ink.toArgb(), panelTop.toArgb(), panelBottom.toArgb(),
        barTop.toArgb(), barBottom.toArgb(), dialog.toArgb(), fabCore.toArgb(), fabEdge.toArgb(), fabIcon.toArgb(),
    )

    internal fun withColors(c: IntArray, borderAlpha: Float, glow: Float, radius: Float, style: PanelStyle) = Palette(
        Color(c[0]), Color(c[1]), Color(c[2]), Color(c[3]), Color(c[4]),
        Color(c[5]), Color(c[6]), Color(c[7]), Color(c[8]), Color(c[9]), Color(c[10]),
        Color(c[11]), Color(c[12]), Color(c[13]), Color(c[14]), Color(c[15]),
        Color(c[16]), Color(c[17]), Color(c[18]), Color(c[19]), Color(c[20]), Color(c[21]),
        borderAlpha, glow, radius, style,
    )
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Colours are mixed in OKLab by the Rust core (Kotlin fallback with the same maths). */
fun mixPalette(a: Palette, b: Palette, t: Float): Palette {
    if (t <= 0f) return a
    if (t >= 1f) return b
    val c = NativeCore.mixColors(a.colorsArgb(), b.colorsArgb(), t)
    return a.withColors(
        c,
        lerpF(a.borderAlpha, b.borderAlpha, t),
        lerpF(a.glow, b.glow, t),
        lerpF(a.radius, b.radius, t),
        if (t < 0.5f) a.style else b.style,
    )
}

/** The theme that already shipped with the app. [accent]: 0 gold, 1 teal, 2 violet. */
fun defaultPalette(accent: Int): Palette {
    val base = Palette(
        bg = Color(0xFF07070A),
        gold = Color(0xFFF7CF6B),
        goldHi = Color(0xFFFFD978),
        goldDeep = Color(0xFFC98E1E),
        goldPale = Color(0xFFFFE9A8),
        green = Color(0xFF2BE8A8),
        greenDeep = Color(0xFF0C7A56),
        red = Color(0xFFFF6B7A),
        redDeep = Color(0xFFB9283A),
        blue = Color(0xFF4DA3FF),
        purple = Color(0xFFB58CFF),
        text = Color(0xFFF4EFE3),
        muted = Color(0xFFB4AE9F),
        ink = Color(0xFF1A1204),
        panelTop = Color(0x17FFFFFF),
        panelBottom = Color(0x05FFFFFF),
        barTop = Color(0xEB1E1A0E),
        barBottom = Color(0xF50A0A0C),
        dialog = Color(0xFF14141A),
        fabCore = Color(0xFF3A3320),
        fabEdge = Color(0xFF0B0B0E),
        fabIcon = Color(0xFFF7CF6B),
        borderAlpha = 0.38f,
        glow = 0.25f,
        radius = 18f,
        style = PanelStyle.GLASS,
    )
    return when (accent) {
        1 -> base.copy(gold = Color(0xFF22D3C5), goldHi = Color(0xFF7BEFE4), goldDeep = Color(0xFF0E8F86), goldPale = Color(0xFFC6FBF5), fabIcon = Color(0xFF22D3C5))
        2 -> base.copy(gold = Color(0xFFB58CFF), goldHi = Color(0xFFD3BBFF), goldDeep = Color(0xFF7A4FD6), goldPale = Color(0xFFEBDDFF), fabIcon = Color(0xFFB58CFF))
        else -> base
    }
}

/** The "Gold" theme: black-gold panels, gold edge light, strong glow (the TRADENEX prototype look). */
val GoldPalette = Palette(
    bg = Color(0xFF070608),
    gold = Color(0xFFF2B53D),
    goldHi = Color(0xFFFFD884),
    goldDeep = Color(0xFFB9780F),
    goldPale = Color(0xFFFFE7A8),
    green = Color(0xFF1FD673),
    greenDeep = Color(0xFF128A4A),
    red = Color(0xFFFF3D4F),
    redDeep = Color(0xFFA3202D),
    blue = Color(0xFF2F8CFF),
    purple = Color(0xFFB36BFF),
    text = Color(0xFFF7EDD8),
    muted = Color(0xFFAB9E82),
    ink = Color(0xFF1A1004),
    panelTop = Color(0xFF1A130B),
    panelBottom = Color(0xFF0D0A07),
    barTop = Color(0xF51A130B),
    barBottom = Color(0xFA090705),
    dialog = Color(0xFF130F0A),
    fabCore = Color(0xFFFFD884),
    fabEdge = Color(0xFFA8730F),
    fabIcon = Color(0xFF1A1004),
    borderAlpha = 0.55f,
    glow = 0.45f,
    radius = 18f,
    style = PanelStyle.GILDED,
)

/** Static palette used for the little previews in Settings (does not follow the active theme). */
fun paletteOf(id: ThemeId, accent: Int = 0): Palette = when (id) {
    ThemeId.DEFAULT -> defaultPalette(accent)
    ThemeId.GOLD -> GoldPalette
}

/**
 * Colour access for the whole UI. Every getter reads the current (possibly mid-animation) palette
 * from snapshot state, so composables that use them recompose when the theme changes.
 */
object Tx {
    var palette: Palette by mutableStateOf(defaultPalette(0))
        internal set

    val Bg: Color get() = palette.bg
    val Gold: Color get() = palette.gold
    val GoldHi: Color get() = palette.goldHi
    val GoldDeep: Color get() = palette.goldDeep
    val GoldPale: Color get() = palette.goldPale
    val Green: Color get() = palette.green
    val GreenDeep: Color get() = palette.greenDeep
    val Red: Color get() = palette.red
    val RedDeep: Color get() = palette.redDeep
    val Blue: Color get() = palette.blue
    val Purple: Color get() = palette.purple
    val Text: Color get() = palette.text
    val Muted: Color get() = palette.muted
    val Ink: Color get() = palette.ink
    val PanelTop: Color get() = palette.panelTop
    val PanelBottom: Color get() = palette.panelBottom
    val BarTop: Color get() = palette.barTop
    val BarBottom: Color get() = palette.barBottom
    val Dialog: Color get() = palette.dialog
    val FabCore: Color get() = palette.fabCore
    val FabEdge: Color get() = palette.fabEdge
    val FabIcon: Color get() = palette.fabIcon

    val borderAlpha: Float get() = palette.borderAlpha
    val radius: Float get() = palette.radius
    val isGilded: Boolean get() = palette.style == PanelStyle.GILDED

    /** 0 off, 0.5 soft, 1 normal, 1.6 intense: the user's glow slider. */
    val glowLevel: Float get() = floatArrayOf(0f, 0.5f, 1f, 1.6f)[ThemeController.glow.coerceIn(0, 3)]

    /** Alpha used for glow / shadow colours. */
    val glowAlpha: Float get() = (palette.glow * glowLevel).coerceIn(0f, 0.9f)
}

/** Selected theme + effect toggles, persisted in SharedPreferences. */
object ThemeController {
    var theme: ThemeId by mutableStateOf(ThemeId.DEFAULT)
        private set
    var goldAvailable: Boolean by mutableStateOf(true)
        private set
    var accent: Int by mutableIntStateOf(0)
        private set

    /** 0 off, 1 soft, 2 normal, 3 intense. */
    var glow: Int by mutableIntStateOf(2)
        private set
    var particles: Boolean by mutableStateOf(true)
        private set
    var shimmer: Boolean by mutableStateOf(true)
        private set
    var crown: Boolean by mutableStateOf(true)
        private set

    /** Incremented on every theme change; the ripple overlay watches it. */
    var switchTick: Int by mutableIntStateOf(0)
        private set

    private var prefs: SharedPreferences? = null

    fun target(): Palette = paletteOf(theme, accent)

    /** Call once from MainActivity before setContent. Sets the palette immediately (no animation). */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("tradenex_theme", Context.MODE_PRIVATE)
        prefs = p
        goldAvailable = p.getBoolean("gold_available", true)
        theme = ThemeId.values().firstOrNull { it.key == p.getString("theme", null) && (it != ThemeId.GOLD || goldAvailable) } ?: ThemeId.DEFAULT
        accent = p.getInt("accent", 0).coerceIn(0, 2)
        glow = p.getInt("glow", 2).coerceIn(0, 3)
        particles = p.getBoolean("particles", true)
        shimmer = p.getBoolean("shimmer", true)
        crown = p.getBoolean("crown", true)
        Tx.palette = target()
    }

    private fun save() {
        prefs?.edit()
            ?.putString("theme", theme.key)
            ?.putBoolean("gold_available", goldAvailable)
            ?.putInt("accent", accent)
            ?.putInt("glow", glow)
            ?.putBoolean("particles", particles)
            ?.putBoolean("shimmer", shimmer)
            ?.putBoolean("crown", crown)
            ?.apply()
    }

    fun select(id: ThemeId) {
        if (id == theme) return
        theme = id
        switchTick++
        save()
    }

    fun removeGoldTheme() {
        if (!goldAvailable) return
        goldAvailable = false
        if (theme == ThemeId.GOLD) { theme = ThemeId.DEFAULT; switchTick++ }
        save()
    }

    @JvmName("updateAccentValue")
    fun setAccent(v: Int) {
        accent = v.coerceIn(0, 2)
        save()
    }

    @JvmName("updateGlowValue")
    fun setGlow(v: Int) {
        glow = v.coerceIn(0, 3)
        save()
    }

    @JvmName("updateParticlesValue")
    fun setParticles(v: Boolean) {
        particles = v
        save()
    }

    @JvmName("updateShimmerValue")
    fun setShimmer(v: Boolean) {
        shimmer = v
        save()
    }

    @JvmName("updateCrownValue")
    fun setCrown(v: Boolean) {
        crown = v
        save()
    }

    fun reset() {
        theme = ThemeId.DEFAULT
        accent = 0
        glow = 2
        particles = true
        shimmer = true
        crown = true
        switchTick++
        save()
    }
}

/** Forces RTL (Persian first) and animates the palette whenever the selected theme or accent changes. */
@Composable
fun TradenexTheme(content: @Composable () -> Unit) {
    val target = ThemeController.target()
    val instant = AppState.settings.animations == 0
    LaunchedEffect(target) {
        val start = Tx.palette
        if (start == target) return@LaunchedEffect
        if (instant) {
            Tx.palette = target
        } else {
            animate(0f, 1f, animationSpec = tween(700, easing = FastOutSlowInEasing)) { v, _ ->
                Tx.palette = mixPalette(start, target, v)
            }
            Tx.palette = target
        }
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Tx.Gold,
            onPrimary = Tx.Ink,
            background = Tx.Bg,
            surface = Tx.Bg,
            onBackground = Tx.Text,
            onSurface = Tx.Text,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}

package com.tradenex.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.Tx

/** Animated aurora background shared by every screen (+ gold dust and the shared shimmer clock in Gold). */
@Composable
fun TxBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "aurora")
    val highMotion = AppState.settings.animations >= 2
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = if (highMotion) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    val shimmer: State<Float> = t.animateFloat(
        initialValue = 0f,
        targetValue = if (highMotion) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(4800, easing = LinearEasing)),
        label = "shimmer",
    )
    val boost = if (Tx.isGilded) 1.45f else 1f
    val candleGlow by androidx.compose.animation.animateColorAsState(CandleStyle.resolve(), tween(600), label = "candleGlow")
    Box(Modifier.fillMaxSize().background(Tx.Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            // soft glow in the colour of today's candle (green / red / gold) behind the bottom bar
            drawRect(
                Brush.radialGradient(
                    listOf(candleGlow.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * (0.2f + 0.4f * a), size.height * 1.02f),
                    radius = size.width * 0.95f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Tx.Gold.copy(alpha = (0.22f * boost).coerceAtMost(0.6f)), Color.Transparent),
                    center = Offset(size.width * (0.95f - 0.35f * a), size.height * 0.04f),
                    radius = size.width * 0.9f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Tx.Green.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * (0.05f + 0.3f * a), size.height * 0.45f),
                    radius = size.width * 0.8f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Tx.Red.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width, size.height * (0.95f - 0.25f * a)),
                    radius = size.width * 0.8f,
                ),
            )
        }
        if (Tx.isGilded && ThemeController.particles && AppState.settings.animations >= 2) {
            AmbientDust(Modifier.fillMaxSize(), count = if (AppState.settings.animations == 2) 80 else 48)
        }
        CompositionLocalProvider(LocalShimmerPhase provides shimmer) {
            content()
        }
        ThemeRipple()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    border: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Tx.radius.dp)
    val glow = Tx.Gold.copy(alpha = Tx.glowAlpha)
    val cardBrush = if (Tx.isGilded) {
        Brush.linearGradient(listOf(Tx.PanelTop, Tx.PanelBottom))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.025f), Color.White.copy(alpha = 0.008f)))
    }
    Box(
        modifier
            // No Modifier.shadow here: on translucent surfaces the shadow shows THROUGH the card as a grey inner rectangle.
            .clip(shape)
            .background(cardBrush)
            .border(1.dp, if (border != Color.Transparent) border else glow.copy(alpha = (Tx.glowAlpha * 0.9f).coerceIn(0.06f, 0.30f)), shape)
            .then(if (AppState.settings.animations >= 2) Modifier.shimmerBorder(shape) else Modifier),
        content = content,
    )
}

@Composable
fun TxHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) {
            IconBox(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", onBack)
        }
        Text(title, color = Tx.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun IconBox(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Tx.Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = Tx.Gold)
    }
}

@Composable
fun TxChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Tx.Gold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f), label = "chipbg")
    val bd by animateColorAsState(if (selected) Tx.Gold else Color.White.copy(alpha = 0.16f), label = "chipbd")
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, bd, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Tx.GoldPale else Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "btn")
    val glow by t.animateFloat(
        initialValue = 0.25f,
        targetValue = if (AppState.settings.animations >= 2) 0.6f else 0.25f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "glow",
    )
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(14.dp, shape, ambientColor = Tx.Gold.copy(alpha = glow), spotColor = Tx.Gold.copy(alpha = glow))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Tx.GoldHi, Tx.GoldDeep)))
            .border(1.dp, Tx.GoldPale, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Tx.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier, sub: String? = null) {
    GlassCard(modifier.height(68.dp)) {
        Column(Modifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = Tx.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, color = Tx.Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Tx.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dot")
    val s by t.animateFloat(
        initialValue = 0.8f,
        targetValue = if (AppState.settings.animations >= 2) 2.2f else 0.8f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "s",
    )
    Box(modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(8.dp).scale(s).clip(CircleShape).background(color.copy(alpha = 0.35f)))
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun TxBottomBar(current: String, onNavigate: (String) -> Unit) {
    CandleNavBar(current, onNavigate)
}

/** Shared single-line text input used by settings and forms. */
@Composable
fun TxField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

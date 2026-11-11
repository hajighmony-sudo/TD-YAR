package com.tradenex.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.SessionEngine
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.TxHeader
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.theme.Tx
import kotlinx.coroutines.delay

/** Minute of the UTC day, refreshed every 15 seconds. */
@Composable
fun rememberUtcMinute(): State<Int> = produceState(initialValue = SessionEngine.currentUtcMinute()) {
    while (true) {
        delay(15_000)
        value = SessionEngine.currentUtcMinute()
    }
}

/** Standard screen: header + scrolling column. */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TxHeader(title, onBack, trailing)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tx.Gold.copy(alpha = 0.28f)))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}


/** Shared TRADIX identity panel used across feature screens. */
@Composable
fun SectionHero(eyebrow: String, title: String, subtitle: String, accent: Color = Tx.Gold) {
    GlassCard(Modifier.fillMaxWidth(), border = accent.copy(alpha = 0.34f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.width(4.dp).height(54.dp)
                    .background(accent, RoundedCornerShape(4.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(eyebrow.uppercase(), color = accent, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                Text(title, color = Tx.Text, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                Text(subtitle, color = Tx.Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Text("TX", color = accent.copy(alpha = 0.22f), fontSize = 25.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
        }
    }
}

/** Staggered fade + slide entrance. Shows immediately when animations are set to "low". */
@Composable
fun Reveal(index: Int, content: @Composable () -> Unit) {
    val level = AppState.settings.animations
    val instant = level == 0
    val duration = when (level) { 0 -> 0; 1 -> 150; else -> 420 }
    var shown by remember { mutableStateOf(instant) }
    LaunchedEffect(level, index) {
        if (!instant) {
            delay(index * if (level == 1) 18L else 70L)
            shown = true
        } else {
            shown = true
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(duration)) + slideInVertically(tween(duration)) { it / 8 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
fun TxField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier, secure: Boolean = false) {
    val shape = RoundedCornerShape(13.dp)
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(color = Tx.Text, fontSize = 14.sp),
        cursorBrush = SolidColor(Tx.Gold),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color.White.copy(alpha = 0.04f), shape)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(hint, color = Tx.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            }
        },
    )
}

@Composable
fun RowLabel(label: String, value: String, valueColor: Color = Tx.Text) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp)
    }
}

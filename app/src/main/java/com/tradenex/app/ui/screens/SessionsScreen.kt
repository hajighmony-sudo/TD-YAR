package com.tradenex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.OverlapInfo
import com.tradenex.app.core.SessionEngine
import com.tradenex.app.core.SessionState
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.SessionTimeline
import com.tradenex.app.ui.components.SpinningGlobe
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.time.Instant

@Composable
fun SessionsScreen(nav: NavController) {
    val settings = AppState.settings
    val utc by rememberUtcMinute()
    val nowInstant = Instant.now()
    val displayZone = SessionEngine.displayZone(settings.tzMode)
    val states = SessionEngine.statesAt(nowInstant, displayZone)
    var mask = 0
    states.forEachIndexed { i, s -> if (s.open) mask = mask or (1 shl i) }
    val localNow = nowInstant.atZone(displayZone)
    val nowLocal = localNow.hour * 60 + localNow.minute
    val zoneName = displayZone.id
    val overlaps = remember(utc / 5) { SessionEngine.overlapsAt(nowInstant) }

    Page(
        title = "سشن‌های بازار",
        onBack = { nav.popBackStack() },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TxChip("کره زمین", settings.showGlobe, { AppState.settings = AppState.settings.copy(showGlobe = true) })
                TxChip("نوار زمانی", !settings.showGlobe, { AppState.settings = AppState.settings.copy(showGlobe = false) })
            }
        },
    ) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                if (settings.showGlobe) {
                    SpinningGlobe(mask, utc, Modifier.fillMaxWidth(), settings.animations)
                } else {
                    Column(Modifier.padding(16.dp)) {
                        SessionTimeline(states, nowLocal, Modifier.fillMaxWidth().height(130.dp))
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("00", "06", "12", "18", "24").forEach { Text(it, color = Tx.Muted, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    "ساعت‌ها به وقت $zoneName · اکنون ${SessionEngine.clock(nowLocal)}",
                    color = Tx.Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Text("منطقه زمانی نمایش", color = Tx.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TxChip("محلی", settings.tzMode == 2, {
                        AppState.settings = AppState.settings.copy(tzMode = 2)
                        AppState.persist()
                    })
                    TxChip("تهران", settings.tzMode == 0, {
                        AppState.settings = AppState.settings.copy(tzMode = 0)
                        AppState.persist()
                    })
                    TxChip("UTC", settings.tzMode == 1, {
                        AppState.settings = AppState.settings.copy(tzMode = 1)
                        AppState.persist()
                    })
                }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    SectionTitle("وضعیت زنده")
                    states.forEach { SessionRow(it) }
                }
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Venn(Modifier.width(120.dp).height(92.dp), overlaps[2].minutes / 60)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("نقشه همپوشانی", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        overlaps.forEach { OverlapRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: SessionState) {
    val end = s.localStart + s.length
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(s.def.color))
        Text(s.def.name, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp).padding(start = 8.dp))
        Text("${SessionEngine.clock(s.localStart)} – ${SessionEngine.clock(end)}", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            (if (s.open) "تا بسته‌شدن " else "تا بازشدن ") + SessionEngine.duration(s.minutesToChange),
            color = Tx.Muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        val c = if (s.open) Tx.Green else Tx.Red
        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
            Text(if (s.open) "باز" else "بسته", color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OverlapRow(o: OverlapInfo) {
    val p by animateFloatAsState(o.percent / 100f, tween(900), label = "ov")
    Column {
        Row {
            Text("${o.a} + ${o.b}", color = Tx.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${o.percent}%" + if (o.activeNow) " · فعال" else "", color = if (o.activeNow) Tx.Green else Tx.Gold, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))) {
            Box(
                Modifier
                    .fillMaxWidth(p)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(Tx.Gold, Tx.Red))),
            )
        }
    }
}

@Composable
private fun Venn(modifier: Modifier, hours: Int) {
    Canvas(modifier) {
        val r = size.height * 0.40f
        val cy = size.height / 2f
        val c1 = Offset(size.width * 0.36f, cy)
        val c2 = Offset(size.width * 0.64f, cy)
        drawCircle(Tx.Gold.copy(alpha = 0.35f), r, c1)
        drawCircle(Tx.Red.copy(alpha = 0.30f), r, c2)
        val clip = Path().apply { addOval(Rect(c1, r)) }
        clipPath(clip) { drawCircle(Color.White.copy(alpha = 0.5f), r, c2) }
        drawCircle(Tx.Gold, r, c1, style = Stroke(1.5.dp.toPx()))
        drawCircle(Tx.Red, r, c2, style = Stroke(1.5.dp.toPx()))
    }
}

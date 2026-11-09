package com.tradenex.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.ui.theme.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private class City(val name: String, val lat: Double, val lon: Double, val color: Color, val session: Int)

private val cities: List<City>
    get() = listOf(
        City("Sydney", -33.9, 151.2, Tx.Blue, 0),
        City("Tokyo", 35.7, 139.7, Tx.Purple, 1),
        City("London", 51.5, -0.1, Tx.Gold, 2),
        City("New York", 40.7, -74.0, Tx.Red, 3),
    )

private const val TEHRAN_LAT = 35.7
private const val TEHRAN_LON = 51.4
private val sessionNames = listOf("سیدنی", "توکیو", "لندن", "نیویورک")

/** Countries that light up with each trading session (ISO 3166-1 alpha-3). */
private val sessionCountries: Map<String, Int> = buildMap {
    listOf("AUS", "NZL", "PNG", "FJI").forEach { put(it, 0) }
    listOf("JPN", "KOR", "PRK", "CHN", "TWN", "MYS", "IDN", "PHL", "THA", "VNM", "IND", "MNG", "KAZ").forEach { put(it, 1) }
    listOf("GBR", "IRL", "FRA", "DEU", "CHE", "ITA", "ESP", "PRT", "NLD", "BEL", "AUT", "SWE", "NOR", "DNK", "FIN", "POL", "CZE", "GRC", "TUR", "ARE", "SAU").forEach { put(it, 2) }
    listOf("USA", "CAN", "MEX", "BRA", "ARG", "CHL", "COL").forEach { put(it, 3) }
}

private fun unit(lat: Double, lon: Double): DoubleArray {
    val la = lat * PI / 180
    val lo = lon * PI / 180
    return doubleArrayOf(cos(la) * cos(lo), cos(la) * sin(lo), sin(la))
}

/**
 * Interactive globe with real country borders.
 * - drag to rotate; after ~2.5 s without touch it spins on its own again
 * - countries of an open trading session glow in the session's colour (Sydney blue, Tokyo purple, London gold, New York red)
 * - when two sessions overlap, animated orbits connect their countries
 * - tap a country to see its name and session
 *
 * @param openMask bit i = session i is open (0 Sydney, 1 Tokyo, 2 London, 3 New York)
 * @param utcMinute minute of the UTC day, used to place the sun (night-side shading)
 * @param speed 0 = slow, 1 = normal, 2 = fast (auto-rotation)
 */
@Composable
fun SpinningGlobe(openMask: Int, utcMinute: Int, modifier: Modifier = Modifier, speed: Int = 1) {
    val context = LocalContext.current
    val countries by produceState(emptyList<Country>()) {
        value = withContext(Dispatchers.Default) { runCatching { WorldGeo.load(context) }.getOrDefault(emptyList()) }
    }
    val t = rememberInfiniteTransition(label = "globe")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "pulse")
    val dash by t.animateFloat(0f, 40f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "dash")

    var lon0 by remember { mutableFloatStateOf(20f) }
    var tilt by remember { mutableFloatStateOf(0.35f) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var picked by remember { mutableStateOf<Country?>(null) }
    val degPerSec = when (speed) { 0 -> 4f; 2 -> 16f; else -> 8f }

    LaunchedEffect(speed) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
                    if (System.currentTimeMillis() - lastTouch > 2500) {
                        lon0 += degPerSec * dt
                        tilt += (0.35f - tilt) * min(1f, dt * 0.8f)
                    }
                }
                last = now
            }
        }
    }

    val sun = unit(-15.5, (12.0 - utcMinute / 60.0) * 15.0)

    Box(modifier.aspectRatio(1f)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { lastTouch = System.currentTimeMillis() },
                        onDragEnd = { lastTouch = System.currentTimeMillis() },
                        onDragCancel = { lastTouch = System.currentTimeMillis() },
                    ) { change, drag ->
                        change.consume()
                        lastTouch = System.currentTimeMillis()
                        val r = size.minDimension * 0.40f
                        lon0 -= drag.x / r * (180f / PI.toFloat())
                        tilt = (tilt + drag.y / r * 0.9f).coerceIn(-1.15f, 1.15f)
                    }
                }
                .pointerInput(countries) {
                    detectTapGestures { pos ->
                        lastTouch = System.currentTimeMillis()
                        val r = size.minDimension * 0.40f
                        val x = (pos.x - size.width / 2f) / r
                        val y = (size.height / 2f - pos.y) / r
                        val rr = x * x + y * y
                        if (rr > 1f) { picked = null; return@detectTapGestures }
                        val z = sqrt(1f - rr)
                        val ct = cos(tilt); val st = sin(tilt)
                        val y0 = y * ct + z * st
                        val z0 = -y * st + z * ct
                        val cl = cos(Math.toRadians(lon0.toDouble())).toFloat(); val sl = sin(Math.toRadians(lon0.toDouble())).toFloat()
                        val wx = z0 * cl - x * sl
                        val wy = x * cl + z0 * sl
                        val lat = Math.toDegrees(asin(y0.toDouble().coerceIn(-1.0, 1.0)))
                        val lon = Math.toDegrees(atan2(wy.toDouble(), wx.toDouble()))
                        picked = countries.firstOrNull { WorldGeo.contains(it, lon, lat) }
                    }
                },
        ) {
            val c = center
            val r = size.minDimension * 0.40f
            val gilded = Tx.isGilded
            val cityList = cities
            val ct = cos(tilt.toDouble()).toFloat(); val st = sin(tilt.toDouble()).toFloat()
            val l0 = Math.toRadians(lon0.toDouble())
            val cl = cos(l0).toFloat(); val sl = sin(l0).toFloat()

            // world vector -> view vector (x right, y up, z towards viewer)
            fun rotated(v: DoubleArray, k: Double = 1.0): DoubleArray {
                val x1 = v[1] * cl - v[0] * sl
                val z0 = v[0] * cl + v[1] * sl
                val y0 = v[2]
                return doubleArrayOf(x1 * k, (y0 * ct - z0 * st) * k, (y0 * st + z0 * ct) * k)
            }
            fun screen(p: DoubleArray): Offset = Offset(c.x + (p[0] * r).toFloat(), c.y - (p[1] * r).toFloat())

            // orbit ring behind the globe + atmosphere
            rotate(-18f, c) {
                drawOval(Tx.Gold.copy(alpha = 0.30f), Offset(c.x - r * 1.28f, c.y - r * 0.34f), Size(r * 2.56f, r * 0.68f), style = Stroke(1.2.dp.toPx()))
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.Transparent, (if (gilded) Tx.Gold else Tx.Blue).copy(alpha = if (gilded) 0.5f else 0.45f), Color.Transparent),
                    center = c, radius = r * 1.22f,
                ),
                radius = r * 1.22f, center = c,
            )
            drawCircle(
                Brush.radialGradient(
                    if (gilded) listOf(Color(0xFF2B1F0D), Color(0xFF120D07), Color(0xFF050403)) else listOf(Color(0xFF1D5FBF), Color(0xFF0C2C66), Color(0xFF040C22)),
                    center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.5f,
                ),
                radius = r, center = c,
            )

            // graticule
            for (latDeg in listOf(-60, -30, 0, 30, 60)) {
                var prev: Offset? = null
                var lonDeg = -180
                while (lonDeg <= 180) {
                    val p = rotated(unit(latDeg.toDouble(), lonDeg.toDouble()))
                    val o = if (p[2] > 0) screen(p) else null
                    if (prev != null && o != null) drawLine((if (gilded) Tx.Gold else Color(0xFF7FB8FF)).copy(alpha = if (gilded) 0.10f else 0.16f), prev, o, 0.6.dp.toPx())
                    prev = o
                    lonDeg += 6
                }
            }

            // countries: filled + bordered; session countries glow in the session colour
            val fill = Path()
            val line = Path()
            val landBase = if (gilded) Tx.GoldHi else Color(0xFF7FE3D0)
            for (country in countries) {
                val centroidZ = (country.cz * st + (country.cx * cl + country.cy * sl) * ct)
                if (centroidZ < -country.sinRadius - 0.02f) continue
                val session = sessionCountries[country.iso]
                val open = session != null && (openMask shr session) and 1 == 1
                val isIran = country.iso == "IRN"
                val sessionColor = session?.let { cityList[it].color }
                val day = max(0.0, (country.cx * sun[0] + country.cy * sun[1] + country.cz * sun[2])).toFloat()
                val isPicked = picked?.iso == country.iso

                fill.reset(); line.reset()
                for (ring in country.xyz) {
                    var started = false
                    var lineOn = false
                    val n = ring.size / 3
                    var firstX = 0f; var firstY = 0f
                    for (i in 0 until n) {
                        val x = ring[i * 3]; val y = ring[i * 3 + 1]; val z = ring[i * 3 + 2]
                        val vx = y * cl - x * sl
                        val z0 = x * cl + y * sl
                        var vy = z * ct - z0 * st
                        val vzz = z * st + z0 * ct
                        var px = vx
                        if (vzz < 0f) { // keep back-side points on the rim so the filled shape stays closed
                            val len = sqrt(px * px + vy * vy).let { if (it < 1e-4f) 1e-4f else it }
                            px /= len; vy /= len
                        }
                        val sx = c.x + px * r
                        val sy = c.y - vy * r
                        if (!started) { fill.moveTo(sx, sy); firstX = sx; firstY = sy; started = true } else fill.lineTo(sx, sy)
                        if (vzz >= 0f) {
                            if (!lineOn) { line.moveTo(sx, sy); lineOn = true } else line.lineTo(sx, sy)
                        } else lineOn = false
                    }
                    if (started) {
                        fill.close()
                        if (lineOn) line.lineTo(firstX, firstY)
                    }
                }
                val fillColor = when {
                    open -> sessionColor!!.copy(alpha = (0.50f + 0.20f * pulse) * (0.75f + 0.25f * day))
                    session != null -> sessionColor!!.copy(alpha = 0.16f)
                    isIran -> Tx.Green.copy(alpha = 0.30f)
                    else -> landBase.copy(alpha = 0.10f + 0.10f * day)
                }
                drawPath(fill, fillColor)
                if (open) {
                    drawPath(line, sessionColor!!.copy(alpha = 0.16f + 0.14f * pulse), style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                val borderColor = when {
                    isPicked -> Color.White
                    open -> sessionColor!!.copy(alpha = 0.95f)
                    session != null -> sessionColor!!.copy(alpha = 0.55f)
                    isIran -> Tx.Green.copy(alpha = 0.95f)
                    else -> landBase.copy(alpha = 0.55f)
                }
                drawPath(line, borderColor, style = Stroke((if (isPicked) 2.2f else if (open) 1.3f else 0.8f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // orbits between overlapping sessions: every lit country is tied to the other session's anchor city
            fun arc(a: DoubleArray, b: DoubleArray, color: Color, width: Float, travel: Float) {
                val path = Path()
                var started = false
                var dot: Offset? = null
                for (s in 0..28) {
                    val f = s / 28.0
                    val m = doubleArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)
                    val len = sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2])
                    if (len < 1e-6) continue
                    val p = rotated(doubleArrayOf(m[0] / len, m[1] / len, m[2] / len), 1.0 + 0.16 * sin(PI * f))
                    if (p[2] > 0) {
                        val o = screen(p)
                        if (!started) { path.moveTo(o.x, o.y); started = true } else path.lineTo(o.x, o.y)
                        if (kotlin.math.abs(f - travel) < 0.02) dot = o
                    } else started = false
                }
                drawPath(path, color, style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), dash)))
                dot?.let { drawCircle(Color.White.copy(alpha = 0.9f), 2.6.dp.toPx(), it) }
            }
            val overlaps = listOf(0 to 1, 1 to 2, 2 to 3)
            for ((a, b) in overlaps) {
                if ((openMask shr a) and 1 == 0 || (openMask shr b) and 1 == 0) continue
                val ca = cityList[a]; val cb = cityList[b]
                val va = unit(ca.lat, ca.lon); val vb = unit(cb.lat, cb.lon)
                arc(va, vb, Color.White.copy(alpha = 0.85f), 1.8.dp.toPx(), pulse)
                for (country in countries) {
                    val s = sessionCountries[country.iso] ?: continue
                    val v = doubleArrayOf(country.cx.toDouble(), country.cy.toDouble(), country.cz.toDouble())
                    if (s == a) arc(v, vb, ca.color.copy(alpha = 0.55f), 0.9.dp.toPx(), pulse)
                    if (s == b) arc(v, va, cb.color.copy(alpha = 0.55f), 0.9.dp.toPx(), 1f - pulse)
                }
            }

            // session anchor cities
            for (city in cityList) {
                val p = rotated(unit(city.lat, city.lon))
                if (p[2] <= 0) continue
                val o = screen(p)
                val open = (openMask shr city.session) and 1 == 1
                if (open) {
                    drawCircle(city.color.copy(alpha = 0.55f * (1f - pulse)), (6f + 18f * pulse).dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
                    drawCircle(city.color.copy(alpha = 0.25f), 14.dp.toPx(), o)
                }
                drawCircle(Color.White, (if (open) 5.5f else 3.8f).dp.toPx(), o)
                drawCircle(city.color, (if (open) 4f else 2.8f).dp.toPx(), o)
            }
            // you are here (Tehran)
            val th = rotated(unit(TEHRAN_LAT, TEHRAN_LON))
            if (th[2] > 0) {
                val o = screen(th)
                drawCircle(Tx.Green.copy(alpha = 0.5f * (1f - pulse)), (4f + 12f * pulse).dp.toPx(), o, style = Stroke(1.2.dp.toPx()))
                drawCircle(Tx.Green, 3.2.dp.toPx(), o)
            }

            // rim light and a satellite on the orbit
            drawCircle((if (gilded) Tx.GoldHi else Tx.Blue).copy(alpha = 0.55f), r, c, style = Stroke(1.5.dp.toPx()))
            val ang = (lon0 * 2f) * PI / 180.0
            rotate(-18f, c) {
                drawCircle(Tx.Gold, 3.5.dp.toPx(), Offset(c.x + (cos(ang) * r * 1.28).toFloat(), c.y + (sin(ang) * r * 0.34).toFloat()))
            }
        }
        picked?.let { country ->
            val s = sessionCountries[country.iso]
            val label = country.name + if (s != null) " · سشن ${sessionNames[s]}" + (if ((openMask shr s) and 1 == 1) " (باز)" else " (بسته)") else ""
            Text(
                label, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            )
        }
    }
}

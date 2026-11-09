package com.tradenex.app.ui.components

import android.content.Context
import java.io.DataInputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** One country: rings as unit vectors (x,y,z triples) and as lon/lat pairs (for tap hit-tests). */
class Country(
    val iso: String,
    val name: String,
    val xyz: List<FloatArray>,
    val lonLat: List<FloatArray>,
    val cx: Float, val cy: Float, val cz: Float,
    /** sin of the angular radius of the country's bounding cap, used for cheap horizon culling. */
    val sinRadius: Float,
)

/**
 * Real country borders from assets/world_borders.bin (public-domain Natural-Earth-derived data, simplified).
 * Binary: u16 count; per country { iso[3], u8 nameLen, name, u16 rings; per ring { u16 pts; pts x (i16 lon*100, i16 lat*100) } }.
 */
object WorldGeo {
    @Volatile private var cached: List<Country>? = null

    fun load(context: Context): List<Country> {
        cached?.let { return it }
        val out = ArrayList<Country>(180)
        DataInputStream(context.applicationContext.assets.open("world_borders.bin").buffered()).use { s ->
            val n = s.readUnsignedShort()
            repeat(n) {
                val isoBytes = ByteArray(3).also { s.readFully(it) }
                val nameBytes = ByteArray(s.readUnsignedByte()).also { s.readFully(it) }
                val rings = s.readUnsignedShort()
                val xyz = ArrayList<FloatArray>(rings)
                val ll = ArrayList<FloatArray>(rings)
                var sx = 0.0; var sy = 0.0; var sz = 0.0
                repeat(rings) {
                    val pts = s.readUnsignedShort()
                    val v = FloatArray(pts * 3)
                    val l = FloatArray(pts * 2)
                    for (i in 0 until pts) {
                        val lon = s.readShort() / 100.0
                        val lat = s.readShort() / 100.0
                        val la = lat * PI / 180; val lo = lon * PI / 180
                        val x = cos(la) * cos(lo); val y = cos(la) * sin(lo); val z = sin(la)
                        v[i * 3] = x.toFloat(); v[i * 3 + 1] = y.toFloat(); v[i * 3 + 2] = z.toFloat()
                        l[i * 2] = lon.toFloat(); l[i * 2 + 1] = lat.toFloat()
                        sx += x; sy += y; sz += z
                    }
                    xyz.add(v); ll.add(l)
                }
                val len = sqrt(sx * sx + sy * sy + sz * sz).let { if (it < 1e-9) 1.0 else it }
                val cx = (sx / len).toFloat(); val cy = (sy / len).toFloat(); val cz = (sz / len).toFloat()
                var minDot = 1f
                xyz.forEach { v ->
                    for (i in 0 until v.size / 3) {
                        val d = v[i * 3] * cx + v[i * 3 + 1] * cy + v[i * 3 + 2] * cz
                        if (d < minDot) minDot = d
                    }
                }
                val sinR = sqrt((1f - minDot * minDot).coerceAtLeast(0f))
                out.add(Country(String(isoBytes), String(nameBytes), xyz, ll, cx, cy, cz, if (minDot < 0f) 1f else sinR))
            }
        }
        cached = out
        return out
    }

    /** Point-in-polygon on lon/lat (rings that straddle the antimeridian are shifted into 0..360). */
    fun contains(c: Country, lon: Double, lat: Double): Boolean {
        for (ring in c.lonLat) {
            var minLon = Float.MAX_VALUE; var maxLon = -Float.MAX_VALUE
            for (i in 0 until ring.size / 2) { minLon = minOf(minLon, ring[i * 2]); maxLon = maxOf(maxLon, ring[i * 2]) }
            val wraps = maxLon - minLon > 180f
            fun fx(x: Float) = if (wraps && x < 0f) x + 360.0 else x.toDouble()
            val px = if (wraps && lon < 0) lon + 360.0 else lon
            var inside = false
            val n = ring.size / 2
            var j = n - 1
            for (i in 0 until n) {
                val xi = fx(ring[i * 2]); val yi = ring[i * 2 + 1].toDouble()
                val xj = fx(ring[j * 2]); val yj = ring[j * 2 + 1].toDouble()
                if ((yi > lat) != (yj > lat) && px < (xj - xi) * (lat - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
            if (inside) return true
        }
        return false
    }
}

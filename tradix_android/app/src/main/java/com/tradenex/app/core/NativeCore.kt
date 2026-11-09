package com.tradenex.app.core

/** [net], [winRate] (0..1), [profitFactor], [maxDrawdown] (<= 0). */
data class TradeStats(
    val net: Double,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
)

/** Extended deterministic metrics from realized P&L only. */
data class ExtendedTradeStats(
    val net: Double,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val expectancy: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val recoveryFactor: Double,
    val largestWinShare: Double,
    val largestLossShare: Double,
    val sampleSize: Int,
)

/**
 * Bridge to the Rust library `libtradenex_core.so` (see /rust).
 * When the library is not bundled the identical Kotlin fallback is used,
 * so the app always works; [isNative] tells which engine is active.
 */
object NativeCore {
    private val loaded: Boolean = try {
        System.loadLibrary("tradenex_core")
        true
    } catch (_: Throwable) {
        false
    }

    val isNative: Boolean get() = loaded

    private external fun nSessionMask(minuteUtc: Int): Int
    private external fun nOverlapMinutes(a: Int, b: Int): Int
    private external fun nMinutesToChange(minuteUtc: Int, session: Int): Int
    private external fun nStats(pnls: DoubleArray): DoubleArray
    private external fun nPerformanceMetrics(pnls: DoubleArray): DoubleArray
    private external fun nMixColors(a: IntArray, b: IntArray, t: Float): IntArray
    private external fun nParticlesInit(state: FloatArray, seed: Int)
    private external fun nParticlesStep(state: FloatArray, dt: Float, t: Float, seed: Int)

    /** Floats per particle: x, y, vx, vy, phase, size, alpha. */
    const val PARTICLE_STRIDE = 7

    /** Mixes two packed-ARGB palettes in OKLab (used for the animated theme switch). */
    fun mixColors(a: IntArray, b: IntArray, t: Float): IntArray =
        if (loaded) nMixColors(a, b, t) else Fallback.mixColors(a, b, t)

    fun particlesInit(state: FloatArray, seed: Int) {
        if (loaded) nParticlesInit(state, seed) else Fallback.particlesInit(state, seed)
    }

    fun particlesStep(state: FloatArray, dt: Float, t: Float, seed: Int) {
        if (loaded) nParticlesStep(state, dt, t, seed) else Fallback.particlesStep(state, dt, t, seed)
    }

    fun sessionMask(minuteUtc: Int): Int =
        if (loaded) nSessionMask(minuteUtc) else Fallback.sessionMask(minuteUtc)

    fun overlapMinutes(a: Int, b: Int): Int =
        if (loaded) nOverlapMinutes(a, b) else Fallback.overlapMinutes(a, b)

    fun minutesToChange(minuteUtc: Int, session: Int): Int =
        if (loaded) nMinutesToChange(minuteUtc, session) else Fallback.minutesToChange(minuteUtc, session)

    fun stats(pnls: DoubleArray): TradeStats {
        val r = if (loaded) nStats(pnls) else Fallback.stats(pnls)
        return TradeStats(r[0], r[1], r[2], r[3])
    }

    fun performanceMetrics(pnls: DoubleArray): ExtendedTradeStats {
        val values = if (loaded) nPerformanceMetrics(pnls) else Fallback.performanceMetrics(pnls)
        return ExtendedTradeStats(
            net = values[0],
            winRate = values[1],
            profitFactor = values[2],
            maxDrawdown = values[3],
            expectancy = values[4],
            averageWin = values[5],
            averageLoss = values[6],
            recoveryFactor = values[7],
            largestWinShare = values[8],
            largestLossShare = values[9],
            sampleSize = pnls.count { it.isFinite() },
        )
    }
}

/** Same maths as rust/src/lib.rs. */
internal object Fallback {
    // start, end in UTC minutes: Sydney, Tokyo, London, New York
    private val sessions = arrayOf(intArrayOf(1320, 420), intArrayOf(0, 540), intArrayOf(480, 1020), intArrayOf(780, 1320))

    private fun isOpen(minute: Int, start: Int, end: Int): Boolean =
        if (start < end) minute in start until end else (minute >= start || minute < end)

    fun sessionMask(minuteUtc: Int): Int {
        val m = ((minuteUtc % 1440) + 1440) % 1440
        var mask = 0
        for (i in sessions.indices) if (isOpen(m, sessions[i][0], sessions[i][1])) mask = mask or (1 shl i)
        return mask
    }

    fun overlapMinutes(a: Int, b: Int): Int {
        if (a !in 0..3 || b !in 0..3) return 0
        var n = 0
        for (m in 0 until 1440) {
            if (isOpen(m, sessions[a][0], sessions[a][1]) && isOpen(m, sessions[b][0], sessions[b][1])) n++
        }
        return n
    }

    fun minutesToChange(minuteUtc: Int, session: Int): Int {
        if (session !in 0..3) return 0
        val now = ((minuteUtc % 1440) + 1440) % 1440
        val open = isOpen(now, sessions[session][0], sessions[session][1])
        for (step in 1..1440) {
            if (isOpen((now + step) % 1440, sessions[session][0], sessions[session][1]) != open) return step
        }
        return 0
    }

    fun performanceMetrics(pnls: DoubleArray): DoubleArray {
        val values = pnls.filter { it.isFinite() }
        if (values.isEmpty()) return DoubleArray(10)
        val net = values.sum()
        val wins = values.filter { it > 0.0 }
        val losses = values.filter { it < 0.0 }
        val grossWin = wins.sum()
        val grossLoss = losses.sumOf { -it }
        var equity = 0.0
        var peak = 0.0
        var maxDrawdown = 0.0
        values.forEach { value ->
            equity += value
            peak = maxOf(peak, equity)
            maxDrawdown = minOf(maxDrawdown, equity - peak)
        }
        return doubleArrayOf(
            net,
            wins.size.toDouble() / values.size,
            if (grossLoss > 0.0) grossWin / grossLoss else if (grossWin > 0.0) 99.0 else 0.0,
            maxDrawdown,
            net / values.size,
            if (wins.isEmpty()) 0.0 else grossWin / wins.size,
            if (losses.isEmpty()) 0.0 else grossLoss / losses.size,
            if (maxDrawdown < 0.0) net / -maxDrawdown else 0.0,
            if (grossWin > 0.0) (wins.maxOrNull() ?: 0.0) / grossWin else 0.0,
            if (grossLoss > 0.0) (losses.minOrNull()?.let { -it } ?: 0.0) / grossLoss else 0.0,
        )
    }

    fun stats(pnls: DoubleArray): DoubleArray {
        if (pnls.isEmpty()) return doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        var net = 0.0
        var gain = 0.0
        var loss = 0.0
        var wins = 0
        var equity = 0.0
        var peak = 0.0
        var dd = 0.0
        for (p in pnls) {
            net += p
            if (p > 0) {
                gain += p
                wins++
            } else {
                loss += -p
            }
            equity += p
            if (equity > peak) peak = equity
            if (equity - peak < dd) dd = equity - peak
        }
        val pf = if (loss > 0) gain / loss else if (gain > 0) 99.0 else 0.0
        return doubleArrayOf(net, wins.toDouble() / pnls.size, pf, dd)
    }

    // ---------- colour mixing (same maths as rust/src/fx.rs) ----------

    private fun toLin(c: Float): Float = if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

    private fun fromLin(v: Float): Float {
        val c = v.coerceIn(0f, 1f)
        return if (c <= 0.0031308f) c * 12.92f else (1.055 * Math.pow(c.toDouble(), 1.0 / 2.4) - 0.055).toFloat()
    }

    private fun cbrt(x: Float): Float = Math.cbrt(x.toDouble()).toFloat()

    private fun mixOne(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(c: Int, sh: Int) = ((c ushr sh) and 0xFF) / 255f
        fun lab(c: Int): FloatArray {
            val r = toLin(ch(c, 16)); val g = toLin(ch(c, 8)); val bl = toLin(ch(c, 0))
            val l = cbrt(0.41222147f * r + 0.53633255f * g + 0.051445995f * bl)
            val m = cbrt(0.2119035f * r + 0.6806995f * g + 0.10739696f * bl)
            val s = cbrt(0.08830246f * r + 0.28171885f * g + 0.6299787f * bl)
            return floatArrayOf(
                0.21045426f * l + 0.7936178f * m - 0.004072047f * s,
                1.9779985f * l - 2.4285922f * m + 0.4505937f * s,
                0.025904037f * l + 0.78277177f * m - 0.80867577f * s,
            )
        }
        val la = lab(a); val lb = lab(b)
        val wa = (1f - k) * ch(a, 24)
        val wb = k * ch(b, 24)
        val kk = if (wa + wb > 1e-6f) wb / (wa + wb) else k
        val L = la[0] + (lb[0] - la[0]) * kk
        val A = la[1] + (lb[1] - la[1]) * kk
        val B = la[2] + (lb[2] - la[2]) * kk
        val l0 = L + 0.39633778f * A + 0.21580376f * B
        val m0 = L - 0.105561346f * A - 0.06385417f * B
        val s0 = L - 0.08948418f * A - 1.2914855f * B
        val l = l0 * l0 * l0; val m = m0 * m0 * m0; val s = s0 * s0 * s0
        val r = fromLin(4.0767417f * l - 3.3077116f * m + 0.23096994f * s)
        val g = fromLin(-1.268438f * l + 2.6097574f * m - 0.3413194f * s)
        val bl = fromLin(-0.0041960863f * l - 0.7034186f * m + 1.7076147f * s)
        val al = ch(a, 24) + (ch(b, 24) - ch(a, 24)) * k
        fun q(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (q(al) shl 24) or (q(r) shl 16) or (q(g) shl 8) or q(bl)
    }

    fun mixColors(a: IntArray, b: IntArray, t: Float): IntArray {
        val n = minOf(a.size, b.size)
        return IntArray(n) { mixOne(a[it], b[it], t) }
    }

    // ---------- gold dust (same maths as rust/src/fx.rs) ----------

    private const val S = 7

    private fun next(seed: IntArray): Float {
        var x = seed[0]
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        seed[0] = x
        return (x ushr 8) / 16777216f
    }

    private fun spawn(st: FloatArray, o: Int, seed: IntArray, atBottom: Boolean) {
        st[o] = next(seed)
        st[o + 1] = if (atBottom) 1.02f else next(seed)
        st[o + 2] = (next(seed) - 0.5f) * 0.02f
        st[o + 3] = -(0.012f + next(seed) * 0.05f)
        st[o + 4] = next(seed) * 6.2831855f
        st[o + 5] = 0.25f + next(seed) * 0.75f
        st[o + 6] = 0f
    }

    fun particlesInit(state: FloatArray, seed: Int) {
        val sd = intArrayOf(if (seed == 0) 1 else seed)
        var o = 0
        while (o + S <= state.size) {
            spawn(state, o, sd, false)
            state[o + 6] = 0.5f
            o += S
        }
    }

    fun particlesStep(state: FloatArray, dt0: Float, t: Float, seed: Int) {
        val dt = dt0.coerceIn(0f, 0.1f)
        val sd = intArrayOf((((if (seed == 0) 1 else seed) xor (t * 1000f).toInt()) or 1))
        var o = 0
        while (o + S <= state.size) {
            val sway = Math.sin((t * 0.7f + state[o + 4]).toDouble()).toFloat() * 0.012f
            state[o] += (state[o + 2] + sway) * dt
            state[o + 1] += state[o + 3] * dt
            if (state[o] < 0f) state[o] += 1f else if (state[o] > 1f) state[o] -= 1f
            if (state[o + 1] < -0.03f) spawn(state, o, sd, true)
            val tw = 0.5f + 0.5f * Math.sin((t * 1.6f + state[o + 4]).toDouble()).toFloat()
            val edge = ((state[o + 1] + 0.03f) / 0.15f).coerceIn(0f, 1f) * ((1.05f - state[o + 1]) / 0.15f).coerceIn(0f, 1f)
            state[o + 6] = (0.2f + 0.8f * tw) * edge
            o += S
        }
    }
}

package com.tradenex.app.core

import com.tradenex.app.data.Trade
import java.util.Random
import kotlin.math.max

/**
 * Risk statistics popular in trading journals (Edgewonk / TraderSync / TradesViz style):
 * Kelly fraction, R-multiples, drawdown curve and a bootstrap Monte-Carlo simulation.
 * Pure Kotlin so it is unit-testable. These are statistics about past trades, not predictions or advice.
 */
object RiskMath {
    /** Full Kelly fraction f* = W - (1 - W) / R (R = avg win / avg loss). Negative means no edge. */
    fun kelly(winRate: Double, payoff: Double): Double =
        if (payoff <= 0.0) 0.0 else winRate - (1.0 - winRate) / payoff

    /** pnl / plannedRisk for trades that recorded a planned risk. */
    fun rMultiples(trades: List<Trade>): List<Double> =
        trades.mapNotNull { t -> t.plannedRisk?.takeIf { it > 0.0 }?.let { t.pnl / it } }

    /** Drawdown from the running peak in percent (<= 0) for an equity curve. */
    fun drawdownSeries(equity: DoubleArray): DoubleArray {
        var peak = Double.NEGATIVE_INFINITY
        return DoubleArray(equity.size) { i ->
            peak = max(peak, equity[i])
            if (peak > 0) (equity[i] - peak) / peak * 100.0 else 0.0
        }
    }

    class Bands(
        val p5: DoubleArray,
        val p50: DoubleArray,
        val p95: DoubleArray,
        /** share of simulated runs that ever lost [ruinFraction] of the starting balance */
        val ruinProbability: Double,
        val medianFinal: Double,
        /** 95th percentile of the worst peak-to-trough drop across runs, in percent (positive number) */
        val worstDrawdownP95: Double,
    )

    /** Resamples [pnls] with replacement to simulate [horizon] future trades, [runs] times. Deterministic for a given [seed]. */
    fun monteCarlo(pnls: DoubleArray, start: Double, horizon: Int, runs: Int, ruinFraction: Double, seed: Long = 42L): Bands? {
        if (pnls.size < 2 || horizon < 1 || runs < 10 || start <= 0.0) return null
        val rnd = Random(seed)
        val paths = Array(runs) { DoubleArray(horizon + 1) }
        val maxDd = DoubleArray(runs)
        var ruined = 0
        val floor = start * (1.0 - ruinFraction)
        for (r in 0 until runs) {
            var e = start
            var peak = start
            var wentBroke = false
            paths[r][0] = e
            for (i in 1..horizon) {
                e += pnls[rnd.nextInt(pnls.size)]
                paths[r][i] = e
                if (e > peak) peak = e
                if (peak > 0) maxDd[r] = max(maxDd[r], (peak - e) / peak * 100.0)
                if (e <= floor) wentBroke = true
            }
            if (wentBroke) ruined++
        }
        val p5 = DoubleArray(horizon + 1); val p50 = DoubleArray(horizon + 1); val p95 = DoubleArray(horizon + 1)
        val col = DoubleArray(runs)
        for (i in 0..horizon) {
            for (r in 0 until runs) col[r] = paths[r][i]
            col.sort()
            p5[i] = col[(runs * 0.05).toInt().coerceIn(0, runs - 1)]
            p50[i] = col[runs / 2]
            p95[i] = col[(runs * 0.95).toInt().coerceIn(0, runs - 1)]
        }
        maxDd.sort()
        return Bands(p5, p50, p95, ruined.toDouble() / runs, p50[horizon], maxDd[(runs * 0.95).toInt().coerceIn(0, runs - 1)])
    }
}

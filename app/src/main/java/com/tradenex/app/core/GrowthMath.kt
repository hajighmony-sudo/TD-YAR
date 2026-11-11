package com.tradenex.app.core

import kotlin.math.ln
import kotlin.math.pow

object GrowthMath {
    /** Balance after [periods] compounding periods at [ratePct] each. */
    fun compound(start: Double, ratePct: Double, periods: Int): Double = start * (1 + ratePct / 100.0).pow(periods)

    /** Gain (in %) needed to recover from a [lossPct] drawdown. Returns infinity at 100% loss. */
    fun recoveryPct(lossPct: Double): Double = if (lossPct >= 100.0) Double.POSITIVE_INFINITY else lossPct / (100.0 - lossPct) * 100.0

    /** Periods needed to grow [start] to [target] at [ratePct] per period (null if impossible). */
    fun periodsToTarget(start: Double, target: Double, ratePct: Double): Double? {
        if (start <= 0 || target <= start || ratePct <= 0) return null
        return ln(target / start) / ln(1 + ratePct / 100.0)
    }
}

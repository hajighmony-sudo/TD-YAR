package com.tradenex.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskMathTest {
    @Test fun kellyClassic() {
        // 60% win rate, payoff 1.0 -> 20%
        assertEquals(0.2, RiskMath.kelly(0.6, 1.0), 1e-9)
        assertTrue(RiskMath.kelly(0.4, 1.0) < 0)
        assertEquals(0.0, RiskMath.kelly(0.6, 0.0), 1e-9)
    }

    @Test fun drawdownFromPeak() {
        val dd = RiskMath.drawdownSeries(doubleArrayOf(100.0, 110.0, 99.0, 120.0))
        assertEquals(0.0, dd[1], 1e-9)
        assertEquals(-10.0, dd[2], 1e-9)
        assertEquals(0.0, dd[3], 1e-9)
    }

    @Test fun monteCarloIsDeterministicAndOrdered() {
        val pnls = doubleArrayOf(10.0, -5.0, 8.0, -4.0, 12.0, -6.0)
        val a = RiskMath.monteCarlo(pnls, 1000.0, 50, 200, 0.3, seed = 7)
        val b = RiskMath.monteCarlo(pnls, 1000.0, 50, 200, 0.3, seed = 7)
        assertNotNull(a)
        assertEquals(a!!.medianFinal, b!!.medianFinal, 0.0)
        for (i in a.p50.indices) assertTrue(a.p5[i] <= a.p50[i] && a.p50[i] <= a.p95[i])
        assertEquals(0.0, a.ruinProbability, 1e-9) // positive expectancy and tiny losses: cannot lose 30%
    }

    @Test fun monteCarloNeedsData() {
        assertNull(RiskMath.monteCarlo(doubleArrayOf(1.0), 100.0, 10, 100, 0.3))
    }
}

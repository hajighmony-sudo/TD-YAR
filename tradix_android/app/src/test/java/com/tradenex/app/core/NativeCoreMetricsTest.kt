package com.tradenex.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeCoreMetricsTest {
    @Test
    fun fallbackComputesExtendedMetricsFromFiniteRealizedPnl() {
        val values = Fallback.performanceMetrics(doubleArrayOf(100.0, -50.0, 150.0, -100.0, Double.NaN))
        assertEquals(100.0, values[0], 0.000001)
        assertEquals(0.5, values[1], 0.000001)
        assertEquals(250.0 / 150.0, values[2], 0.000001)
        assertEquals(-100.0, values[3], 0.000001)
        assertEquals(25.0, values[4], 0.000001)
        assertEquals(125.0, values[5], 0.000001)
        assertEquals(75.0, values[6], 0.000001)
        assertEquals(1.0, values[7], 0.000001)
        assertEquals(0.6, values[8], 0.000001)
        assertEquals(2.0 / 3.0, values[9], 0.000001)
    }

    @Test
    fun fallbackReturnsUnavailableLikeZeroMetricsForNoFiniteTrades() {
        assertEquals(DoubleArray(10).toList(), Fallback.performanceMetrics(doubleArrayOf(Double.NaN, Double.POSITIVE_INFINITY)).toList())
    }

    @Test
    fun profitFactorUsesDocumentedUpperSentinelWhenThereAreNoLosses() {
        val values = Fallback.performanceMetrics(doubleArrayOf(10.0, 20.0))
        assertEquals(99.0, values[2], 0.000001)
    }
}

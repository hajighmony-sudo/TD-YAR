package com.tradenex.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TetherMathTest {
    @Test fun roundTripBreakEvenHasZeroProfit() {
        val be = TetherMath.roundTrip(100.0, 60000.0, 60000.0, 0.5).breakEvenSell
        assertEquals(0.0, TetherMath.roundTrip(100.0, 60000.0, be, 0.5).profit, 1e-6)
    }
    @Test fun conversionAfterFee() {
        assertEquals(99.65, TetherMath.usdtForToman(6_000_000.0, 60000.0, 0.35), 1e-9)
        assertEquals(0.0, TetherMath.afterNetworkFee(0.5, 1.0), 0.0)
    }
}

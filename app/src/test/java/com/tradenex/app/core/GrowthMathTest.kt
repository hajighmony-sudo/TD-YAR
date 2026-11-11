package com.tradenex.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GrowthMathTest {
    @Test fun recoveryAndCompound() {
        assertEquals(100.0, GrowthMath.recoveryPct(50.0), 1e-9)
        assertEquals(1210.0, GrowthMath.compound(1000.0, 10.0, 2), 1e-6)
        assertEquals(2.0, GrowthMath.periodsToTarget(1000.0, 1210.0, 10.0)!!, 1e-9)
    }
}

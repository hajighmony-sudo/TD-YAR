package com.tradenex.app.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ForexDomainTest {
    @Test
    fun eurUsdBuyCalculatesPipsAndNetPnl() {
        val result = ForexPnlCalculator.calculate(
            FxPnlInput(
                instrument = FxInstrument.standard("EURUSD"),
                side = FxSide.BUY,
                lots = BigDecimal("0.10"),
                entryPrice = BigDecimal("1.10000"),
                exitPrice = BigDecimal("1.10100"),
                quoteToAccountRate = BigDecimal.ONE,
                commissionAccountCurrency = BigDecimal("1.00"),
                plannedRiskAccountCurrency = BigDecimal("5.00"),
            ),
        )
        assertEquals(10.0, result.pips.toDouble(), 1e-8)
        assertEquals(10.0, result.grossAccountCurrency.toDouble(), 1e-8)
        assertEquals(9.0, result.netAccountCurrency.toDouble(), 1e-8)
        assertEquals(1.8, result.rMultiple!!.toDouble(), 1e-8)
    }

    @Test
    fun usdJpyUsesOneHundredthPip() {
        val instrument = FxInstrument.standard("USDJPY")
        assertEquals(0.01, instrument.pipSize.toDouble(), 1e-12)
    }

    @Test
    fun sellProfitUsesReversePriceDifference() {
        val result = ForexPnlCalculator.calculate(
            FxPnlInput(
                instrument = FxInstrument.standard("GBPUSD"),
                side = FxSide.SELL,
                lots = BigDecimal("0.01"),
                entryPrice = BigDecimal("1.2500"),
                exitPrice = BigDecimal("1.2400"),
                quoteToAccountRate = BigDecimal.ONE,
            ),
        )
        assertEquals(10.0, result.netAccountCurrency.toDouble(), 1e-8)
        assertEquals(100.0, result.pips.toDouble(), 1e-8)
        assertNull(result.rMultiple)
    }

    @Test
    fun cryptoAndMetalsAreRejectedAsForexPairs() {
        assertThrows(IllegalArgumentException::class.java) { FxInstrument.standard("BTCUSD") }
        assertThrows(IllegalArgumentException::class.java) { FxInstrument.standard("XAUUSD") }
    }

    @Test
    fun invalidConversionRateIsRejectedRatherThanGuessed() {
        assertThrows(IllegalArgumentException::class.java) {
            ForexPnlCalculator.calculate(
                FxPnlInput(
                    instrument = FxInstrument.standard("EURGBP"),
                    side = FxSide.BUY,
                    lots = BigDecimal("0.10"),
                    entryPrice = BigDecimal("0.8500"),
                    exitPrice = BigDecimal("0.8510"),
                    quoteToAccountRate = BigDecimal.ZERO,
                ),
            )
        }
    }
}

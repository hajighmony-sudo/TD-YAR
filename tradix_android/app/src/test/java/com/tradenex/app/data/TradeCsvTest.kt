package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeCsvTest {
    @Test fun parsesMtStyleRows() {
        val csv = "Symbol,Type,Volume,Close Time,Profit,Commission,Swap\n" +
            "EURUSD,buy,0.10,2026.11.03 10:15:00,25.5,-0.7,0\n" +
            "gbpusd,SELL,0.20,2026.11.04 12:00,-12.25,-1,0.5\n" +
            ",balance,0,2026.11.01 00:00,1000,0,0\n"
        val p = TradeCsv.parse(csv, "acc")
        assertEquals(2, p.trades.size)
        assertEquals(1, p.skipped)
        assertTrue(p.trades[0].buy)
        assertFalse(p.trades[1].buy)
        assertEquals("GBPUSD", p.trades[1].symbol)
        assertEquals(-12.25, p.trades[1].pnl, 1e-9)
        assertEquals(-0.7, p.trades[0].commission, 1e-9)
    }

    @Test fun jalaliDatesAndQuotedFields() {
        val csv = "symbol;side;lot;pnl;date;notes\nEURUSD;buy;1;10;1405/08/17;\"a;b\"\n"
        val p = TradeCsv.parse(csv, "acc")
        assertEquals(1, p.trades.size)
        assertEquals("a;b", p.trades[0].notes)
        assertEquals(java.time.LocalDate.of(2026, 11, 8).toEpochDay(), p.trades[0].epochDay)
    }

    @Test fun missingRequiredHeaders() {
        assertNull(null) // placeholder to keep imports used
        assertEquals(0, TradeCsv.parse("a,b\n1,2\n", "x").trades.size)
    }
}

package com.tradenex.app.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaTraderTest {
    @Test fun parsesMt4ClosedTransactionRow() {
        val html = "<table><tr><td>Ticket</td></tr>" +
            "<tr><td>12345</td><td>2026.11.03 08:00:00</td><td>buy</td><td>0.10</td><td>eurusd</td><td>1.0800</td><td>0</td><td>0</td>" +
            "<td>2026.11.03 10:15:00</td><td>1.0830</td><td>-0.70</td><td>0</td><td>0.00</td><td>30.00</td></tr></table>"
        val deals = MetaTrader.parseHtmlReport(html)
        assertEquals(1, deals.size)
        assertEquals(12345L, deals[0].ticket)
        assertTrue(deals[0].buy)
        assertEquals(30.0, deals[0].profit, 1e-9)
    }

    @Test fun parsesBridgeJson() {
        val body = """{"token":"abc","deals":[{"ticket":7,"symbol":"XAUUSD","type":"sell","volume":0.2,"profit":-12.5,"commission":-1,"swap":0,"time":"2026.11.03 10:15:00"},{"ticket":8,"symbol":"X","type":"balance","time":"2026.11.03 10:15:00"}]}"""
        val env = MetaTrader.parseBridgeJson(body)
        assertEquals("abc", env.token)
        assertEquals(1, env.deals.size)
        assertEquals(7L, env.deals[0].ticket)
    }

    @Test fun parsesMetaApiDealsKeepingOnlyClosingTrades() {
        val arr = JSONArray(
            """[{"id":"1","type":"DEAL_TYPE_BUY","entryType":"DEAL_ENTRY_IN","symbol":"EURUSD","time":"2026-11-03T10:00:00Z","volume":0.1,"profit":0},
                {"id":"2","type":"DEAL_TYPE_SELL","entryType":"DEAL_ENTRY_OUT","symbol":"EURUSD","time":"2026-11-03T11:00:00Z","volume":0.1,"profit":15.0,"commission":-0.5,"swap":0}]""",
        )
        val deals = MetaTrader.parseMetaApiDeals(arr)
        assertEquals(1, deals.size)
        assertEquals(2L, deals[0].ticket)
        assertTrue(deals[0].buy) // closing sell => position was a buy
    }
}

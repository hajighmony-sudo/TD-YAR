package com.tradenex.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Latest USDT price in Toman from Nobitex's public market-stats endpoint (rial / 10). Throws on any failure. */
object UsdtPrice {
    fun latestToman(): Double {
        val c = URL("https://api.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=rls").openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        try {
            val o = JSONObject(c.inputStream.bufferedReader().readText())
            return o.getJSONObject("stats").getJSONObject("usdt-rls").getString("latest").toDouble() / 10.0
        } finally { c.disconnect() }
    }
}

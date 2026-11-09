package com.tradenex.app.data

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import kotlin.concurrent.thread

/**
 * MetaTrader 4/5 connectivity. There is no official retail API for a phone to talk to a terminal, so three real routes are offered:
 *  1. Direct bridge: an Expert Advisor in the terminal POSTs closed deals to a tiny HTTP server inside this app (same Wi-Fi, no cloud).
 *  2. Statement import: MT4/MT5 "Report" (HTML) or CSV history exports.
 *  3. MetaApi cloud: pull history-deals through the MetaApi REST API with the user's own token.
 * Imported trades get a stable id (ID_BASE + ticket) so re-importing never creates duplicates.
 */
object MetaTrader {
    const val ID_BASE = 7_000_000_000_000L

    /** [buy] is the POSITION direction (a closing sell deal closes a buy position). profit/commission/swap use MetaTrader signs. */
    data class Deal(
        val ticket: Long,
        val symbol: String,
        val buy: Boolean,
        val volume: Double,
        val profit: Double,
        val commission: Double,
        val swap: Double,
        val closeEpochDay: Long,
        val closeMillis: Long,
        val openMillis: Long = 0L,
    )

    class ImportResult(val added: Int, val duplicates: Int, val skipped: Int)

    private fun num(s: String): Double? =
        s.replace(" ", "").replace(" ", "").replace(",", "").replace("٫", ".").toDoubleOrNull()

    fun toTrades(deals: List<Deal>, accountId: String, existingIds: Set<Long>): Pair<List<Trade>, Int> {
        var dup = 0
        val out = ArrayList<Trade>()
        val seen = HashSet(existingIds)
        for (d in deals) {
            val id = ID_BASE + d.ticket
            if (!seen.add(id)) { dup++; continue }
            out.add(
                Trade(
                    id = id, symbol = d.symbol.uppercase().replace("/", ""), buy = d.buy, lot = d.volume.takeIf { it > 0 } ?: 0.01,
                    pnl = d.profit + d.commission + d.swap, strategy = "", tags = listOf("metatrader"), epochDay = d.closeEpochDay, emotion = "",
                    accountId = accountId, openedAtMillis = d.openMillis, closedAtMillis = d.closeMillis,
                    commission = d.commission, swap = d.swap, notes = "MT#${d.ticket}",
                ),
            )
        }
        return out to dup
    }

    /** Must be called on the main thread (it mutates Compose state). */
    fun importDeals(deals: List<Deal>, accountId: String = AppState.selectedAccount.id): ImportResult {
        val (trades, dup) = toTrades(deals, accountId, AppState.trades.map { it.id }.toSet())
        val added = AppState.addTrades(trades)
        return ImportResult(added, dup, trades.size - added)
    }

    // ---------- HTML report (MT4 "Detailed Statement" / MT5 "Trade History Report") ----------

    private val rowRx = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val cellRx = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val tagRx = Regex("<[^>]+>")

    private fun clean(s: String) = tagRx.replace(s, "").replace("&nbsp;", " ").replace("&amp;", "&").trim()

    private fun dealFrom(ticket: Long, symbol: String, positionBuy: Boolean, vol: String, profit: String, com: String, swap: String, close: String, open: String = ""): Deal? {
        val cd = TradeCsv.parseDate(close) ?: return null
        val p = num(profit) ?: return null
        if (symbol.isBlank()) return null
        return Deal(ticket, symbol, positionBuy, num(vol) ?: 0.0, p, num(com) ?: 0.0, num(swap) ?: 0.0, cd.first, cd.second, TradeCsv.parseDate(open)?.second ?: 0L)
    }

    fun parseHtmlReport(html: String): List<Deal> {
        val deals = ArrayList<Deal>()
        var mt5: Map<String, Int>? = null
        for (row in rowRx.findAll(html)) {
            val cells = cellRx.findAll(row.groupValues[1]).map { clean(it.groupValues[1]) }.toList()
            if (cells.size < 8) continue
            val lower = cells.map { it.lowercase() }
            if ("direction" in lower && "profit" in lower && "deal" in lower) { mt5 = lower.withIndex().associate { it.value to it.index }; continue }
            val h = mt5
            if (h != null && cells.size >= h.size - 2) {
                val dirI = h["direction"]!!
                if (lower.getOrNull(dirI) == "out") {
                    val ticket = cells.getOrNull(h["deal"]!!)?.toLongOrNull() ?: continue
                    val closingSell = lower.getOrNull(h["type"] ?: -1) == "sell"
                    val fee = h["fee"]?.let { num(cells.getOrElse(it) { "" }) } ?: 0.0
                    dealFrom(
                        ticket, cells.getOrElse(h["symbol"]!!) { "" }, closingSell, cells.getOrElse(h["volume"]!!) { "" },
                        cells.getOrElse(h["profit"]!!) { "" },
                        ((num(cells.getOrElse(h["commission"] ?: -1) { "" }) ?: 0.0) + fee).toString(),
                        cells.getOrElse(h["swap"] ?: -1) { "" }, cells.getOrElse(h["time"]!!) { "" },
                    )?.let(deals::add)
                    continue
                }
            }
            // MT4 closed transaction: Ticket | Open Time | Type | Size | Item | Price | S/L | T/P | Close Time | Price | Commission | Taxes | Swap | Profit
            if (cells.size >= 14) {
                val ticket = cells[0].toLongOrNull() ?: continue
                val type = lower[2]
                if (type != "buy" && type != "sell") continue
                dealFrom(ticket, cells[4], type == "buy", cells[3], cells[13], cells[10], cells[12], cells[8], cells[1])?.let(deals::add)
            }
        }
        return deals
    }

    // ---------- direct bridge JSON ----------

    class Envelope(val token: String, val deals: List<Deal>)

    fun parseBridgeJson(body: String): Envelope {
        val o = JSONObject(body)
        val arr = o.optJSONArray("deals") ?: JSONArray()
        val deals = ArrayList<Deal>()
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i)
            val close = TradeCsv.parseDate(d.optString("time")) ?: continue
            val type = d.optString("type").lowercase()
            if (type != "buy" && type != "sell") continue
            deals.add(
                Deal(
                    d.getLong("ticket"), d.optString("symbol"), type == "buy", d.optDouble("volume", 0.0), d.optDouble("profit", 0.0),
                    d.optDouble("commission", 0.0), d.optDouble("swap", 0.0), close.first, close.second,
                    TradeCsv.parseDate(d.optString("openTime"))?.second ?: 0L,
                ),
            )
        }
        return Envelope(o.optString("token"), deals.filter { it.symbol.isNotBlank() })
    }

    // ---------- MetaApi cloud ----------

    /** Pulls closed deals via MetaApi (https://metaapi.cloud). Needs the user's own API token and MetaApi account id. */
    fun fetchMetaApi(region: String, token: String, accountId: String, fromMillis: Long, toMillis: Long): List<Deal> {
        require(token.isNotBlank() && accountId.isNotBlank()) { "توکن و شناسه‌ی حساب MetaApi لازم است." }
        val from = Instant.ofEpochMilli(fromMillis).toString()
        val to = Instant.ofEpochMilli(toMillis).toString()
        val url = URL("https://mt-client-api-v1.${region.trim().ifBlank { "new-york" }}.agiliumtrade.ai/users/current/accounts/${accountId.trim()}/history-deals/time/$from/$to?limit=1000&offset=0")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("auth-token", token.trim())
        c.setRequestProperty("Accept", "application/json")
        try {
            if (c.responseCode !in 200..299) {
                val err = c.errorStream?.bufferedReader()?.readText().orEmpty().take(200)
                error("MetaApi پاسخ ${c.responseCode} داد. $err")
            }
            val text = c.inputStream.bufferedReader().readText()
            val arr = if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text).optJSONArray("historyDeals") ?: JSONArray()
            return parseMetaApiDeals(arr)
        } finally {
            c.disconnect()
        }
    }

    fun parseMetaApiDeals(arr: JSONArray): List<Deal> {
        val out = ArrayList<Deal>()
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i)
            val type = d.optString("type")
            val entry = d.optString("entryType")
            if ((type != "DEAL_TYPE_BUY" && type != "DEAL_TYPE_SELL") || (entry != "DEAL_ENTRY_OUT" && entry != "DEAL_ENTRY_INOUT" && entry != "DEAL_ENTRY_OUT_BY")) continue
            val t = runCatching { Instant.parse(d.getString("time")) }.getOrNull() ?: continue
            val ticket = d.optString("id").toLongOrNull() ?: continue
            out.add(
                Deal(
                    ticket, d.optString("symbol"), type == "DEAL_TYPE_SELL", d.optDouble("volume", 0.0), d.optDouble("profit", 0.0),
                    d.optDouble("commission", 0.0), d.optDouble("swap", 0.0), t.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay(), t.toEpochMilli(),
                ),
            )
        }
        return out.filter { it.symbol.isNotBlank() }
    }
}

/** Tiny HTTP server (POST /deals, GET /ping) for the direct EA bridge. Runs only while started from the MetaTrader screen. */
object MtBridge {
    var running by mutableStateOf(false); private set
    var received by mutableIntStateOf(0); private set
    var lastMessage by mutableStateOf(""); private set
    var port = 8765
    var token = ""
    @Volatile private var server: ServerSocket? = null
    private val main = Handler(Looper.getMainLooper())

    fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>().firstOrNull { it.isSiteLocalAddress }?.hostAddress
    }.getOrNull()

    fun start() {
        if (running) return
        thread(isDaemon = true, name = "tradix-mt-bridge") {
            val ss = try { ServerSocket(port) } catch (e: Exception) {
                main.post { lastMessage = "راه‌اندازی ممکن نشد: ${e.message}" }
                return@thread
            }
            server = ss
            main.post { running = true; lastMessage = "منتظر Expert Advisor…" }
            while (!ss.isClosed) {
                val s = try { ss.accept() } catch (e: Exception) { break }
                thread(isDaemon = true) { runCatching { handle(s) }; runCatching { s.close() } }
            }
            main.post { running = false }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        running = false
    }

    private fun readLine(i: InputStream): String? {
        val b = ByteArrayOutputStream()
        while (true) {
            val c = i.read()
            if (c < 0) return if (b.size() == 0) null else b.toString("UTF-8")
            if (c == '\n'.code) return b.toString("UTF-8").trimEnd('\r')
            b.write(c)
            if (b.size() > 8192) return null
        }
    }

    private fun reply(s: Socket, code: Int, text: String) {
        val body = text.toByteArray()
        val head = "HTTP/1.1 $code ${if (code == 200) "OK" else "ERR"}\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
        s.getOutputStream().apply { write(head.toByteArray()); write(body); flush() }
    }

    private fun handle(s: Socket) {
        s.soTimeout = 8000
        val input = BufferedInputStream(s.getInputStream())
        val request = readLine(input) ?: return
        var length = 0
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            if (line.startsWith("content-length:", ignoreCase = true)) length = line.substringAfter(':').trim().toIntOrNull() ?: 0
        }
        if (request.startsWith("GET /ping")) { reply(s, 200, "{\"app\":\"TRADIX\"}"); return }
        if (!request.startsWith("POST /deals")) { reply(s, 404, "{\"error\":\"not found\"}"); return }
        if (length <= 0 || length > 2_000_000) { reply(s, 400, "{\"error\":\"bad length\"}"); return }
        val buf = ByteArray(length)
        var got = 0
        while (got < length) { val r = input.read(buf, got, length - got); if (r < 0) break; got += r }
        val env = runCatching { MetaTrader.parseBridgeJson(String(buf, 0, got, Charsets.UTF_8)) }.getOrElse { reply(s, 400, "{\"error\":\"bad json\"}"); return }
        if (token.isBlank() || env.token != token) { reply(s, 401, "{\"error\":\"bad token\"}"); return }
        main.post {
            val r = MetaTrader.importDeals(env.deals)
            received += r.added
            lastMessage = "${r.added} معامله‌ی جدید دریافت شد" + if (r.duplicates > 0) " (${r.duplicates} تکراری)" else ""
        }
        reply(s, 200, "{\"received\":${env.deals.size}}")
    }
}

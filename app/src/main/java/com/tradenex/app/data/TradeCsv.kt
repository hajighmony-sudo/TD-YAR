package com.tradenex.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Generic CSV import (MT4/MT5 history exports, broker statements, or this app's own export).
 * Header names are matched case-insensitively with common aliases. Rows that cannot be parsed are skipped and counted.
 */
object TradeCsv {
    data class Parsed(val trades: List<Trade>, val skipped: Int)

    private val symbolKeys = listOf("symbol", "instrument", "pair", "item", "نماد")
    private val sideKeys = listOf("type", "side", "direction", "action", "جهت")
    private val lotKeys = listOf("lot", "lots", "volume", "size", "size/lots", "حجم")
    private val pnlKeys = listOf("profit", "pnl", "p&l", "net", "net profit", "result", "سود/زیان", "سود")
    private val dateKeys = listOf("close time", "closetime", "time", "date", "open time", "opentime", "closed", "تاریخ")
    private val commissionKeys = listOf("commission", "کمیسیون")
    private val swapKeys = listOf("swap", "سواپ")
    private val notesKeys = listOf("notes", "comment", "note", "یادداشت")
    private val strategyKeys = listOf("strategy", "setup", "استراتژی")

    fun splitLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var q = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && q && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> q = !q
                (ch == ',' || ch == ';' || ch == '\t') && !q -> { out.add(sb.toString().trim()); sb.setLength(0) }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString().trim())
        return out
    }

    private fun idx(header: List<String>, keys: List<String>): Int =
        header.indexOfFirst { h -> keys.any { it == h } }

    private fun number(s: String): Double? =
        s.replace(" ", "").replace(" ", "").replace(",", "").replace("٫", ".")
            .map { if (it in '۰'..'۹') '0' + (it - '۰') else it }.joinToString("").toDoubleOrNull()

    /** Accepts yyyy-MM-dd, yyyy.MM.dd, yyyy/MM/dd (also Jalali 14xx/..), optionally followed by a time. */
    fun parseDate(raw: String): Pair<Long, Long>? {
        val s = raw.trim().replace('T', ' ')
        val datePart = s.substringBefore(' ')
        val timePart = s.substringAfter(' ', "")
        val date = JalaliCalendar.parseDateInput(datePart.replace('.', '-')) ?: return null
        val time = runCatching { java.time.LocalTime.parse(timePart.take(8).let { if (it.length == 5) "$it:00" else it }) }.getOrNull()
        val millis = if (time != null) LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() else 0L
        return date.toEpochDay() to millis
    }

    fun parse(text: String, accountId: String, idStart: Long = System.currentTimeMillis()): Parsed {
        val lines = text.removePrefix("﻿").lines().filter { it.isNotBlank() }
        if (lines.size < 2) return Parsed(emptyList(), 0)
        val header = splitLine(lines[0]).map { it.lowercase().trim() }
        val iSym = idx(header, symbolKeys); val iSide = idx(header, sideKeys); val iLot = idx(header, lotKeys)
        val iPnl = idx(header, pnlKeys); val iDate = idx(header, dateKeys)
        val iCom = idx(header, commissionKeys); val iSwap = idx(header, swapKeys)
        val iNotes = idx(header, notesKeys); val iStrat = idx(header, strategyKeys)
        if (iSym < 0 || iPnl < 0 || iDate < 0) return Parsed(emptyList(), lines.size - 1)
        var skipped = 0
        val trades = ArrayList<Trade>()
        for ((n, line) in lines.drop(1).withIndex()) {
            val c = splitLine(line)
            fun at(i: Int) = if (i in c.indices) c[i] else ""
            val symbol = at(iSym).uppercase().replace("/", "")
            val pnl = number(at(iPnl))
            val date = parseDate(at(iDate))
            val side = at(iSide).lowercase()
            // balance / deposit rows in MT exports have no real symbol
            if (symbol.isBlank() || pnl == null || date == null || side.contains("balance") || side.contains("deposit") || side.contains("withdraw")) { skipped++; continue }
            val lot = number(at(iLot))?.takeIf { it > 0 } ?: 0.01
            trades.add(
                Trade(
                    id = idStart + n, symbol = symbol, buy = !(side.startsWith("sell") || side.contains("short") || side == "s" || side.contains("فروش")),
                    lot = lot, pnl = pnl, strategy = at(iStrat), tags = emptyList(), epochDay = date.first, emotion = "",
                    accountId = accountId, closedAtMillis = date.second,
                    commission = number(at(iCom)) ?: 0.0, swap = number(at(iSwap)) ?: 0.0, notes = at(iNotes),
                ),
            )
        }
        return Parsed(trades, skipped)
    }
}

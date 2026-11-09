package com.tradenex.app

import com.tradenex.app.core.SessionEngine
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Trade
import com.tradenex.app.data.TxAiReviewStatus
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.time.Instant

/**
 * Flutter host boundary for TX AI. Only the selected account's journal fields are exposed;
 * broker credentials and account secrets are never sent to Dart.
 */
class TxAiFlutterActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "loadSelectedAccountSnapshot" -> result.success(selectedAccountSnapshot())
                    "saveEpisodeReview" -> {
                        val accountId = call.argument<String>("accountId")
                        val episodeId = call.argument<String>("episodeId")
                        val status = TxAiReviewStatus.fromStorageKey(call.argument<String>("status"))
                        if (accountId != AppState.selectedAccount.id || episodeId.isNullOrBlank() || status == null) {
                            result.error("invalid_review", "Review payload is invalid or the selected account changed.", null)
                        } else {
                            AppState.reviewEpisode(accountId, episodeId, status)
                            result.success(null)
                        }
                    }
                    "setInsightPinned" -> {
                        val accountId = call.argument<String>("accountId")
                        val insightId = call.argument<String>("insightId")
                        val pinned = call.argument<Boolean>("pinned")
                        if (accountId != AppState.selectedAccount.id || insightId.isNullOrBlank() || pinned == null) {
                            result.error("invalid_pin", "Pin payload is invalid or the selected account changed.", null)
                        } else {
                            AppState.setInsightPinned(accountId, insightId, pinned)
                            result.success(null)
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }

    override fun onDestroy() {
        AppState.persist()
        super.onDestroy()
    }

    private fun selectedAccountSnapshot(): Map<String, Any?> {
        val account = AppState.selectedAccount
        val trades = AppState.visibleTrades
        val displayZone = SessionEngine.displayZone(AppState.settings.tzMode)
        val nowDisplay = Instant.now().atZone(displayZone)
        return mapOf(
            "accountId" to account.id,
            "accountName" to account.name,
            "currency" to account.baseCurrency,
            "startingBalance" to account.startingBalance,
            "riskPercent" to AppState.settings.riskPercent,
            "displayTimezone" to displayZone.id,
            "currentDisplayDateKey" to nowDisplay.toLocalDate().toString(),
            "currentMonthKey" to "%04d-%02d".format(java.util.Locale.US, nowDisplay.year, nowDisplay.monthValue),
            "trades" to trades.map(::tradePayload),
            "episodeReviews" to AppState.txAiEpisodeReviews
                .filter { it.accountId == account.id }
                .map { mapOf("episodeId" to it.episodeId, "status" to it.status.storageKey) },
            "pinnedInsights" to AppState.txAiInsightPins
                .filter { it.accountId == account.id }
                .map { mapOf("insightId" to it.insightId, "pinned" to it.pinned) },
            "behavioralPatterns" to com.tradenex.app.data.TxAiBehavioralPatternAnalyzer.analyze(
                trades,
                AppState.strategies.toList(),
                AppState.strategyRevisions.toList(),
            ).map { pattern ->
                mapOf(
                    "id" to pattern.id,
                    "title" to pattern.type.label,
                    "triggerTradeId" to pattern.triggerTradeId,
                    "linkedTradeIds" to pattern.linkedTradeIds,
                    "evidenceScore" to pattern.evidenceScore,
                    "actualNetPnl" to pattern.actualNetPnl,
                    "reasons" to pattern.reasons,
                )
            },
        )
    }

    private fun tradePayload(trade: Trade): Map<String, Any?> {
        val displayTime = trade.openedAtMillis.takeIf { it > 0L }?.let {
            Instant.ofEpochMilli(it).atZone(SessionEngine.displayZone(AppState.settings.tzMode))
        }
        val displayDate = displayTime?.toLocalDate()
        val displayCloseMonth = trade.closedAtMillis.takeIf { it > 0L }?.let {
            Instant.ofEpochMilli(it).atZone(SessionEngine.displayZone(AppState.settings.tzMode)).toLocalDate()
        }
        return mapOf(
        "id" to trade.id,
        "accountId" to trade.accountId,
        "symbol" to trade.symbol,
        "isBuy" to trade.buy,
        "pnl" to trade.pnl,
        "lot" to trade.lot,
        "epochDay" to trade.epochDay,
        "strategy" to trade.strategy,
        "strategyVersion" to trade.strategyVersion,
        "tags" to trade.tags,
        "emotion" to trade.emotion,
        "openedAtMillis" to trade.openedAtMillis.takeIf { it > 0L },
        "closedAtMillis" to trade.closedAtMillis.takeIf { it > 0L },
        "plannedRisk" to trade.plannedRisk,
        "timeframe" to trade.timeframe.takeIf { it.isNotBlank() },
        "marketCondition" to trade.marketCondition.takeIf { it.isNotBlank() },
        "strategyFollowed" to trade.strategyFollowed,
        "session" to sessionFor(trade),
        "entryPrice" to trade.entryPrice,
        "exitPrice" to trade.exitPrice,
        "stopLossPrice" to trade.stopLossPrice,
        "takeProfitPrice" to trade.takeProfitPrice,
        "displayHour" to displayTime?.hour,
        "displayWeekday" to displayTime?.dayOfWeek?.value,
        "displayMinute" to displayTime?.minute,
        "displayDateKey" to displayDate?.toString(),
        "displayMonthKey" to displayDate?.let { "%04d-%02d".format(java.util.Locale.US, it.year, it.monthValue) },
        "displayClosedMonthKey" to displayCloseMonth?.let { "%04d-%02d".format(java.util.Locale.US, it.year, it.monthValue) },
    )
    }

    private fun sessionFor(trade: Trade): String? {
        if (trade.openedAtMillis <= 0L) return null
        val instant = Instant.ofEpochMilli(trade.openedAtMillis)
        return SessionEngine.defs.firstOrNull { def ->
            val local = instant.atZone(def.zoneId).toLocalTime()
            if (def.localOpen < def.localClose) {
                !local.isBefore(def.localOpen) && local.isBefore(def.localClose)
            } else {
                !local.isBefore(def.localOpen) || local.isBefore(def.localClose)
            }
        }?.name
    }

    companion object {
        private const val CHANNEL = "tradenex/tx_ai"
    }
}

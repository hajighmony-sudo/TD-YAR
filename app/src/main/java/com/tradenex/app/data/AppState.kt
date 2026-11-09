package com.tradenex.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradenex.app.core.NativeCore
import com.tradenex.app.core.TradeStats
import java.time.LocalDate

data class Trade(
    val id: Long,
    val symbol: String,
    val buy: Boolean,
    val lot: Double,
    val pnl: Double,
    val strategy: String,
    val tags: List<String>,
    val epochDay: Long,
    val emotion: String,
    val accountId: String = "default",
    val openedAtMillis: Long = 0L,
    val closedAtMillis: Long = 0L,
    val entryPrice: Double? = null,
    val exitPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val takeProfitPrice: Double? = null,
    val commission: Double = 0.0,
    val swap: Double = 0.0,
    val otherFees: Double = 0.0,
    val notes: String = "",
    val plannedRisk: Double? = null,
    val strategyVersion: Int = 1,
    val timeframe: String = "",
    val marketCondition: String = "",
    val strategyFollowed: Boolean? = null,
    val tradeName: String = "",
)

data class TradingAccount(
    val id: String,
    val name: String,
    val broker: String,
    val type: String,
    val baseCurrency: String,
    val startingBalance: Double,
    val archived: Boolean = false,
)

data class Strategy(val id: Long, val name: String, val rules: List<String>, val active: Boolean, val version: Int = 1)

data class StrategyRevision(
    val id: Long,
    val strategyId: Long,
    val version: Int,
    val name: String,
    val rules: List<String>,
    val createdAt: Long,
)

enum class ContentKind(val label: String) {
    TEXT("فقط متن"),
    IMAGE("فقط عکس"),
    VIDEO("فقط ویدیو"),
    TEXT_IMAGE("متن + عکس"),
    IMAGE_TEXT("عکس + متن"),
    TEXT_VIDEO("متن + ویدیو"),
    VIDEO_TEXT("ویدیو + متن"),
}

data class Lesson(val id: Long, val title: String, val kind: ContentKind, val progress: Float)

data class PositionFill(
    val id: Long,
    val volume: Double,
    val price: Double,
    val realizedPnl: Double = 0.0,
    val atMillis: Long = System.currentTimeMillis(),
)

data class JournalPosition(
    val id: Long,
    val accountId: String,
    val symbol: String,
    val buy: Boolean,
    val strategy: String,
    val entryFills: List<PositionFill>,
    val exitFills: List<PositionFill> = emptyList(),
    val notes: String = "",
    val openedAtMillis: Long = System.currentTimeMillis(),
) {
    val enteredVolume: Double get() = entryFills.sumOf { it.volume }
    val exitedVolume: Double get() = exitFills.sumOf { it.volume }
    val openVolume: Double get() = (enteredVolume - exitedVolume).coerceAtLeast(0.0)
    val averageEntry: Double get() = if (enteredVolume <= 0.0) 0.0 else entryFills.sumOf { it.volume * it.price } / enteredVolume
    val averageExit: Double get() = if (exitedVolume <= 0.0) 0.0 else exitFills.sumOf { it.volume * it.price } / exitedVolume
    val realizedPnl: Double get() = exitFills.sumOf { it.realizedPnl }
    val isClosed: Boolean get() = openVolume <= 1e-8
}

data class AppSettings(
    val language: Int = 0,
    val showGlobe: Boolean = true,
    val tzMode: Int = 2,
    val reminderOn: Boolean = true,
    val reminderHour: Int = 22,
    val newsImpact: Int = 0,
    val startBalance: Double = 10_000.0,
    val defaultLot: Double = 0.10,
    val riskPercent: Double = 1.0,
    val dailyLossLimitPercent: Double = 3.0,
    val maxTradesPerDay: Int = 5,
    val haptics: Boolean = true,
    val animations: Int = 1,
    val accent: Int = 0,
    val weekendHoliday: Boolean = true,
    val sessionAlerts: Boolean = true,
    val newsAlerts: Boolean = true,
    val autoBackup: Boolean = false,
    val hideBalance: Boolean = false,
    val profileFirstName: String = "",
    val profileLastName: String = "",
    val profilePhotoUri: String = "",
)

object AppState {
    val trades = mutableStateListOf<Trade>()
    val positions = mutableStateListOf<JournalPosition>()
    val accounts = mutableStateListOf(
        TradingAccount("default", "حساب اصلی", "", "REAL", "USD", 10_000.0),
    )
    var selectedAccountId by mutableStateOf("default")
    val selectedAccount: TradingAccount
        get() = accounts.firstOrNull { it.id == selectedAccountId && !it.archived }
            ?: accounts.firstOrNull { !it.archived }
            ?: TradingAccount("default", "حساب اصلی", "", "REAL", "USD", 10_000.0)
    val visibleTrades: List<Trade>
        get() = trades.filter { it.accountId == selectedAccountId }
    // User-created content only: no preloaded strategy templates or sample records.
    val strategies = mutableStateListOf<Strategy>()
    val strategyRevisions = mutableStateListOf<StrategyRevision>()
    val txAiEpisodeReviews = mutableStateListOf<TxAiEpisodeReview>()
    val txAiInsightPins = mutableStateListOf<TxAiInsightPin>()
    // Tags are created by the user; never seed demo hashtags into a new journal.
    val hashtags = mutableStateListOf<String>()
    // The education library starts empty and is populated only by the user.
    val lessons = mutableStateListOf<Lesson>()
    var settings by mutableStateOf(AppSettings())
    private var localStore: LocalStore? = null

    fun initialize(context: android.content.Context) {
        if (localStore != null) return
        localStore = LocalStore(context)
        val snapshot = localStore?.readSnapshot()
        if (snapshot != null) {
            val target = MutableSnapshot(
                trades, positions, accounts, strategies, strategyRevisions, hashtags, lessons, settings,
                selectedAccountId, txAiEpisodeReviews, txAiInsightPins,
            )
            SnapshotCodec.restore(snapshot, target)
            settings = target.settings
            selectedAccountId = target.selectedAccountId
        }
    }

    fun persist() {
        val payload = SnapshotCodec.encode(
            trades, positions, accounts, strategies, strategyRevisions, hashtags, lessons, settings, selectedAccountId,
            txAiEpisodeReviews, txAiInsightPins,
        )
        localStore?.writeSnapshot(payload)
    }

    fun stats(): TradeStats = NativeCore.stats(visibleTrades.map { it.pnl }.toDoubleArray())

    /** Bulk add (CSV import): one persist for the whole batch. Invalid rows are skipped; returns how many were added. */
    fun addTrades(list: List<Trade>): Int {
        val ok = list.filter { it.symbol.trim().matches(Regex("[\\p{L}\\p{N}._/-]{1,24}")) && it.lot > 0.0 && it.pnl.isFinite() }
        if (ok.isEmpty()) return 0
        trades.addAll(ok)
        persist()
        return ok.size
    }

    fun addTrade(t: Trade) {
        require(t.symbol.trim().matches(Regex("[\\p{L}\\p{N}._/-]{1,24}"))) {
            "Trade symbol must contain 1-24 letters, digits, dots, underscores, slashes or hyphens."
        }
        require(t.lot > 0.0 && t.pnl.isFinite()) { "Trade volume and P&L must be valid." }
        trades.add(t)
        persist()
    }

    fun addPosition(position: JournalPosition) {
        require(position.symbol.isNotBlank() && position.entryFills.isNotEmpty())
        require(position.entryFills.all { it.volume > 0.0 && it.price > 0.0 && it.volume.isFinite() && it.price.isFinite() })
        positions.add(position)
        persist()
    }

    fun addPositionEntry(positionId: Long, fill: PositionFill) {
        require(fill.volume > 0.0 && fill.price > 0.0 && fill.volume.isFinite() && fill.price.isFinite())
        val index = positions.indexOfFirst { it.id == positionId }
        if (index < 0) return
        val current = positions[index]
        require(!current.isClosed) { "Closed positions cannot be reopened." }
        positions[index] = current.copy(entryFills = current.entryFills + fill)
        persist()
    }

    fun addPositionExit(positionId: Long, fill: PositionFill) {
        require(fill.volume > 0.0 && fill.price > 0.0 && fill.volume.isFinite() && fill.price.isFinite() && fill.realizedPnl.isFinite())
        val index = positions.indexOfFirst { it.id == positionId }
        if (index < 0) return
        val current = positions[index]
        require(!current.isClosed && fill.volume <= current.openVolume + 1e-8) { "Exit volume exceeds the open position volume." }
        val updated = current.copy(exitFills = current.exitFills + fill)
        positions[index] = updated
        if (updated.isClosed && trades.none { it.id == updated.id }) {
            val closedAt = updated.exitFills.maxOfOrNull { it.atMillis } ?: System.currentTimeMillis()
            val closeDate = java.time.Instant.ofEpochMilli(closedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            trades.add(
                Trade(
                    id = updated.id,
                    symbol = updated.symbol,
                    buy = updated.buy,
                    lot = updated.exitedVolume,
                    pnl = updated.realizedPnl,
                    strategy = updated.strategy,
                    tags = emptyList(),
                    epochDay = closeDate.toEpochDay(),
                    emotion = "",
                    accountId = updated.accountId,
                    openedAtMillis = updated.openedAtMillis,
                    closedAtMillis = closedAt,
                    entryPrice = updated.averageEntry,
                    exitPrice = updated.averageExit,
                    notes = updated.notes,
                    tradeName = "پوزیشن تکمیل‌شده",
                ),
            )
        }
        persist()
    }

    fun deletePosition(positionId: Long) {
        positions.removeAll { it.id == positionId }
        persist()
    }

    fun episodeReview(accountId: String, episodeId: String): TxAiReviewStatus? =
        TxAiReviewRules.reviewFor(txAiEpisodeReviews, accountId, episodeId)

    fun reviewEpisode(accountId: String, episodeId: String, status: TxAiReviewStatus) {
        require(accountId.isNotBlank() && episodeId.isNotBlank())
        val next = TxAiReviewRules.upsertReview(
            txAiEpisodeReviews.toList(),
            TxAiEpisodeReview(accountId, episodeId, status, System.currentTimeMillis()),
        )
        txAiEpisodeReviews.clear()
        txAiEpisodeReviews.addAll(next)
        persist()
    }

    fun isInsightPinned(accountId: String, insightId: String): Boolean =
        TxAiReviewRules.isPinned(txAiInsightPins, accountId, insightId)

    fun setInsightPinned(accountId: String, insightId: String, pinned: Boolean) {
        require(accountId.isNotBlank() && insightId.isNotBlank())
        val next = TxAiReviewRules.setPinned(txAiInsightPins.toList(), accountId, insightId, pinned)
        txAiInsightPins.clear()
        txAiInsightPins.addAll(next)
        persist()
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    fun updateTrade(id: Long, transform: (Trade) -> Trade) {
        val index = trades.indexOfFirst { it.id == id }
        if (index >= 0) {
            trades[index] = transform(trades[index])
            persist()
        }
    }

    fun deleteTrade(id: Long) {
        trades.removeAll { it.id == id }
        persist()
    }

    fun addStrategy(strategy: Strategy) {
        strategies.add(strategy)
        persist()
    }

    fun updateStrategy(id: Long, transform: (Strategy) -> Strategy) {
        val index = strategies.indexOfFirst { it.id == id }
        if (index >= 0) {
            strategies[index] = transform(strategies[index])
            persist()
        }
    }

    fun deleteStrategy(id: Long) {
        strategies.removeAll { it.id == id }
        strategyRevisions.removeAll { it.strategyId == id }
        persist()
    }

    fun addHashtag(tag: String) {
        val normalized = tag.trim().let { if (it.startsWith("#")) it else "#$it" }
        if (normalized.length > 1 && normalized !in hashtags) {
            hashtags.add(normalized)
            persist()
        }
    }

    fun removeHashtag(tag: String) {
        hashtags.remove(tag)
        persist()
    }

    fun exportSnapshotJson(): String = SnapshotCodec.encode(
        trades, positions, accounts, strategies, strategyRevisions, hashtags, lessons, settings, selectedAccountId,
        txAiEpisodeReviews, txAiInsightPins,
    ).toString(2)

    fun importSnapshotJson(raw: String): Boolean {
        val snapshot = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return false
        if (snapshot.optInt("version", 0) != 1) return false
        val target = MutableSnapshot(
            trades, positions, accounts, strategies, strategyRevisions, hashtags, lessons, settings,
            selectedAccountId, txAiEpisodeReviews, txAiInsightPins,
        )
        SnapshotCodec.restore(snapshot, target)
        settings = target.settings
        selectedAccountId = target.selectedAccountId
        persist()
        return true
    }
}

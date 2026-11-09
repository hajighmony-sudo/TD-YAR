package com.tradenex.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Atomic local snapshot store. It deliberately uses Android's built-in SQLite API so the
 * existing Gradle/Kotlin toolchain and UI remain untouched. The JSON payload is versioned;
 * a future migration can normalize high-volume trade rows without losing existing user data.
 */
internal class LocalStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "tradix_local.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE app_snapshot (id INTEGER PRIMARY KEY CHECK(id = 1), schema_version INTEGER NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Snapshot schema is versioned independently. Never drop a user's journal on upgrade.
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS app_snapshot (id INTEGER PRIMARY KEY CHECK(id = 1), schema_version INTEGER NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        }
    }

    @Synchronized
    fun readSnapshot(): JSONObject? {
        readableDatabase.rawQuery(
            "SELECT payload FROM app_snapshot WHERE id = 1",
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return runCatching { JSONObject(cursor.getString(0)) }.getOrNull()
        }
    }

    @Synchronized
    fun writeSnapshot(payload: JSONObject) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("id", 1)
                put("schema_version", SNAPSHOT_VERSION)
                put("payload", payload.toString())
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("app_snapshot", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val SNAPSHOT_VERSION = 1
    }
}

internal object SnapshotCodec {
    fun encode(
        trades: List<Trade>,
        positions: List<JournalPosition>,
        accounts: List<TradingAccount>,
        strategies: List<Strategy>,
        strategyRevisions: List<StrategyRevision>,
        hashtags: List<String>,
        lessons: List<Lesson>,
        settings: AppSettings,
        selectedAccountId: String,
        txAiEpisodeReviews: List<TxAiEpisodeReview> = emptyList(),
        txAiInsightPins: List<TxAiInsightPin> = emptyList(),
    ): JSONObject = JSONObject().apply {
        put("version", 1)
        put("selectedAccountId", selectedAccountId)
        put("txAiEpisodeReviews", JSONArray().apply {
            txAiEpisodeReviews.forEach { review ->
                put(JSONObject().apply {
                    put("accountId", review.accountId)
                    put("episodeId", review.episodeId)
                    put("status", review.status.storageKey)
                    put("updatedAtMillis", review.updatedAtMillis)
                })
            }
        })
        put("txAiInsightPins", JSONArray().apply {
            txAiInsightPins.forEach { pin ->
                put(JSONObject().apply {
                    put("accountId", pin.accountId)
                    put("insightId", pin.insightId)
                    put("pinned", pin.pinned)
                })
            }
        })
        put("accounts", JSONArray().apply {
            accounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("name", a.name); put("broker", a.broker)
                    put("type", a.type); put("baseCurrency", a.baseCurrency)
                    put("startingBalance", a.startingBalance); put("archived", a.archived)
                })
            }
        })
        put("trades", JSONArray().apply {
            trades.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("symbol", t.symbol); put("buy", t.buy)
                    put("lot", t.lot); put("pnl", t.pnl); put("strategy", t.strategy)
                    put("tags", JSONArray(t.tags)); put("epochDay", t.epochDay); put("emotion", t.emotion)
                    put("accountId", t.accountId); put("openedAtMillis", t.openedAtMillis)
                    put("closedAtMillis", t.closedAtMillis)
                    t.entryPrice?.let { put("entryPrice", it) }; t.exitPrice?.let { put("exitPrice", it) }
                    t.stopLossPrice?.let { put("stopLossPrice", it) }; t.takeProfitPrice?.let { put("takeProfitPrice", it) }
                    put("commission", t.commission); put("swap", t.swap); put("otherFees", t.otherFees)
                    put("notes", t.notes); put("tradeName", t.tradeName); t.plannedRisk?.let { put("plannedRisk", it) }
                    put("strategyVersion", t.strategyVersion); put("timeframe", t.timeframe); put("marketCondition", t.marketCondition)
                    t.strategyFollowed?.let { put("strategyFollowed", it) }
                })
            }
        })
        put("positions", JSONArray().apply {
            positions.forEach { position ->
                put(JSONObject().apply {
                    put("id", position.id); put("accountId", position.accountId)
                    put("symbol", position.symbol); put("buy", position.buy)
                    put("strategy", position.strategy); put("notes", position.notes)
                    put("openedAtMillis", position.openedAtMillis)
                    put("entryFills", JSONArray().apply {
                        position.entryFills.forEach { fill ->
                            put(JSONObject().apply {
                                put("id", fill.id); put("volume", fill.volume); put("price", fill.price); put("atMillis", fill.atMillis)
                            })
                        }
                    })
                    put("exitFills", JSONArray().apply {
                        position.exitFills.forEach { fill ->
                            put(JSONObject().apply {
                                put("id", fill.id); put("volume", fill.volume); put("price", fill.price)
                                put("realizedPnl", fill.realizedPnl); put("atMillis", fill.atMillis)
                            })
                        }
                    })
                })
            }
        })
        put("strategies", JSONArray().apply {
            strategies.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("rules", JSONArray(s.rules))
                    put("active", s.active); put("version", s.version)
                })
            }
        })
        put("strategyRevisions", JSONArray().apply {
            strategyRevisions.forEach { v ->
                put(JSONObject().apply {
                    put("id", v.id); put("strategyId", v.strategyId); put("version", v.version)
                    put("name", v.name); put("rules", JSONArray(v.rules)); put("createdAt", v.createdAt)
                })
            }
        })
        put("hashtags", JSONArray(hashtags))
        put("lessons", JSONArray().apply {
            lessons.forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id); put("title", l.title); put("kind", l.kind.name)
                    put("progress", l.progress.toDouble())
                })
            }
        })
        put("settings", JSONObject().apply {
            put("language", settings.language); put("showGlobe", settings.showGlobe)
            put("tzMode", settings.tzMode); put("reminderOn", settings.reminderOn)
            put("reminderHour", settings.reminderHour); put("newsImpact", settings.newsImpact)
            put("startBalance", settings.startBalance); put("defaultLot", settings.defaultLot)
            put("riskPercent", settings.riskPercent); put("dailyLossLimitPercent", settings.dailyLossLimitPercent)
            put("maxTradesPerDay", settings.maxTradesPerDay); put("haptics", settings.haptics)
            put("animations", settings.animations); put("accent", settings.accent)
            put("weekendHoliday", settings.weekendHoliday); put("sessionAlerts", settings.sessionAlerts)
            put("newsAlerts", settings.newsAlerts); put("autoBackup", settings.autoBackup)
            put("hideBalance", settings.hideBalance)
            put("profileFirstName", settings.profileFirstName)
            put("profileLastName", settings.profileLastName)
            put("profilePhotoUri", settings.profilePhotoUri)
        })
    }

    fun restore(snapshot: JSONObject, target: MutableSnapshot) {
        if (snapshot.optInt("version", 0) != 1) return
        target.accounts.clear()
        val accountRows = snapshot.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accountRows.length()) {
            val row = accountRows.optJSONObject(i) ?: continue
            target.accounts.add(
                TradingAccount(
                    id = row.optString("id"),
                    name = row.optString("name"),
                    broker = row.optString("broker"),
                    type = row.optString("type", ""),
                    baseCurrency = row.optString("baseCurrency", ""),
                    startingBalance = row.optDouble("startingBalance", 0.0),
                    archived = row.optBoolean("archived", false),
                ),
            )
        }
        if (target.accounts.isNotEmpty() && target.accounts.none { !it.archived }) {
            target.accounts[0] = target.accounts[0].copy(archived = false)
        }
        val requestedAccount = snapshot.optString("selectedAccountId", "")
        target.selectedAccountId = target.accounts.firstOrNull { it.id == requestedAccount && !it.archived }?.id
            ?: target.accounts.firstOrNull { !it.archived }?.id.orEmpty()
        target.txAiEpisodeReviews.clear()
        val reviewRows = snapshot.optJSONArray("txAiEpisodeReviews") ?: JSONArray()
        for (i in 0 until reviewRows.length()) {
            val row = reviewRows.optJSONObject(i) ?: continue
            val accountId = row.optString("accountId")
            val episodeId = row.optString("episodeId")
            val status = TxAiReviewStatus.fromStorageKey(row.optString("status"))
            if (accountId.isNotBlank() && episodeId.isNotBlank() && status != null) {
                target.txAiEpisodeReviews.add(
                    TxAiEpisodeReview(accountId, episodeId, status, row.optLong("updatedAtMillis")),
                )
            }
        }
        target.txAiInsightPins.clear()
        val pinRows = snapshot.optJSONArray("txAiInsightPins") ?: JSONArray()
        for (i in 0 until pinRows.length()) {
            val row = pinRows.optJSONObject(i) ?: continue
            val accountId = row.optString("accountId")
            val insightId = row.optString("insightId")
            if (accountId.isNotBlank() && insightId.isNotBlank()) {
                target.txAiInsightPins.add(TxAiInsightPin(accountId, insightId, row.optBoolean("pinned", false)))
            }
        }
        target.trades.clear()
        val tradeRows = snapshot.optJSONArray("trades") ?: JSONArray()
        for (i in 0 until tradeRows.length()) {
            val row = tradeRows.optJSONObject(i) ?: continue
            val tags = row.optJSONArray("tags") ?: JSONArray()
            target.trades.add(
                Trade(
                    id = row.optLong("id"),
                    symbol = row.optString("symbol", ""),
                    buy = row.optBoolean("buy", true),
                    lot = row.optDouble("lot", 0.0),
                    pnl = row.optDouble("pnl", 0.0),
                    strategy = row.optString("strategy", ""),
                    tags = (0 until tags.length()).mapNotNull { tags.optString(it).takeIf(String::isNotBlank) },
                    epochDay = row.optLong("epochDay"),
                    emotion = row.optString("emotion", ""),
                    accountId = row.optString("accountId", ""),
                    openedAtMillis = row.optLong("openedAtMillis", 0L),
                    closedAtMillis = row.optLong("closedAtMillis", 0L),
                    entryPrice = row.optDouble("entryPrice").takeIf { row.has("entryPrice") },
                    exitPrice = row.optDouble("exitPrice").takeIf { row.has("exitPrice") },
                    stopLossPrice = row.optDouble("stopLossPrice").takeIf { row.has("stopLossPrice") },
                    takeProfitPrice = row.optDouble("takeProfitPrice").takeIf { row.has("takeProfitPrice") },
                    commission = row.optDouble("commission", 0.0),
                    swap = row.optDouble("swap", 0.0),
                    otherFees = row.optDouble("otherFees", 0.0),
                    notes = row.optString("notes", ""),
                    tradeName = row.optString("tradeName", ""),
                    plannedRisk = row.optDouble("plannedRisk").takeIf { row.has("plannedRisk") },
                    strategyVersion = row.optInt("strategyVersion", 1),
                    timeframe = row.optString("timeframe", ""),
                    marketCondition = row.optString("marketCondition", ""),
                    strategyFollowed = if (row.has("strategyFollowed") && !row.isNull("strategyFollowed")) row.optBoolean("strategyFollowed") else null,
                ),
            )
        }
        target.positions.clear()
        val positionRows = snapshot.optJSONArray("positions") ?: JSONArray()
        for (i in 0 until positionRows.length()) {
            val row = positionRows.optJSONObject(i) ?: continue
            fun readFills(key: String, exits: Boolean): List<PositionFill> {
                val rows = row.optJSONArray(key) ?: JSONArray()
                return (0 until rows.length()).mapNotNull { index ->
                    val fill = rows.optJSONObject(index) ?: return@mapNotNull null
                    val volume = fill.optDouble("volume", 0.0)
                    val price = fill.optDouble("price", 0.0)
                    if (!volume.isFinite() || volume <= 0.0 || !price.isFinite() || price <= 0.0) return@mapNotNull null
                    PositionFill(
                        id = fill.optLong("id", System.currentTimeMillis() + index),
                        volume = volume,
                        price = price,
                        realizedPnl = if (exits) fill.optDouble("realizedPnl", 0.0) else 0.0,
                        atMillis = fill.optLong("atMillis", 0L),
                    )
                }
            }
            val entries = readFills("entryFills", false)
            val exits = readFills("exitFills", true)
            if (entries.isNotEmpty() && exits.sumOf { it.volume } <= entries.sumOf { it.volume } + 1e-8) {
                target.positions.add(
                    JournalPosition(
                        id = row.optLong("id"),
                        accountId = row.optString("accountId", "default"),
                        symbol = row.optString("symbol", ""),
                        buy = row.optBoolean("buy", true),
                        strategy = row.optString("strategy", ""),
                        entryFills = entries,
                        exitFills = exits,
                        notes = row.optString("notes", ""),
                        openedAtMillis = row.optLong("openedAtMillis", 0L),
                    ),
                )
            }
        }
        target.strategies.clear()
        val strategyRows = snapshot.optJSONArray("strategies") ?: JSONArray()
        for (i in 0 until strategyRows.length()) {
            val row = strategyRows.optJSONObject(i) ?: continue
            val rules = row.optJSONArray("rules") ?: JSONArray()
            target.strategies.add(
                Strategy(
                    row.optLong("id"), row.optString("name"),
                    (0 until rules.length()).mapNotNull { rules.optString(it).takeIf(String::isNotBlank) },
                    row.optBoolean("active", true),
                    row.optInt("version", 1),
                ),
            )
        }
        target.strategyRevisions.clear()
        val revisionRows = snapshot.optJSONArray("strategyRevisions") ?: JSONArray()
        for (i in 0 until revisionRows.length()) {
            val row = revisionRows.optJSONObject(i) ?: continue
            val rules = row.optJSONArray("rules") ?: JSONArray()
            target.strategyRevisions.add(
                StrategyRevision(
                    id = row.optLong("id"),
                    strategyId = row.optLong("strategyId"),
                    version = row.optInt("version", 1),
                    name = row.optString("name"),
                    rules = (0 until rules.length()).mapNotNull { rules.optString(it).takeIf(String::isNotBlank) },
                    createdAt = row.optLong("createdAt"),
                ),
            )
        }
        target.hashtags.clear()
        val tagRows = snapshot.optJSONArray("hashtags") ?: JSONArray()
        for (i in 0 until tagRows.length()) tagRows.optString(i).takeIf(String::isNotBlank)?.let(target.hashtags::add)

        target.lessons.clear()
        val lessonRows = snapshot.optJSONArray("lessons") ?: JSONArray()
        for (i in 0 until lessonRows.length()) {
            val row = lessonRows.optJSONObject(i) ?: continue
            val kind = runCatching { ContentKind.valueOf(row.optString("kind")) }.getOrDefault(ContentKind.TEXT)
            target.lessons.add(Lesson(row.optLong("id"), row.optString("title"), kind, row.optDouble("progress").toFloat().coerceIn(0f, 1f)))
        }

        snapshot.optJSONObject("settings")?.let { s ->
            target.settings = AppSettings(
                language = s.optInt("language", 0), showGlobe = s.optBoolean("showGlobe", true),
                tzMode = s.optInt("tzMode", 2), reminderOn = s.optBoolean("reminderOn", true),
                reminderHour = s.optInt("reminderHour", 22).coerceIn(0, 23),
                newsImpact = s.optInt("newsImpact", 0), startBalance = s.optDouble("startBalance", 10000.0),
                defaultLot = s.optDouble("defaultLot", 0.1), riskPercent = s.optDouble("riskPercent", 1.0),
                dailyLossLimitPercent = s.optDouble("dailyLossLimitPercent", 3.0).coerceIn(0.5, 20.0),
                maxTradesPerDay = s.optInt("maxTradesPerDay", 5).coerceIn(1, 100),
                haptics = s.optBoolean("haptics", true), animations = s.optInt("animations", 1),
                accent = s.optInt("accent", 0), weekendHoliday = s.optBoolean("weekendHoliday", true),
                sessionAlerts = s.optBoolean("sessionAlerts", true), newsAlerts = s.optBoolean("newsAlerts", true),
                autoBackup = s.optBoolean("autoBackup", false), hideBalance = s.optBoolean("hideBalance", false),
                profileFirstName = s.optString("profileFirstName", ""),
                profileLastName = s.optString("profileLastName", ""),
                profilePhotoUri = s.optString("profilePhotoUri", ""),
            )
        }
    }
}

internal data class MutableSnapshot(
    val trades: MutableList<Trade>,
    val positions: MutableList<JournalPosition>,
    val accounts: MutableList<TradingAccount>,
    val strategies: MutableList<Strategy>,
    val strategyRevisions: MutableList<StrategyRevision>,
    val hashtags: MutableList<String>,
    val lessons: MutableList<Lesson>,
    var settings: AppSettings,
    var selectedAccountId: String,
    val txAiEpisodeReviews: MutableList<TxAiEpisodeReview>,
    val txAiInsightPins: MutableList<TxAiInsightPin>,
)

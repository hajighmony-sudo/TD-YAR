package com.tradenex.app.data

import com.tradenex.app.core.SessionEngine
import java.time.Instant

/** Observable journal patterns; these labels are not diagnoses or causal claims. */
enum class TxAiBehaviorPatternType(val label: String) {
    OVERTRADING_AFTER_WIN("افزایش تراکم معاملات پس از برد"),
    SIZE_UP_AFTER_WIN("افزایش حجم پس از برد"),
    SIZE_UP_AFTER_LOSS("افزایش حجم پس از زیان"),
    EXIT_BEFORE_TARGET("خروج ثبت‌شده پیش از حدسود"),
    SESSION_DRIFT("معامله خارج از سشن مجاز استراتژی"),
}

data class TxAiBehaviorPattern(
    val id: String,
    val accountId: String,
    val type: TxAiBehaviorPatternType,
    val triggerTradeId: Long,
    val linkedTradeIds: List<Long>,
    val evidenceScore: Int,
    val actualNetPnl: Double,
    val reasons: List<String>,
)

/**
 * Conservative detectors that only use explicit journal fields.
 * Stop widening and true early-exit path analysis are intentionally omitted:
 * the current journal does not retain stop-edit events or intratrade price paths.
 */
object TxAiBehavioralPatternAnalyzer {
    private const val ACTIVITY_WINDOW_MILLIS = 2 * 60 * 60 * 1000L
    private const val MIN_POST_WIN_TRADES = 3
    private const val INCREASE_THRESHOLD = 1.25

    fun analyze(
        trades: List<Trade>,
        strategies: List<Strategy> = emptyList(),
        revisions: List<StrategyRevision> = emptyList(),
    ): List<TxAiBehaviorPattern> {
        val output = mutableListOf<TxAiBehaviorPattern>()
        trades.groupBy { it.accountId }.forEach { (accountId, accountTrades) ->
            val ordered = accountTrades.sortedWith(
                compareBy<Trade> { it.openedAtMillis.takeIf { value -> value > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.epochDay }
                    .thenBy { it.id },
            )

            val coveredActivityTrades = mutableSetOf<Long>()
            ordered.forEach { winner ->
                val closedAt = winner.closedAtMillis
                if (winner.pnl <= 0.0 || closedAt <= 0L) return@forEach
                val following = ordered.filter { trade ->
                    trade.id != winner.id &&
                        trade.openedAtMillis >= closedAt &&
                        trade.openedAtMillis <= closedAt + ACTIVITY_WINDOW_MILLIS
                }.sortedBy { it.openedAtMillis }
                if (following.size < MIN_POST_WIN_TRADES) return@forEach
                val cluster = following.take(MIN_POST_WIN_TRADES)
                if (cluster.any { it.id in coveredActivityTrades }) return@forEach
                coveredActivityTrades.addAll(cluster.map { it.id })
                val linked = listOf(winner.id) + cluster.map { it.id }
                val score = (25 + (following.size - MIN_POST_WIN_TRADES).coerceAtMost(4) * 10).coerceAtMost(65)
                output += TxAiBehaviorPattern(
                    id = "$accountId:post-win-activity:${winner.id}",
                    accountId = accountId,
                    type = TxAiBehaviorPatternType.OVERTRADING_AFTER_WIN,
                    triggerTradeId = cluster.first().id,
                    linkedTradeIds = linked,
                    evidenceScore = score,
                    actualNetPnl = (listOf(winner) + cluster).sumOf { it.pnl },
                    reasons = listOf(
                        "پس از معامله برنده، ${following.size} معامله در بازه حداکثر دو ساعت ثبت شده است.",
                        "این شاخص فقط تراکم فعالیت را نشان می‌دهد و به‌تنهایی اثبات‌کننده Overtrading نیست.",
                    ),
                )
            }

            ordered.zipWithNext().forEach { (previous, current) ->
                if (previous.pnl <= 0.0 || previous.closedAtMillis <= 0L ||
                    current.openedAtMillis < previous.closedAtMillis ||
                    previous.lot <= 0.0 || current.lot <= previous.lot * INCREASE_THRESHOLD
                ) return@forEach
                output += TxAiBehaviorPattern(
                    id = "$accountId:size-up-after-win:${current.id}",
                    accountId = accountId,
                    type = TxAiBehaviorPatternType.SIZE_UP_AFTER_WIN,
                    triggerTradeId = current.id,
                    linkedTradeIds = listOf(previous.id, current.id),
                    evidenceScore = 25,
                    actualNetPnl = previous.pnl + current.pnl,
                    reasons = listOf(
                        "حجم معامله بعد از برد قبلی بیش از ۲۵٪ افزایش یافته است.",
                        "افزایش حجم یک رفتار قابل مشاهده است؛ انگیزه یا اعتمادبه‌نفس از آن استنباط نمی‌شود.",
                    ),
                )
            }

            if (strategies.isNotEmpty()) {
                ordered.forEach { trade ->
                    if (trade.openedAtMillis <= 0L || trade.strategy.isBlank()) return@forEach
                    val revision = revisions.lastOrNull {
                        it.name.equals(trade.strategy, ignoreCase = true) &&
                            it.version == trade.strategyVersion
                    }
                    val strategy = strategies.firstOrNull {
                        it.name.equals(trade.strategy, ignoreCase = true)
                    }
                    val rules = revision?.rules
                        ?: strategy?.takeIf { it.version == trade.strategyVersion }?.rules
                        ?: emptyList()
                    val ruleText = rules.joinToString(" ").lowercase()
                    val allowedSessions = listOf(
                        "Sydney" to listOf("sydney", "سیدنی"),
                        "Tokyo" to listOf("tokyo", "توکیو"),
                        "London" to listOf("london", "لندن"),
                        "New York" to listOf("new york", "نیویورک"),
                    ).filter { (_, terms) -> terms.any { term -> ruleText.contains(term) } }
                        .map { it.first }
                        .toSet()
                    if (allowedSessions.isEmpty()) return@forEach
                    val instant = Instant.ofEpochMilli(trade.openedAtMillis)
                    val activeSessions = SessionEngine.defs.filter { definition ->
                        val local = instant.atZone(definition.zoneId).toLocalTime()
                        if (definition.localOpen < definition.localClose) {
                            !local.isBefore(definition.localOpen) && local.isBefore(definition.localClose)
                        } else {
                            !local.isBefore(definition.localOpen) || local.isBefore(definition.localClose)
                        }
                    }.map { it.name }
                    if (activeSessions.any { it in allowedSessions }) return@forEach
                    val actualSession = activeSessions.ifEmpty { listOf("بین سشن‌ها") }
                    output += TxAiBehaviorPattern(
                        id = "$accountId:session-drift:${trade.id}",
                        accountId = accountId,
                        type = TxAiBehaviorPatternType.SESSION_DRIFT,
                        triggerTradeId = trade.id,
                        linkedTradeIds = listOf(trade.id),
                        evidenceScore = 30,
                        actualNetPnl = trade.pnl,
                        reasons = listOf(
                            "زمان ورود در ${actualSession.joinToString(" / ")} ثبت شده، اما قوانین نسخه استراتژی فقط این سشن‌ها را مجاز دانسته‌اند: ${allowedSessions.joinToString("، ")}.",
                            "در هم‌پوشانی سشن‌ها، وجود هر سشن مجاز مانع هشدار اشتباه می‌شود.",
                        ),
                    )
                }
            }

            ordered.zipWithNext().forEach { (previous, current) ->
                if (previous.pnl >= 0.0 || previous.closedAtMillis <= 0L ||
                    current.openedAtMillis < previous.closedAtMillis ||
                    previous.lot <= 0.0 || current.lot <= previous.lot * INCREASE_THRESHOLD
                ) return@forEach
                output += TxAiBehaviorPattern(
                    id = "$accountId:size-up-after-loss:${current.id}",
                    accountId = accountId,
                    type = TxAiBehaviorPatternType.SIZE_UP_AFTER_LOSS,
                    triggerTradeId = current.id,
                    linkedTradeIds = listOf(previous.id, current.id),
                    evidenceScore = 25,
                    actualNetPnl = previous.pnl + current.pnl,
                    reasons = listOf(
                        "حجم معامله بعد از زیان قبلی بیش از ۲۵٪ افزایش یافته است.",
                        "این تغییر حجم به‌تنهایی اثبات‌کننده معامله انتقامی نیست.",
                    ),
                )
            }

            ordered.forEach { trade ->
                val entry = trade.entryPrice
                val exit = trade.exitPrice
                val target = trade.takeProfitPrice
                if (trade.pnl <= 0.0 || entry == null || exit == null || target == null ||
                    !entry.isFinite() || !exit.isFinite() || !target.isFinite()
                ) return@forEach
                val beforeTarget = if (trade.buy) {
                    target > entry && exit > entry && exit < target
                } else {
                    target < entry && exit < entry && exit > target
                }
                if (!beforeTarget) return@forEach
                output += TxAiBehaviorPattern(
                    id = "$accountId:exit-before-target:${trade.id}",
                    accountId = accountId,
                    type = TxAiBehaviorPatternType.EXIT_BEFORE_TARGET,
                    triggerTradeId = trade.id,
                    linkedTradeIds = listOf(trade.id),
                    evidenceScore = 20,
                    actualNetPnl = trade.pnl,
                    reasons = listOf(
                        "قیمت خروج ثبت‌شده در مسیر سود بوده اما به حدسود برنامه‌ریزی‌شده نرسیده است.",
                        "بدون مسیر قیمت درون معامله، نمی‌توان نتیجه گرفت حدسود هرگز لمس نشده است.",
                    ),
                )
            }
        }
        return output.sortedWith(compareByDescending<TxAiBehaviorPattern> { it.evidenceScore }.thenBy { it.id })
    }
}

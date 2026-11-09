package com.tradenex.app.data

/**
 * Explainable, deterministic revenge-trading pattern detector.
 *
 * A score measures recorded evidence only; it is NOT a probability or a diagnosis.
 * Missing timestamps/risk/volume are ignored rather than guessed.
 */
enum class RevengeClass { USER_TAGGED, STRONG_EVIDENCE, POSSIBLE, LOSS_SEQUENCE_ONLY }

data class RevengeEvidence(
    val tradeId: Long,
    val previousTradeId: Long,
    val gapMinutes: Long?,
    val previousWasLoss: Boolean,
    val rapidReentry: Boolean,
    val riskIncreased: Boolean,
    val volumeIncreased: Boolean,
    val explicitPlanViolation: Boolean,
    val explicitRevengeTag: Boolean,
    val emotionalSignal: Boolean,
    val consecutiveLosses: Int,
    val score: Int,
    val classification: RevengeClass,
    val reasons: List<String>,
)

data class RevengeEpisode(
    val id: String,
    val accountId: String,
    val triggerTradeId: Long,
    val previousLossTradeId: Long,
    val tradeIds: List<Long>,
    val actualNetPnl: Double,
    val evidence: RevengeEvidence,
)

object RevengeTradingAnalyzer {
    private const val RAPID_REENTRY_MINUTES = 15L
    private const val INCREASE_THRESHOLD = 1.25

    fun analyze(trades: List<Trade>): List<RevengeEpisode> {
        val episodes = mutableListOf<RevengeEpisode>()
        val byAccount = trades.groupBy { it.accountId }
        byAccount.forEach { (accountId, accountTrades) ->
            val ordered = accountTrades.sortedWith(
                compareBy<Trade> { it.openedAtMillis.takeIf { t -> t > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.epochDay }.thenBy { it.id },
            )
            for (index in ordered.indices) {
                val current = ordered[index]
                if (index == 0) continue
                val previous = ordered[index - 1]
                if (previous.pnl >= 0.0) continue
                // Count losses strictly before the current re-entry, not including it.
                var priorLossStreak = 0
                var cursor = index - 1
                while (cursor >= 0 && ordered[cursor].pnl < 0.0) {
                    priorLossStreak++
                    cursor--
                }

                val gap = if (current.openedAtMillis > 0L && previous.openedAtMillis > 0L &&
                    current.openedAtMillis >= previous.openedAtMillis
                ) (current.openedAtMillis - previous.openedAtMillis) / 60_000L else null
                val rapid = gap != null && gap <= RAPID_REENTRY_MINUTES
                val riskUp = increased(previous.plannedRisk, current.plannedRisk)
                val sizeUp = increased(previous.lot, current.lot)
                val tags = current.tags.map { it.lowercase() }
                val revengeTag = tags.any { it.contains("revenge") || it.contains("انتقام") }
                val planViolation = current.strategyFollowed == false
                val emotion = current.emotion.lowercase().let {
                    it.contains("anger") || it.contains("angry") || it.contains("خشم") ||
                        it.contains("عصبان") || it.contains("fomo")
                } || tags.any { it.contains("fomo") || it.contains("anger") || it.contains("خشم") }

                val hasBehavioralEvidence = riskUp || sizeUp || planViolation || revengeTag || emotion || priorLossStreak >= 2
                if (!hasBehavioralEvidence) continue

                val reasons = buildList {
                    if (rapid) add("ورود مجدد در فاصله $gap دقیقه پس از معامله زیان‌ده")
                    if (riskUp) add("افزایش ریسک برنامه‌ریزی‌شده بیش از ۲۵٪")
                    if (sizeUp) add("افزایش حجم بیش از ۲۵٪")
                    if (planViolation) add("ثبت صریح معامله به‌عنوان خارج از پلن")
                    if (revengeTag) add("تگ Revenge به‌صورت دستی ثبت شده")
                    if (emotion) add("ثبت حالت ذهنی یا تگ مرتبط با خشم/FOMO")
                    if (priorLossStreak >= 2) add("ورود پس از حداقل دو زیان متوالی")
                }
                val score = (
                    (if (rapid) 15 else 0) +
                    (if (riskUp) 20 else 0) +
                    (if (sizeUp) 10 else 0) +
                    (if (planViolation) 20 else 0) +
                    (if (revengeTag) 30 else 0) +
                    (if (emotion) 10 else 0) +
                    (if (priorLossStreak >= 2) 10 else 0)
                ).coerceAtMost(100)
                if (score == 0) continue

                val classification = when {
                    revengeTag -> RevengeClass.USER_TAGGED
                    score >= 45 -> RevengeClass.STRONG_EVIDENCE
                    score >= 15 -> RevengeClass.POSSIBLE
                    else -> RevengeClass.LOSS_SEQUENCE_ONLY
                }
                val evidence = RevengeEvidence(
                    tradeId = current.id, previousTradeId = previous.id, gapMinutes = gap,
                    previousWasLoss = true, rapidReentry = rapid, riskIncreased = riskUp,
                    volumeIncreased = sizeUp, explicitPlanViolation = planViolation,
                    explicitRevengeTag = revengeTag, emotionalSignal = emotion,
                    consecutiveLosses = priorLossStreak, score = score,
                    classification = classification, reasons = reasons,
                )
                episodes += RevengeEpisode(
                    id = "$accountId:${previous.id}:${current.id}",
                    accountId = accountId, triggerTradeId = current.id,
                    previousLossTradeId = previous.id, tradeIds = listOf(previous.id, current.id),
                    actualNetPnl = previous.pnl + current.pnl, evidence = evidence,
                )
            }
        }
        return episodes.sortedByDescending { it.evidence.score }
    }

    private fun increased(previous: Double?, current: Double?): Boolean =
        previous != null && current != null && previous > 0.0 &&
            current > previous * INCREASE_THRESHOLD

    private fun increased(previous: Double, current: Double): Boolean =
        previous > 0.0 && current > previous * INCREASE_THRESHOLD
}

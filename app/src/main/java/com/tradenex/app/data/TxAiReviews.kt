package com.tradenex.app.data

/** User decisions about a behavioral episode. These are annotations, not changes to trade facts. */
enum class TxAiReviewStatus(val storageKey: String) {
    CONFIRMED("confirmed"),
    NOT_REVENGE("not_revenge"),
    UNSURE("unsure");

    companion object {
        fun fromStorageKey(value: String?): TxAiReviewStatus? =
            entries.firstOrNull { it.storageKey == value }
    }
}

data class TxAiEpisodeReview(
    val accountId: String,
    val episodeId: String,
    val status: TxAiReviewStatus,
    val updatedAtMillis: Long,
)

data class TxAiInsightPin(
    val accountId: String,
    val insightId: String,
    val pinned: Boolean,
)

/** Pure account-scoped update helpers, kept deterministic for persistence tests. */
object TxAiReviewRules {
    fun reviewFor(
        reviews: List<TxAiEpisodeReview>,
        accountId: String,
        episodeId: String,
    ): TxAiReviewStatus? = reviews.lastOrNull {
        it.accountId == accountId && it.episodeId == episodeId
    }?.status

    fun isPinned(
        pins: List<TxAiInsightPin>,
        accountId: String,
        insightId: String,
    ): Boolean = pins.lastOrNull {
        it.accountId == accountId && it.insightId == insightId
    }?.pinned == true

    fun upsertReview(
        reviews: List<TxAiEpisodeReview>,
        review: TxAiEpisodeReview,
    ): List<TxAiEpisodeReview> = reviews
        .filterNot { it.accountId == review.accountId && it.episodeId == review.episodeId } + review

    fun setPinned(
        pins: List<TxAiInsightPin>,
        accountId: String,
        insightId: String,
        pinned: Boolean,
    ): List<TxAiInsightPin> = pins
        .filterNot { it.accountId == accountId && it.insightId == insightId } +
        TxAiInsightPin(accountId, insightId, pinned)
}

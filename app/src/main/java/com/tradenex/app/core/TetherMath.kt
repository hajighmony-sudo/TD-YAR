package com.tradenex.app.core

/** Pure USDT (Tether) arithmetic for the calculator. Prices are in Toman per 1 USDT; fees are percentages. */
object TetherMath {
    /** Toman needed to buy [usdt] at [buyPrice] including the exchange [feePct] (fee taken in USDT, so you pay for more). */
    fun tomanToBuy(usdt: Double, buyPrice: Double, feePct: Double): Double = usdt * buyPrice / (1 - feePct / 100.0)

    /** USDT received for [toman] at [buyPrice] after the exchange fee. */
    fun usdtForToman(toman: Double, buyPrice: Double, feePct: Double): Double =
        if (buyPrice <= 0) 0.0 else toman / buyPrice * (1 - feePct / 100.0)

    /** Toman received for selling [usdt] at [sellPrice] after the exchange fee. */
    fun tomanForSell(usdt: Double, sellPrice: Double, feePct: Double): Double = usdt * sellPrice * (1 - feePct / 100.0)

    /** USDT that actually arrives after a fixed network fee (e.g. TRC20 ≈ 1 USDT) is deducted. */
    fun afterNetworkFee(usdt: Double, networkFee: Double): Double = (usdt - networkFee).coerceAtLeast(0.0)

    data class RoundTrip(val spent: Double, val received: Double, val profit: Double, val profitPct: Double, val breakEvenSell: Double)

    /** Buy [usdt] at [buy], later sell at [sell]; both legs pay [feePct]. */
    fun roundTrip(usdt: Double, buy: Double, sell: Double, feePct: Double): RoundTrip {
        val spent = tomanToBuy(usdt, buy, feePct)
        val received = tomanForSell(usdt, sell, feePct)
        val profit = received - spent
        val f = 1 - feePct / 100.0
        return RoundTrip(spent, received, profit, if (spent == 0.0) 0.0 else profit / spent * 100, if (f <= 0) 0.0 else buy / (f * f))
    }

    /** Spread between the exchange's sell price (you buy at it) and buy price (you sell at it), in percent. */
    fun spreadPct(ask: Double, bid: Double): Double = if (ask <= 0) 0.0 else (ask - bid) / ask * 100
}

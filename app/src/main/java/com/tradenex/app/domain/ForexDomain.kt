package com.tradenex.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Pure Forex-domain primitives. This file is intentionally independent of Compose and the
 * current UI so it can be adopted screen-by-screen without changing the existing theme.
 */
enum class FxSide { BUY, SELL }

data class FxInstrument(
    val symbol: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val pipSize: BigDecimal,
    val contractSize: BigDecimal = BigDecimal("100000"),
) {
    init {
        require(symbol.matches(Regex("[A-Z]{6}"))) { "Forex symbols must be six-letter currency pairs." }
        require(baseCurrency in CURRENCIES && quoteCurrency in CURRENCIES) {
            "Only ISO fiat currency pairs are supported; crypto and metals are not Forex pairs."
        }
        require(baseCurrency != quoteCurrency)
        require(pipSize > BigDecimal.ZERO)
        require(contractSize > BigDecimal.ZERO)
        require(symbol == baseCurrency + quoteCurrency)
    }

    companion object {
        private val CURRENCIES = setOf(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD",
            "SEK", "NOK", "DKK", "SGD", "HKD", "CNH", "CNY", "MXN",
            "ZAR", "TRY", "PLN", "HUF", "CZK", "ILS", "THB", "INR",
        )

        /** Standard FX pip convention; broker-specific metadata should override when necessary. */
        fun standard(symbol: String, contractSize: BigDecimal = BigDecimal("100000")): FxInstrument {
            val normalized = symbol.uppercase(Locale.ROOT)
            require(normalized.matches(Regex("[A-Z]{6}"))) {
                "Only six-letter Forex currency pairs are supported."
            }
            val base = normalized.substring(0, 3)
            val quote = normalized.substring(3, 6)
            require(base in CURRENCIES && quote in CURRENCIES && base != quote) {
                "Only ISO fiat currency pairs are supported; crypto and metals are not Forex pairs."
            }
            val pip = if (quote == "JPY") BigDecimal("0.01") else BigDecimal("0.0001")
            return FxInstrument(normalized, base, quote, pip, contractSize)
        }
    }
}

data class FxPnlInput(
    val instrument: FxInstrument,
    val side: FxSide,
    val lots: BigDecimal,
    val entryPrice: BigDecimal,
    val exitPrice: BigDecimal,
    /** Units of account currency per one unit of quote currency. Must be supplied explicitly. */
    val quoteToAccountRate: BigDecimal,
    /** Costs below are already expressed in account currency. */
    val commissionAccountCurrency: BigDecimal = BigDecimal.ZERO,
    val swapAccountCurrency: BigDecimal = BigDecimal.ZERO,
    val otherFeesAccountCurrency: BigDecimal = BigDecimal.ZERO,
    /** Optional planned risk in account currency. */
    val plannedRiskAccountCurrency: BigDecimal? = null,
)

data class FxPnlResult(
    val pips: BigDecimal,
    val grossQuoteCurrency: BigDecimal,
    val grossAccountCurrency: BigDecimal,
    val totalCostsAccountCurrency: BigDecimal,
    val netAccountCurrency: BigDecimal,
    val pipValueQuoteCurrency: BigDecimal,
    val rMultiple: BigDecimal?,
)

/**
 * Calculates a fully closed FX trade. No conversion rate is inferred and no missing value is
 * silently defaulted. Costs are supplied in account currency to avoid mixing currencies.
 */
object ForexPnlCalculator {
    private val moneyScale = 8
    private val displayScale = 2

    fun calculate(input: FxPnlInput): FxPnlResult {
        require(input.lots > BigDecimal.ZERO) { "Lots must be greater than zero." }
        require(input.entryPrice > BigDecimal.ZERO && input.exitPrice > BigDecimal.ZERO) {
            "Entry and exit prices must be greater than zero."
        }
        require(input.quoteToAccountRate > BigDecimal.ZERO) {
            "A valid quote-to-account conversion rate is required."
        }
        require(input.plannedRiskAccountCurrency == null ||
            input.plannedRiskAccountCurrency > BigDecimal.ZERO) {
            "Planned risk must be greater than zero."
        }

        val signedPriceMove = when (input.side) {
            FxSide.BUY -> input.exitPrice.subtract(input.entryPrice)
            FxSide.SELL -> input.entryPrice.subtract(input.exitPrice)
        }
        val units = input.instrument.contractSize.multiply(input.lots)
        val grossQuote = signedPriceMove.multiply(units)
        val pipValueQuote = input.instrument.pipSize.multiply(units)
        val pips = signedPriceMove.divide(input.instrument.pipSize, moneyScale, RoundingMode.HALF_UP)
        val grossAccount = grossQuote.multiply(input.quoteToAccountRate)
        val costs = input.commissionAccountCurrency
            .add(input.swapAccountCurrency)
            .add(input.otherFeesAccountCurrency)
        val net = grossAccount.subtract(costs)
        val r = input.plannedRiskAccountCurrency?.let {
            net.divide(it, 6, RoundingMode.HALF_UP)
        }

        return FxPnlResult(
            pips = pips.stripTrailingZeros(),
            grossQuoteCurrency = grossQuote.setScale(moneyScale, RoundingMode.HALF_UP),
            grossAccountCurrency = grossAccount.setScale(displayScale, RoundingMode.HALF_UP),
            totalCostsAccountCurrency = costs.setScale(displayScale, RoundingMode.HALF_UP),
            netAccountCurrency = net.setScale(displayScale, RoundingMode.HALF_UP),
            pipValueQuoteCurrency = pipValueQuote.setScale(moneyScale, RoundingMode.HALF_UP),
            rMultiple = r,
        )
    }
}

/** A session is defined by its own IANA timezone, not a fixed UTC offset. */
data class FxSessionDefinition(
    val id: String,
    val displayName: String,
    val zoneId: ZoneId,
    val localOpen: LocalTime,
    val localClose: LocalTime,
) {
    fun stateAt(instant: Instant): FxSessionState {
        val localNow = instant.atZone(zoneId)
        val time = localNow.toLocalTime()
        val open = isWithin(time)
        val nextBoundary = nextBoundaryAfter(localNow)
        val seconds = java.time.Duration.between(localNow, nextBoundary).seconds.coerceAtLeast(0)
        return FxSessionState(
            id = id,
            displayName = displayName,
            isOpen = open,
            localTime = localNow,
            secondsToNextChange = seconds,
            nextChangeAt = nextBoundary.toInstant(),
        )
    }

    private fun isWithin(time: LocalTime): Boolean =
        if (localOpen == localClose) true
        else if (localOpen < localClose) !time.isBefore(localOpen) && time.isBefore(localClose)
        else !time.isBefore(localOpen) || time.isBefore(localClose)

    private fun nextBoundaryAfter(now: ZonedDateTime): ZonedDateTime {
        val todayOpen = now.toLocalDate().atTime(localOpen).atZone(zoneId)
        val todayClose = now.toLocalDate().atTime(localClose).atZone(zoneId)
        val candidates = listOf(todayOpen, todayClose,
            todayOpen.plusDays(1), todayClose.plusDays(1))
        return candidates.filter { it.isAfter(now) }.minOrNull()
            ?: now.plusDays(1)
    }
}

data class FxSessionState(
    val id: String,
    val displayName: String,
    val isOpen: Boolean,
    val localTime: ZonedDateTime,
    val secondsToNextChange: Long,
    val nextChangeAt: Instant,
)

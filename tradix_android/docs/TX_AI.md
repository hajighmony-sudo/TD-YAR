# TX AI, trade analytics, and Persian calendar

## Scope and navigation

TX AI is available from the app's More / hamburger-style menu and uses only the selected account's saved journal trades. It is an explainable local analytics engine; it does not send data to an external AI provider, issue trade signals, or execute orders.

## Behavioral profile and breakdowns

The report groups recorded trades by:
- Forex session, using each session's IANA timezone and daylight-saving rules
- weekday, calendar day, and Monday-start trading week
- recorded timeframe and market condition
- recorded emotional state and symbol
- named strategy

A best-pattern insight is withheld until at least five matching trades exist. A missing timestamp, timeframe, market condition, emotion, or compliance value is not guessed or backfilled.

## Counterfactual scenarios

### Excluding off-plan trades

A trade is classified as off-plan only when the user explicitly marks it as outside the plan or records a behavior tag such as FOMO, Revenge, No-plan, or Off-strategy. A blank strategy name alone is not treated as proof of a rule violation.

The hypothetical result is the sum of P&L for trades not classified as off-plan. The report also displays the difference from actual recorded P&L.

### Reducing risk by 50%

The illustrative reduced-risk result is 50% of recorded net P&L. This assumes P&L scales linearly with position size. It is not a market replay or broker execution simulation and does not model margin, slippage, liquidity, or changed trader behavior.

## Monthly progress scores

Each score is independent and may be withheld when data coverage is insufficient.

| Score | Method |
|---|---|
| Discipline | Requires at least five monthly trades and behavioral tags on at least 70% of them; score is reduced by recorded FOMO, Revenge, Overtrading, No-plan, or Off-strategy flags. |
| Risk management | Requires at least three trades with a recorded planned cash risk; checks planned risk against the account's configured risk budget. |
| Strategy adherence | Requires at least five monthly trades, at least three explicit compliance records, and at least 70% coverage; score is the share marked as following the plan. |
| Decision quality | Share of trades with entry, exit, stop-loss, and strategy recorded; requires at least five monthly trades. |

The six-month trend includes months with no trades; missing data is shown as unavailable, not as zero performance.

## Trade-based equity candles

The performance chart now creates one candle for every recorded trade in the selected period. Each candle opens at equity before that trade and closes at equity after its recorded P&L. Since the journal does not contain intratrade OHLC or tick data, the wick uses only the two observable equity values; no intratrade high/low is invented. The chart shows up to the latest 60 trades with compact candle bodies.

## Heatmap and Jalali calendar

The performance heatmap scales color intensity against the largest absolute daily P&L in its recent window instead of fixed account-currency thresholds. Its day labels use Jalali dates.

The trade calendar uses Jalali month boundaries, Persian date labels, Saturday-first week layout, month navigation, and a selectable day detail panel listing that day's saved trades.

## Inspiration from public GitHub projects

The feature review used public project documentation as design references, not as copied implementation:
- [MyTradeBook](https://github.com/najamaltaf88/MyTradeBook): account-scoped journaling, psychology context, strategy review, heatmaps, and coaching workflows.
- [TradeLedger](https://github.com/manju0912/tradeledger): calendar P&L heatmap, symbol breakdowns, and drill-down from analytics to trade records.
- [Trade Ledger Pro](https://github.com/TheCodeWiz/Trade-Ledger-pro): drawdown, risk/reward, daily/weekly/monthly performance views, and coaching concepts.

## Explicit limitations

- The economic-news tab has no live provider configured. The UI does not display fabricated sample events; a trusted news feed/API integration is still required.
- No intratrade OHLC is available from manual journal entries. A future broker/MT connector could supply that data.
- TX AI is local deterministic analytics, not an LLM-based coach. This keeps current analysis offline and reproducible.
- Historical trades without explicit strategy compliance remain unclassified; no inferred compliance score is created.


## Behavioral Lab — Revenge Trading (initial implementation)

The first local detector is implemented in `RevengeTradingAnalyzer`. It is account-scoped and creates a linked pair consisting of the prior losing trade and the subsequent trigger trade. It records the time gap, whether planned risk or lot size increased by more than 25%, explicit off-plan status, Revenge/FOMO/anger tags or emotion text, and the consecutive-loss count before the re-entry.

The evidence score uses deterministic rule weights: rapid re-entry (15), risk increase (20), size increase (10), explicit plan violation (20), explicit Revenge tag (30), anger/FOMO signal (10), and two or more prior consecutive losses (10), capped at 100. This is an evidence index, not a psychological probability. A user-entered Revenge tag is labeled separately as `USER_TAGGED`; algorithmic evidence is never represented as user confirmation.

The TX AI screen currently shows the linked previous-loss and trigger trades, their recorded P&L, the observed time gap, the evidence reasons and the evidence score. It does not aggregate episode P&L into an account-level cost because overlapping pairs could double-count a trade. The current detector is intentionally conservative: missing timestamps, risk or emotion are not inferred.

Not yet implemented in this first slice: persisted user review states, dedicated episode filtering/drill-down, overlapping-episode consolidation, and the remaining Behavioral Lab detectors (overtrading after wins, early exit, widened stop, session drift and overconfidence). See [TX_AI_MASTER_SPEC.md](TX_AI_MASTER_SPEC.md) for the full target inventory and delivery gates.


## Extended metrics and user review persistence


## Extended metrics and user review persistence

The Android TX AI summary reads the extended metric vector from the Rust native core (with a parity Kotlin fallback):

1. Net realized P&L.
2. Win rate over finite realized P&L records.
3. Profit factor (gross profit / gross loss; capped display sentinel 99 when there are no losses).
4. Maximum peak-to-trough drawdown over the ordered realized-P&L sequence, starting from zero.
5. Expectancy (mean realized P&L per valid trade).
6. Average winning trade.
7. Average losing trade as an absolute amount (the UI displays it as a negative amount).
8. Recovery factor (net / absolute maximum drawdown; unavailable-like zero when drawdown is zero).
9. Largest winning trade as a share of gross winning P&L.
10. Largest losing trade as a share of gross losing P&L.

Non-finite P&L values are excluded. These metrics describe the order supplied by the journal and do not represent intratrade equity or market OHLC.

Behavioral episode reviews and pinned insights are stored locally with account-scoped keys. Review values are user annotations and never rewrite source trade fields. Legacy snapshots without these arrays restore with no reviews or pins.

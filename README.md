# TD-YAR

A Flutter/Dart local-first **manual trading journal and trader reflection companion**.

## Product scope

TD-YAR is a record-keeping, review, and learning tool. Users manually enter all trade records.

The five core foundations now include:
1. Manual trade journal with long/short trades, multiple entry/exit fills, FIFO matching, fees, fill notes, tags, and trade notes.
2. Performance analytics with dated partial-exit realizations, date-range reporting, drawdown, expectancy, profit factor, and breakdowns by symbol, strategy, account, direction, weekday, and hour.
3. User-authored strategy library with setup/entry/exit/risk rules, checklists, tags, archive lifecycle, and trade association.
4. Daily journal with pre-market plan, market context, execution review, mistakes, strengths, lessons, next-day plan, goals, tags, and linked trades.
5. Timezone-aware market sessions for Sydney, Tokyo, London, and New York, including open/closed status, next session times, custom schedules, overnight sessions, daylight-saving changes, and overlap windows.

Additional foundations:
- Personal risk profiles and per-trade/daily limit checks
- Trading psychology journal and descriptive insights
- Daily, weekly, monthly, and custom-range trading calendar
- Local SQLite persistence and versioned migrations (schema version 5)

Explicitly out of scope:
- Exchange, broker, or MetaTrader connections
- Live market data or automated price retrieval
- Order placement or automated trading
- Trade signals or investment recommendations
- UI/UX screens (presentation layer is intentionally deferred)

## Architecture

- `lib/core/domain`: entities and deterministic calculations
- `lib/core/data`: repository contracts and SQLite implementations
- `lib/features`: reserved for future presentation modules
- `test`: domain unit tests

The domain layer does not depend on Flutter widgets. SQLite is the initial local-first storage layer. The session clock uses the IANA timezone database so market-local hours account for daylight-saving transitions.

## Getting started

Requires Flutter and Dart from a current stable Flutter SDK.

```bash
flutter pub get
flutter analyze
flutter test
```

## Accounting and analytics notes

Executions are manually recorded as entry or exit fills. Exits are matched to entry fills using FIFO within each trade record. Each exit fill creates a dated realization, so partial exits contribute to the correct day and reporting range. Fees are allocated proportionally to the quantity closed. Reports include realized gross/net P&L, fees, win rate, average win/loss, profit factor, expectancy, monetary drawdown, average duration, and breakdowns by symbol, strategy, account, direction, weekday, and hour.

Unrealized P&L is not estimated because this phase has no market-data feed. Daily loss-limit checks sum losing realizations separately, so winning trades do not conceal losses when checking that limit. Metrics are bookkeeping calculations from user-entered values, not forecasts or trading advice.

Psychology insights summarize self-reported journal data and compare linked realized outcomes. These comparisons are descriptive only; they do not establish causation or assess mental health.

## Session time rules

Session definitions store an IANA timezone and local open/close times. Status and overlap calculations are converted to UTC instants internally, including daylight-saving transitions. Default session hours are configurable starting points, not exchange guarantees or real-time exchange status feeds.

## Inspiration and licensing

The product workflow takes inspiration from public feature concepts and the separation of a pure calculation engine in [LuxAlgo Trade Journal](https://github.com/LuxAlgo/trade-journal). TD-YAR is an independent Flutter/Dart implementation; no LuxAlgo source code or branding is copied. Review third-party licenses before adding dependencies or source code.

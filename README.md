# TD-YAR

A Flutter/Dart local-first **manual trading journal and trader reflection companion**.

## Product scope

TD-YAR is a record-keeping, review, and learning tool. Users manually enter all trade records.

Included foundations:
- Manual trade journal with long/short trades, multiple entry/exit fills, FIFO matching, fees, fill notes, tags, and trade notes
- Editing and deletion of fills with full-history validation
- Trade filtering by symbol, creation range, direction, strategy, and account label
- Realized performance analytics, including date-correct partial-exit reporting
- Personal risk profiles, planned stop/target risk, per-trade and daily limit checks
- User-authored strategy library with lifecycle management and trade association
- Trading psychology journal with mood, focus, confidence, stress, plan adherence, FOMO and revenge-trade tags
- Descriptive behavior/outcome comparisons (not causal or diagnostic conclusions)
- Daily, Monday-to-Sunday weekly, monthly, and custom-range calendar summaries
- Local SQLite persistence and versioned migrations (schema version 4)

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

The domain layer does not depend on Flutter widgets, SQLite, or provider SDKs. SQLite is the initial local-first storage layer. Database migrations are versioned and preserve existing records.

## Getting started

Requires Flutter and Dart from a current stable Flutter SDK.

```bash
flutter pub get
flutter analyze
flutter test
```

## Accounting and analytics notes

Executions are manually recorded as entry or exit fills. Exits are matched to entry fills using FIFO within each trade record. Each exit fill creates a dated realization, so partial exits contribute to the correct day and reporting range. Fees are allocated proportionally to the quantity closed. Reports include realized gross/net P&L, fees, win rate, average win/loss, profit factor, expectancy, monetary drawdown, average duration, and breakdowns by symbol, strategy, weekday, and hour.

Unrealized P&L is not estimated because this phase has no market-data feed. A daily loss-limit check sums losing realizations separately, so winning trades do not conceal losses when checking that limit. Metrics are bookkeeping calculations from user-entered values, not forecasts or trading advice.

Psychology insights summarize self-reported journal data and compare linked realized outcomes. These comparisons are descriptive only; they do not establish causation or assess mental health.

## Inspiration and licensing

The product workflow takes inspiration from the public feature concepts and separation of a pure calculation engine in [LuxAlgo Trade Journal](https://github.com/LuxAlgo/trade-journal). TD-YAR is an independent Flutter/Dart implementation; no LuxAlgo source code or branding is copied. Review third-party licenses before adding dependencies or source code.

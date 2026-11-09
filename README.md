# TD-YAR

A Flutter/Dart local-first **manual trading journal and trader reflection companion**.

## Product scope

TD-YAR is a record-keeping, review, and learning tool. Users manually enter all trade records.

Included foundations:
- Manual trade journal and FIFO realized P&L
- Performance analytics
- Personal risk profiles and per-trade risk records
- User-authored strategy playbooks
- Trading psychology journal and descriptive insights
- Monthly trading calendar
- Local SQLite persistence and versioned migration

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

The domain layer does not depend on Flutter widgets, SQLite, or provider SDKs. SQLite is the initial local-first storage layer.

## Getting started

Requires Flutter and Dart from a current stable Flutter SDK.

```bash
flutter pub get
flutter analyze
flutter test
```

## P&L and analytics notes

Executions are manually recorded as entry or exit fills. Exits are matched to entry fills using FIFO within each trade record. The analytics engine reports realized gross/net P&L, allocated fees, win rate, average win/loss, profit factor, expectancy, maximum monetary drawdown, average duration, and breakdowns by symbol, strategy, weekday, and hour.

Unrealized P&L is not estimated because this phase has no market-data feed. Metrics are bookkeeping calculations from user-entered values, not forecasts or trading advice.

## Inspiration and licensing

The product workflow takes inspiration from the public feature concepts and separation of a pure calculation engine in [LuxAlgo Trade Journal](https://github.com/LuxAlgo/trade-journal). TD-YAR is an independent Flutter/Dart implementation; no LuxAlgo source code or branding is copied. Review third-party licenses before adding dependencies or source code.

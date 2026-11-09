# TD-YAR Architecture

## Product boundary

TD-YAR is a manual-first trading journal and trader reflection companion. All trade data is entered by the user. This repository phase does not connect to exchanges, brokers, MetaTrader, market-data feeds, or order execution services. It does not issue trade signals or investment recommendations.

## Architecture rules

- Feature and domain logic is plain Dart and independent of Flutter widgets.
- SQLite is the initial local-first persistence layer.
- Repositories isolate storage from domain services.
- Calculations are deterministic and based only on user-entered records.
- UI/UX is intentionally not included in this phase.
- Database changes must use explicit versioned migrations.

## Current domain modules

### 1. Manual trade journal

- `Trade` stores symbol, direction, account label, tags, notes, contract multiplier, and optional strategy association.
- `TradeExecution` stores entry/exit fills, quantity, price, fee, and execution timestamp.
- `TradeCalculator` matches exits to entry lots using FIFO and computes realized gross/net P&L, allocated fees, and remaining open quantity.
- Exits exceeding available open quantity are rejected.

### 2. Performance analytics

- `PerformanceAnalytics` aggregates closed/partially closed trade outcomes.
- Metrics include win rate, gross/net P&L, fees, average win/loss, profit factor, expectancy, maximum monetary drawdown, average duration, and breakdowns by symbol, strategy, weekday, and hour.
- `PerformanceAnalyticsService` loads records through `TradeRepository`.
- Open positions do not receive estimated unrealized P&L because no market-price feed exists.

### 3. Risk management

- `RiskProfile` stores a manually configured account balance, currency, per-trade risk limit, daily loss limit, and daily trade-count limit.
- `TradeRiskRecord` stores a planned entry, stop, target, quantity, balance snapshot, estimated monetary risk/reward, risk/reward ratio, and risk percentage.
- `RiskCalculator` validates long/short price relationships and checks configured limits. Results are informational bookkeeping, not recommendations.

### 4. Strategy manager

- `Strategy` stores the user's setup conditions, entry/exit/risk rules, timeframes, checklist, tags, and archive state.
- Strategies support create, update, list, archive, and delete.
- Trades can be associated with a strategy through `strategy_id`; deleting a strategy clears that association without deleting trade history.

### 5. Trading psychology

- `PsychologyEntry` records mood, confidence, focus, stress, plan adherence, FOMO/revenge-trade flags, emotions, notes, and an optional trade link.
- Ratings are validated on a 1–5 scale.
- `PsychologyAnalytics` summarizes the journal and compares linked realized outcomes by plan-adherence flag. This is descriptive correlation, not causal inference or mental-health assessment.

### 6. Trading calendar

- `TradingCalendar` builds a month of day summaries containing realized P&L, trade wins/losses, journal counts, and psychology-entry counts.
- `TradingCalendarService` composes performance, journal, and psychology repositories for a requested month.

## Persistence

SQLite schema version 2 includes:
- `trades`
- `trade_executions`
- `journal_entries`
- `playbooks`
- `attachments` (metadata only)
- `strategies`
- `risk_profiles`
- `trade_risk_records`
- `psychology_entries`

Version 1 databases are upgraded by adding `trades.strategy_id` and creating the new feature tables. The migration preserves existing trade and journal records.

## Validation

Run:

```bash
flutter pub get
flutter analyze
flutter test
```

The repository contains unit tests for FIFO/P&L calculations and initial analytics/risk rules. Tests must be run in a Flutter SDK environment before merging.

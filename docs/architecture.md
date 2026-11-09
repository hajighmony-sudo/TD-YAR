# TD-YAR Architecture

## Product boundary

TD-YAR is a manual-first trading journal and trader reflection companion. Users enter trades and all executions themselves. This phase has no exchange, broker, MetaTrader, market-data, or order-execution connection and provides no trading signals or investment recommendations.

## Architecture rules

- Domain logic is plain Dart and independent of Flutter widgets.
- SQLite is the initial local-first persistence layer.
- Repositories isolate storage from domain services.
- Calculations use only user-entered values.
- UI/UX is intentionally excluded from this phase.
- Database changes use explicit, data-preserving migrations.

## Five completed domain foundations

### 1. Manual trade journal

- `Trade` records symbol, long/short direction, account label, tags, notes, contract multiplier, and optional strategy association.
- `TradeExecution` records multiple entry/exit fills, quantity, price, fee, timestamp, and per-fill notes.
- Fills can be created, edited, and deleted. A mutation is accepted only when the complete resulting history remains valid.
- Direction and contract multiplier cannot be changed after fills exist because doing so would reinterpret historical P&L.
- Trades can be filtered by symbol, creation-date range, direction, strategy, and account.
- `TradeCalculator` validates finite values and unique fill IDs, matches exits FIFO, allocates fees proportionally, and rejects over-closing.

### 2. Performance analytics

- `PerformanceAnalytics` creates a dated realization for every exit fill, including partial exits.
- Date-range reports include only realizations in that range; each trade is aggregated once for trade-level statistics.
- Metrics include realized gross/net P&L, allocated fees, win rate, average win/loss, profit factor, expectancy, maximum monetary drawdown over the chronological realization stream, average duration, and breakdowns by symbol, strategy, weekday, and hour.
- `PerformanceAnalyticsService` loads data through `TradeRepository`.
- Open positions have no estimated unrealized P&L because there is no market-price feed.

### 3. Risk management

- `RiskProfile` stores account balance/currency and per-trade, daily-loss, and daily-trade-count limits.
- `TradeRiskRecord` stores planned entry/stop/target, quantity, balance snapshot, monetary risk/reward, R:R, and account risk percentage.
- Risk calculations validate long/short stop and target placement, finite numeric inputs, and profile bounds.
- Daily loss checks sum negative realized fills independently; winning fills do not offset losing fills for limit evaluation.
- A planned trade is checked against both the per-trade cap and remaining daily loss allowance.
- These are user-configured bookkeeping checks, not investment recommendations.

### 4. Strategy manager

- `Strategy` stores setup conditions, entry/exit/risk rules, timeframes, checklists, tags, market, description, and archive state.
- `StrategyManagementService` validates strategy data and enforces case-insensitive unique names.
- SQLite create/update checks uniqueness transactionally. Strategies can be listed, archived, restored, and deleted.
- Trades can be associated with a strategy. Deleting a strategy clears its trade associations without deleting trade history.

### 5. Trading psychology

- `PsychologyEntry` records mood, confidence, focus, stress, plan adherence, FOMO/revenge-trade flags, emotion tags, notes, and optional trade association.
- Ratings are constrained to 1–5 and entries can be created, edited, filtered, and deleted.
- `PsychologyAnalytics` summarizes averages, mood counts, plan adherence, FOMO/revenge counts, and linked realized P&L comparisons.
- These are descriptive correlations, not causal conclusions or mental-health assessments.

### Trading calendar

- `TradingCalendar` builds daily summaries for day, Monday-to-Sunday week, month, or custom range.
- Partial exits are assigned to their actual realization dates.
- Day summaries include realized net/gross P&L, fees, wins/losses, best/worst trade, average trade P&L, win rate, journal count, and psychology-entry count.

## Persistence

SQLite schema version 4 includes:
- `trades`
- `trade_executions`
- `journal_entries`
- `playbooks`
- `attachments` (metadata only)
- `strategies`
- `risk_profiles`
- `trade_risk_records`
- `psychology_entries`

Migrations add the strategy association, advanced feature tables and indexes, and execution notes. Existing trade and journal records are preserved.

## Validation

Run:

```bash
flutter pub get
flutter analyze
flutter test
```

CI is configured to run dependency resolution, static analysis, and tests on pull requests and pushes to the development branch.

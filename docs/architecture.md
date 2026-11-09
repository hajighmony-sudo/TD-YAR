# TD-YAR Architecture

## Product boundary

TD-YAR is a manual-first trading journal and trader reflection companion. Users enter trades and all executions themselves. This phase has no exchange, broker, MetaTrader, market-data, or order-execution connection and provides no trading signals or investment recommendations.

## Architecture rules

- Domain logic is plain Dart and independent of Flutter widgets.
- SQLite is the initial local-first persistence layer.
- Repositories isolate storage from domain services.
- Calculations use only user-entered values and timezone database rules.
- UI/UX is intentionally excluded from this phase.
- Database changes use explicit, data-preserving migrations.

## Five requested core foundations

### 1. Manual trade journal

- `Trade` records symbol, long/short direction, account label, tags, notes, contract multiplier, and optional strategy association.
- `TradeExecution` records multiple entry/exit fills, quantity, price, fee, timestamp, and per-fill notes.
- Fills can be created, edited, and deleted. A mutation is accepted only when the complete resulting FIFO history remains valid.
- Direction and contract multiplier cannot be changed after fills exist because doing so would reinterpret historical P&L.
- Trades can be filtered by symbol, creation-date range, direction, strategy, and account.
- `TradeCalculator` validates finite values and unique fill IDs, matches exits FIFO, allocates fees proportionally, and rejects over-closing.

### 2. Performance analytics

- `PerformanceAnalytics` creates a dated realization for every exit fill, including partial exits.
- Date-range reports include only realizations in that range; each trade is aggregated once for trade-level statistics.
- Metrics include realized gross/net P&L, allocated fees, win rate, average win/loss, profit factor, expectancy, maximum monetary drawdown over the chronological realization stream, average duration, and breakdowns by symbol, strategy, account, direction, weekday, and hour.
- `PerformanceAnalyticsService` loads records through `TradeRepository`.
- Open positions have no estimated unrealized P&L because there is no market-price feed.

### 3. Strategy manager

- `Strategy` stores setup conditions, entry/exit/risk rules, timeframes, checklists, tags, market, description, and archive state.
- `StrategyManagementService` validates strategy data and enforces case-insensitive unique names.
- SQLite create/update checks uniqueness transactionally. Strategies can be listed, archived, restored, and deleted.
- Trades can be associated with a strategy. Deleting a strategy clears its trade associations and strategy-owned attachment metadata without deleting trade history.

### 4. Daily journal

- `JournalEntry` supports a daily title and notes plus pre-market plan, market context, execution review, mistakes, strengths, lessons, next-day plan, goals, tags, and linked trades.
- Entries can be created, updated, retrieved by day or date range, and deleted.
- `DailyJournalService` provides the UI-independent lifecycle and date-range operations.
- `DailyJournalAnalytics` reports entry count, distinct active days, current/longest journaling streak, days with plans/reviews, unique linked trades, and tag frequency.

### 5. Trading sessions

- `TradingSessionDefinition` stores session name, IANA timezone, local open/close times, weekdays, and enabled state.
- Default definitions cover Sydney, Tokyo, London, and New York; users can add, edit, disable, and delete definitions.
- `TradingSessions` computes current open/closed status, current and next occurrence times, and pairwise overlap windows.
- The IANA timezone database is used for daylight-saving transitions and local session clocks. Overnight sessions are supported.
- Session overlap intervals are returned in UTC instants, ready for conversion by the presentation layer.

## Additional domain modules

### Risk management

- `RiskProfile` stores account balance/currency and per-trade, daily-loss, and daily-trade-count limits.
- `TradeRiskRecord` stores planned entry/stop/target, quantity, balance snapshot, monetary risk/reward, R:R, and account risk percentage.
- `RiskCalculator` validates long/short price relationships, finite numeric inputs, and profile bounds.
- `RiskManagementService` loads that day's realized exits and counts unique trades with entry fills that day. Daily losses are summed independently, so winning fills do not conceal losses.
- Planned risk is checked against both the per-trade cap and remaining daily loss allowance. These are informational bookkeeping checks, not recommendations.

### Trading psychology

- `PsychologyEntry` records mood, confidence, focus, stress, plan adherence, FOMO/revenge-trade flags, emotion tags, notes, and an optional trade link.
- Ratings are constrained to 1–5 and entries can be created, edited, filtered, and deleted.
- `PsychologyAnalytics` summarizes averages, mood counts, plan adherence, FOMO/revenge counts, and linked realized outcome comparisons.
- These are descriptive correlations, not causal conclusions or mental-health assessments.

### Trading calendar

- `TradingCalendar` builds daily summaries for day, Monday-to-Sunday week, month, or custom range.
- Partial exits are assigned to their actual realization dates.
- Day summaries include realized net/gross P&L, fees, wins/losses, best/worst trade, average trade P&L, win rate, journal count, and psychology-entry count.

## Persistence

SQLite schema version 5 includes:
- `trades`
- `trade_executions`
- `journal_entries` (daily review fields, goals and tags)
- `playbooks`
- `attachments` (metadata only)
- `strategies`
- `risk_profiles`
- `trade_risk_records`
- `psychology_entries`
- `trading_sessions`

Migrations add the strategy association, advanced feature tables and indexes, execution notes, daily journal fields, and session definitions. Existing trade and journal records are preserved.

## Validation

Run:

```bash
flutter pub get
flutter analyze
flutter test
```

CI runs dependency resolution, static analysis, and tests on pull requests.


## Modules 6–11
- **Psychology:** mood, confidence, focus, stress, plan adherence, FOMO/revenge flags, emotion tags and linked outcomes. Date filters are applied consistently to diary entries and realized outcomes; insights remain descriptive only.
- **Risk:** persisted risk profiles and per-trade records, planned risk/reward validation, daily loss and trade-count checks. These are bookkeeping guardrails, not recommendations.
- **Calendar:** daily, Monday-to-Sunday weekly, monthly, yearly and custom-range summaries, with partial exits assigned to actual realization dates.
- **Learning library:** text, image, video and mixed resources; category, caption, tags, favorites, source URL and related strategy IDs. Media references local paths; UI file capture remains separate.
- **Reports/backup:** JSON export of trades with fills, journal entries, strategies and performance summary; CSV export by realized exit; SQLite backup and signature-validated restore. PDF and cloud sync are not included.
- **Core infrastructure:** manual account profiles, app key/value settings and append-only audit events, behind repository contracts and SQLite implementations.

SQLite schema version 6 adds learning resources, account profiles, settings and audit events without dropping prior records.

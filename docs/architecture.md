# TD-YAR Architecture

## Product boundary

TD-YAR is a manual-first trading journal and trader reflection companion. Users enter trades and all executions themselves. There is no exchange, broker, MetaTrader, market-data, or order-execution connection and no trading signals or investment recommendations.

## Architecture rules

- Domain logic is plain Dart and independent of Flutter widgets.
- SQLite is the initial local-first persistence layer.
- Repository contracts isolate storage from domain services.
- Calculations use only user-entered values and timezone database rules.
- UI/UX is intentionally excluded from this phase.
- Database changes use explicit, data-preserving migrations.

## Modules 1–5

1. **Manual trade journal:** long/short trade records, multiple entry/exit fills, FIFO matching, fee allocation, notes, tags, strategy links and history validation.
2. **Performance analytics:** dated partial-exit realizations, realized gross/net P&L, fees, win rate, average win/loss, profit factor, expectancy, drawdown, duration and symbol/strategy/account/direction/time breakdowns. Open P&L is not estimated without market prices.
3. **Strategy manager:** user-authored setup, entry, exit and risk rules, timeframes, checklists, tags, archive/restore and trade associations.
4. **Daily journal:** pre-market plan, market context, execution review, mistakes, strengths, lessons, next-day plan, goals, tags and linked trades. Includes streak and journal-activity analytics.
5. **Trading sessions:** Sydney, Tokyo, London and New York defaults; custom IANA-timezone schedules; open/closed state, next occurrence, overnight windows, daylight-saving transitions and overlaps.

## Modules 6–11

### 6. Trading psychology

- Daily or trade-linked records store mood, confidence, focus, stress, plan adherence, FOMO/revenge flags, emotion tags and notes.
- Ratings are validated in the 1–5 range. Records support create, update, lookup, date/trade filtering and delete.
- Analytics summarize averages, mood frequency, plan adherence and counts, then compare linked realized outcomes.
- Comparisons are descriptive only. They do not infer causation or assess mental health.

### 7. Risk management

- Reusable profiles store account-balance snapshots, currency, maximum per-trade risk percentage, daily loss percentage and trade-count limits.
- Per-trade records store entry, stop, target, quantity, multiplier, planned monetary risk/reward, R:R and risk percentage.
- Calculations validate long/short stop and target placement, finite positive inputs and profile bounds.
- Daily checks count unique trades with entry fills on the selected local day and sum losing realized exits independently, so wins do not hide losses.
- Results are informational bookkeeping guardrails, not trading instructions or recommendations.

### 8. Trading calendar

- Supports day, Monday-to-Sunday week, month, year and custom date ranges.
- Each day aggregates partial exits on their actual realization date, with trade count, wins/losses/breakeven, gross/net P&L, fees, best/worst result, average result, win rate, journal count and psychology-entry count.
- Date ranges are inclusive by local calendar date.

### 9. Learning library

- Supports text-only, image-only, video-only and mixed text-plus-media resources.
- Metadata includes title, body, caption, category, tags, favorite state, source URL and related strategy IDs.
- Service validation enforces content-type requirements and HTTP(S) source links; optional strategy-repository validation checks linked strategy IDs.
- Media is represented by a local path. File picking, copying, permissions and thumbnail generation belong to the future platform/presentation layer.

### 10. Reports and portability

- CSV export lists realized exit records.
- PDF export summarizes historical performance metrics.
- Versioned JSON archive export covers trades with complete fill histories, daily journal, strategies, psychology entries, risk profiles and trade-risk records, learning resources, session definitions, accounts, settings, audit events and a performance snapshot.
- Optional repository dependencies are reported in the archive manifest so a caller can identify omitted modules.
- SQLite backup validates database integrity, schema version and required tables. Restore stages a candidate file and keeps a rollback copy until the restored database opens successfully.
- JSON archive import and cloud synchronization are not included yet. Binary media files are referenced by local path and are not embedded in JSON exports.

### 11. Core infrastructure

- Manual trading accounts support currency, opening balance, notes and archive lifecycle.
- Key/value settings provide local app configuration.
- Append-only audit events store event/entity identifiers, JSON-object details and UTC timestamps.
- Account names are unique case-insensitively; IDs, names, currencies and balances are validated.
- These primitives do not imply broker or exchange account connectivity.

## Persistence and migrations

SQLite schema version **6** includes trades, executions, journal entries, strategies, playbooks, attachments metadata, psychology entries, risk profiles, per-trade risk records, trading sessions, learning resources, manual trading accounts, app settings and audit events.

Migrations preserve existing trade/journal data and add feature tables/indexes incrementally. Schema creation is centralized in `AppDatabase`.

## Validation

Run:

```bash
flutter pub get
flutter analyze
flutter test
```

Do not mark a build as verified until these commands have been run in a Flutter-enabled environment.

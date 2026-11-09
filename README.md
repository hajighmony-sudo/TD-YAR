# TD-YAR

A Flutter/Dart local-first **manual trading journal and trader reflection companion**.

## Product scope

TD-YAR is a record-keeping, review and learning tool. Users manually enter all trade records.

### Core foundations (1–5)
1. Manual long/short trade journal with multiple entry/exit fills, FIFO matching, fee allocation, notes, tags and strategy association.
2. Performance analytics with dated partial-exit realizations, date-range reports, drawdown, expectancy, profit factor and breakdowns by symbol, strategy, account, direction, weekday and hour.
3. User-authored strategy library with setup/entry/exit/risk rules, checklists, tags, archive lifecycle and trade association.
4. Daily journal with pre-market plan, market context, execution review, mistakes, strengths, lessons, next-day plan, goals, tags and linked trades.
5. Timezone-aware market sessions for Sydney, Tokyo, London and New York, including open/closed status, next session times, custom schedules, overnight sessions, daylight-saving changes and overlap windows.

### Additional foundations (6–11)
6. **Trading psychology:** mood, confidence, focus, stress, plan adherence, FOMO/revenge flags, emotion tags, notes and descriptive linked-outcome comparisons.
7. **Risk management:** reusable risk profiles, per-trade risk/reward records and daily realized-loss/trade-count checks. Informational guardrails only.
8. **Trading calendar:** day, Monday-to-Sunday week, month, year and custom-range summaries; partial exits are assigned to their actual realization dates.
9. **Learning library:** text-only, image-only, video-only and mixed resources, with category, caption, tags, favorites, source URL and related strategy links. Media uses local-path references.
10. **Reports and portability:** realization CSV, performance PDF, full local-data JSON archive and SQLite backup/restore with integrity/schema validation and rollback protection.
11. **Core infrastructure:** manual account profiles, key/value settings and append-only audit-event history.

SQLite schema version is **6**. The JSON archive includes complete fill histories and supported module data when their repositories are supplied. Media binaries are not embedded; paths remain local to the device. JSON archive import and cloud synchronization are not implemented.

## Explicitly out of scope
- Exchange, broker or MetaTrader connections
- Live market data or automated price retrieval
- Order placement or automated trading
- Trade signals or investment recommendations
- UI/UX screens (presentation layer is intentionally deferred)

## Architecture

- `lib/core/domain`: entities and deterministic calculations
- `lib/core/data`: repository contracts and SQLite implementations
- `lib/features`: reserved for future presentation modules
- `test`: domain unit tests

The domain layer does not depend on Flutter widgets. SQLite is the initial local-first storage layer. Session calculations use IANA timezone rules for daylight-saving transitions.

## Getting started

Requires Flutter and Dart from a current stable Flutter SDK.

```bash
flutter pub get
flutter analyze
flutter test
```

## Accounting and analytics notes

Executions are manually recorded as entry or exit fills. Exits are matched to entry fills using FIFO within each trade record. Each exit fill creates a dated realization, so partial exits contribute to the correct day and reporting range. Fees are allocated proportionally to the quantity closed. Unrealized P&L is not estimated because this phase has no market-data feed.

Daily loss-limit checks sum losing realizations separately, so winning trades do not conceal losses. Psychology comparisons are descriptive only and do not establish causation. All calculations use manually entered records and are not forecasts or trading advice.

## Inspiration and licensing

The product workflow takes inspiration from public feature concepts and the separation of a pure calculation engine in [LuxAlgo Trade Journal](https://github.com/LuxAlgo/trade-journal). TD-YAR is an independent Flutter/Dart implementation; no LuxAlgo source code or branding is copied. Review third-party licenses before adding dependencies or source code.

# TRADIX — Architecture & Data Model

## Architecture direction

Retain the existing Kotlin + Jetpack Compose app, current Tx theme/components, and Rust/JNI calculation core where it is already appropriate. Add layers incrementally:

- UI: existing Compose screens and reusable components.
- Presentation: screen state + ViewModels (introduced incrementally).
- Domain: pure Kotlin models, validation, P&L, risk, session and analytics use cases.
- Data: Room database, DAO, repository implementations, migrations.
- Platform: WorkManager reminders, media storage, import/export, notifications.
- Optional services: approved economic calendar provider adapter; optional encrypted backup/sync.

Do not migrate the app to Flutter or replace the current UI stack.

## Proposed Room entities

### Account
`id: UUID PK`, `name`, `brokerLabel?`, `accountType`, `baseCurrency`, `startingBalance`, `createdAt`, `archivedAt?`.

### FxInstrument
`symbol PK`, `baseCurrency`, `quoteCurrency`, `pipSize`, `defaultContractSize`, `enabled`.
Only currency pairs are enabled in the Forex universe.

### Trade
`id: UUID PK`, `accountId FK`, `instrumentSymbol FK`, `side`, `status`, `openedAt`, `closedAt?`, `strategyVersionId?`, `sessionId?`, `emotion?`, `setupNotes?`, `exitReason?`, `createdAt`, `updatedAt`, `deletedAt?`.

### TradeFill
`id: UUID PK`, `tradeId FK`, `kind`, `quantityLots`, `price`, `executedAt`, `commission`, `swap`, `otherFees`, `conversionRate?`, `conversionSource?`.
Fills support partial entry/exit and weighted average prices.

### TradeRisk
`tradeId PK/FK`, `stopLossPrice?`, `takeProfitPrice?`, `plannedRiskAmount?`, `plannedRiskPercent?`, `riskCurrency`.

### Strategy
`id: UUID PK`, `name`, `description`, `archivedAt?`, `createdAt`, `updatedAt`.

### StrategyVersion
`id: UUID PK`, `strategyId FK`, `versionNumber`, `rulesJson`, `checklistJson`, `effectiveFrom`, `createdAt`.
Unique index: (`strategyId`, `versionNumber`).

### Tag / TradeTag
Normalized tags and many-to-many trade links.

### SessionDefinition
`id PK`, `name`, `ianaZoneId`, `localOpenTime`, `localCloseTime`, `enabled`, `customHoursJson?`.

### Lesson / LessonBlock
Lesson metadata plus ordered typed blocks. Blocks store text and references to media, never large binary blobs in Room.

### MediaAsset
`id PK`, `relativePath`, `mimeType`, `sizeBytes`, `sha256`, `createdAt`, `thumbnailPath?`.

### CalendarEvent
`id PK`, `source`, `externalId?`, `currency`, `title`, `impact`, `scheduledAtUtc`, `previous?`, `forecast?`, `actual?`, `updatedAt`.

### DayReview
`localDate PK`, `accountId?`, `dayType`, `reason?`, `note?`, `reviewedAt?`.

### Goal / GoalProgress / AppPreference / BackupManifest
Separate user goals, progress snapshots, locale/timezone/reminder preferences, and backup integrity metadata.

## Relationships
- Account 1—N Trade
- Trade 1—N TradeFill
- Trade 0—1 TradeRisk
- Strategy 1—N StrategyVersion
- StrategyVersion 1—N Trade
- Trade N—N Tag
- Lesson 1—N LessonBlock
- LessonBlock N—1 MediaAsset (optional)
- Account 1—N DayReview (optional account-specific review)

## Calculation rules

### Pip movement
For most non-JPY pairs, pip size is 0.0001; for JPY-quoted pairs, typically 0.01. Instrument metadata is authoritative and editable for broker conventions.

### Pip value
For a standard lot, pip value in quote currency is:
`pipSize × contractSize × lots`.
Convert quote currency to account currency using an explicit quote-to-account conversion rate. If unavailable, mark account-currency P&L pending conversion; never guess.

### Gross P&L
For Buy: `(exitPrice - entryPrice) × contractSize × lots`.
For Sell: `(entryPrice - exitPrice) × contractSize × lots`.
Convert from quote currency to account currency; then subtract commissions, swaps, and fees using their recorded currencies/conversion rates.

### R multiple
`netPnl / plannedRiskAmount` only when planned risk is positive and in the same reporting currency.

### Equity and performance candles
Sort realized cash-flow events by timestamp. For each period:
- Open = equity before the first event in the bucket.
- High/Low = max/min of equity after each event (including open).
- Close = equity after the final event.
These are journal performance candles, not FX market candles.

### Drawdown
Track running peak equity and calculate `(equity - peak) / peak`. Handle zero/negative peak explicitly.

## Indexes and constraints
- Trades: (`accountId`, `openedAt`), (`instrumentSymbol`, `openedAt`), (`strategyVersionId`), (`status`).
- Fills: (`tradeId`, `executedAt`).
- Events: (`scheduledAtUtc`, `currency`, `impact`).
- Lessons: (`folderId`, `updatedAt`); blocks: (`lessonId`, `position`).
- Foreign keys enabled; deletes use explicit cascade/restrict rules.
- Use database transactions for trade/fill import and backup restore.

## Migration strategy
The current app uses in-memory AppState and seeded examples. Introduce Room as a parallel repository, then migrate user-created data through a one-time importer. Do not drop the old in-memory path until screens have been switched and migration tests pass. Demo fixtures must be isolated and removable; never migrate fixtures as real trades.

# TRADIX — Product Requirements Document

**Product:** TRADIX  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Languages:** Persian (fa-IR, RTL) and English (en, LTR)  
**Scope:** Full Forex trading journal and trader development workspace  
**Principle:** Manual-first, offline-first, no trade execution, no signals.

## 1. Product vision

TRADIX helps Forex traders record every trade, understand performance, build repeatable strategies, study their own behavior, and organize learning material. The product is a journal and analytics tool—not a broker, signal service, or execution terminal.

## 2. Non-negotiable repository/UI constraints

- Preserve the current visual identity, theme tokens, gradients, cards, typography, spacing, animations, navigation conventions, chart styling, globe, and reusable components.
- Do not replace `TradenexTheme`, `Tx` color tokens, `TxBackground`, `GlassCard`, `TxBottomBar`, or existing screen layouts as a redesign.
- Prefer additive domain/data/repository/use-case code and small screen integrations.
- Keep changes isolated on `feature/tradix-forex-journal`; never push feature work to `main`.
- Existing functionality must remain usable. Every schema change must be additive and migration-safe.
- Remove crypto from the product universe; Forex currency pairs only by default. Do not silently treat XAUUSD or index CFDs as FX currency pairs. Metals/CFDs can only be enabled later as a separately-labelled optional instrument class.

## 3. Target users and jobs

1. A discretionary Forex trader who enters trades manually and needs accurate P&L.
2. A strategy-driven trader who wants rules, versions, and execution checklists.
3. A trader who wants to correlate results with sessions, pairs, weekdays, and emotions.
4. A learner who wants a personal knowledge base with mixed media.
5. A disciplined trader who wants reminders, planned rest days, and behavioral insights.

## 4. Product principles

- Manual entry is the primary workflow; no broker credentials are required.
- Offline use is fully supported; sync/backup is optional and explicitly enabled.
- Financial calculations must be auditable and use decimal-safe arithmetic.
- Never infer missing prices, conversion rates, commissions, or swaps.
- Distinguish zero P&L, no trades, market holiday, planned rest, and missing data.
- Persian and English are complete locales; direction follows the selected locale.
- User data export, backup, restore, and deletion are first-class features.
- AI features are optional assistants, never authoritative financial advice.

## 5. Functional requirements

### 5.1 Dashboard
- Account selector and account summary (balance, equity, net P&L).
- Today/week/month performance, trade count, win rate, profit factor, expectancy, average R, max drawdown.
- Equity curve and recent trades.
- Best/worst day, pair, strategy, and session.
- Discipline score based on process adherence, not raw profit.
- Quick actions: Add Trade, Add Note, Add Lesson, Mark Rest Day.
- Empty states for new users; seeded demo data must not be mixed with real user records.

### 5.2 Accounts
- Multiple independent accounts, each with name, broker label, account type (demo/real/prop), base currency, starting balance, and active/archive status.
- Manual-first; no password or broker login storage.
- Account-specific trades and analytics; optional all-account overview.
- Account currency is the reporting currency. Conversion inputs and their source/time must be explicit.

### 5.3 Trade journal
- Create, view, edit, duplicate, archive, and delete trades.
- Fields: account, symbol (FX pair), direction, open/close timestamps, entry/exit prices, lots, contract size, stop loss, take profit, commission, swap, fees, strategy/version, session, tags, setup notes, exit reason, emotion, screenshots/media.
- Support partial closes and multiple fills without double-counting.
- Trade status: open/closed; open trades contribute unrealized P&L only when a current price is explicitly available.
- Filters: date, account, pair, side, result, strategy, session, tags, emotion.
- CSV import/export with preview, validation, duplicate detection, and rollback.

### 5.4 Forex P&L and risk
- Pair-aware pip size (JPY and non-JPY pairs).
- Pip value based on quote currency, lot size, contract size, and conversion to account currency.
- Gross P&L, commission, swap, fees, and net P&L shown separately.
- R multiple and risk amount calculated only when required stop/risk data exists.
- Do not assume every account uses USD or every contract is 100,000 units; allow broker-specific contract size.
- Display calculation details and missing conversion-rate warnings.

### 5.5 Strategies
- Create/edit/archive/delete personal strategies.
- Strategy fields: name, description, market conditions, pairs, sessions, timeframe, entry rules, invalidation, exit rules, risk model, checklist, media, tags.
- Strategy versioning (V1/V2/…) with immutable snapshots and effective dates.
- Link trades and lessons to a strategy version.
- Compare performance by strategy and version.

### 5.6 Sessions
- Sydney, Tokyo, London, New York.
- Live open/closed state, local time, countdown, progress, overlap timeline.
- IANA time zones and daylight-saving transitions; user-configurable session hours.
- Existing globe/timeline presentation is retained. Globe mode remains a presentation choice.
- Session attribution uses the trade's open timestamp and configured session definitions.

### 5.7 Analytics
- Equity curve.
- Performance OHLC candles (not market-price candles): open/high/low/close of cumulative realized P&L or equity within each period.
- Periods: daily, weekly, monthly, yearly.
- Heatmaps: calendar P&L, weekday, hour, pair, strategy × session.
- Metrics: net/gross P&L, win rate, profit factor, expectancy, average win/loss, average R, max drawdown, streaks, trade count.
- Every metric has a documented formula and handles empty/partial periods.

### 5.8 Calendar and economic news
- Personal trading calendar and notes.
- Economic events: currency, title, impact, scheduled time, previous, forecast, actual, source, last-updated timestamp.
- Forex Factory is a desired source, but integration must use a permitted and reliable feed/API or user-imported calendar data. Do not build brittle scraping or bypass access controls.
- Timezone-aware event display and high-impact event filters.
- News-to-trade correlation is descriptive, not causal.

### 5.9 Trading Academy
- Block editor: headings, paragraphs, images, videos, audio, PDF, links, quote, checklist, table, formula.
- Supports text-only, image-only, video-only, and all mixed combinations.
- Folders, tags, favorites, search, progress, review date, quiz, and links to strategies/trades.
- Media stored locally with metadata and user-controlled backup; enforce file-size and format limits.

### 5.10 Discipline and psychology
- Optional local-time daily no-trade prompt (default 22:00), configurable or disabled.
- Ask why no trade: planned rest, no valid setup, market closed/holiday, personal schedule, missed opportunity, other.
- Planned rest days suppress reminders and do not count as failures.
- Journal emotions, rule adherence, overtrading warnings, consecutive-loss cooling-off prompts.
- Never shame users or imply that a no-trade day is inherently bad.

### 5.11 Goals, replay, reports, and coach
- Goals by process and performance, with progress tracking.
- Trade replay from user-entered events and attached chart screenshots; no fabricated market data.
- Report builder with PDF/CSV/XLSX export.
- Optional AI coach based only on user-authorized journal data; explain evidence and uncertainty, allow opt-out, never place trades.

### 5.12 Localization/accessibility
- Persian and English; RTL/LTR based on locale.
- Localized dates, numbers, currencies, and pluralization.
- Respect font scaling, screen readers, contrast, and reduced-motion settings.

## 6. Non-functional requirements

- Offline-first storage; no network required for core journaling.
- Database transactions and migrations.
- Encrypt sensitive local data and backups; never store broker passwords.
- WorkManager for approximate reminders/background work; request Android 13+ notification permission when needed.
- Avoid exact alarms unless product behavior truly requires exact timing and Android policy permits it.
- Crash-safe media handling and backup restore validation.
- Unit tests for calculations, date boundaries, DST, analytics, and migrations.
- Performance target: responsive lists and dashboards with thousands of trades through paging/aggregation.

## 7. Success criteria

- A user can create an account and record a complete FX trade offline.
- P&L reconciles to independently calculated examples for USD-quoted, JPY-quoted, and cross pairs.
- App restart does not lose journal data.
- Persian/English switch changes strings and layout direction without changing visual design.
- Session states remain correct across DST changes.
- Analytics exclude demo fixtures from real account data.
- No feature branch change touches the existing theme files without an explicit bug fix and review.

## 8. Out of scope

- Trade execution, copy trading, signal generation, broker password collection.
- Crypto assets in the Forex journal.
- Unlicensed scraping of economic calendars.
- Claims of guaranteed profitability or predictive performance.

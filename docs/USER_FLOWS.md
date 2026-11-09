# TRADIX — Screen Map & User Flows

## Navigation map

Keep the existing bottom bar and visual conventions. Extend the current routes rather than replacing navigation.

- Home: account overview, KPI cards, equity preview, recent trades, quick actions.
- Add Trade: guided manual trade form.
- Performance: metrics, equity curve, performance candles, heatmaps, filters.
- Education: folders, lessons, mixed-media editor, quizzes.
- More: accounts, sessions, strategies, calendar, psychology, goals, reports, settings.
- Trade Detail: fills, costs, screenshots, notes, linked strategy/version.
- Strategy Detail/Editor: rules, checklist, versions, linked trades.
- Account Detail/Editor: currency, type, starting balance, archive.
- Calendar Day/Event Detail: personal notes and economic events.
- Session Detail: session hours, current state, overlap and DST-aware local time.

## Flow A — First launch
1. Welcome → choose Persian or English.
2. Create first manual account (name, demo/real/prop, base currency, starting balance).
3. Choose timezone and preferred session display (globe/timeline).
4. Explain that TRADIX does not execute trades or provide signals.
5. Land on Home with an empty-state dashboard and Add Trade action.

## Flow B — Record a trade
1. Home → Add Trade.
2. Select account → pair → Buy/Sell.
3. Enter open/close time and entry/exit prices; allow open trade without exit.
4. Enter lot size and broker contract size (default clearly labelled).
5. Optional SL/TP, commission, swap, fees, strategy version, session, tags, emotion, notes, screenshots.
6. Validate required fields and chronological consistency.
7. Preview calculation: gross P&L, costs, net P&L, pip movement, conversion assumptions.
8. Save transactionally → Trade Detail → update analytics.

## Flow C — Manage strategies
1. More → Strategies → Create.
2. Add setup, market conditions, pair/session/timeframe constraints.
3. Define entry, invalidation, exit, and risk rules.
4. Build checklist and attach learning material.
5. Save V1. Editing published rules creates V2; old trades remain linked to V1.
6. Compare versions after sufficient sample size.

## Flow D — Review performance
1. Performance → select account(s) and date range.
2. Switch daily/weekly/monthly/yearly.
3. Inspect equity curve and performance OHLC.
4. Tap a heatmap cell → filtered trade list.
5. Drill into pair/session/strategy/emotion.
6. Export report with selected filters.

## Flow E — Sessions
1. Home/More → Sessions.
2. Choose globe or 24-hour timeline.
3. View local clock, open/closed state, time remaining, overlap.
4. Edit user session hours if broker/market conventions differ.
5. Session attribution is calculated from timestamp and timezone rules, not a fixed UTC offset.

## Flow F — Academy
1. Education → folder → New Lesson.
2. Add ordered blocks (text, image, video, audio, PDF, link, quote, checklist, table, formula).
3. Preview and save.
4. Tag/favorite and link lesson to strategy/trade.
5. Track progress and schedule review/quiz.

## Flow G — No-trade day
1. If enabled and no trade is recorded by configured local time, show a gentle prompt.
2. User selects planned rest / no setup / market holiday / schedule / missed opportunity / other.
3. Planned rest and market closures are excluded from discipline penalties.
4. User may add a note; reminder is not repeated after a response.

## Flow H — Import/export and backup
1. Settings → Data.
2. Export CSV for trades or full encrypted backup.
3. Import preview → validate fields, timezone, currency, duplicate candidates.
4. Confirm import → transaction; show rejected rows and reasons.
5. Restore verifies backup integrity before replacing or merging data.

## Interaction rules
- Destructive actions require confirmation and offer undo where practical.
- Never silently overwrite a trade or strategy version.
- All empty, loading, error, permission-denied, and offline states have explicit UI.
- Preserve current components, motion language, colors, and layout density.

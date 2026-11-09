# TRADIX — Journal & TX AI Completion Blueprint

Status: implementation blueprint for the isolated feature branch. A capability is not considered complete merely because it appears in this document; each item must have a working data path, empty/error states, tests, and acceptance evidence.

## Product contract

- Local-first, offline-capable, Persian-first and RTL-aware.
- Account isolation is mandatory for every query, chart, export, and behavioral episode.
- Never fabricate fills, intratrade OHLC, prices, news, broker sync, or psychological certainty.
- No trade execution, copy trading, signals, or broker passwords.
- Missing data is represented as unavailable, not zero or inferred.
- Preserve current Tx theme tokens, components, navigation conventions, and unrelated screens.
- All work stays on `feature/tradix-forex-journal`; `main` is not a target for implementation commits.

## Reference product patterns reviewed

- TradeZella: broad journal workflow, reports, strategy templates, replay and backtesting.
- TraderSync: mobile journal, broker imports, replay and AI-assisted review.
- Edgewonk: psychology, mistake tracking and process-oriented review.
- TradesViz: broad analytics and customizable reporting.
- Tradervue: trade history, reports, tags and sharing.
- MyTradeBook, TradeLedger and Trade Ledger Pro: open-source design references for local journaling, calendar drill-down and trade analytics. Their code and visual identity are not to be copied.

## Feature inventory and acceptance gates

### A. Journal foundation
- Multiple isolated accounts; real/demo/prop metadata and base currency.
- Complete trade lifecycle: open, partial close, multiple fills, final close, fees, swap, commission and manual corrections.
- Immutable execution/fill records; edits retain an audit trail.
- CSV import wizard: mapping, preview, validation, duplicate detection, account selection, rollback and import summary.
- CSV/XLSX/PDF exports with filters, date range, timezone and account scope.
- Encrypted backup/restore, integrity check, conflict preview and user-controlled deletion.
- Search, saved filters, tags, attachments, screenshots and notes.
- No broker password storage. Any future broker read-only connector must be separately consented and revokeable.

Acceptance: restart-safe data, schema migration tests, import idempotency, export round-trip and cross-account isolation tests.

### B. Performance analytics
- Net/gross P&L, win rate, profit factor, expectancy, average win/loss, average R, payoff ratio, max drawdown, recovery factor and streaks.
- Equity curve and trade-based equity candles; market OHLC is only shown when sourced from real market data.
- Calendar P&L heatmap, hour-of-day, weekday, session, symbol, strategy/version, timeframe and market-condition analysis.
- Profit concentration, hold-time and exit efficiency; MAE/MFE only when actual excursion data exists.
- Confidence/sample labels and minimum sample gates on every comparative insight.
- Filters combine account, date, symbol, direction, session, strategy, tags, emotion and result.

Acceptance: every metric has a documented formula, stable sorting, sample-size disclosure and deterministic unit tests.

### C. TX AI Command Center
- Edge Score assembled from explicitly defined components; show component values and data coverage.
- Six-axis radar: Win Rate, Profit Factor, Expectancy, Consistency, Discipline and Session Edge.
- Monthly Process Score: Discipline, Plan Adherence, Risk Recording and Decision Quality.
- Tilt indicator is an evidence summary, never a clinical or probabilistic diagnosis.
- Best/worst session and setup only when sample gates pass.
- Daily/weekly/monthly narratives generated from local deterministic rules; every statement links to supporting trades and fields.

### D. Edge Explorer
- Multi-dimensional filters and saved views.
- Pair/session/strategy comparison matrix.
- Hour × weekday × outcome heatmap (15-minute granularity only with sufficient timestamps).
- Risk/reward scatter using recorded risk and realized result; unknown risk is excluded and counted as missing.
- Comparison mode between strategies and immutable strategy versions.

### E. Behavioral Lab
- Revenge Trading episodes with trigger trade, previous losing trade, time gap, risk/size changes, explicit plan violation, tags, emotion and loss streak.
- Overtrading after win/loss, early exit, widened stop, size-up after loss, session drift and overconfidence flags.
- Each card has Detection, Evidence, Linked Trades, Cost and Suggested Action sections.
- Evidence score is not a probability. User tags are distinct from algorithmic evidence.
- User review states: Confirmed by user / Not revenge / Unsure; review changes are auditable and do not silently rewrite trade data.
- Episode IDs prevent duplicate counting. Episode-level P&L and account-level cost are separately calculated to avoid double counting.
- Personal rules: after N losses, after risk increase, after daily loss cap, and user-defined conditions; soft warnings only.

Acceptance: planned quick re-entry is not automatically revenge; missing data is never inferred; episodes are account-scoped and reproducible; user corrections persist.

### F. Counterfactual Studio
- Exclude explicit off-plan trades, reduce risk, selected session/strategy only, or exclude trades after N consecutive losses.
- Actual vs scenario equity, delta and affected trade list.
- Every scenario lists assumptions, excluded trades and limitations.
- No scenario is described as a backtest unless it replays real historical market data and execution assumptions.

### G. Process Coach & planning
- Daily plan, risk cap, goal, checklist, mood, focus, notes and no-trade reason.
- Weekly autopsy and monthly score timeline.
- Process commitments and review completion.
- Planned rest days suppress reminders and are not discipline failures.
- Soft cooling-off prompts; never block broker actions.

### H. Trade replay and backtesting
- Journal replay from user-saved screenshots and event notes is supported without invented candles.
- Market replay/backtesting requires a licensed or otherwise permitted historical market-data source, timezone/session handling, spread, commission, slippage and execution model.
- Replay cursor, speed controls, annotations, before/after review and strategy-rule checklist.
- Clearly label journal-only replay vs market-data replay.

### I. Insight Feed and reporting
- Daily, weekly and monthly insight cards with evidence, metric, action, pin/save and drill-down.
- PDF/CSV/XLSX reports; optional image share.
- QR share includes only user-selected non-sensitive summaries; never credentials or raw account identifiers.
- Optional voice notes are local and user-controlled.

### J. Academy, news and integrations
- Academy supports mixed ordered content blocks, folders, tags, search, favorites, progress, quizzes and trade/strategy links.
- Economic calendar uses a permitted provider or user import; source, actual/forecast/previous, impact and updated timestamp are mandatory.
- No brittle scraping, fabricated sample events or unsupported "live" claims.
- Future read-only broker/MetaTrader adapters must expose capability/permission checks, sync status, reconciliation and revoke controls.

## Target technical architecture

- Flutter/Dart owns the TX AI module UI and its interaction model. The current Flutter package is a standalone demo and must be embedded through a deliberately tested Android host integration before claiming UI migration complete.
- Rust owns deterministic, testable heavy analytics and scenario calculations with stable input/output schemas.
- Dart owns Persian narrative composition and presentation-level orchestration.
- JavaScript Canvas is reserved for advanced interactive visualizations where Flutter's native chart layer is insufficient; it is not a requirement for every chart.
- C is not a parallel business-logic layer. Use C only for a specific native dependency or stable FFI boundary with measurable benefit.
- Kotlin remains a minimal Android host/bridge until the Flutter module is integrated; existing Compose screens remain untouched during staged migration.
- Local storage migration must preserve the existing SQLite snapshot before introducing normalized, versioned tables. Never discard user data during migration.

## Current branch status at blueprint creation

- Existing: local Forex journal, multiple accounts, strategy snapshots, CSV export, calendar, sessions, education library, weekly planner, reminders, TX AI descriptive analytics and counterfactual basics.
- Newly added in this branch batch: deterministic Revenge Trading evidence analyzer, account-scoped episode output, linked trigger/previous-loss trade presentation in TX AI, and unit tests.
- Not yet complete: persistent user review corrections, episode filters/drill-down screen, remaining Behavioral Lab detectors, Edge Explorer, full Counterfactual Studio, full Command Center radar, Flutter embedding, advanced replay/backtesting, normalized encrypted storage, backup/restore, live economic calendar and broker integrations.
- Do not mark the product release-ready until latest CI, migration tests, Persian RTL smoke tests and physical-device install/launch checks pass.

## Delivery order

1. Stabilize journal data model, persistence and migration safety.
2. Complete Behavioral Lab episode model, review persistence and all linked-trade drill-downs.
3. Complete Command Center and Edge Explorer with deterministic Rust analytics and formula tests.
4. Complete Counterfactual Studio and Process Coach.
5. Embed Flutter TX AI UI without changing existing theme or unrelated Compose routes.
6. Add replay/backtesting only after market data and execution model are real.
7. Add export/backup, localization/accessibility and final device validation.


## Implementation update — current coding batch

Implemented on `feature/tradix-forex-journal` after the initial blueprint:

- Rust: deterministic realized-P&L metrics for net, win rate, profit factor, drawdown, expectancy, average win/loss, recovery factor and largest win/loss concentration; non-finite values are excluded.
- Rust: C ABI entry point `tx_ai_performance_metrics` for a future Dart FFI boundary.
- Dart: `TxRustAnalytics` FFI wrapper and typed `TxRustMetrics` result contract.
- Flutter: six-axis `TxRadar`, `TxInsightCard`, `TxBehavioralBadge`, `TxHeatmap`, `TxCounterfactualSlider`, `TxProcessTimeline`, `TxAiCommandCenter`, and an interactive filtered `TxEdgeExplorer` with a risk/reward scatter.
- Flutter tests: widget tests for radar interaction, insight pin action, heatmap interaction and unavailable process scores.
- CI: separate Flutter format/analyze/widget-test job.

Integration boundary (important): `flutter_fx` is still a standalone Flutter package/app, not yet embedded in the Android application. The FFI wrapper requires the Rust shared library to be packaged for each target ABI and is not yet wired to a live account snapshot. The new UI components accept host-provided account-scoped data; they do not create fake journal data. Do not claim the Flutter screens are visible inside the production Android app until the add-to-app bridge, native library packaging, and end-to-end account-data mapping are completed and tested.

Next coding gates:
1. Confirm the latest Android, Rust and Flutter CI jobs pass on the branch HEAD.
2. Integrate Flutter add-to-app with the smallest possible Android host bridge; do not rewrite existing Compose screens or theme.
3. Build/package Rust `cdylib` for arm64-v8a, armeabi-v7a and x86_64 and validate Dart FFI on device.
4. Connect the selected-account journal snapshot to typed Dart models and test account isolation end-to-end.
5. Persist behavioral review corrections and add the remaining Behavioral Lab detectors.
6. Implement saved views, cross-analysis matrix, scenario engine and insight lifecycle with persistence.


## Implementation update — durable review and native metrics batch


## Implementation update — durable review and native metrics batch

Added on `feature/tradix-forex-journal`:

- Account-scoped Behavioral Lab review states: Confirmed / Not Revenge / Unsure. Each decision is keyed by account + episode ID and records its update timestamp.
- Account-scoped Insight pin preferences keyed by account + deterministic insight key.
- Both preference collections are included in the versioned local snapshot. Older version-1 snapshots remain readable because missing preference arrays restore as empty collections.
- TX AI screen now exposes the review controls and pin/unpin actions; each action persists through the existing atomic SQLite snapshot path.
- Extended performance metrics are now available to the Android host through the Rust JNI boundary, with a deterministic Kotlin fallback: expectancy, average win/loss, recovery factor, and largest win/loss concentration in addition to net, win rate, profit factor and max drawdown.
- Added pure unit tests for review/pin account isolation and update behavior, plus Kotlin fallback metric parity.

Still not release-complete:

- Flutter widgets and the Dart FFI/data bridge remain in the standalone `flutter_fx` package; they are not embedded into the Android navigation or connected to the live account snapshot.
- The current Android screen remains Compose. Rust JNI metrics are now used there; this does not mean the Flutter module has been integrated.
- Remaining Behavioral Lab detectors (including overtrading-after-win, early-exit evidence, session drift and stop-widening) require additional structured trade history. Stop-widening cannot be inferred from a single final stop value; it needs stop-change audit events.
- Full persisted saved views, complete multi-axis aggregation, advanced replay/backtesting, encrypted backup/restore, normalized storage migration and real economic-calendar provider are still outstanding.
- CI for the latest commit and real-device install/launch/RTL verification must pass before release claims.

Do not fabricate missing stop history, market excursion data, live news, or broker data. Keep this PR Draft and do not merge it.


## Implementation update — conservative Behavioral Lab expansion



## Implementation update — conservative Behavioral Lab expansion

Added three account-scoped, deterministic journal pattern detectors:

- Post-win activity cluster: requires a timestamped winning trade followed by at least three timestamped entries within two hours. The UI calls it an activity-density signal, not proof of overtrading.
- Size increase after a win: requires the immediately following journal trade to be opened after the previous winning trade closed and its recorded lot size to exceed the prior lot by more than 25%. It does not infer overconfidence or motivation.
- Exit before recorded target: requires a positive realized result and explicit entry, exit and take-profit prices; direction-aware logic checks whether the exit price is between entry and target. The UI explicitly states that without intratrade market path data it cannot know whether the target was touched earlier.

All three detectors include linked trade IDs, evidence reasons and account-scoped user review controls. They do not infer stop widening because no stop-edit audit trail exists. CI tests cover timestamp requirements, thresholds, long/short target direction and missing-data behavior.


## Implementation update — advanced performance slices



## Implementation update — advanced performance slices

The local Kotlin analytics report now additionally provides:

- Strategy-version comparisons keyed by the recorded strategy name and version.
- 15-minute entry-time buckets using the timezone selected in app settings (Tehran, UTC, or device zone).
- Average and median holding duration only when both open and close timestamps are present and ordered.
- Average realized R-multiple only for trades with a finite, positive recorded planned-risk amount.
- Maximum consecutive win/loss streaks ordered by close time, falling back to journal date only when close time is unavailable.
- Both best and weakest session summaries require at least five timestamped trades in the session.

Unit tests cover timezone buckets, strategy version grouping, hold-time calculations, R-multiple and streaks. These are descriptive journal metrics, not market replay or predictive claims.


## Implementation update — Flutter add-to-app and live account bridge



## Implementation update — Flutter add-to-app and live account bridge

The integration path is now implemented in source:

- `scripts/bootstrap_flutter_module.sh` generates the Android Flutter module from the canonical `flutter_fx/lib` Dart sources; generated output is ignored and reproducible.
- The Android navigation launches the Flutter TX AI Activity when the module is bootstrapped, and retains the existing Compose TX AI screen as a native-only fallback.
- The Flutter host MethodChannel exposes only the selected account's journal records, account risk settings, session/timezone fields and account-scoped review/pin preferences. It does not expose broker passwords or connection credentials.
- Flutter can persist episode reviews and insight pins through host methods that validate the selected account before writing.
- The Dart dashboard now composes Command Center, Edge Explorer, strategy/emotion matrix, selected-timezone weekday/hour heatmap, Behavioral Lab, Rust-backed counterfactuals, process timeline and period-scoped Insight Feed.
- Dart FFI consumes the packaged Rust shared library; the Android host also calls Rust performance metrics through JNI. The Rust build workflow produces arm64-v8a, armeabi-v7a and x86_64 libraries.

Verification status at this update:

- Rust unit tests and Android-ABI Rust library build have passed in the latest observed completed run.
- Flutter analyzer previously caught one redundant null assertion; that code was corrected. The newest branch-head Flutter analysis and widget tests are still pending.
- Flutter module bootstrap succeeded in the latest observed Android job. Android Gradle compilation, unit tests and APK packaging were still running at the last check.
- No real-device install, activity launch, RTL screenshot, process-death persistence or ABI-by-ABI runtime test has yet been confirmed.

Remaining work before calling TX AI complete:

- Resolve any remaining Flutter analyzer, Android Gradle or unit-test failures and verify the latest PR head specifically.
- Verify real device launch and FFI symbol loading on each supported ABI.
- Add the remaining Behavioral Lab detectors and complete review persistence for all event types; stop widening requires stop-change audit history that is not currently stored.
- Finish saved views, full multi-dimensional matrix and interactive 3D heatmap, complete counterfactual controls, insight scheduling, export/QR/voice-note workflows and localization.
- Validate account switching, large-history pagination/performance and database migration/backup recovery.

All changes remain on `feature/tradix-forex-journal`; keep PR #1 Draft and unmerged. Do not alter `main`.


## Implementation update — complete journal-backed Behavioral Lab feed

- Added an explicit size-up-after-loss signal alongside size-up-after-win; neither signal alone is labeled Revenge Trading.
- Added session-drift detection only when the recorded strategy rule names allowed sessions. Historical strategy revisions are used when available; if the trade's version has no matching rule record, the detector withholds the result.
- Session overlap is handled by checking every active IANA-zone session, so an allowed overlapping session prevents a false drift flag. A trade outside all active sessions is reported as between sessions.
- Flutter receives the same deterministic pattern records from the Android host, displays linked trade details, restores user review states and persists review changes through the account-validated channel.
- Stop widening remains unavailable until stop-loss change history is captured; it is intentionally not inferred from the final stop-loss value.

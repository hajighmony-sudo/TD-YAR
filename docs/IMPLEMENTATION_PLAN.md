# TRADIX — Android Implementation Plan

## Repository baseline

- Existing app is Kotlin + Jetpack Compose, with a custom dark/gold Tx theme and existing screens/components.
- Existing routes already include Home, Performance, Education, More, Add Trade, Sessions, Strategies, Calendar, Tags, Settings.
- Rust/JNI and Kotlin fallback exist for session/statistics calculations.
- The README explicitly notes that current data is in memory, sample news is placeholder, reminders/media/language switch are incomplete, and no build machine was used for the initial scaffold.
- Do not rewrite the theme or navigation wholesale.

## Phase 0 — Safety baseline
1. Keep all work on `feature/tradix-forex-journal`.
2. Run existing CI and Gradle sync/build before changing code.
3. Capture screenshots of all current screens and document visual tokens.
4. Establish unit/instrumentation test baseline.
5. Add a UI regression checklist for theme, navigation, cards, globe, charts, RTL.

## Phase 1 — Domain and persistence
1. Add pure Kotlin Forex domain models and validation.
2. Add Room, KSP/plugin versions compatible with the current Gradle/Kotlin toolchain.
3. Implement entities, DAOs, repositories, and migrations.
4. Replace seeded sample data with explicit demo mode; remove BTC from Forex fixtures.
5. Add import/export and account isolation.
6. Test persistence across process restart and migration from legacy AppState.

## Phase 2 — Accurate trade engine
1. FX pair metadata and pip-size rules.
2. Lot/contract-size-aware gross P&L.
3. Quote-to-account currency conversion with source/timestamp.
4. Partial fills, commissions, swaps, fees, and open positions.
5. Risk amount, R multiple, and calculation breakdown.
6. Add independent calculation test vectors for EURUSD, USDJPY, and EURGBP with USD account.

## Phase 3 — Strategy and review system
1. CRUD strategies and immutable versions.
2. Entry/exit/invalidation rules and checklist.
3. Link trades to strategy versions.
4. Emotion, notes, tags, planned rest, and day review.
5. Process-based discipline metrics.

## Phase 4 — Analytics
1. Equity curve based on realized events.
2. Performance candles by day/week/month/year.
3. Calendar heatmap and pair/hour/weekday/strategy-session breakdowns.
4. Metric definitions and empty-state handling.
5. CSV/PDF/XLSX exports.

## Phase 5 — Sessions and calendar
1. Replace fixed UTC assumptions with IANA zone/DST-aware session engine.
2. Preserve current globe and timeline UI.
3. Add user-configurable session hours and overlap calculation.
4. Add provider adapter for permitted economic calendar data.
5. Do not scrape Forex Factory without permission or stable supported access.

## Phase 6 — Academy and media
1. Lesson/folder/block schema.
2. Media picker, thumbnails, local files, playback, PDF view.
3. Search, favorites, tags, review, quiz.
4. Link lessons to strategies and trades.
5. Media backup and size controls.

## Phase 7 — Notifications and coach
1. WorkManager-based local 22:00 reminder (configurable, timezone-aware).
2. Exclude planned rest days, weekends/market closures, and already-reviewed days.
3. Add session/news alerts.
4. Add optional AI coach only after data quality, consent, privacy, and export controls exist.

## Phase 8 — Quality and release
- Build debug and release variants.
- Unit tests: P&L, conversions, DST, analytics, strategy versioning.
- Database migration and backup/restore tests.
- UI smoke tests in Persian RTL and English LTR.
- Accessibility and large-font checks.
- Test offline behavior and Android notification permission denial.
- Verify APK install/launch and no crashes on supported API levels.

## Acceptance gate for each phase
- No change to Tx theme tokens or existing reusable UI components unless a specific bug is demonstrated.
- Existing screens remain reachable and functional.
- New feature is isolated, tested, and documented.
- No placeholder is represented as live market/news data.
- CI passes before the phase is considered complete.

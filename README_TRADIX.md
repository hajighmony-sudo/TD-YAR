# TRADIX

دفترچه معاملات، آموزش و ساعت سشن‌های فارکس. **سیگنال نمی‌دهد و معامله انجام نمی‌دهد.**
Forex trade journal, learning hub and live session clock. It never trades and never gives signals.

## Stack

| Layer | Tech | Where |
|---|---|---|
| Android host + existing app UI | Kotlin, Jetpack Compose (RTL, Persian first) | `app/` |
| TX AI UI | Dart / Flutter add-to-app module; account data through a read-only MethodChannel | `flutter_fx/` |
| Deterministic native core | Rust (JNI + C ABI): sessions, performance metrics, revenge evidence and scenarios | `rust/` |
| Flutter module output | Generated from canonical Dart sources; do not edit generated files | `flutter_module/` |
| CI | GitHub Actions: Rust tests + Android unit tests/APK + Flutter analyze/widget tests | `.github/workflows/ci.yml` |

## Screens

Home dashboard, trade history (filters, edit/delete, CSV export), manual trade entry (manual P&L or price-based FX calculation),
multi-account profiles (real/demo/prop, base currency, active account), performance (equity candles and heatmaps),
strategies (create/edit/delete and version snapshots), DST-aware sessions (globe/timeline), education,
calendar (personal trades + cached Forex Factory weekly economic feed), tags, and settings.

## Themes (Settings > تم‌ها)

| Theme | Look |
|---|---|
| **Default** | The original translucent glass look. Accent colour: gold / teal / violet. |
| **Gold** | Black-gold panels with a gold edge, strong glow, shimmering borders, gold dust, crown on the logo, a dark-amber globe with night-side city lights. |

The switch is animated: every colour is mixed in OKLab by the Rust core (`rust/src/fx.rs`, Kotlin fallback with the same maths)
and a gold ripple expands over the screen. Choice, accent, glow level (off / soft / normal / intense) and the Gold-only
effects (dust, shimmer, crown) are saved in SharedPreferences. The gold dust is simulated in Rust per frame
(`particles_step`). `flutter_fx/` has the same effects in Dart (`TxPanel`, `AmbientDust`, `CrownPulse`, `ThemeReveal`);
`flutter run` there shows a Default/Gold toggle.

## Build

1. Open in Android Studio (Koala+), let it sync. No Gradle wrapper is committed: use Studio's Gradle or run
   `gradle wrapper --gradle-version 8.9` once.
2. Optional, for the Rust core: `cargo install cargo-ndk`, set `ANDROID_NDK_HOME`, then `./rust/build-android.sh`.
   Without the `.so` the app uses the identical Kotlin fallback (`NativeCore.isNative` tells which one is active).
3. Install Flutter stable, then run `bash scripts/bootstrap_flutter_module.sh` from the repository root. This generates
   the Android add-to-app project from `flutter_fx/lib`. Run the Gradle build after bootstrapping. Without the generated
   module, native-only development still falls back to the existing Compose TX AI screen.

## Status / honest notes

- Core journal data now persists locally in an atomic SQLite snapshot; trade, strategy, lesson, tag and settings state are restored on app launch.
- Synthetic trade fixtures were removed. Price-based P&L calculation is restricted to supported fiat FX pairs; manual journal entry accepts user-defined asset symbols (for example, XAUUSD or BTCUSDT) without inventing unsupported pip values.
- Trade records now carry optional entry/exit, stop-loss/take-profit, commission, swap, fees, notes, account reference, date, and strategy-version metadata. Quick-add supports manual realized P&L or price-derived Forex P&L.
- The calculation engine uses pair-aware pip sizes, explicit quote-to-account conversion, costs, and optional R multiple. Unit tests cover major-pair, JPY, short-side, invalid-conversion, and FX-only validation cases.
- Multiple account profiles can be created, selected, and archived. Home, performance, strategy comparisons, calendar, tags, trade history, and CSV exports are scoped to the selected account.
- Trade history supports BUY/SELL/WIN/LOSS filters, edit/delete with confirmation, and account-scoped CSV export.
- Strategy edits create revision snapshots and increment the active version; new trades retain the strategy version used.
- Sydney, Tokyo, London, and New York session state and overlap use IANA market timezones with daylight-saving transitions, while the existing globe/timeline components are retained.
- The position journal supports multiple entry/exit fills, partial closes, average prices and realized P&L. Strategy revision history has view/restore support. Education supports ordered text/image/video/document blocks. Strategy attachments and revision attachments are stored as URI references and included in password-encrypted streaming `.tx` exports; the export path is chunked and does not load multi-gigabyte media into memory. New installs no longer receive a synthetic $10,000 account, EURUSD trade defaults, or a preselected lot/result; users must enter account and trade details themselves. Existing user data is preserved, with only an untouched synthetic starter account migrated away.
- Remaining: normalized Room schema and migrations (the current store is an atomic SQLite snapshot), full Persian/English localization across every screen, and device/launcher-specific custom shortcut removal. The economic calendar reads Forex Factory's public weekly JSON export with local caching and request throttling; it is an unofficial feed endpoint and can be unavailable or change. Flutter add-to-app generation and Android embedding must pass the current CI and device launch checks before release.
- Existing theme tokens, colors, reusable components, and navigation styling were preserved; screens were extended additively. Run the latest Android unit tests/build and Rust tests before considering this branch release-ready.


## Newly added trader workflow modules

- **Weekly plan:** Saturday/Sunday are shown as non-trading days; Monday-Friday have separate daily records for trade count (0-100), gross profit/loss, risk cap, daily goal, discipline checklist, plan adherence, mood, notes, and no-trade reason. Week totals and rule-based review prompts are calculated from the saved daily records.
- **Daily no-trade review:** a WorkManager reminder is scheduled for the user-selected local hour. Weekday notifications open the weekly review form directly. NEX Notify also checks session open/close transitions and schedules opt-in calendar-event reminders. Android 13+ notification permission is requested at runtime. WorkManager timing is battery-optimized and is not an exact-alarm guarantee.
- **Education library:** lessons support ordered text, image, video, and document attachments, persisted document URI permissions, open-in-app handling, progress, deletion, and QR scanning/import. Strategies also support user-selected file attachments and versioned attachment snapshots.
- **QR lesson sharing:** new QR payloads encrypt the lesson title and text blocks with password-derived AES-GCM; legacy unencrypted QR payloads remain importable. Attached binary files cannot fit inside a QR; use the password-encrypted streaming `.tx` package for lessons, strategies, revisions and attached files. The password must be shared separately and securely.
- **Forex sessions:** device-local timezone is now the default display mode. Session calculations use IANA market timezones so London/New York/Sydney DST shifts and current overlaps are reflected in the displayed local clock.

The weekly and education modules use isolated local stores so existing trade journal snapshots and other feature models are not rewritten by these additions.


## TX AI — Explainable trader analytics

The hamburger menu includes **TX AI**, a local, account-scoped analysis view built from the user's saved journal:
- Performance by trading session, calendar day, weekday, week, strategy and recorded strategy version, 15-minute entry window, timeframe, market condition and emotion.
- Rust-backed command-center metrics: expectancy, profit factor, drawdown, recovery factor, average win/loss and profit/loss concentration.
- Holding-time mean/median, realized R multiple, consecutive win/loss streaks and confidence-gated best/weakest sessions.
- Trader profile labels are conservative, sample-aware summaries of observed session/strategy concentration, not psychological diagnoses.
- Counterfactual views show the net result after excluding trades flagged as outside-plan and a simple 50%-size risk scenario. The latter assumes P&L scales linearly and is not a backtest.
- Monthly process scores separately cover discipline tags, planned-risk records, strategy-field coverage and decision-record completeness. Scores are withheld when sample coverage is insufficient.
- Behavioral Lab includes explainable Revenge evidence, post-win activity density, size-up after win/loss, recorded exit-before-target and explicit strategy-session drift patterns. User review decisions and pinned insights persist locally with account-scoped keys.
- New trade entries capture actual open time, timeframe, market condition and planned cash risk. Older entries remain compatible; fields not present in historical records are not inferred.
- Flutter TX AI reads only the currently selected account's journal snapshot. It never receives broker credentials; reviews and insight pins are written back through a validated host channel.

TX AI is descriptive coaching, not investment advice, not a prediction engine, and it never places trades. The analytics are deterministic and explainable; no external AI service or cloud upload is used.

## App icons
Settings > آیکون‌ها lets the user pick one of 12 launcher icons (adaptive icons via `activity-alias`, see `AndroidManifest.xml` and `AppIconsSection.kt`). The golden bull (`bull`) is the default app icon.

## NEX Notify (notifications)

- Native Android channels for journal, reminders and reports; persistent local inbox with category filters, read/unread state, completion, clear-all and category/global preferences.
- Action buttons support opening the relevant screen, marking a journal reminder done and snoozing it for 30 minutes.
- Trade-save confirmation, daily review reminders, weekly recap, session state transitions and scheduled calendar-event reminders are routed through the same notification system.
- No notification-listener or overlay permission is used.

## Interactive globe (Sessions)
`ui/components/Globe.kt` + `WorldGeo.kt` + `assets/world_borders.bin`: real country borders (simplified public-domain data), drag to rotate, auto-spin after 2.5 s idle, countries of open sessions glow in the session colour, animated orbits connect overlapping sessions, tap a country for its name/session.

## MetaTrader & Insights (new)
- **متاترید** (More menu): direct Wi-Fi bridge via the `metatrader/TradixBridge.mq4|mq5` Expert Advisors, MT4/MT5 HTML/CSV report import, and MetaApi cloud import. Trades are de-duplicated by ticket. Limits: the EAs have not been compiled in MetaEditor, the bridge only runs while its screen is open on the same network, MetaApi needs your own account/token.
- **بینش‌ها** (More menu): profit factor, expectancy, payoff, streaks, and P&L breakdowns by symbol, weekday, hour, strategy, emotion, tag and side.
- Code is verified only by CI; it was not compiled locally.

# Project history (summary of the whole design/build chat)

1. **Nov 7** – Mobile-only black-gold glass UI/UX for the forex journal (reference: bull/bear, detailed globe, candle performance, heatmaps); trade entry by lot size + profit/loss. Delivered as a 7-screen design canvas.
2. **Nov 7–8** – Real project: Kotlin/Jetpack Compose app, Rust core (`rust/`), Dart/Flutter animations (`flutter_fx/`), "#" hashtags in the More menu, detailed Settings.
3. **Nov 8** – Bugs fixed; Jalali (Shamsi) calendar; working heatmaps (daily/weekly/monthly); small candles; per-trade candle chart ("معاملات"); Rust extended stats.
4. **Nov 8** – 12 selectable launcher icons in Settings (golden bull = main icon).
5. **Nov 9** – Grey inner rectangle on cards fixed; Home chart with live/daily/weekly/monthly/yearly candles; encrypted `.tx` lesson packages; interactive globe with real country borders lit by trading session; hide/restore icons and custom pinned icon.

The user's full written requirements are in `docs/USER_REQUIREMENTS_FA.txt`. Code was written without a compiler available in the build environment: run CI first.

# TD-YAR

A Flutter/Dart foundation for a **manual trading journal and trader reflection companion**.

## Scope of the first phase

TD-YAR is a record-keeping and review tool. All records are entered by the user.

- Manual trade and execution records
- Auditable FIFO-based realized P&L calculations
- Daily journal entries
- User-authored playbooks and checklists
- Local-first SQLite persistence
- A foundation for analytics, calendar views, attachments, and portable backups

**Explicitly out of scope for this phase:** exchange or broker connections, MetaTrader connections, order placement, live market data, trade signals, and automated trading. The app does not provide investment recommendations.

## Architecture

The project follows a feature-first direction with a framework-independent domain core:

- `lib/core/domain`: entities and deterministic business calculations
- `lib/core/data`: local persistence and repository implementations
- `lib/features`: future feature modules; no UI screens are included in this foundation
- `test`: unit tests for domain rules

The domain layer does not depend on Flutter widgets, SQLite, network services, or provider SDKs.

## Getting started

Requires Flutter and Dart from a current stable Flutter SDK.

```bash
flutter pub get
flutter test
```

## P&L calculation notes

Executions are manually recorded as entry or exit fills. The calculator matches exits against earlier entries using FIFO within one trade record. It reports realized gross P&L, fees allocated to closed quantity, realized net P&L, and remaining open quantity. It does not estimate unrealized P&L because this phase has no market-data feed.

For a long record, realized gross P&L is (exit price - entry price) × matched quantity × contract multiplier. For a short record, the price difference is reversed. Fees are allocated proportionally to matched quantities.

These are bookkeeping calculations from user-entered values, not forecasts or trading advice.

## Inspiration and licensing

The product workflow takes inspiration from the public feature concepts and separation of a pure calculation engine in [LuxAlgo Trade Journal](https://github.com/LuxAlgo/trade-journal). TD-YAR is an independent Flutter/Dart implementation; no LuxAlgo source code or branding is copied. Review third-party licenses before adding any dependency or source code.

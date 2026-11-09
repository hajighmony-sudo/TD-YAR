# Architecture

## Principles

1. **Manual-first:** the user is the source of every trade record. No broker, exchange, MetaTrader, market-data, or order-execution integration is part of this phase.
2. **Local-first:** SQLite is the initial persistence layer. Domain calculations remain deterministic and independently testable.
3. **Domain before presentation:** business rules live in plain Dart and must not import Flutter widgets, database packages, or network clients.
4. **Auditable calculations:** every reported metric can be recomputed from the saved executions and documented rules.
5. **User-authored knowledge:** playbooks and journal notes store the user's own observations; they do not generate trade signals or recommendations.

## Current modules

- `core/domain/models`: trade, execution, journal, and playbook entities
- `core/domain/services`: FIFO execution matching and realized P&L
- `core/data/local`: SQLite schema and database lifecycle
- `core/data/repositories`: repository contracts and SQLite implementation

## Data model

- A `Trade` is one manually created position cycle for one symbol and one direction.
- A `TradeExecution` is an entry or exit fill belonging to that trade.
- Exit fills are matched to entry fills in chronological FIFO order.
- An exit that exceeds the currently open quantity is rejected.
- Entry fees are allocated to closed quantity in proportion to the quantity consumed from each entry fill. Exit fees are allocated across the matched quantity of that exit fill.
- Unmatched entry quantity remains open. No unrealized P&L is estimated.

## SQLite tables

- `trades`
- `trade_executions`
- `journal_entries`
- `playbooks`
- `attachments` (metadata only; binary files remain in app-managed local storage)

Schema changes must increment the database version and include an explicit migration.

## Planned next layers (not implemented here)

- Analytics aggregation and date-range filters
- Calendar summaries
- JSON/CSV export and validated import
- Attachment file management
- Routine and review templates
- Riverpod providers and application services
- Presentation/UI layer, added only in a later phase

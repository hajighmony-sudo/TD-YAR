import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';

class AppDatabase {
  AppDatabase._();

  static final AppDatabase instance = AppDatabase._();
  static const String _databaseName = 'td_yar.db';
  static const int _databaseVersion = 3;

  Database? _database;

  Future<Database> get database async {
    final existing = _database;
    if (existing != null) return existing;
    final directory = await getDatabasesPath();
    final opened = await openDatabase(
      path.join(directory, _databaseName),
      version: _databaseVersion,
      onCreate: _createSchema,
      onUpgrade: _upgradeSchema,
      onConfigure: (db) async => db.execute('PRAGMA foreign_keys = ON'),
    );
    _database = opened;
    return opened;
  }

  Future<void> _createSchema(Database db, int version) async {
    await db.execute('''
      CREATE TABLE trades (
        id TEXT PRIMARY KEY, symbol TEXT NOT NULL,
        direction TEXT NOT NULL CHECK (direction IN ('long', 'short')),
        account_label TEXT, notes TEXT NOT NULL DEFAULT '',
        tags_json TEXT NOT NULL DEFAULT '[]',
        contract_multiplier REAL NOT NULL DEFAULT 1 CHECK (contract_multiplier > 0),
        strategy_id TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE trade_executions (
        id TEXT PRIMARY KEY, trade_id TEXT NOT NULL,
        kind TEXT NOT NULL CHECK (kind IN ('entry', 'exit')),
        quantity REAL NOT NULL CHECK (quantity > 0),
        price REAL NOT NULL CHECK (price > 0),
        fee REAL NOT NULL DEFAULT 0 CHECK (fee >= 0),
        executed_at TEXT NOT NULL,
        FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE CASCADE
      )
    ''');
    await db.execute('''
      CREATE TABLE journal_entries (
        id TEXT PRIMARY KEY, entry_date TEXT NOT NULL,
        title TEXT NOT NULL DEFAULT '', content TEXT NOT NULL DEFAULT '',
        linked_trade_ids_json TEXT NOT NULL DEFAULT '[]',
        created_at TEXT NOT NULL, updated_at TEXT NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE playbooks (
        id TEXT PRIMARY KEY, name TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        checklist_json TEXT NOT NULL DEFAULT '[]',
        tags_json TEXT NOT NULL DEFAULT '[]',
        created_at TEXT NOT NULL, updated_at TEXT NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE attachments (
        id TEXT PRIMARY KEY, owner_type TEXT NOT NULL,
        owner_id TEXT NOT NULL, local_path TEXT NOT NULL,
        mime_type TEXT NOT NULL, created_at TEXT NOT NULL
      )
    ''');
    await _createAdvancedTables(db);
  }

  Future<void> _upgradeSchema(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      await db.execute('ALTER TABLE trades ADD COLUMN strategy_id TEXT');
      await _createAdvancedTables(db);
    }
    if (oldVersion < 3) await _createIndexes(db);
  }

  Future<void> _createAdvancedTables(Database db) async {
    await db.execute('''
      CREATE TABLE IF NOT EXISTS strategies (
        id TEXT PRIMARY KEY, name TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '', market TEXT NOT NULL DEFAULT '',
        timeframes_json TEXT NOT NULL DEFAULT '[]',
        setup_conditions_json TEXT NOT NULL DEFAULT '[]',
        entry_rules_json TEXT NOT NULL DEFAULT '[]',
        exit_rules_json TEXT NOT NULL DEFAULT '[]',
        risk_rules_json TEXT NOT NULL DEFAULT '[]',
        checklist_json TEXT NOT NULL DEFAULT '[]',
        tags_json TEXT NOT NULL DEFAULT '[]',
        is_archived INTEGER NOT NULL DEFAULT 0,
        created_at TEXT NOT NULL, updated_at TEXT NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE IF NOT EXISTS risk_profiles (
        id TEXT PRIMARY KEY, name TEXT NOT NULL,
        account_balance REAL NOT NULL CHECK (account_balance > 0),
        currency TEXT NOT NULL,
        max_risk_per_trade_percent REAL NOT NULL CHECK (max_risk_per_trade_percent >= 0),
        max_daily_loss_percent REAL NOT NULL CHECK (max_daily_loss_percent >= 0),
        max_trades_per_day INTEGER NOT NULL CHECK (max_trades_per_day >= 0),
        created_at TEXT NOT NULL, updated_at TEXT NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE IF NOT EXISTS trade_risk_records (
        id TEXT PRIMARY KEY, trade_id TEXT NOT NULL UNIQUE,
        entry_price REAL NOT NULL, stop_price REAL NOT NULL,
        target_price REAL NOT NULL, quantity REAL NOT NULL,
        contract_multiplier REAL NOT NULL,
        account_balance_snapshot REAL NOT NULL,
        planned_risk_amount REAL NOT NULL,
        planned_reward_amount REAL NOT NULL,
        risk_reward_ratio REAL NOT NULL, risk_percent REAL NOT NULL,
        notes TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL,
        FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE CASCADE
      )
    ''');
    await db.execute('''
      CREATE TABLE IF NOT EXISTS psychology_entries (
        id TEXT PRIMARY KEY, trade_id TEXT, entry_date TEXT NOT NULL,
        mood TEXT NOT NULL,
        confidence INTEGER NOT NULL CHECK (confidence BETWEEN 1 AND 5),
        focus INTEGER NOT NULL CHECK (focus BETWEEN 1 AND 5),
        stress INTEGER NOT NULL CHECK (stress BETWEEN 1 AND 5),
        followed_plan INTEGER NOT NULL DEFAULT 1,
        is_fomo INTEGER NOT NULL DEFAULT 0,
        is_revenge_trade INTEGER NOT NULL DEFAULT 0,
        notes TEXT NOT NULL DEFAULT '',
        emotions_json TEXT NOT NULL DEFAULT '[]',
        created_at TEXT NOT NULL, updated_at TEXT NOT NULL,
        FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE SET NULL
      )
    ''');
    await _createIndexes(db);
  }

  Future<void> _createIndexes(Database db) async {
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_trade_executions_trade_time ON trade_executions(trade_id, executed_at)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_trade_executions_time ON trade_executions(executed_at)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_trades_symbol_created ON trades(symbol, created_at)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_trades_strategy ON trades(strategy_id)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_journal_entries_date ON journal_entries(entry_date)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_psychology_date ON psychology_entries(entry_date)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_psychology_trade ON psychology_entries(trade_id)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_strategies_name ON strategies(name COLLATE NOCASE)',
    );
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_risk_profiles_updated ON risk_profiles(updated_at)',
    );
  }

  Future<void> close() async {
    final current = _database;
    _database = null;
    if (current != null) await current.close();
  }
}

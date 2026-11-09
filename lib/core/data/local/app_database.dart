import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';

class AppDatabase {
  AppDatabase._();

  static final AppDatabase instance = AppDatabase._();
  static const String _databaseName = 'td_yar.db';
  static const int _databaseVersion = 7;

  Database? _database;

  Future<String> get databaseFile async => path.join(await getDatabasesPath(), _databaseName);

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
        account_label TEXT, account_id TEXT REFERENCES trading_accounts(id) ON DELETE SET NULL,
        notes TEXT NOT NULL DEFAULT '',
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
        executed_at TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '',
        FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE CASCADE
      )
    ''');
    await db.execute('''
      CREATE TABLE journal_entries (
        id TEXT PRIMARY KEY, entry_date TEXT NOT NULL,
        title TEXT NOT NULL DEFAULT '', content TEXT NOT NULL DEFAULT '',
        pre_market_plan TEXT NOT NULL DEFAULT '',
        market_context TEXT NOT NULL DEFAULT '',
        execution_review TEXT NOT NULL DEFAULT '',
        mistakes TEXT NOT NULL DEFAULT '',
        strengths TEXT NOT NULL DEFAULT '',
        lessons TEXT NOT NULL DEFAULT '',
        next_day_plan TEXT NOT NULL DEFAULT '',
        goals_json TEXT NOT NULL DEFAULT '[]',
        tags_json TEXT NOT NULL DEFAULT '[]',
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
    await _createFoundationTables(db);
  }

  Future<void> _upgradeSchema(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      await db.execute('ALTER TABLE trades ADD COLUMN strategy_id TEXT');
      await _createAdvancedTables(db);
    }
    if (oldVersion < 3) await _createIndexes(db);
    if (oldVersion < 4) {
      await db.execute(
        "ALTER TABLE trade_executions ADD COLUMN notes TEXT NOT NULL DEFAULT ''",
      );
    }
    if (oldVersion < 5) {
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN pre_market_plan TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN market_context TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN execution_review TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN mistakes TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN strengths TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN lessons TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN next_day_plan TEXT NOT NULL DEFAULT ''",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN goals_json TEXT NOT NULL DEFAULT '[]'",
      );
      await db.execute(
        "ALTER TABLE journal_entries ADD COLUMN tags_json TEXT NOT NULL DEFAULT '[]'",
      );
      await _createSessionTable(db);
    }
    if (oldVersion < 6) await _createFoundationTables(db);
    if (oldVersion < 7) await _upgradeTradeAccounts(db);
  }

  Future<void> _upgradeTradeAccounts(Database db) async {
    // Preserve legacy labels while assigning a real account record to each.
    await db.execute(
      'ALTER TABLE trades ADD COLUMN account_id TEXT REFERENCES trading_accounts(id) ON DELETE SET NULL',
    );
    await db.execute('''
      INSERT OR IGNORE INTO trading_accounts
        (id, name, currency, initial_balance, is_archived, notes, created_at, updated_at)
      SELECT
        'legacy-' || lower(hex(randomblob(16))),
        TRIM(account_label),
        'UNSPECIFIED',
        0,
        0,
        'Imported automatically from a legacy trade account label',
        MIN(created_at),
        MAX(updated_at)
      FROM trades
      WHERE account_label IS NOT NULL AND TRIM(account_label) != ''
      GROUP BY TRIM(account_label) COLLATE NOCASE
    ''');
    await db.execute('''
      UPDATE trades
      SET account_id = (
        SELECT id FROM trading_accounts
        WHERE name = TRIM(trades.account_label) COLLATE NOCASE
        LIMIT 1
      )
      WHERE account_label IS NOT NULL AND TRIM(account_label) != ''
    ''');
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_trades_account_id_created ON trades(account_id, created_at)',
    );
    await _createAuditTriggers(db);
  }

  Future<void> _createFoundationTables(Database db) async {
    await db.execute('''CREATE TABLE IF NOT EXISTS learning_resources (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, body TEXT NOT NULL DEFAULT '', media_path TEXT, caption TEXT NOT NULL DEFAULT '', category TEXT NOT NULL DEFAULT '', tags_json TEXT NOT NULL DEFAULT '[]', related_strategy_ids_json TEXT NOT NULL DEFAULT '[]', source_url TEXT, is_favorite INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL, updated_at TEXT NOT NULL)''');
    await db.execute('''CREATE TABLE IF NOT EXISTS trading_accounts (id TEXT PRIMARY KEY, name TEXT NOT NULL COLLATE NOCASE, currency TEXT NOT NULL, initial_balance REAL NOT NULL DEFAULT 0, is_archived INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL, updated_at TEXT NOT NULL)''');
    await db.execute('CREATE UNIQUE INDEX IF NOT EXISTS idx_trading_accounts_name ON trading_accounts(name COLLATE NOCASE)');
    await db.execute('CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT NOT NULL, updated_at TEXT NOT NULL)');
    await db.execute('''CREATE TABLE IF NOT EXISTS audit_events (id TEXT PRIMARY KEY, event_type TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id TEXT NOT NULL, details_json TEXT NOT NULL DEFAULT '{}', occurred_at TEXT NOT NULL)''');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_learning_updated ON learning_resources(updated_at)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_learning_category ON learning_resources(category)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_events(occurred_at)');
    await _createAuditTriggers(db);
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
    await _createSessionTable(db);
    await _createIndexes(db);
  }

  Future<void> _createSessionTable(Database db) async {
    await db.execute('''
      CREATE TABLE IF NOT EXISTS trading_sessions (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        timezone_id TEXT NOT NULL,
        open_hour INTEGER NOT NULL CHECK (open_hour BETWEEN 0 AND 23),
        open_minute INTEGER NOT NULL CHECK (open_minute BETWEEN 0 AND 59),
        close_hour INTEGER NOT NULL CHECK (close_hour BETWEEN 0 AND 23),
        close_minute INTEGER NOT NULL CHECK (close_minute BETWEEN 0 AND 59),
        weekdays_json TEXT NOT NULL DEFAULT '[1,2,3,4,5]',
        is_enabled INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      )
    ''');
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_trading_sessions_enabled ON trading_sessions(is_enabled)',
    );
  }

  Future<void> _createAuditTriggers(Database db) async {
    const auditedTables = <Map<String, String>>[
      {'table': 'trades', 'entity': 'trade', 'key': 'id'},
      {'table': 'trade_executions', 'entity': 'trade_execution', 'key': 'id'},
      {'table': 'journal_entries', 'entity': 'journal_entry', 'key': 'id'},
      {'table': 'strategies', 'entity': 'strategy', 'key': 'id'},
      {'table': 'playbooks', 'entity': 'playbook', 'key': 'id'},
      {'table': 'psychology_entries', 'entity': 'psychology_entry', 'key': 'id'},
      {'table': 'risk_profiles', 'entity': 'risk_profile', 'key': 'id'},
      {'table': 'trade_risk_records', 'entity': 'trade_risk_record', 'key': 'id'},
      {'table': 'trading_sessions', 'entity': 'trading_session', 'key': 'id'},
      {'table': 'learning_resources', 'entity': 'learning_resource', 'key': 'id'},
      {'table': 'trading_accounts', 'entity': 'trading_account', 'key': 'id'},
      {'table': 'app_settings', 'entity': 'app_setting', 'key': 'key'},
    ];

    for (final spec in auditedTables) {
      final table = spec['table']!;
      final entity = spec['entity']!;
      final key = spec['key']!;
      for (final operation in ['insert', 'update', 'delete']) {
        final trigger = 'audit_${table}_${operation}';
        final row = operation == 'delete' ? 'OLD' : 'NEW';
        await db.execute('''
          CREATE TRIGGER IF NOT EXISTS $trigger
          AFTER ${operation.toUpperCase()} ON $table
          BEGIN
            INSERT INTO audit_events
              (id, event_type, entity_type, entity_id, details_json, occurred_at)
            VALUES (
              lower(hex(randomblob(16))),
              '$operation',
              '$entity',
              CAST($row.$key AS TEXT),
              '{}',
              strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
            );
          END
        ''');
      }
    }

    await db.execute('''
      CREATE TRIGGER IF NOT EXISTS audit_events_no_update
      BEFORE UPDATE ON audit_events
      BEGIN
        SELECT RAISE(ABORT, 'audit_events are append-only');
      END
    ''');
    await db.execute('''
      CREATE TRIGGER IF NOT EXISTS audit_events_no_delete
      BEFORE DELETE ON audit_events
      BEGIN
        SELECT RAISE(ABORT, 'audit_events are append-only');
      END
    ''');
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
      'CREATE INDEX IF NOT EXISTS idx_trades_account_created ON trades(account_label, created_at)',
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

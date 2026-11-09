import 'data/local/app_database.dart';
import 'data/repositories/app_infrastructure_repository.dart';
import 'data/repositories/journal_repository.dart';
import 'data/repositories/learning_repository.dart';
import 'data/repositories/playbook_repository.dart';
import 'data/repositories/psychology_repository.dart';
import 'data/repositories/risk_repository.dart';
import 'data/repositories/strategy_repository.dart';
import 'data/repositories/trade_repository.dart';
import 'data/repositories/trading_session_repository.dart';
import 'data/repositories/sqlite_app_infrastructure_repository.dart';
import 'data/repositories/sqlite_journal_repository.dart';
import 'data/repositories/sqlite_learning_repository.dart';
import 'data/repositories/sqlite_playbook_repository.dart';
import 'data/repositories/sqlite_psychology_repository.dart';
import 'data/repositories/sqlite_risk_repository.dart';
import 'data/repositories/sqlite_strategy_repository.dart';
import 'data/repositories/sqlite_trade_repository.dart';
import 'data/repositories/sqlite_trading_session_repository.dart';
import 'domain/services/daily_journal_service.dart';
import 'domain/services/database_backup_service.dart';
import 'domain/services/learning_library_service.dart';
import 'domain/services/json_archive_import_service.dart';
import 'domain/services/performance_analytics_service.dart';
import 'domain/services/psychology_analytics_service.dart';
import 'domain/services/report_export_service.dart';
import 'domain/services/risk_management_service.dart';
import 'domain/services/strategy_management_service.dart';
import 'domain/services/trading_calendar_service.dart';
import 'domain/services/trading_session_service.dart';

/// Composition root for the complete offline TD-YAR domain.
///
/// All SQLite repositories share one database instance. The object is intended
/// to be created once at application startup and injected into presentation
/// state/providers. It deliberately contains no Flutter UI dependencies.
class TdYarServices {
  TdYarServices._({
    required this.database,
    required this.trades,
    required this.journals,
    required this.playbooks,
    required this.strategies,
    required this.psychology,
    required this.risk,
    required this.sessions,
    required this.learning,
    required this.infrastructure,
    required this.performance,
    required this.dailyJournal,
    required this.calendar,
    required this.psychologyAnalytics,
    required this.riskManagement,
    required this.strategyManagement,
    required this.sessionManagement,
    required this.learningLibrary,
    required this.reports,
    required this.backups,
    required this.archiveImporter,
  });

  factory TdYarServices({AppDatabase? database}) {
    final db = database ?? AppDatabase.instance;
    final trades = SqliteTradeRepository(appDatabase: db);
    final journals = SqliteJournalRepository(appDatabase: db);
    final playbooks = SqlitePlaybookRepository(appDatabase: db);
    final strategies = SqliteStrategyRepository(database: db);
    final psychology = SqlitePsychologyRepository(database: db);
    final risk = SqliteRiskRepository(database: db);
    final sessions = SqliteTradingSessionRepository(database: db);
    final learning = SqliteLearningRepository(database: db);
    final infrastructure = SqliteAppInfrastructureRepository(database: db);

    final performance = PerformanceAnalyticsService(tradeRepository: trades);
    final dailyJournal = DailyJournalService(repository: journals);
    final calendar = TradingCalendarService(
      analyticsService: performance,
      journalRepository: journals,
      psychologyRepository: psychology,
    );
    final psychologyAnalytics = PsychologyAnalyticsService(
      psychologyRepository: psychology,
      performanceService: performance,
    );
    final riskManagement = RiskManagementService(
      riskRepository: risk,
      tradeRepository: trades,
      performanceService: performance,
    );
    final strategyManagement = StrategyManagementService(strategies);
    final sessionManagement = TradingSessionService(repository: sessions);
    final learningLibrary = LearningLibraryService(
      learning,
      strategyRepository: strategies,
    );
    final reports = ReportExportService(
      tradeRepository: trades,
      performanceService: performance,
      journalRepository: journals,
      strategyRepository: strategies,
      psychologyRepository: psychology,
      riskRepository: risk,
      learningRepository: learning,
      tradingSessionRepository: sessions,
      infrastructureRepository: infrastructure,
      playbookRepository: playbooks,
      database: db,
    );

    return TdYarServices._(
      database: db,
      trades: trades,
      journals: journals,
      playbooks: playbooks,
      strategies: strategies,
      psychology: psychology,
      risk: risk,
      sessions: sessions,
      learning: learning,
      infrastructure: infrastructure,
      performance: performance,
      dailyJournal: dailyJournal,
      calendar: calendar,
      psychologyAnalytics: psychologyAnalytics,
      riskManagement: riskManagement,
      strategyManagement: strategyManagement,
      sessionManagement: sessionManagement,
      learningLibrary: learningLibrary,
      reports: reports,
      backups: DatabaseBackupService(database: db),
      archiveImporter: JsonArchiveImportService(database: db),
    );
  }

  final AppDatabase database;
  final TradeRepository trades;
  final JournalRepository journals;
  final PlaybookRepository playbooks;
  final StrategyRepository strategies;
  final PsychologyRepository psychology;
  final RiskRepository risk;
  final TradingSessionRepository sessions;
  final LearningRepository learning;
  final AppInfrastructureRepository infrastructure;

  final PerformanceAnalyticsService performance;
  final DailyJournalService dailyJournal;
  final TradingCalendarService calendar;
  final PsychologyAnalyticsService psychologyAnalytics;
  final RiskManagementService riskManagement;
  final StrategyManagementService strategyManagement;
  final TradingSessionService sessionManagement;
  final LearningLibraryService learningLibrary;
  final ReportExportService reports;
  final DatabaseBackupService backups;
  final JsonArchiveImportService archiveImporter;

  /// Opens and verifies the local database before the app exposes services.
  Future<void> initialize() async {
    await database.database;
  }

  /// Closes the shared SQLite connection during application shutdown.
  /// Services can be initialized again after this call.
  Future<void> close() => database.close();
}

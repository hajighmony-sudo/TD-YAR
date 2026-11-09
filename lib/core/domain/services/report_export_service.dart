import 'dart:convert';
import 'dart:typed_data';

import 'package:pdf/widgets.dart' as pw;

import '../../data/repositories/app_infrastructure_repository.dart';
import '../../data/repositories/journal_repository.dart';
import '../../data/repositories/learning_repository.dart';
import '../../data/repositories/psychology_repository.dart';
import '../../data/repositories/playbook_repository.dart';
import '../../data/local/app_database.dart';
import '../../data/repositories/risk_repository.dart';
import '../../data/repositories/strategy_repository.dart';
import '../../data/repositories/trading_session_repository.dart';
import '../../data/repositories/trade_repository.dart';
import '../models/performance_report.dart';
import '../models/trade.dart';
import 'performance_analytics_service.dart';

/// Exports manually recorded data. Optional repositories keep this service
/// source-compatible with smaller installations while allowing a full archive.
class ReportExportService {
  const ReportExportService({
    required TradeRepository tradeRepository,
    required PerformanceAnalyticsService performanceService,
    required JournalRepository journalRepository,
    required StrategyRepository strategyRepository,
    PsychologyRepository? psychologyRepository,
    RiskRepository? riskRepository,
    LearningRepository? learningRepository,
    TradingSessionRepository? tradingSessionRepository,
    AppInfrastructureRepository? infrastructureRepository,
    PlaybookRepository? playbookRepository,
    AppDatabase? database,
  })  : _trades = tradeRepository,
        _performance = performanceService,
        _journals = journalRepository,
        _strategies = strategyRepository,
        _psychology = psychologyRepository,
        _risk = riskRepository,
        _learning = learningRepository,
        _sessions = tradingSessionRepository,
        _infrastructure = infrastructureRepository,
        _playbooks = playbookRepository,
        _database = database;

  final TradeRepository _trades;
  final PerformanceAnalyticsService _performance;
  final JournalRepository _journals;
  final StrategyRepository _strategies;
  final PsychologyRepository? _psychology;
  final RiskRepository? _risk;
  final LearningRepository? _learning;
  final TradingSessionRepository? _sessions;
  final AppInfrastructureRepository? _infrastructure;
  final PlaybookRepository? _playbooks;
  final AppDatabase? _database;

  Future<String> exportJson({DateTime? from, DateTime? to, String? accountId, String? strategyId}) async {
    _validate(from, to);
    final allTrades = await _trades.getTrades(accountId: accountId, strategyId: strategyId);
    final selectedTrades = <Map<String, Object?>>[];
    final selectedTradeIds = <String>{};
    for (final trade in allTrades) {
      final fills = await _trades.getExecutions(trade.id);
      final matchesRange = fills.any((fill) =>
          (from == null || !fill.executedAt.isBefore(from)) &&
          (to == null || !fill.executedAt.isAfter(to)));
      if (from != null || to != null) {
        if (!matchesRange) continue;
      }
      // Keep every fill for a selected trade so FIFO history remains replayable.
      selectedTradeIds.add(trade.id);
      selectedTrades.add({
        'trade': trade.toMap(),
        'executions': fills.map((fill) => fill.toMap()).toList(),
      });
    }

    final journals = await _journals.getEntries(from: from, to: to);
    final strategies = await _strategies.getAll(includeArchived: true);
    final playbooks = _playbooks == null
        ? <Map<String, Object?>>[]
        : (await _playbooks.getPlaybooks()).map((item) => item.toMap()).toList();
    final attachments = _database == null
        ? <Map<String, Object?>>[]
        : (await (await _database.database).query('attachments'))
            .map((row) => Map<String, Object?>.from(row))
            .toList();
    final report = await _performance.buildReport(from: from, to: to, accountId: accountId, strategyId: strategyId);
    final psychology = _psychology == null
        ? <Map<String, Object?>>[]
        : (await _psychology.getEntries(from: from, to: to))
            .map((entry) => entry.toMap())
            .toList();
    final riskProfiles = _risk == null
        ? <Map<String, Object?>>[]
        : (await _risk.getProfiles())
            .map((profile) => profile.toMap())
            .toList();
    final tradeRisks = <Map<String, Object?>>[];
    if (_risk != null) {
      for (final id in selectedTradeIds) {
        final record = await _risk.getTradeRisk(id);
        if (record != null) tradeRisks.add(record.toMap());
      }
    }
    final learning = _learning == null
        ? <Map<String, Object?>>[]
        : (await _learning.list())
            .map((resource) => resource.toMap())
            .toList();
    final sessions = _sessions == null
        ? <Map<String, Object?>>[]
        : (await _sessions.getAll(includeDisabled: true))
            .map((session) => session.toMap())
            .toList();

    final accounts = _infrastructure == null
        ? <Map<String, Object?>>[]
        : (await _infrastructure.listAccounts(includeArchived: true))
            .map((account) => account.toMap())
            .toList();
    final settings = _infrastructure == null
        ? <String, String>{}
        : await _infrastructure.getSettings();
    final auditEvents = _infrastructure == null
        ? <Map<String, Object?>>[]
        : (await _infrastructure.getAuditEvents(limit: 5000))
            .map((event) => {
                  'id': event.id,
                  'event_type': event.eventType,
                  'entity_type': event.entityType,
                  'entity_id': event.entityId,
                  'details_json': event.detailsJson,
                  'occurred_at': event.occurredAt.toUtc().toIso8601String(),
                })
            .toList();

    return const JsonEncoder.withIndent('  ').convert({
      'format': 'td-yar-export',
      'schema_version': 2,
      'exported_at': DateTime.now().toUtc().toIso8601String(),
      'filters': {
        'from': from?.toUtc().toIso8601String(),
        'to': to?.toUtc().toIso8601String(),
        'account_id': accountId,
        'strategy_id': strategyId,
      },
      'trades': selectedTrades,
      'journal_entries': journals.map((entry) => entry.toMap()).toList(),
      'strategies': strategies.map((strategy) => strategy.toMap()).toList(),
      'playbooks': playbooks,
      'attachments': attachments,
      'psychology_entries': psychology,
      'risk_profiles': riskProfiles,
      'trade_risk_records': tradeRisks,
      'learning_resources': learning,
      'trading_sessions': sessions,
      'accounts': accounts,
      'settings': settings,
      'audit_events': auditEvents,
      'performance': _reportMap(report),
      'optional_modules_included': {
        'psychology': _psychology != null,
        'risk': _risk != null,
        'learning': _learning != null,
        'sessions': _sessions != null,
        'infrastructure': _infrastructure != null,
        'playbooks': _playbooks != null,
        'attachments': _database != null,
      },
    });
  }

  Future<Uint8List> exportPdf({DateTime? from, DateTime? to, String? accountId, String? strategyId, TradeDirection? direction}) async {
    _validate(from, to);
    final report = await _performance.buildReport(from: from, to: to, accountId: accountId, strategyId: strategyId, direction: direction);
    final document = pw.Document();
    final rows = <List<String>>[
      ['Metric', 'Value'],
      ['Closed trades', '${report.totalTrades}'],
      ['Wins / losses / breakeven',
        '${report.wins} / ${report.losses} / ${report.breakeven}'],
      ['Win rate', '${report.winRate.toStringAsFixed(2)}%'],
      ['Gross P&L', report.grossPnl.toStringAsFixed(2)],
      ['Net P&L', report.netPnl.toStringAsFixed(2)],
      ['Fees', report.totalFees.toStringAsFixed(2)],
      ['Average win', report.averageWin.toStringAsFixed(2)],
      ['Average loss', report.averageLoss.toStringAsFixed(2)],
      ['Profit factor', report.profitFactor?.toStringAsFixed(2) ?? 'N/A'],
      ['Expectancy', report.expectancy.toStringAsFixed(2)],
      ['Maximum drawdown', report.maxDrawdown.toStringAsFixed(2)],
    ];
    document.addPage(pw.MultiPage(build: (context) => [
      pw.Header(level: 0, child: pw.Text('TD-YAR Performance Report')),
      pw.Text(
        'Period: ${from?.toIso8601String() ?? 'All time'} — '
        '${to?.toIso8601String() ?? 'All time'}',
      ),
      pw.SizedBox(height: 16),
      pw.TableHelper.fromTextArray(data: rows),
      pw.SizedBox(height: 18),
      pw.Text(
        'This report summarizes manually entered journal records. '
        'It is historical bookkeeping, not a forecast or trading recommendation.',
      ),
    ]));
    return document.save();
  }

  Future<String> exportRealizationsCsv({
    DateTime? from,
    DateTime? to,
    String? accountId,
    String? strategyId,
    TradeDirection? direction,
  }) async {
    _validate(from, to);
    final report = await _performance.buildReport(from: from, to: to, accountId: accountId, strategyId: strategyId, direction: direction);
    final rows = <List<Object?>>[
      [
        'trade_id', 'execution_id', 'symbol', 'direction', 'strategy_id',
        'account', 'closed_at', 'quantity', 'gross_pnl', 'fees', 'net_pnl',
        'duration_minutes',
      ],
      ...report.realizations.map((item) => [
        item.tradeId,
        item.executionId,
        item.symbol,
        item.direction.name,
        item.strategyId ?? '',
        item.accountLabel ?? '',
        item.closedAt.toUtc().toIso8601String(),
        item.closedQuantity,
        item.grossPnl,
        item.fees,
        item.netPnl,
        item.duration.inMinutes,
      ]),
    ];
    return rows
        .map((row) => row.map(csvCell).join(','))
        .join('\r\n');
  }

  static String csvCell(Object? value) {
    final text = value?.toString() ?? '';
    return '"${text.replaceAll('"', '""')}"';
  }

  static void _validate(DateTime? from, DateTime? to) {
    if (from != null && to != null && to.isBefore(from)) {
      throw ArgumentError('Export end date must not precede start date.');
    }
  }

  static Map<String, Object?> _reportMap(PerformanceReport report) => {
    'total_trades': report.totalTrades,
    'wins': report.wins,
    'losses': report.losses,
    'breakeven': report.breakeven,
    'net_pnl': report.netPnl,
    'gross_pnl': report.grossPnl,
    'fees': report.totalFees,
    'win_rate': report.winRate,
    'average_win': report.averageWin,
    'average_loss': report.averageLoss,
    'profit_factor': report.profitFactor,
    'expectancy': report.expectancy,
    'max_drawdown': report.maxDrawdown,
  };
}

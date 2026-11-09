import 'package:flutter/services.dart';

/// Safe, account-scoped journal payload exchanged with the Android host.
/// It contains journal fields only; broker credentials and account secrets
/// must never be included in this contract.
class TxAiTradeRecord {
  const TxAiTradeRecord({
    required this.id,
    required this.accountId,
    required this.symbol,
    required this.isBuy,
    required this.pnl,
    required this.lot,
    required this.epochDay,
    required this.strategy,
    required this.tags,
    required this.emotion,
    this.openedAtMillis,
    this.closedAtMillis,
    this.plannedRisk,
    this.timeframe,
    this.marketCondition,
    this.strategyFollowed,
    this.strategyVersion = 1,
    this.session,
    this.entryPrice,
    this.exitPrice,
    this.stopLossPrice,
    this.takeProfitPrice,
    this.displayHour,
    this.displayWeekday,
    this.displayMinute,
    this.displayDateKey,
    this.displayMonthKey,
    this.displayClosedMonthKey,
  });

  final int id;
  final String accountId;
  final String symbol;
  final bool isBuy;
  final double pnl;
  final double lot;
  final int epochDay;
  final String strategy;
  final List<String> tags;
  final String emotion;
  final int? openedAtMillis;
  final int? closedAtMillis;
  final double? plannedRisk;
  final String? timeframe;
  final String? marketCondition;
  final bool? strategyFollowed;
  final int strategyVersion;
  final String? session;
  final double? entryPrice;
  final double? exitPrice;
  final double? stopLossPrice;
  final double? takeProfitPrice;
  final int? displayHour;
  final int? displayWeekday;
  final int? displayMinute;
  final String? displayDateKey;
  final String? displayMonthKey;
  final String? displayClosedMonthKey;

  static TxAiTradeRecord? fromMap(
    Map<Object?, Object?> map, {
    required String selectedAccountId,
  }) {
    final accountId = map['accountId'];
    if (accountId is! String || accountId != selectedAccountId) return null;
    final id = map['id'];
    final symbol = map['symbol'];
    final isBuy = map['isBuy'];
    final pnl = _finiteNumber(map['pnl']);
    final lot = _finiteNumber(map['lot']);
    if (id is! int || symbol is! String || isBuy is! bool || pnl == null || lot == null || lot <= 0) {
      return null;
    }
    final rawTags = map['tags'];
    final tags = rawTags is List
        ? rawTags.whereType<String>().toList(growable: false)
        : const <String>[];
    return TxAiTradeRecord(
      id: id,
      accountId: accountId,
      symbol: symbol,
      isBuy: isBuy,
      pnl: pnl,
      lot: lot,
      epochDay: map['epochDay'] is int ? map['epochDay'] as int : 0,
      strategy: map['strategy'] is String ? map['strategy'] as String : '',
      tags: tags,
      emotion: map['emotion'] is String ? map['emotion'] as String : '',
      openedAtMillis: map['openedAtMillis'] is int ? map['openedAtMillis'] as int : null,
      closedAtMillis: map['closedAtMillis'] is int ? map['closedAtMillis'] as int : null,
      plannedRisk: _finiteNumber(map['plannedRisk']),
      timeframe: map['timeframe'] is String ? map['timeframe'] as String : null,
      marketCondition: map['marketCondition'] is String ? map['marketCondition'] as String : null,
      strategyFollowed: map['strategyFollowed'] is bool ? map['strategyFollowed'] as bool : null,
      strategyVersion: map['strategyVersion'] is int ? map['strategyVersion'] as int : 1,
      session: map['session'] is String ? map['session'] as String : null,
      entryPrice: _finiteNumber(map['entryPrice']),
      exitPrice: _finiteNumber(map['exitPrice']),
      stopLossPrice: _finiteNumber(map['stopLossPrice']),
      takeProfitPrice: _finiteNumber(map['takeProfitPrice']),
      displayHour: map['displayHour'] is int ? map['displayHour'] as int : null,
      displayWeekday: map['displayWeekday'] is int ? map['displayWeekday'] as int : null,
      displayMinute: map['displayMinute'] is int ? map['displayMinute'] as int : null,
      displayDateKey: map['displayDateKey'] is String ? map['displayDateKey'] as String : null,
      displayMonthKey: map['displayMonthKey'] is String ? map['displayMonthKey'] as String : null,
      displayClosedMonthKey: map['displayClosedMonthKey'] is String ? map['displayClosedMonthKey'] as String : null,
    );
  }
}

class TxAiBehaviorPatternRecord {
  const TxAiBehaviorPatternRecord({
    required this.id,
    required this.title,
    required this.triggerTradeId,
    required this.linkedTradeIds,
    required this.evidenceScore,
    required this.actualNetPnl,
    required this.reasons,
    this.reviewStatus,
  });

  final String id;
  final String title;
  final int triggerTradeId;
  final List<int> linkedTradeIds;
  final int evidenceScore;
  final double actualNetPnl;
  final List<String> reasons;
  final String? reviewStatus;

  static TxAiBehaviorPatternRecord? fromMap(
    Map<Object?, Object?> map,
    Map<String, String> reviews,
  ) {
    final id = map['id'];
    final title = map['title'];
    final triggerTradeId = map['triggerTradeId'];
    final evidenceScore = map['evidenceScore'];
    final actualNetPnl = _finiteNumber(map['actualNetPnl']);
    if (id is! String ||
        id.isEmpty ||
        title is! String ||
        triggerTradeId is! int ||
        evidenceScore is! int ||
        actualNetPnl == null) {
      return null;
    }
    final rawIds = map['linkedTradeIds'];
    final linkedIds = rawIds is List
        ? rawIds.whereType<int>().toList(growable: false)
        : <int>[];
    final rawReasons = map['reasons'];
    final reasons = rawReasons is List
        ? rawReasons.whereType<String>().toList(growable: false)
        : <String>[];
    return TxAiBehaviorPatternRecord(
      id: id,
      title: title,
      triggerTradeId: triggerTradeId,
      linkedTradeIds: List<int>.unmodifiable(linkedIds),
      evidenceScore: evidenceScore.clamp(0, 100).toInt(),
      actualNetPnl: actualNetPnl,
      reasons: List<String>.unmodifiable(reasons),
      reviewStatus: reviews[id],
    );
  }
}

class TxAiAccountSnapshot {
  const TxAiAccountSnapshot({
    required this.accountId,
    required this.accountName,
    required this.currency,
    required this.trades,
    this.startingBalance = 0,
    this.riskPercent = 1.0,
    this.displayTimezone = 'UTC',
    this.currentDisplayDateKey,
    this.currentMonthKey,
    this.episodeReviews = const <String, String>{},
    this.pinnedInsights = const <String>{},
    this.behavioralPatterns = const <TxAiBehaviorPatternRecord>[],
  });

  final String accountId;
  final String accountName;
  final String currency;
  final List<TxAiTradeRecord> trades;
  final double startingBalance;
  final double riskPercent;
  final String displayTimezone;
  final String? currentDisplayDateKey;
  final String? currentMonthKey;
  final Map<String, String> episodeReviews;
  final Set<String> pinnedInsights;
  final List<TxAiBehaviorPatternRecord> behavioralPatterns;

  factory TxAiAccountSnapshot.fromMap(Map<Object?, Object?> map) {
    final accountId = map['accountId'];
    if (accountId is! String || accountId.isEmpty) {
      throw const FormatException('Selected account id is missing.');
    }
    final rawTrades = map['trades'];
    final trades = <TxAiTradeRecord>[];
    if (rawTrades is List) {
      for (final item in rawTrades) {
        if (item is Map) {
          final record = TxAiTradeRecord.fromMap(
            Map<Object?, Object?>.from(item),
            selectedAccountId: accountId,
          );
          if (record != null) trades.add(record);
        }
      }
    }
    final reviews = <String, String>{};
    final rawReviews = map['episodeReviews'];
    if (rawReviews is List) {
      for (final item in rawReviews) {
        if (item is Map) {
          final row = Map<Object?, Object?>.from(item);
          final id = row['episodeId'];
          final status = row['status'];
          if (id is String && id.isNotEmpty && status is String) reviews[id] = status;
        }
      }
    }
    final patterns = <TxAiBehaviorPatternRecord>[];
    final rawPatterns = map['behavioralPatterns'];
    if (rawPatterns is List) {
      for (final item in rawPatterns) {
        if (item is Map) {
          final pattern = TxAiBehaviorPatternRecord.fromMap(
            Map<Object?, Object?>.from(item),
            reviews,
          );
          if (pattern != null) patterns.add(pattern);
        }
      }
    }
    final pins = <String>{};
    final rawPins = map['pinnedInsights'];
    if (rawPins is List) {
      for (final item in rawPins) {
        if (item is Map) {
          final row = Map<Object?, Object?>.from(item);
          final id = row['insightId'];
          if (id is String && id.isNotEmpty && row['pinned'] == true) pins.add(id);
        }
      }
    }
    return TxAiAccountSnapshot(
      accountId: accountId,
      accountName: map['accountName'] is String ? map['accountName'] as String : '',
      currency: map['currency'] is String ? map['currency'] as String : '',
      trades: List<TxAiTradeRecord>.unmodifiable(trades),
      startingBalance: _finiteNumber(map['startingBalance']) ?? 0,
      riskPercent: _finiteNumber(map['riskPercent']) ?? 1.0,
      displayTimezone: map['displayTimezone'] is String ? map['displayTimezone'] as String : 'UTC',
      currentDisplayDateKey: map['currentDisplayDateKey'] is String ? map['currentDisplayDateKey'] as String : null,
      currentMonthKey: map['currentMonthKey'] is String ? map['currentMonthKey'] as String : null,
      episodeReviews: Map<String, String>.unmodifiable(reviews),
      pinnedInsights: Set<String>.unmodifiable(pins),
      behavioralPatterns: List<TxAiBehaviorPatternRecord>.unmodifiable(patterns),
    );
  }

  List<double> get realizedPnls => trades.map((trade) => trade.pnl).toList(growable: false);
}

double? _finiteNumber(Object? value) {
  if (value is! num) return null;
  final number = value.toDouble();
  return number.isFinite ? number : null;
}

/// Host method-channel boundary. The Android host must implement the
/// loadSelectedAccountSnapshot method and return the selected account's journal only.
class TxAiDataBridge {
  TxAiDataBridge({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel('tradenex/tx_ai');

  final MethodChannel _channel;

  Future<TxAiAccountSnapshot> loadSelectedAccountSnapshot() async {
    final payload = await _channel.invokeMapMethod<Object?, Object?>(
      'loadSelectedAccountSnapshot',
    );
    if (payload == null) {
      throw const FormatException('The host returned no TX AI account snapshot.');
    }
    return TxAiAccountSnapshot.fromMap(payload);
  }

  Future<void> saveEpisodeReview({
    required String accountId,
    required String episodeId,
    required String status,
  }) async {
    await _channel.invokeMethod<void>('saveEpisodeReview', {
      'accountId': accountId,
      'episodeId': episodeId,
      'status': status,
    });
  }

  Future<void> setInsightPinned({
    required String accountId,
    required String insightId,
    required bool pinned,
  }) async {
    await _channel.invokeMethod<void>('setInsightPinned', {
      'accountId': accountId,
      'insightId': insightId,
      'pinned': pinned,
    });
  }
}

import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/data/repositories/risk_repository.dart';
import 'package:td_yar/core/data/repositories/trade_repository.dart';
import 'package:td_yar/core/domain/models/risk_profile.dart';
import 'package:td_yar/core/domain/models/trade.dart';
import 'package:td_yar/core/domain/models/trade_execution.dart';
import 'package:td_yar/core/domain/models/trade_risk_record.dart';
import 'package:td_yar/core/domain/services/performance_analytics_service.dart';
import 'package:td_yar/core/domain/services/risk_management_service.dart';

void main() {
  test('daily risk counts unique opened trades and does not net away losses', () async {
    final day = DateTime.utc(2026, 6, 2);
    final trade = Trade(
      id: 't1',
      symbol: 'EURUSD',
      direction: TradeDirection.long,
      createdAt: day,
      updatedAt: day,
    );
    final repository = _MemoryTradeRepository(
      [trade],
      {
        't1': [
          TradeExecution(
            id: 'entry-1', tradeId: 't1', kind: ExecutionKind.entry,
            quantity: 2, price: 100, executedAt: day.add(const Duration(hours: 9)),
          ),
          TradeExecution(
            id: 'exit-win', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 110, executedAt: day.add(const Duration(hours: 10)),
          ),
          TradeExecution(
            id: 'exit-loss', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 80, executedAt: day.add(const Duration(hours: 11)),
          ),
        ],
      },
    );
    final riskRepository = _MemoryRiskRepository(RiskProfile(
      id: 'profile', name: 'Main', accountBalance: 1000, currency: 'USD',
      maxRiskPerTradePercent: 1, maxDailyLossPercent: 1,
      maxTradesPerDay: 5, createdAt: day, updatedAt: day,
    ));
    final service = RiskManagementService(
      riskRepository: riskRepository,
      tradeRepository: repository,
      performanceService: PerformanceAnalyticsService(
        tradeRepository: repository,
      ),
    );

    final result = await service.evaluateDay(
      riskProfileId: 'profile',
      date: day,
    );
    expect(result.dailyLossWithinLimit, isFalse);
    expect(result.remainingDailyLossAllowance, 0);
    expect(result.remainingTradeSlots, 4);
  });
}

class _MemoryTradeRepository implements TradeRepository {
  _MemoryTradeRepository(this.trades, this.executions);

  final List<Trade> trades;
  final Map<String, List<TradeExecution>> executions;

  @override
  Future<void> createTrade(Trade trade) async {
    trades.add(trade);
  }

  @override
  Future<void> updateTrade(Trade trade) async {
    final index = trades.indexWhere((item) => item.id == trade.id);
    if (index < 0) throw StateError('Not found');
    trades[index] = trade;
  }

  @override
  Future<Trade?> getTrade(String id) async {
    for (final trade in trades) {
      if (trade.id == id) return trade;
    }
    return null;
  }

  @override
  Future<List<Trade>> getTrades({
    String? symbol,
    DateTime? createdFrom,
    DateTime? createdTo,
    TradeDirection? direction,
    String? strategyId,
    String? accountLabel,
  }) async => trades.where((trade) {
        if (symbol != null && trade.symbol.toUpperCase() != symbol.toUpperCase()) {
          return false;
        }
        if (direction != null && trade.direction != direction) return false;
        if (strategyId != null && trade.strategyId != strategyId) return false;
        if (accountLabel != null && trade.accountLabel != accountLabel) return false;
        return true;
      }).toList();

  @override
  Future<void> addExecution(TradeExecution execution) async {
    executions.putIfAbsent(execution.tradeId, () => []).add(execution);
  }

  @override
  Future<void> updateExecution(TradeExecution execution) async {
    final list = executions[execution.tradeId]!;
    final index = list.indexWhere((item) => item.id == execution.id);
    if (index < 0) throw StateError('Not found');
    list[index] = execution;
  }

  @override
  Future<void> deleteExecution(String tradeId, String executionId) async {
    executions[tradeId]?.removeWhere((item) => item.id == executionId);
  }

  @override
  Future<List<TradeExecution>> getExecutions(String tradeId) async =>
      executions[tradeId] ?? const [];

  @override
  Future<void> deleteTrade(String id) async {
    trades.removeWhere((item) => item.id == id);
    executions.remove(id);
  }
}

class _MemoryRiskRepository implements RiskRepository {
  _MemoryRiskRepository(this.profile);

  final RiskProfile profile;

  @override
  Future<void> saveProfile(RiskProfile value) async {}

  @override
  Future<RiskProfile?> getProfile(String id) async =>
      id == profile.id ? profile : null;

  @override
  Future<List<RiskProfile>> getProfiles() async => [profile];

  @override
  Future<void> deleteProfile(String id) async {}

  @override
  Future<void> saveTradeRisk(TradeRiskRecord record) async {}

  @override
  Future<TradeRiskRecord?> getTradeRisk(String tradeId) async => null;

  @override
  Future<void> deleteTradeRisk(String tradeId) async {}
}

import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/risk_profile.dart';
import 'package:td_yar/core/domain/models/trade.dart';
import 'package:td_yar/core/domain/models/trade_execution.dart';
import 'package:td_yar/core/domain/services/performance_analytics.dart';
import 'package:td_yar/core/domain/services/risk_calculator.dart';

void main() {
  final opened = DateTime.utc(2026, 2, 2, 9);
  final closed = DateTime.utc(2026, 2, 2, 10);

  Trade trade({
    TradeDirection direction = TradeDirection.long,
    String id = 't1',
    String? strategyId = 's1',
  }) => Trade(
        id: id,
        symbol: 'EURUSD',
        direction: direction,
        strategyId: strategyId,
        createdAt: opened,
        updatedAt: closed,
      );

  TradeExecution fill({
    required String id,
    required String tradeId,
    required ExecutionKind kind,
    required double quantity,
    required double price,
    DateTime? at,
  }) => TradeExecution(
        id: id,
        tradeId: tradeId,
        kind: kind,
        quantity: quantity,
        price: price,
        executedAt: at ?? opened,
      );

  test('analytics summarizes realized outcomes and strategy breakdown', () {
    final report = const PerformanceAnalytics().analyze(
      trades: [trade()],
      executionsByTrade: {
        't1': [
          fill(
            id: 'e1', tradeId: 't1', kind: ExecutionKind.entry,
            quantity: 1, price: 100,
          ),
          fill(
            id: 'x1', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 110, at: closed,
          ),
        ],
      },
    );

    expect(report.totalTrades, 1);
    expect(report.wins, 1);
    expect(report.winRate, 100);
    expect(report.netPnl, 10);
    expect(report.byStrategy.single.key, 's1');
    expect(report.byAccount.single.key, 'بدون حساب');
    expect(report.byDirection.single.key, 'long');
  });

  test('risk calculator computes planned risk and reward for a long', () {
    final record = RiskCalculator.createTradeRiskRecord(
      id: 'risk-1',
      trade: trade(),
      entryPrice: 100,
      stopPrice: 95,
      targetPrice: 110,
      quantity: 2,
      accountBalance: 1000,
      createdAt: opened,
    );
    expect(record.plannedRiskAmount, 10);
    expect(record.plannedRewardAmount, 20);
    expect(record.riskRewardRatio, 2);
    expect(record.riskPercent, 1);
  });

  test('risk limit checker identifies exceeded per-trade risk', () {
    final profile = RiskProfile(
      id: 'p1',
      name: 'Main',
      accountBalance: 1000,
      currency: 'USD',
      maxRiskPerTradePercent: 1,
      maxDailyLossPercent: 3,
      maxTradesPerDay: 5,
      createdAt: opened,
      updatedAt: opened,
    );
    final result = RiskCalculator.checkLimits(
      profile: profile,
      plannedRiskAmount: 15,
      realizedDailyLoss: 5,
      tradesTakenToday: 2,
    );
    expect(result.plannedRiskWithinLimit, isFalse);
    expect(result.dailyLossWithinLimit, isTrue);
    expect(result.plannedRiskWithinDailyAllowance, isTrue);
    expect(result.remainingTradeSlots, 3);
  });
  test('partial exits are assigned to their own realization dates', () {
    final secondDay = DateTime.utc(2026, 2, 3, 10);
    final report = const PerformanceAnalytics().analyze(
      trades: [trade()],
      executionsByTrade: {
        't1': [
          fill(
            id: 'e1', tradeId: 't1', kind: ExecutionKind.entry,
            quantity: 2, price: 100, at: opened,
          ),
          fill(
            id: 'x1', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 110, at: closed,
          ),
          fill(
            id: 'x2', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 90, at: secondDay,
          ),
        ],
      },
    );

    expect(report.realizations, hasLength(2));
    expect(report.realizations.first.netPnl, 10);
    expect(report.realizations.last.netPnl, -10);
    expect(report.totalTrades, 1);
    expect(report.breakeven, 1);
    expect(report.netPnl, 0);
    expect(report.maxDrawdown, 10);

    final firstDay = const PerformanceAnalytics().analyze(
      trades: [trade()],
      executionsByTrade: {
        't1': [
          fill(
            id: 'e1', tradeId: 't1', kind: ExecutionKind.entry,
            quantity: 2, price: 100, at: opened,
          ),
          fill(
            id: 'x1', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 110, at: closed,
          ),
          fill(
            id: 'x2', tradeId: 't1', kind: ExecutionKind.exit,
            quantity: 1, price: 90, at: secondDay,
          ),
        ],
      },
      from: DateTime.utc(2026, 2, 2),
      to: DateTime.utc(2026, 2, 2, 23, 59, 59),
    );
    expect(firstDay.netPnl, 10);
    expect(firstDay.realizations, hasLength(1));
  });

  test('risk calculator rejects non-finite inputs and invalid profiles', () {
    expect(
      () => RiskCalculator.createTradeRiskRecord(
        id: 'bad', trade: trade(),
        entryPrice: double.nan, stopPrice: 95, targetPrice: 110,
        quantity: 1, accountBalance: 1000,
      ),
      throwsArgumentError,
    );
    final invalid = RiskProfile(
      id: 'bad', name: 'Bad', accountBalance: 1000, currency: 'USD',
      maxRiskPerTradePercent: 101, maxDailyLossPercent: 3,
      maxTradesPerDay: 5, createdAt: opened, updatedAt: opened,
    );
    expect(
      () => RiskCalculator.checkLimits(
        profile: invalid, plannedRiskAmount: 1,
        realizedDailyLoss: 0, tradesTakenToday: 0,
      ),
      throwsArgumentError,
    );
  });

}

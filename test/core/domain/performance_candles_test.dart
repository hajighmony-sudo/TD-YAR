import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/data/repositories/trade_repository.dart';
import 'package:td_yar/core/domain/models/performance_candle.dart';
import 'package:td_yar/core/domain/models/trade.dart';
import 'package:td_yar/core/domain/models/trade_execution.dart';
import 'package:td_yar/core/domain/services/performance_analytics_service.dart';

class _MemoryTradeRepository implements TradeRepository {
  _MemoryTradeRepository(this.trade, this.fills);

  final Trade trade;
  final List<TradeExecution> fills;

  @override
  Future<List<Trade>> getTrades({
    String? symbol,
    DateTime? createdFrom,
    DateTime? createdTo,
    TradeDirection? direction,
    String? strategyId,
    String? accountLabel,
    String? accountId,
  }) async =>
      [trade];

  @override
  Future<List<TradeExecution>> getExecutions(String tradeId) async => fills;

  @override
  Future<void> createTrade(Trade trade) async => throw UnimplementedError();
  @override
  Future<void> updateTrade(Trade trade) async => throw UnimplementedError();
  @override
  Future<Trade?> getTrade(String id) async => trade.id == id ? trade : null;
  @override
  Future<void> addExecution(TradeExecution execution) async => throw UnimplementedError();
  @override
  Future<void> updateExecution(TradeExecution execution) async => throw UnimplementedError();
  @override
  Future<void> deleteExecution(String tradeId, String executionId) async => throw UnimplementedError();
  @override
  Future<void> deleteTrade(String id) async => throw UnimplementedError();
}

void main() {
  test('builds daily, weekly and yearly realized-equity candles', () async {
    final entryAt = DateTime(2026, 1, 5, 9);
    final exitAt = DateTime(2026, 1, 5, 10);
    final trade = Trade(
      id: 'trade-1',
      symbol: 'TEST',
      direction: TradeDirection.long,
      createdAt: entryAt,
      updatedAt: exitAt,
    );
    final repository = _MemoryTradeRepository(trade, [
      TradeExecution(
        id: 'entry-1',
        tradeId: trade.id,
        kind: ExecutionKind.entry,
        quantity: 1,
        price: 100,
        executedAt: entryAt,
      ),
      TradeExecution(
        id: 'exit-1',
        tradeId: trade.id,
        kind: ExecutionKind.exit,
        quantity: 1,
        price: 110,
        executedAt: exitAt,
      ),
    ]);
    final service = PerformanceAnalyticsService(tradeRepository: repository);

    for (final timeframe in PerformanceCandleTimeframe.values) {
      final candles = await service.buildCandles(timeframe: timeframe);
      expect(candles, hasLength(1));
      expect(candles.single.open, 0);
      expect(candles.single.high, 10);
      expect(candles.single.low, 0);
      expect(candles.single.close, 10);
      expect(candles.single.netPnl, 10);
      expect(candles.single.realizationCount, 1);
    }
  });
}

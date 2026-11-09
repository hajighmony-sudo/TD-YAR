import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/trade.dart';
import 'package:td_yar/core/domain/models/trade_execution.dart';
import 'package:td_yar/core/domain/services/trade_calculator.dart';

void main() {
  final openedAt = DateTime.utc(2026, 1, 1, 9);
  final closedAt = DateTime.utc(2026, 1, 1, 10);

  Trade makeTrade(TradeDirection direction) => Trade(
        id: 'trade-1',
        symbol: 'TEST',
        direction: direction,
        createdAt: openedAt,
        updatedAt: closedAt,
      );

  TradeExecution fill({
    required String id,
    required ExecutionKind kind,
    required double quantity,
    required double price,
    double fee = 0,
    DateTime? at,
  }) =>
      TradeExecution(
        id: id,
        tradeId: 'trade-1',
        kind: kind,
        quantity: quantity,
        price: price,
        fee: fee,
        executedAt: at ?? openedAt,
      );

  group('TradeCalculator', () {
    test('calculates a long trade and allocates fees', () {
      final metrics = TradeCalculator.calculate(
        trade: makeTrade(TradeDirection.long),
        executions: [
          fill(
            id: 'entry',
            kind: ExecutionKind.entry,
            quantity: 2,
            price: 100,
            fee: 1,
          ),
          fill(
            id: 'exit',
            kind: ExecutionKind.exit,
            quantity: 2,
            price: 110,
            fee: 1,
            at: closedAt,
          ),
        ],
      );

      expect(metrics.grossRealizedPnl, 20);
      expect(metrics.feesAllocatedToClosedQuantity, 2);
      expect(metrics.netRealizedPnl, 18);
      expect(metrics.openQuantity, 0);
      expect(metrics.closedQuantity, 2);
    });

    test('calculates a short trade with the reversed price difference', () {
      final metrics = TradeCalculator.calculate(
        trade: makeTrade(TradeDirection.short),
        executions: [
          fill(
            id: 'entry',
            kind: ExecutionKind.entry,
            quantity: 3,
            price: 50,
          ),
          fill(
            id: 'exit',
            kind: ExecutionKind.exit,
            quantity: 3,
            price: 45,
            at: closedAt,
          ),
        ],
      );

      expect(metrics.grossRealizedPnl, 15);
      expect(metrics.netRealizedPnl, 15);
    });

    test('matches partial exits to entry fills in FIFO order', () {
      final metrics = TradeCalculator.calculate(
        trade: makeTrade(TradeDirection.long),
        executions: [
          fill(
            id: 'entry-1',
            kind: ExecutionKind.entry,
            quantity: 1,
            price: 100,
          ),
          fill(
            id: 'entry-2',
            kind: ExecutionKind.entry,
            quantity: 1,
            price: 105,
            at: DateTime.utc(2026, 1, 1, 9, 15),
          ),
          fill(
            id: 'exit-1',
            kind: ExecutionKind.exit,
            quantity: 1.5,
            price: 110,
            at: closedAt,
          ),
        ],
      );

      expect(metrics.closedQuantity, 1.5);
      expect(metrics.openQuantity, 0.5);
      expect(metrics.averageEntryPrice, closeTo(103.333333, 0.00001));
      expect(metrics.grossRealizedPnl, 12.5);
    });

    test('rejects exits larger than the recorded open quantity', () {
      expect(
        () => TradeCalculator.calculate(
          trade: makeTrade(TradeDirection.long),
          executions: [
            fill(
              id: 'exit',
              kind: ExecutionKind.exit,
              quantity: 1,
              price: 100,
            ),
          ],
        ),
        throwsStateError,
      );
    });

    test('keeps unmatched entry quantity open', () {
      final metrics = TradeCalculator.calculate(
        trade: makeTrade(TradeDirection.long),
        executions: [
          fill(
            id: 'entry',
            kind: ExecutionKind.entry,
            quantity: 4,
            price: 100,
            fee: 2,
          ),
        ],
      );

      expect(metrics.openQuantity, 4);
      expect(metrics.closedQuantity, 0);
      expect(metrics.netRealizedPnl, 0);
      expect(metrics.totalFees, 2);
    });
  });
}

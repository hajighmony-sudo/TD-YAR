import 'dart:collection';

import '../models/trade.dart';
import '../models/trade_execution.dart';
import '../models/trade_metrics.dart';

/// Deterministic bookkeeping calculations for manually recorded executions.
/// Exits are matched to entry fills in FIFO order.
class TradeCalculator {
  const TradeCalculator._();

  static const double _epsilon = 1e-10;

  static TradeMetrics calculate({
    required Trade trade,
    required List<TradeExecution> executions,
  }) {
    if (trade.id.trim().isEmpty || trade.symbol.trim().isEmpty) {
      throw ArgumentError('Trade id and symbol are required.');
    }
    if (!trade.contractMultiplier.isFinite ||
        trade.contractMultiplier <= 0) {
      throw ArgumentError.value(
        trade.contractMultiplier,
        'trade.contractMultiplier',
        'Must be finite and greater than zero.',
      );
    }

    final ids = <String>{};
    final sorted = [...executions]..sort(_compareExecutions);
    final openLots = Queue<_OpenLot>();
    var totalFees = 0.0;
    var closedQuantity = 0.0;
    var grossRealizedPnl = 0.0;
    var allocatedFees = 0.0;
    var weightedEntry = 0.0;
    var weightedExit = 0.0;

    for (final execution in sorted) {
      if (execution.tradeId != trade.id) {
        throw ArgumentError(
          'Every execution must belong to the supplied trade.',
        );
      }
      if (execution.id.trim().isEmpty || !ids.add(execution.id)) {
        throw ArgumentError('Execution ids must be non-empty and unique.');
      }
      if (!execution.quantity.isFinite ||
          !execution.price.isFinite ||
          !execution.fee.isFinite ||
          execution.quantity <= 0 ||
          execution.price <= 0 ||
          execution.fee < 0) {
        throw ArgumentError(
          'Quantity and price must be finite and positive; fee must be finite and non-negative.',
        );
      }

      totalFees += execution.fee;
      if (!totalFees.isFinite) {
        throw ArgumentError('Execution fee total exceeds numeric range.');
      }

      if (execution.kind == ExecutionKind.entry) {
        openLots.add(_OpenLot(
          remainingQuantity: execution.quantity,
          price: execution.price,
          remainingFee: execution.fee,
        ));
        continue;
      }

      var remainingExitQuantity = execution.quantity;
      while (remainingExitQuantity > _epsilon) {
        if (openLots.isEmpty) {
          throw StateError(
            'Exit quantity exceeds the open quantity for this trade.',
          );
        }

        final lot = openLots.first;
        final matched = remainingExitQuantity < lot.remainingQuantity
            ? remainingExitQuantity
            : lot.remainingQuantity;
        final entryFeePart =
            lot.remainingFee * (matched / lot.remainingQuantity);
        final exitFeePart = execution.fee * (matched / execution.quantity);
        final priceDifference = trade.direction == TradeDirection.long
            ? execution.price - lot.price
            : lot.price - execution.price;

        grossRealizedPnl +=
            priceDifference * matched * trade.contractMultiplier;
        allocatedFees += entryFeePart + exitFeePart;
        weightedEntry += lot.price * matched;
        weightedExit += execution.price * matched;
        closedQuantity += matched;
        remainingExitQuantity -= matched;
        lot.remainingQuantity -= matched;
        lot.remainingFee -= entryFeePart;

        if (!grossRealizedPnl.isFinite || !allocatedFees.isFinite ||
            !closedQuantity.isFinite || !weightedEntry.isFinite ||
            !weightedExit.isFinite) {
          throw ArgumentError('Calculated execution metrics exceed numeric range.');
        }
        if (lot.remainingQuantity <= _epsilon) {
          openLots.removeFirst();
        }
      }
    }

    final openQuantity =
        openLots.fold<double>(0, (sum, lot) => sum + lot.remainingQuantity);

    return TradeMetrics(
      closedQuantity: closedQuantity,
      openQuantity: openQuantity,
      averageEntryPrice:
          closedQuantity == 0 ? 0 : weightedEntry / closedQuantity,
      averageExitPrice:
          closedQuantity == 0 ? 0 : weightedExit / closedQuantity,
      grossRealizedPnl: grossRealizedPnl,
      feesAllocatedToClosedQuantity: allocatedFees,
      totalFees: totalFees,
      netRealizedPnl: grossRealizedPnl - allocatedFees,
    );
  }

  static int _compareExecutions(TradeExecution a, TradeExecution b) {
    final timeOrder = a.executedAt.compareTo(b.executedAt);
    if (timeOrder != 0) return timeOrder;
    if (a.kind != b.kind) {
      return a.kind == ExecutionKind.entry ? -1 : 1;
    }
    return a.id.compareTo(b.id);
  }
}

class _OpenLot {
  _OpenLot({
    required this.remainingQuantity,
    required this.price,
    required this.remainingFee,
  });

  double remainingQuantity;
  final double price;
  double remainingFee;
}

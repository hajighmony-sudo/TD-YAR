import '../models/risk_profile.dart';
import '../models/trade.dart';
import '../models/trade_risk_record.dart';

class RiskCalculator {
  const RiskCalculator._();

  static TradeRiskRecord createTradeRiskRecord({
    required String id,
    required Trade trade,
    required double entryPrice,
    required double stopPrice,
    required double targetPrice,
    required double quantity,
    required double accountBalance,
    String notes = '',
    DateTime? createdAt,
  }) {
    if (entryPrice <= 0 || stopPrice <= 0 || targetPrice <= 0 ||
        quantity <= 0 || accountBalance <= 0 ||
        trade.contractMultiplier <= 0) {
      throw ArgumentError('Prices, quantity, balance and multiplier must be positive.');
    }

    final stopDistance = trade.direction == TradeDirection.long
        ? entryPrice - stopPrice : stopPrice - entryPrice;
    final targetDistance = trade.direction == TradeDirection.long
        ? targetPrice - entryPrice : entryPrice - targetPrice;
    if (stopDistance <= 0) {
      throw ArgumentError('Stop price must be beyond entry in the loss direction.');
    }
    if (targetDistance <= 0) {
      throw ArgumentError('Target price must be beyond entry in the reward direction.');
    }

    final risk = stopDistance * quantity * trade.contractMultiplier;
    final reward = targetDistance * quantity * trade.contractMultiplier;
    return TradeRiskRecord(
      id: id,
      tradeId: trade.id,
      entryPrice: entryPrice,
      stopPrice: stopPrice,
      targetPrice: targetPrice,
      quantity: quantity,
      contractMultiplier: trade.contractMultiplier,
      accountBalanceSnapshot: accountBalance,
      plannedRiskAmount: risk,
      plannedRewardAmount: reward,
      riskRewardRatio: reward / risk,
      riskPercent: risk / accountBalance * 100,
      notes: notes,
      createdAt: createdAt ?? DateTime.now(),
    );
  }

  static RiskLimitCheck checkLimits({
    required RiskProfile profile,
    required double plannedRiskAmount,
    required double realizedDailyLoss,
    required int tradesTakenToday,
  }) {
    if (plannedRiskAmount < 0 || realizedDailyLoss < 0 || tradesTakenToday < 0) {
      throw ArgumentError('Risk, loss and trade count cannot be negative.');
    }
    final maxTradeRisk = profile.accountBalance *
        profile.maxRiskPerTradePercent / 100;
    final maxDailyLoss = profile.accountBalance *
        profile.maxDailyLossPercent / 100;
    return RiskLimitCheck(
      plannedRiskWithinLimit: plannedRiskAmount <= maxTradeRisk,
      dailyLossWithinLimit: realizedDailyLoss < maxDailyLoss,
      tradeCountWithinLimit: tradesTakenToday < profile.maxTradesPerDay,
      maxTradeRiskAmount: maxTradeRisk,
      maxDailyLossAmount: maxDailyLoss,
      remainingDailyLossAllowance:
          (maxDailyLoss - realizedDailyLoss).clamp(0, double.infinity),
      remainingTradeSlots:
          (profile.maxTradesPerDay - tradesTakenToday).clamp(0, profile.maxTradesPerDay),
    );
  }
}

class RiskLimitCheck {
  const RiskLimitCheck({
    required this.plannedRiskWithinLimit,
    required this.dailyLossWithinLimit,
    required this.tradeCountWithinLimit,
    required this.maxTradeRiskAmount,
    required this.maxDailyLossAmount,
    required this.remainingDailyLossAllowance,
    required this.remainingTradeSlots,
  });

  final bool plannedRiskWithinLimit;
  final bool dailyLossWithinLimit;
  final bool tradeCountWithinLimit;
  final double maxTradeRiskAmount;
  final double maxDailyLossAmount;
  final double remainingDailyLossAllowance;
  final int remainingTradeSlots;

  bool get allLimitsWithinRange =>
      plannedRiskWithinLimit && dailyLossWithinLimit && tradeCountWithinLimit;
}

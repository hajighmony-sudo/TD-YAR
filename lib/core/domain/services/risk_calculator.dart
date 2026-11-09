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
    _requirePositiveFinite(entryPrice, 'entryPrice');
    _requirePositiveFinite(stopPrice, 'stopPrice');
    _requirePositiveFinite(targetPrice, 'targetPrice');
    _requirePositiveFinite(quantity, 'quantity');
    _requirePositiveFinite(accountBalance, 'accountBalance');
    _requirePositiveFinite(trade.contractMultiplier, 'contractMultiplier');
    if (id.trim().isEmpty || trade.id.trim().isEmpty) {
      throw ArgumentError('Risk record and trade ids are required.');
    }

    final stopDistance = trade.direction == TradeDirection.long
        ? entryPrice - stopPrice
        : stopPrice - entryPrice;
    final targetDistance = trade.direction == TradeDirection.long
        ? targetPrice - entryPrice
        : entryPrice - targetPrice;
    if (stopDistance <= 0) {
      throw ArgumentError('Stop price must be on the loss side of entry.');
    }
    if (targetDistance <= 0) {
      throw ArgumentError('Target price must be on the reward side of entry.');
    }
    final risk = stopDistance * quantity * trade.contractMultiplier;
    final reward = targetDistance * quantity * trade.contractMultiplier;
    if (!risk.isFinite || !reward.isFinite) {
      throw ArgumentError('Calculated risk/reward exceeds numeric range.');
    }
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
    validateProfile(profile);
    if (!plannedRiskAmount.isFinite ||
        !realizedDailyLoss.isFinite ||
        plannedRiskAmount < 0 ||
        realizedDailyLoss < 0 ||
        tradesTakenToday < 0) {
      throw ArgumentError('Risk, loss and trade count must be finite and non-negative.');
    }
    final maxTradeRisk =
        profile.accountBalance * profile.maxRiskPerTradePercent / 100;
    final maxDailyLoss =
        profile.accountBalance * profile.maxDailyLossPercent / 100;
    final remaining = (maxDailyLoss - realizedDailyLoss)
        .clamp(0, double.infinity).toDouble();
    return RiskLimitCheck(
      plannedRiskWithinLimit: plannedRiskAmount <= maxTradeRisk,
      dailyLossWithinLimit: realizedDailyLoss < maxDailyLoss ||
          (maxDailyLoss == 0 && realizedDailyLoss == 0),
      tradeCountWithinLimit: tradesTakenToday < profile.maxTradesPerDay,
      maxTradeRiskAmount: maxTradeRisk,
      maxDailyLossAmount: maxDailyLoss,
      remainingDailyLossAllowance: remaining,
      remainingTradeSlots: (profile.maxTradesPerDay - tradesTakenToday)
          .clamp(0, profile.maxTradesPerDay).toInt(),
    );
  }

  static void validateProfile(RiskProfile profile) {
    if (profile.id.trim().isEmpty ||
        profile.name.trim().isEmpty ||
        profile.currency.trim().isEmpty ||
        !profile.accountBalance.isFinite ||
        profile.accountBalance <= 0 ||
        !profile.maxRiskPerTradePercent.isFinite ||
        profile.maxRiskPerTradePercent < 0 ||
        profile.maxRiskPerTradePercent > 100 ||
        !profile.maxDailyLossPercent.isFinite ||
        profile.maxDailyLossPercent < 0 ||
        profile.maxDailyLossPercent > 100 ||
        profile.maxTradesPerDay < 0) {
      throw ArgumentError('Risk profile contains invalid values.');
    }
  }

  static void _requirePositiveFinite(double value, String field) {
    if (!value.isFinite || value <= 0) {
      throw ArgumentError.value(value, field, 'Must be finite and positive.');
    }
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

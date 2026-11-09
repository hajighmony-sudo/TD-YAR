class TradeRiskRecord {
  const TradeRiskRecord({
    required this.id,
    required this.tradeId,
    required this.entryPrice,
    required this.stopPrice,
    required this.targetPrice,
    required this.quantity,
    required this.contractMultiplier,
    required this.accountBalanceSnapshot,
    required this.plannedRiskAmount,
    required this.plannedRewardAmount,
    required this.riskRewardRatio,
    required this.riskPercent,
    required this.createdAt,
    this.notes = '',
  });

  final String id;
  final String tradeId;
  final double entryPrice;
  final double stopPrice;
  final double targetPrice;
  final double quantity;
  final double contractMultiplier;
  final double accountBalanceSnapshot;
  final double plannedRiskAmount;
  final double plannedRewardAmount;
  final double riskRewardRatio;
  final double riskPercent;
  final String notes;
  final DateTime createdAt;

  Map<String, Object?> toMap() => {
        'id': id, 'trade_id': tradeId, 'entry_price': entryPrice,
        'stop_price': stopPrice, 'target_price': targetPrice,
        'quantity': quantity, 'contract_multiplier': contractMultiplier,
        'account_balance_snapshot': accountBalanceSnapshot,
        'planned_risk_amount': plannedRiskAmount,
        'planned_reward_amount': plannedRewardAmount,
        'risk_reward_ratio': riskRewardRatio, 'risk_percent': riskPercent,
        'notes': notes, 'created_at': createdAt.toUtc().toIso8601String(),
      };

  factory TradeRiskRecord.fromMap(Map<String, Object?> map) =>
      TradeRiskRecord(
        id: map['id'] as String,
        tradeId: map['trade_id'] as String,
        entryPrice: (map['entry_price'] as num).toDouble(),
        stopPrice: (map['stop_price'] as num).toDouble(),
        targetPrice: (map['target_price'] as num).toDouble(),
        quantity: (map['quantity'] as num).toDouble(),
        contractMultiplier: (map['contract_multiplier'] as num).toDouble(),
        accountBalanceSnapshot:
            (map['account_balance_snapshot'] as num).toDouble(),
        plannedRiskAmount: (map['planned_risk_amount'] as num).toDouble(),
        plannedRewardAmount: (map['planned_reward_amount'] as num).toDouble(),
        riskRewardRatio: (map['risk_reward_ratio'] as num).toDouble(),
        riskPercent: (map['risk_percent'] as num).toDouble(),
        notes: map['notes'] as String? ?? '',
        createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
      );
}

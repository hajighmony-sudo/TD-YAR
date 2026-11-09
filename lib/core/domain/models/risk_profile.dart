class RiskProfile {
  const RiskProfile({
    required this.id,
    required this.name,
    required this.accountBalance,
    required this.currency,
    required this.maxRiskPerTradePercent,
    required this.maxDailyLossPercent,
    required this.maxTradesPerDay,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String name;
  final double accountBalance;
  final String currency;
  final double maxRiskPerTradePercent;
  final double maxDailyLossPercent;
  final int maxTradesPerDay;
  final DateTime createdAt;
  final DateTime updatedAt;

  Map<String, Object?> toMap() => {
        'id': id, 'name': name, 'account_balance': accountBalance,
        'currency': currency,
        'max_risk_per_trade_percent': maxRiskPerTradePercent,
        'max_daily_loss_percent': maxDailyLossPercent,
        'max_trades_per_day': maxTradesPerDay,
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory RiskProfile.fromMap(Map<String, Object?> map) => RiskProfile(
        id: map['id'] as String,
        name: map['name'] as String,
        accountBalance: (map['account_balance'] as num).toDouble(),
        currency: map['currency'] as String,
        maxRiskPerTradePercent:
            (map['max_risk_per_trade_percent'] as num).toDouble(),
        maxDailyLossPercent:
            (map['max_daily_loss_percent'] as num).toDouble(),
        maxTradesPerDay: (map['max_trades_per_day'] as num).toInt(),
        createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
        updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
      );
}

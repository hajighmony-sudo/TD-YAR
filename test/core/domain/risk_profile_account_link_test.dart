import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/risk_profile.dart';

void main() {
  test('risk profile preserves its manual account relationship', () {
    final profile = RiskProfile(
      id: 'risk-1',
      name: 'Conservative',
      accountId: 'account-1',
      accountBalance: 10000,
      currency: 'USD',
      maxRiskPerTradePercent: 1,
      maxDailyLossPercent: 3,
      maxTradesPerDay: 4,
      createdAt: DateTime.utc(2026, 1, 1),
      updatedAt: DateTime.utc(2026, 1, 2),
    );

    final restored = RiskProfile.fromMap(profile.toMap());
    expect(restored.accountId, 'account-1');
    expect(restored.accountBalance, 10000);
  });

  test('legacy risk profiles without account ids remain readable', () {
    final profile = RiskProfile.fromMap({
      'id': 'risk-legacy',
      'name': 'Legacy',
      'account_balance': 5000,
      'currency': 'USD',
      'max_risk_per_trade_percent': 1,
      'max_daily_loss_percent': 2,
      'max_trades_per_day': 3,
      'created_at': '2026-01-01T00:00:00.000Z',
      'updated_at': '2026-01-01T00:00:00.000Z',
    });
    expect(profile.accountId, isNull);
  });
}

import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tx_ai_data_bridge.dart';

void main() {
  test('snapshot keeps only selected-account trades', () {
    final snapshot = TxAiAccountSnapshot.fromMap({
      'accountId': 'account-1',
      'accountName': 'Main',
      'currency': 'USD',
      'startingBalance': 10000.0,
      'riskPercent': 1.0,
      'displayTimezone': 'Asia/Tehran',
      'currentDisplayDateKey': '2026-11-08',
      'currentMonthKey': '2026-11',
      'episodeReviews': [
        {'episodeId': 'a:1:2', 'status': 'confirmed'},
        {'episodeId': 'account-1:session-drift:1', 'status': 'confirmed'},
      ],
      'pinnedInsights': [
        {'insightId': 'summary-win-rate', 'pinned': true},
      ],
      'behavioralPatterns': [
        {
          'id': 'account-1:session-drift:1',
          'title': 'معامله خارج از سشن مجاز استراتژی',
          'triggerTradeId': 1,
          'linkedTradeIds': [1],
          'evidenceScore': 30,
          'actualNetPnl': 12.5,
          'reasons': ['قانون سشن صریح ثبت شده است.'],
        },
      ],
      'trades': [
        {
          'id': 1,
          'accountId': 'account-1',
          'epochDay': 20695,
          'symbol': 'EURUSD',
          'isBuy': true,
          'pnl': 25.5,
          'lot': 0.1,
          'strategy': 'ICT',
          'tags': ['London'],
          'emotion': 'Calm',
          'strategyFollowed': true,
          'strategyVersion': 3,
          'session': 'London',
          'displayHour': 13,
          'displayWeekday': 2,
          'displayMinute': 45,
          'displayDateKey': '2026-11-03',
          'displayMonthKey': '2026-11',
          'displayClosedMonthKey': '2026-11',
          'entryPrice': 1.1,
          'exitPrice': 1.101,
          'stopLossPrice': 1.099,
          'takeProfitPrice': 1.105,
        },
        {
          'id': 2,
          'accountId': 'account-2',
          'symbol': 'XAUUSD',
          'isBuy': false,
          'pnl': 80,
          'lot': 0.2,
        },
      ],
    });

    expect(snapshot.accountId, 'account-1');
    expect(snapshot.trades, hasLength(1));
    expect(snapshot.trades.single.id, 1);
    expect(snapshot.realizedPnls, [25.5]);
    expect(snapshot.trades.single.strategyFollowed, isTrue);
    expect(snapshot.trades.single.strategyVersion, 3);
    expect(snapshot.trades.single.epochDay, 20695);
    expect(snapshot.trades.single.displayClosedMonthKey, '2026-11');
    expect(snapshot.currentDisplayDateKey, '2026-11-08');
    expect(snapshot.currentMonthKey, '2026-11');
    expect(snapshot.trades.single.session, 'London');
    expect(snapshot.trades.single.displayHour, 13);
    expect(snapshot.trades.single.displayWeekday, 2);
    expect(snapshot.trades.single.displayMonthKey, '2026-11');
    expect(snapshot.startingBalance, 10000.0);
    expect(snapshot.displayTimezone, 'Asia/Tehran');
    expect(snapshot.episodeReviews['a:1:2'], 'confirmed');
    expect(snapshot.pinnedInsights, contains('summary-win-rate'));
    expect(snapshot.behavioralPatterns.single.id, 'account-1:session-drift:1');
    expect(snapshot.behavioralPatterns.single.evidenceScore, 30);
    expect(snapshot.behavioralPatterns.single.reviewStatus, 'confirmed');
  });

  test('invalid or non-finite trade values are not fabricated', () {
    final snapshot = TxAiAccountSnapshot.fromMap({
      'accountId': 'account-1',
      'trades': [
        {
          'id': 1,
          'accountId': 'account-1',
          'symbol': 'EURUSD',
          'isBuy': true,
          'pnl': double.infinity,
          'lot': 0.1,
        },
        {
          'id': 2,
          'accountId': 'account-1',
          'symbol': 'XAUUSD',
          'isBuy': false,
          'pnl': 4.0,
          'lot': 0.0,
        },
      ],
    });

    expect(snapshot.trades, isEmpty);
  });

  test('missing selected account id is rejected', () {
    expect(
      () => TxAiAccountSnapshot.fromMap({'trades': []}),
      throwsFormatException,
    );
  });
}

import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/journal_entry.dart';
import 'package:td_yar/core/domain/models/psychology_entry.dart';
import 'package:td_yar/core/domain/models/trade.dart';
import 'package:td_yar/core/domain/models/trade_realization.dart';
import 'package:td_yar/core/domain/services/trading_calendar.dart';

void main() {
  test('calendar assigns partial exits to the correct dates', () {
    final firstDay = DateTime.utc(2026, 5, 4, 12);
    final secondDay = DateTime.utc(2026, 5, 5, 12);
    final realizations = [
      TradeRealization(
        tradeId: 'trade-1', symbol: 'EURUSD',
        direction: TradeDirection.long, strategyId: 'strategy-1',
        closedAt: firstDay, netPnl: 8, grossPnl: 9, fees: 1,
        closedQuantity: 1, duration: const Duration(hours: 1),
      ),
      TradeRealization(
        tradeId: 'trade-1', symbol: 'EURUSD',
        direction: TradeDirection.long, strategyId: 'strategy-1',
        closedAt: secondDay, netPnl: -3, grossPnl: -2, fees: 1,
        closedQuantity: 1, duration: const Duration(days: 1, hours: 1),
      ),
    ];

    final summaries = const TradingCalendar().buildRange(
      from: firstDay,
      to: secondDay,
      outcomes: const [],
      realizations: realizations,
      journalEntries: const <JournalEntry>[],
      psychologyEntries: const <PsychologyEntry>[],
    );

    expect(summaries, hasLength(2));
    expect(summaries[0].tradeCount, 1);
    expect(summaries[0].netPnl, 8);
    expect(summaries[0].grossPnl, 9);
    expect(summaries[0].fees, 1);
    expect(summaries[1].tradeCount, 1);
    expect(summaries[1].netPnl, -3);
    expect(summaries[1].losses, 1);
  });
}

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tradenex_fx.dart';

void main() {
  testWidgets('TX radar renders six dimensions and supports axis selection', (tester) async {
    int? selected;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxRadar(
          values: const [80, 70, 60, 50, 90, 75],
          onAxisTap: (index) => selected = index,
        ),
      ),
    ));
    expect(find.text('Win Rate'), findsOneWidget);
    expect(find.text('Session Edge'), findsOneWidget);
    await tester.tap(find.text('Win Rate'));
    expect(selected, 0);
  });

  testWidgets('TX insight card exposes evidence and pin action', (tester) async {
    var pinned = false;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxInsightCard(
          title: 'سشن لندن',
          description: 'بر اساس معاملات ثبت‌شده',
          metric: '+125',
          onPin: () => pinned = true,
        ),
      ),
    ));
    expect(find.text('سشن لندن'), findsOneWidget);
    expect(find.text('+125'), findsOneWidget);
    await tester.tap(find.byIcon(Icons.push_pin_outlined));
    expect(pinned, isTrue);
  });

  testWidgets('TX heatmap renders seven weekdays and responds to cell tap', (tester) async {
    var selected = '';
    final cells = List.generate(7, (_) => List<double?>.filled(24, 0));
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxHeatmap(
          cells: cells,
          onCellTap: (day, hour) => selected = '$day:$hour',
        ),
      ),
    ));
    expect(find.text('ش'), findsOneWidget);
    await tester.tap(find.byType(GestureDetector).first);
    expect(selected, isNotEmpty);
  });

  testWidgets('process timeline represents missing score as unavailable', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Scaffold(body: TxProcessTimeline(months: [TxProcessMonth('آبان', null)])),
    ));
    expect(find.text('آبان'), findsOneWidget);
    expect(find.text('—'), findsOneWidget);
  });

  testWidgets('revenge episode exposes linked trades and persists review through host callback', (tester) async {
    TxRevengeReview? reviewed;
    var openedTrades = false;
    const episode = TxRevengeEpisode(
      id: 'account-a:10:11',
      previousTradeId: '10',
      triggerTradeId: '11',
      previousTradeLabel: 'EURUSD SELL · -25.00',
      triggerTradeLabel: 'GBPUSD BUY · -8.00',
      evidenceScore: 55,
      classification: TxRevengeClassification.strongEvidence,
      reasons: ['افزایش ریسک بیش از ۲۵٪', 'ورود مجدد سریع'],
      gapMinutes: 7,
      previousRealizedPnl: -25,
      triggerRealizedPnl: -8,
    );
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxRevengeEpisodeCard(
          episode: episode,
          onShowTrades: () => openedTrades = true,
          onReview: (value) => reviewed = value,
        ),
      ),
    ));
    expect(find.text('Evidence 55/100'), findsOneWidget);
    expect(find.text('خالص دو معامله: -33.00'), findsOneWidget);
    expect(find.textContaining('EURUSD SELL'), findsOneWidget);
    await tester.tap(find.text('مشاهده معاملات'));
    expect(openedTrades, isTrue);
    await tester.tap(find.text('تأیید'));
    expect(reviewed, TxRevengeReview.confirmed);
  });

  test('revenge episode keeps missing realized P&L unavailable', () {
    const episode = TxRevengeEpisode(
      id: 'a:1:2',
      previousTradeId: '1',
      triggerTradeId: '2',
      previousTradeLabel: 'Trade 1',
      triggerTradeLabel: 'Trade 2',
      evidenceScore: 20,
      classification: TxRevengeClassification.possible,
      reasons: [],
      previousRealizedPnl: -10,
    );
    expect(episode.realizedEpisodeNet, isNull);
  });

}

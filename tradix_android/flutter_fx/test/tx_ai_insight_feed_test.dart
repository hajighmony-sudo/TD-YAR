import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tx_ai_insight_feed.dart';

void main() {
  const insights = [
    TxInsight(
      id: 'daily-1',
      period: TxInsightPeriod.daily,
      title: 'افت عملکرد در سشن آسیا',
      reason: 'میانگین نتیجه معاملات این بازه منفی بوده است.',
      metricLabel: 'میانگین هر معامله',
      metricValue: '-8.20',
      action: 'قبل از ورود، چک‌لیست سشن را مرور کن.',
      supportingTradeIds: [1, 2],
    ),
    TxInsight(
      id: 'weekly-1',
      period: TxInsightPeriod.weekly,
      title: 'پایبندی بهتر به پلن',
      reason: 'در این هفته ثبت معاملات طبق پلن بیشتر شده است.',
      metricLabel: 'پایبندی',
      metricValue: '80%',
      action: 'همین روند را ادامه بده.',
      supportingTradeIds: [3],
      pinned: true,
    ),
  ];

  testWidgets('period filter narrows the insight feed', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Scaffold(body: TxInsightFeed(insights: insights)),
    ));

    expect(find.text('افت عملکرد در سشن آسیا'), findsOneWidget);
    expect(find.text('پایبندی بهتر به پلن'), findsOneWidget);

    await tester.tap(find.text('روزانه').first);
    await tester.pump();

    expect(find.text('افت عملکرد در سشن آسیا'), findsOneWidget);
    expect(find.text('پایبندی بهتر به پلن'), findsNothing);
  });

  testWidgets('pin action emits state and updates the label', (tester) async {
    String? insightId;
    bool? pinned;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxInsightFeed(
          insights: insights,
          onPinChanged: (id, value) {
            insightId = id;
            pinned = value;
          },
        ),
      ),
    ));

    await tester.tap(find.text('سنجاق کردن').first);
    await tester.pump();

    expect(insightId, 'daily-1');
    expect(pinned, isTrue);
    expect(find.text('برداشتن سنجاق').first, findsOneWidget);
  });

  testWidgets('evidence drill-down sends supporting trade ids', (tester) async {
    List<int>? opened;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxInsightFeed(
          insights: insights,
          onOpenEvidence: (ids) => opened = ids,
        ),
      ),
    ));

    await tester.tap(find.text('مشاهده شواهد').first);
    expect(opened, [1, 2]);
  });
}

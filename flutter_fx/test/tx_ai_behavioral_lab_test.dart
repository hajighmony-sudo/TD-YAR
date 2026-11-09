import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tx_ai_behavioral_lab.dart';

TxBehavioralEpisode episode({
  String id = 'a:10:11',
  String accountId = 'account-a',
  TxBehavioralEvidenceLevel level = TxBehavioralEvidenceLevel.possible,
}) =>
    TxBehavioralEpisode(
      id: id,
      accountId: accountId,
      previousLossTradeId: 10,
      triggerTradeId: 11,
      linkedTradeIds: const [10, 11],
      title: 'ورود مجدد پس از زیان',
      evidenceLevel: level,
      evidenceScore: 35,
      actualNetPnl: -12.5,
      reasons: const ['افزایش ریسک بیش از ۲۵٪'],
      elapsedMinutes: 8,
    );

void main() {
  testWidgets('Behavioral Lab only renders episodes for the selected account',
      (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxBehavioralLab(
          accountId: 'account-a',
          episodes: [
            episode(),
            episode(id: 'b:20:21', accountId: 'account-b'),
          ],
        ),
      ),
    ));

    expect(find.text('ورود مجدد پس از زیان'), findsOneWidget);
    expect(find.text('اپیزودها'), findsOneWidget);
    expect(find.text('2'), findsNothing);
  });

  testWidgets('review action is emitted and shown locally', (tester) async {
    String? reviewedId;
    TxBehavioralReview? review;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxBehavioralLab(
          accountId: 'account-a',
          episodes: [episode()],
          onReviewChanged: (id, value) {
            reviewedId = id;
            review = value;
          },
        ),
      ),
    ));

    await tester.tap(find.text('تأیید توسط من'));
    await tester.pump();

    expect(reviewedId, 'a:10:11');
    expect(review, TxBehavioralReview.confirmed);
    expect(find.text('بازبینی کاربر: تأیید شده توسط کاربر'), findsOneWidget);
  });

  testWidgets('linked trades are passed to the host for drill-down',
      (tester) async {
    List<int>? openedTrades;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: TxBehavioralLab(
          accountId: 'account-a',
          episodes: [episode()],
          onShowTrades: (ids) => openedTrades = ids,
        ),
      ),
    ));

    await tester.tap(find.text('نمایش معاملات مرتبط'));
    expect(openedTrades, [10, 11]);
  });
}

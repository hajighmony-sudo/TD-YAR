import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tx_ai_behavioral_lab.dart';
import 'package:tradenex_fx/tx_ai_data_bridge.dart';

void main() {
  testWidgets("behavioral pattern feed persists the user's review choice", (tester) async {
    TxBehavioralReview? selectedReview;
    const pattern = TxAiBehaviorPatternRecord(
      id: 'account-1:session-drift:1',
      title: 'معامله خارج از سشن مجاز استراتژی',
      triggerTradeId: 1,
      linkedTradeIds: [1, 2],
      evidenceScore: 30,
      actualNetPnl: 12.5,
      reasons: ['قانون سشن صریح ثبت شده است.'],
    );

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: TxBehavioralPatternFeed(
            patterns: const [pattern],
            onReviewChanged: (id, review) => selectedReview = review,
          ),
        ),
      ),
    ));

    expect(find.text('معامله خارج از سشن مجاز استراتژی'), findsOneWidget);
    await tester.tap(find.text('مورد معتبر نیست'));
    await tester.pump();

    expect(selectedReview, TxBehavioralReview.notRevenge);
    expect(find.text('بازبینی کاربر: مورد معتبر نیست'), findsOneWidget);
  });
}

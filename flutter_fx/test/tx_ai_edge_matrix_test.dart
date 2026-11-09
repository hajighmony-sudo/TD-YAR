import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tradenex_fx/tx_ai_edge_matrix.dart';

void main() {
  testWidgets('matrix withholds performance claims below sample gate',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Scaffold(
        body: TxCrossAnalysisMatrix(
          rowLabels: ['London'],
          columnLabels: ['ICT'],
          cells: [
            TxEdgeMatrixCell(
              rowKey: 'London',
              columnKey: 'ICT',
              tradeCount: 3,
              netPnl: 120,
              winRate: 1,
            ),
          ],
        ),
      ),
    ));

    expect(find.textContaining('3 معامله · نمونه کم'), findsOneWidget);
    expect(find.text('120.00'), findsNothing);
    expect(find.text('100%'), findsNothing);
  });

  testWidgets('matrix displays metrics once sample gate is met',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Scaffold(
        body: TxCrossAnalysisMatrix(
          rowLabels: ['London'],
          columnLabels: ['ICT'],
          cells: [
            TxEdgeMatrixCell(
              rowKey: 'London',
              columnKey: 'ICT',
              tradeCount: 6,
              netPnl: 120,
              winRate: 0.5,
            ),
          ],
        ),
      ),
    ));

    expect(find.text('120.00'), findsOneWidget);
    expect(find.textContaining('50%'), findsOneWidget);
  });
}

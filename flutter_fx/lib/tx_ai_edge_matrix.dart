import 'package:flutter/material.dart';

class TxEdgeMatrixCell {
  const TxEdgeMatrixCell({
    required this.rowKey,
    required this.columnKey,
    required this.tradeCount,
    required this.netPnl,
    required this.winRate,
  });

  final String rowKey;
  final String columnKey;
  final int tradeCount;
  final double? netPnl;
  final double? winRate;
}

/// A two-dimensional comparison matrix. Cells below [minimumSample] show
/// counts only; performance claims are withheld for small samples.
class TxCrossAnalysisMatrix extends StatelessWidget {
  const TxCrossAnalysisMatrix({
    super.key,
    required this.rowLabels,
    required this.columnLabels,
    required this.cells,
    this.minimumSample = 5,
    this.title = 'Cross-analysis Matrix',
  });

  final List<String> rowLabels;
  final List<String> columnLabels;
  final List<TxEdgeMatrixCell> cells;
  final int minimumSample;
  final String title;

  @override
  Widget build(BuildContext context) {
    final lookup = {
      for (final cell in cells) '${cell.rowKey}\u0000${cell.columnKey}': cell,
    };
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(title,
                  style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(
                'هر خانه فقط از معاملات ثبت‌شده ساخته می‌شود؛ برای کمتر از $minimumSample معامله، نتیجه عملکردی اعلام نمی‌شود.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 10),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: DataTable(
                  columns: [
                    const DataColumn(label: Text('گروه')),
                    for (final label in columnLabels)
                      DataColumn(label: Text(label)),
                  ],
                  rows: [
                    for (final row in rowLabels)
                      DataRow(cells: [
                        DataCell(Text(row)),
                        for (final column in columnLabels)
                          DataCell(_cell(lookup['$row\u0000$column'])),
                      ]),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _cell(TxEdgeMatrixCell? cell) {
    if (cell == null || cell.tradeCount == 0) {
      return const Text('—');
    }
    if (cell.tradeCount < minimumSample) {
      return Text('${cell.tradeCount} معامله · نمونه کم');
    }
    final pnl = cell.netPnl?.toStringAsFixed(2) ?? '—';
    final win = cell.winRate == null
        ? '—'
        : '${(cell.winRate!.clamp(0, 1) * 100).round()}%';
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(pnl, style: const TextStyle(fontWeight: FontWeight.w800)),
        Text('$win · ${cell.tradeCount} معامله',
            style: const TextStyle(fontSize: 10)),
      ],
    );
  }
}

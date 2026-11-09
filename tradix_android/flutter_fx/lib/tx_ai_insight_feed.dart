import 'package:flutter/material.dart';

enum TxInsightPeriod { daily, weekly, monthly }

class TxInsight {
  const TxInsight({
    required this.id,
    required this.period,
    required this.title,
    required this.reason,
    required this.metricLabel,
    required this.metricValue,
    required this.action,
    required this.supportingTradeIds,
    this.pinned = false,
  });

  final String id;
  final TxInsightPeriod period;
  final String title;
  final String reason;
  final String metricLabel;
  final String metricValue;
  final String action;
  final List<int> supportingTradeIds;
  final bool pinned;
}

/// Deterministic insight cards with explicit evidence drill-down and pinning.
class TxInsightFeed extends StatefulWidget {
  const TxInsightFeed({
    super.key,
    required this.insights,
    this.onPinChanged,
    this.onOpenEvidence,
  });

  final List<TxInsight> insights;
  final void Function(String insightId, bool pinned)? onPinChanged;
  final void Function(List<int> tradeIds)? onOpenEvidence;

  @override
  State<TxInsightFeed> createState() => _TxInsightFeedState();
}

class _TxInsightFeedState extends State<TxInsightFeed> {
  TxInsightPeriod? _period;
  final Map<String, bool> _pinOverrides = {};

  bool _isPinned(TxInsight insight) => _pinOverrides[insight.id] ?? insight.pinned;

  @override
  Widget build(BuildContext context) {
    final visible = widget.insights.where((insight) =>
        _period == null || insight.period == _period).toList(growable: false);
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text('Insight Feed',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          Wrap(spacing: 6, children: [
            _periodChip('همه', null),
            _periodChip('روزانه', TxInsightPeriod.daily),
            _periodChip('هفتگی', TxInsightPeriod.weekly),
            _periodChip('ماهانه', TxInsightPeriod.monthly),
          ]),
          const SizedBox(height: 8),
          if (visible.isEmpty)
            const Padding(
              padding: EdgeInsets.all(18),
              child: Text('برای این بازه هنوز بینش معتبری ثبت نشده است.',
                  textAlign: TextAlign.center),
            ),
          for (final insight in visible) _insightCard(insight),
        ],
      ),
    );
  }

  Widget _periodChip(String label, TxInsightPeriod? period) => ChoiceChip(
        label: Text(label),
        selected: _period == period,
        onSelected: (_) => setState(() => _period = period),
      );

  Widget _insightCard(TxInsight insight) {
    final pinned = _isPinned(insight);
    final periodLabel = switch (insight.period) {
      TxInsightPeriod.daily => 'روزانه',
      TxInsightPeriod.weekly => 'هفتگی',
      TxInsightPeriod.monthly => 'ماهانه',
    };
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Row(children: [
            Expanded(child: Text(insight.title,
                style: const TextStyle(fontWeight: FontWeight.w800))),
            Text(periodLabel, style: Theme.of(context).textTheme.labelSmall),
          ]),
          const SizedBox(height: 6),
          Text(insight.reason),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: Text(insight.metricLabel)),
            Text(insight.metricValue,
                style: const TextStyle(fontWeight: FontWeight.w800)),
          ]),
          const SizedBox(height: 6),
          Text('اقدام پیشنهادی: ${insight.action}',
              style: Theme.of(context).textTheme.bodySmall),
          const Divider(height: 16),
          Wrap(spacing: 8, children: [
            OutlinedButton.icon(
              onPressed: () {
                setState(() => _pinOverrides[insight.id] = !pinned);
                widget.onPinChanged?.call(insight.id, !pinned);
              },
              icon: Icon(pinned ? Icons.push_pin : Icons.push_pin_outlined),
              label: Text(pinned ? 'برداشتن سنجاق' : 'سنجاق کردن'),
            ),
            OutlinedButton.icon(
              onPressed: widget.onOpenEvidence == null
                  ? null
                  : () => widget.onOpenEvidence!(
                        List<int>.unmodifiable(insight.supportingTradeIds),
                      ),
              icon: const Icon(Icons.search),
              label: const Text('مشاهده شواهد'),
            ),
          ]),
        ]),
      ),
    );
  }
}

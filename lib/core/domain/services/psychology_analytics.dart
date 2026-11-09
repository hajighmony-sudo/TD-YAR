import '../models/performance_report.dart';
import '../models/psychology_entry.dart';
import '../models/psychology_insights.dart';

class PsychologyAnalytics {
  const PsychologyAnalytics();

  PsychologyInsights analyze({
    required List<PsychologyEntry> entries,
    required List<TradeOutcome> outcomes,
  }) {
    if (entries.isEmpty) {
      return const PsychologyInsights(
        entryCount: 0,
        averageConfidence: 0,
        averageFocus: 0,
        averageStress: 0,
        planAdherencePercent: 0,
        fomoCount: 0,
        revengeTradeCount: 0,
        averagePnlWhenPlanFollowed: null,
        averagePnlWhenPlanNotFollowed: null,
      );
    }
    final linkedOutcomes = {
      for (final outcome in outcomes) outcome.tradeId: outcome,
    };
    final followedPnl = <double>[];
    final notFollowedPnl = <double>[];
    for (final entry in entries) {
      final outcome = entry.tradeId == null ? null : linkedOutcomes[entry.tradeId];
      if (outcome == null) continue;
      (entry.followedPlan ? followedPnl : notFollowedPnl).add(outcome.netPnl);
    }
    double average(Iterable<int> values) {
      final list = values.toList(growable: false);
      return list.isEmpty
          ? 0
          : list.fold<int>(0, (sum, value) => sum + value) / list.length;
    }
    double? averagePnl(List<double> values) => values.isEmpty
        ? null
        : values.fold<double>(0, (sum, value) => sum + value) / values.length;

    return PsychologyInsights(
      entryCount: entries.length,
      averageConfidence: average(entries.map((item) => item.confidence)),
      averageFocus: average(entries.map((item) => item.focus)),
      averageStress: average(entries.map((item) => item.stress)),
      planAdherencePercent: entries.where((item) => item.followedPlan).length /
          entries.length * 100,
      fomoCount: entries.where((item) => item.isFomo).length,
      revengeTradeCount: entries.where((item) => item.isRevengeTrade).length,
      averagePnlWhenPlanFollowed: averagePnl(followedPnl),
      averagePnlWhenPlanNotFollowed: averagePnl(notFollowedPnl),
    );
  }
}

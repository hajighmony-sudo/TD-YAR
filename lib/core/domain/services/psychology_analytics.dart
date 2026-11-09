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
    final followed = <double>[];
    final notFollowed = <double>[];
    final fomo = <double>[];
    final noFomo = <double>[];
    final revenge = <double>[];
    final noRevenge = <double>[];
    final moodCounts = <String, int>{};
    var linkedCount = 0;

    for (final entry in entries) {
      final mood = entry.mood.trim();
      moodCounts.update(mood, (count) => count + 1, ifAbsent: () => 1);
      final outcome = entry.tradeId == null
          ? null
          : linkedOutcomes[entry.tradeId];
      if (outcome == null) continue;
      linkedCount++;
      (entry.followedPlan ? followed : notFollowed).add(outcome.netPnl);
      (entry.isFomo ? fomo : noFomo).add(outcome.netPnl);
      (entry.isRevengeTrade ? revenge : noRevenge).add(outcome.netPnl);
    }

    double averageInt(Iterable<int> values) {
      final list = values.toList(growable: false);
      return list.isEmpty
          ? 0
          : list.fold<int>(0, (sum, value) => sum + value) / list.length;
    }

    double? averagePnl(List<double> values) => values.isEmpty
        ? null
        : values.fold<double>(0, (sum, value) => sum + value) / values.length;

    final followedCount = entries.where((item) => item.followedPlan).length;
    return PsychologyInsights(
      entryCount: entries.length,
      linkedTradeEntryCount: linkedCount,
      followedPlanCount: followedCount,
      notFollowedPlanCount: entries.length - followedCount,
      averageConfidence:
          averageInt(entries.map((item) => item.confidence)),
      averageFocus: averageInt(entries.map((item) => item.focus)),
      averageStress: averageInt(entries.map((item) => item.stress)),
      planAdherencePercent: followedCount / entries.length * 100,
      fomoCount: entries.where((item) => item.isFomo).length,
      revengeTradeCount: entries.where((item) => item.isRevengeTrade).length,
      moodCounts: Map.unmodifiable(moodCounts),
      averagePnlWhenPlanFollowed: averagePnl(followed),
      averagePnlWhenPlanNotFollowed: averagePnl(notFollowed),
      averagePnlWhenFomo: averagePnl(fomo),
      averagePnlWithoutFomo: averagePnl(noFomo),
      averagePnlWhenRevengeTrade: averagePnl(revenge),
      averagePnlWithoutRevengeTrade: averagePnl(noRevenge),
    );
  }
}

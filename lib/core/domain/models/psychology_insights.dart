class PsychologyInsights {
  const PsychologyInsights({
    required this.entryCount,
    required this.averageConfidence,
    required this.averageFocus,
    required this.averageStress,
    required this.planAdherencePercent,
    required this.fomoCount,
    required this.revengeTradeCount,
    required this.averagePnlWhenPlanFollowed,
    required this.averagePnlWhenPlanNotFollowed,
    this.linkedTradeEntryCount = 0,
    this.followedPlanCount = 0,
    this.notFollowedPlanCount = 0,
    this.moodCounts = const {},
    this.averagePnlWhenFomo,
    this.averagePnlWithoutFomo,
    this.averagePnlWhenRevengeTrade,
    this.averagePnlWithoutRevengeTrade,
  });

  final int entryCount;
  final int linkedTradeEntryCount;
  final int followedPlanCount;
  final int notFollowedPlanCount;
  final double averageConfidence;
  final double averageFocus;
  final double averageStress;
  final double planAdherencePercent;
  final int fomoCount;
  final int revengeTradeCount;
  final Map<String, int> moodCounts;
  final double? averagePnlWhenPlanFollowed;
  final double? averagePnlWhenPlanNotFollowed;
  final double? averagePnlWhenFomo;
  final double? averagePnlWithoutFomo;
  final double? averagePnlWhenRevengeTrade;
  final double? averagePnlWithoutRevengeTrade;
}

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
  });

  final int entryCount;
  final double averageConfidence;
  final double averageFocus;
  final double averageStress;
  final double planAdherencePercent;
  final int fomoCount;
  final int revengeTradeCount;
  final double? averagePnlWhenPlanFollowed;
  final double? averagePnlWhenPlanNotFollowed;
}

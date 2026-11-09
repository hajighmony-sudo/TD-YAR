import '../../data/repositories/psychology_repository.dart';
import '../models/psychology_insights.dart';
import 'performance_analytics_service.dart';
import 'psychology_analytics.dart';

/// Combines user-entered psychology notes with realized journal outcomes.
class PsychologyAnalyticsService {
  const PsychologyAnalyticsService({
    required PsychologyRepository psychologyRepository,
    required PerformanceAnalyticsService performanceService,
    this.analytics = const PsychologyAnalytics(),
  })  : _psychologyRepository = psychologyRepository,
        _performanceService = performanceService;

  final PsychologyRepository _psychologyRepository;
  final PerformanceAnalyticsService _performanceService;
  final PsychologyAnalytics analytics;

  Future<PsychologyInsights> buildInsights({
    DateTime? from,
    DateTime? to,
  }) async {
    if (from != null && to != null && to.isBefore(from)) {
      throw ArgumentError('End date must not precede start date.');
    }
    final entries = await _psychologyRepository.getEntries(from: from, to: to);
    final report = await _performanceService.buildReport();
    return analytics.analyze(entries: entries, outcomes: report.outcomes);
  }
}

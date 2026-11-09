import '../../data/repositories/journal_repository.dart';
import '../../data/repositories/psychology_repository.dart';
import '../models/calendar_day_summary.dart';
import 'performance_analytics_service.dart';
import 'trading_calendar.dart';

class TradingCalendarService {
  TradingCalendarService({
    required PerformanceAnalyticsService analyticsService,
    required JournalRepository journalRepository,
    required PsychologyRepository psychologyRepository,
    TradingCalendar calendar = const TradingCalendar(),
  })  : _analyticsService = analyticsService,
        _journalRepository = journalRepository,
        _psychologyRepository = psychologyRepository,
        _calendar = calendar;

  final PerformanceAnalyticsService _analyticsService;
  final JournalRepository _journalRepository;
  final PsychologyRepository _psychologyRepository;
  final TradingCalendar _calendar;

  Future<List<CalendarDaySummary>> month(int year, int month) async {
    final start = DateTime(year, month);
    final end = DateTime(year, month + 1);
    final report = await _analyticsService.buildReport(
      from: start,
      to: end.subtract(const Duration(microseconds: 1)),
    );
    final journalEntries = await _journalRepository.getEntries(
      from: start,
      to: end.subtract(const Duration(microseconds: 1)),
    );
    final psychologyEntries = await _psychologyRepository.getEntries(
      from: start,
      to: end.subtract(const Duration(microseconds: 1)),
    );
    return _calendar.buildMonth(
      year: year,
      month: month,
      outcomes: report.outcomes,
      journalEntries: journalEntries,
      psychologyEntries: psychologyEntries,
    );
  }
}

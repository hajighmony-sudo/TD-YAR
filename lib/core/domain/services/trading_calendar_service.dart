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

  Future<List<CalendarDaySummary>> day(DateTime date) =>
      range(date, date);

  Future<List<CalendarDaySummary>> week(DateTime date) {
    final start = DateTime(date.year, date.month, date.day)
        .subtract(Duration(days: date.weekday - DateTime.monday));
    return range(start, DateTime(start.year, start.month, start.day + 6));
  }

  Future<List<CalendarDaySummary>> month(int year, int month) {
    if (month < 1 || month > 12) {
      throw ArgumentError.value(month, 'month', 'Must be between 1 and 12.');
    }
    return range(DateTime(year, month, 1), DateTime(year, month + 1, 0));
  }

  Future<List<CalendarDaySummary>> range(DateTime from, DateTime to) async {
    final start = DateTime(from.year, from.month, from.day);
    final end = DateTime(to.year, to.month, to.day);
    if (end.isBefore(start)) {
      throw ArgumentError('Calendar end date must not precede start date.');
    }
    final inclusiveEnd = DateTime(end.year, end.month, end.day + 1)
        .subtract(const Duration(microseconds: 1));
    final report = await _analyticsService.buildReport(
      from: start,
      to: inclusiveEnd,
    );
    final journalEntries = await _journalRepository.getEntries(
      from: start,
      to: inclusiveEnd,
    );
    final psychologyEntries = await _psychologyRepository.getEntries(
      from: start,
      to: inclusiveEnd,
    );
    return _calendar.buildRange(
      from: start,
      to: end,
      outcomes: report.outcomes,
      realizations: report.realizations,
      journalEntries: journalEntries,
      psychologyEntries: psychologyEntries,
    );
  }
}

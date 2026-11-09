import '../models/calendar_day_summary.dart';
import '../models/journal_entry.dart';
import '../models/performance_report.dart';
import '../models/psychology_entry.dart';

class TradingCalendar {
  const TradingCalendar();

  List<CalendarDaySummary> buildMonth({
    required int year,
    required int month,
    required List<TradeOutcome> outcomes,
    required List<JournalEntry> journalEntries,
    required List<PsychologyEntry> psychologyEntries,
  }) {
    if (month < 1 || month > 12) {
      throw ArgumentError.value(month, 'month', 'Must be between 1 and 12.');
    }
    return buildRange(
      from: DateTime(year, month, 1),
      to: DateTime(year, month + 1, 0),
      outcomes: outcomes,
      journalEntries: journalEntries,
      psychologyEntries: psychologyEntries,
    );
  }

  List<CalendarDaySummary> buildRange({
    required DateTime from,
    required DateTime to,
    required List<TradeOutcome> outcomes,
    required List<JournalEntry> journalEntries,
    required List<PsychologyEntry> psychologyEntries,
  }) {
    final start = DateTime(from.year, from.month, from.day);
    final end = DateTime(to.year, to.month, to.day);
    if (end.isBefore(start)) {
      throw ArgumentError('Calendar end date must not precede start date.');
    }
    final summaries = <CalendarDaySummary>[];
    for (var date = start;
        !date.isAfter(end);
        date = date.add(const Duration(days: 1))) {
      final values = outcomes.where((item) =>
          _sameDate(item.closedAt, date)).toList(growable: false);
      final journalCount = journalEntries.where((item) =>
          _sameDate(item.entryDate, date)).length;
      final psychologyCount = psychologyEntries.where((item) =>
          _sameDate(item.entryDate, date)).length;
      summaries.add(CalendarDaySummary(
        date: date,
        tradeCount: values.length,
        wins: values.where((item) => item.isWin).length,
        losses: values.where((item) => item.isLoss).length,
        netPnl: values.fold<double>(0, (sum, item) => sum + item.netPnl),
        journalEntryCount: journalCount,
        psychologyEntryCount: psychologyCount,
      ));
    }
    return List.unmodifiable(summaries);
  }

  static bool _sameDate(DateTime first, DateTime second) =>
      first.year == second.year &&
      first.month == second.month &&
      first.day == second.day;
}

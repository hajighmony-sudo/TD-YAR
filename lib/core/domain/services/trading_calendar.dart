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
    final daysInMonth = DateTime(year, month + 1, 0).day;
    final summaries = <CalendarDaySummary>[];
    for (var day = 1; day <= daysInMonth; day++) {
      final date = DateTime(year, month, day);
      final dayOutcomes = outcomes.where((item) =>
          item.closedAt.year == year &&
          item.closedAt.month == month &&
          item.closedAt.day == day);
      final dayJournal = journalEntries.where((item) =>
          item.entryDate.year == year &&
          item.entryDate.month == month &&
          item.entryDate.day == day);
      final dayPsychology = psychologyEntries.where((item) =>
          item.entryDate.year == year &&
          item.entryDate.month == month &&
          item.entryDate.day == day);
      final values = dayOutcomes.toList(growable: false);
      summaries.add(CalendarDaySummary(
        date: date,
        tradeCount: values.length,
        wins: values.where((item) => item.isWin).length,
        losses: values.where((item) => item.isLoss).length,
        netPnl: values.fold<double>(0, (sum, item) => sum + item.netPnl),
        journalEntryCount: dayJournal.length,
        psychologyEntryCount: dayPsychology.length,
      ));
    }
    return List.unmodifiable(summaries);
  }
}

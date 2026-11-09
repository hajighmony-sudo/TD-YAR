import '../../data/repositories/journal_repository.dart';
import '../models/daily_journal_insights.dart';
import '../models/journal_entry.dart';
import 'daily_journal_analytics.dart';

/// Coordinates the daily journal lifecycle independently of the UI.
class DailyJournalService {
  const DailyJournalService({
    required JournalRepository repository,
    this.analytics = const DailyJournalAnalytics(),
  }) : _repository = repository;

  final JournalRepository _repository;
  final DailyJournalAnalytics analytics;

  Future<void> save(JournalEntry entry) async {
    if (entry.id.trim().isEmpty) {
      throw ArgumentError('Journal entry id is required.');
    }
    final existing = await _repository.getEntry(entry.id);
    if (existing == null) {
      await _repository.createEntry(entry);
    } else {
      await _repository.updateEntry(entry);
    }
  }

  Future<List<JournalEntry>> getForDay(DateTime date) {
    final start = DateTime(date.year, date.month, date.day);
    final end = DateTime(start.year, start.month, start.day + 1)
        .subtract(const Duration(microseconds: 1));
    return _repository.getEntries(from: start, to: end);
  }

  Future<List<JournalEntry>> getRange(DateTime from, DateTime to) {
    if (to.isBefore(from)) {
      throw ArgumentError('Journal end date must not precede start date.');
    }
    return _repository.getEntries(from: from, to: to);
  }

  Future<void> delete(String id) => _repository.deleteEntry(id);

  Future<DailyJournalInsights> buildInsights({
    DateTime? from,
    DateTime? to,
    DateTime? asOfDate,
  }) async {
    final entries = await _repository.getEntries(from: from, to: to);
    return analytics.analyze(
      entries: entries,
      asOfDate: asOfDate ?? DateTime.now(),
    );
  }
}

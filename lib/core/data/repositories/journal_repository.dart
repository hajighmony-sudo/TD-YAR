import '../../domain/models/journal_entry.dart';

abstract interface class JournalRepository {
  Future<void> createEntry(JournalEntry entry);

  Future<void> updateEntry(JournalEntry entry);

  Future<JournalEntry?> getEntry(String id);

  Future<List<JournalEntry>> getEntries({
    DateTime? from,
    DateTime? to,
  });

  Future<void> deleteEntry(String id);
}

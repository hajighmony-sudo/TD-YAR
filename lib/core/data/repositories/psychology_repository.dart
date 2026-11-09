import '../../domain/models/psychology_entry.dart';

abstract interface class PsychologyRepository {
  Future<void> create(PsychologyEntry entry);
  Future<void> update(PsychologyEntry entry);
  Future<PsychologyEntry?> getById(String id);
  Future<List<PsychologyEntry>> getEntries({
    DateTime? from,
    DateTime? to,
    String? tradeId,
  });
  Future<void> delete(String id);
}

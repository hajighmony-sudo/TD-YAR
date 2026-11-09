import '../../data/repositories/trading_session_repository.dart';
import '../models/trading_session.dart';
import 'trading_sessions.dart';

class TradingSessionService {
  TradingSessionService({
    required TradingSessionRepository repository,
    TradingSessions? calculator,
  })  : _repository = repository,
        _calculator = calculator ?? TradingSessions();

  final TradingSessionRepository _repository;
  final TradingSessions _calculator;

  Future<void> initializeDefaults() async {
    final existing = await _repository.getAll(includeDisabled: true);
    final existingIds = existing.map((item) => item.id).toSet();
    for (final session in TradingSessions.defaultSessions()) {
      if (!existingIds.contains(session.id)) {
        await create(session);
      }
    }
  }

  Future<void> create(TradingSessionDefinition session) async {
    _calculator.validateDefinition(session);
    await _repository.create(session);
  }

  Future<void> update(TradingSessionDefinition session) async {
    _calculator.validateDefinition(session);
    await _repository.update(session);
  }

  Future<void> delete(String id) => _repository.delete(id);

  Future<List<TradingSessionDefinition>> list({
    bool includeDisabled = false,
  }) =>
      _repository.getAll(includeDisabled: includeDisabled);

  Future<List<TradingSessionStatus>> statusesAt(DateTime instant) async {
    final sessions = await _repository.getAll();
    return _calculator.statusesAt(
      instant: instant,
      sessions: sessions,
    );
  }

  Future<List<SessionOverlap>> overlapsBetween({
    required DateTime from,
    required DateTime to,
  }) async {
    final sessions = await _repository.getAll();
    return _calculator.overlapsBetween(
      from: from,
      to: to,
      sessions: sessions,
    );
  }
}

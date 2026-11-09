import '../../domain/models/trading_session.dart';

abstract interface class TradingSessionRepository {
  Future<void> create(TradingSessionDefinition session);
  Future<void> update(TradingSessionDefinition session);
  Future<TradingSessionDefinition?> getById(String id);
  Future<List<TradingSessionDefinition>> getAll({bool includeDisabled = false});
  Future<void> delete(String id);
}

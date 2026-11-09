import '../../domain/models/strategy.dart';

abstract interface class StrategyRepository {
  Future<void> create(Strategy strategy);
  Future<void> update(Strategy strategy);
  Future<Strategy?> getById(String id);
  Future<List<Strategy>> getAll({bool includeArchived = false});
  Future<void> delete(String id);
}

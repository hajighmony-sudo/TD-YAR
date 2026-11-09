import '../../data/repositories/strategy_repository.dart';
import '../models/strategy.dart';

/// Application-level rules for a user's personal strategy library.
class StrategyManagementService {
  const StrategyManagementService(this._repository);

  final StrategyRepository _repository;

  Future<void> create(Strategy strategy) async {
    _validate(strategy);
    await _ensureUniqueName(strategy.name);
    await _repository.create(strategy);
  }

  Future<void> update(Strategy strategy) async {
    _validate(strategy);
    await _ensureUniqueName(strategy.name, excludingId: strategy.id);
    await _repository.update(strategy);
  }

  Future<void> archive(Strategy strategy, {required bool archived}) =>
      _repository.update(_copy(strategy, isArchived: archived));

  Future<void> delete(String id) => _repository.delete(id);
  Future<Strategy?> get(String id) => _repository.getById(id);
  Future<List<Strategy>> list({bool includeArchived = false}) =>
      _repository.getAll(includeArchived: includeArchived);

  Future<void> _ensureUniqueName(String name, {String? excludingId}) async {
    final normalized = name.trim().toLowerCase();
    final all = await _repository.getAll(includeArchived: true);
    if (all.any((item) =>
        item.id != excludingId && item.name.trim().toLowerCase() == normalized)) {
      throw StateError('A strategy with this name already exists.');
    }
  }

  static void _validate(Strategy strategy) {
    if (strategy.id.trim().isEmpty || strategy.name.trim().isEmpty) {
      throw ArgumentError('Strategy id and name are required.');
    }
    final collections = [
      strategy.timeframes, strategy.setupConditions, strategy.entryRules,
      strategy.exitRules, strategy.riskRules, strategy.checklistItems, strategy.tags,
    ];
    if (collections.any((items) => items.any((item) => item.trim().isEmpty))) {
      throw ArgumentError('Strategy lists cannot contain empty values.');
    }
  }

  static Strategy _copy(Strategy value, {required bool isArchived}) => Strategy(
        id: value.id, name: value.name, description: value.description,
        market: value.market, timeframes: value.timeframes,
        setupConditions: value.setupConditions, entryRules: value.entryRules,
        exitRules: value.exitRules, riskRules: value.riskRules,
        checklistItems: value.checklistItems, tags: value.tags,
        isArchived: isArchived, createdAt: value.createdAt,
        updatedAt: DateTime.now(),
      );
}

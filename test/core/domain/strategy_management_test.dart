import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/data/repositories/strategy_repository.dart';
import 'package:td_yar/core/domain/models/strategy.dart';
import 'package:td_yar/core/domain/services/strategy_management_service.dart';

void main() {
  final now = DateTime.utc(2026, 4, 1);

  Strategy strategy(String id, String name, {bool archived = false}) => Strategy(
        id: id,
        name: name,
        description: 'Test strategy',
        createdAt: now,
        updatedAt: now,
        setupConditions: const ['breakout'],
        entryRules: const ['close above range'],
        exitRules: const ['manual exit'],
        riskRules: const ['predefined stop'],
        checklistItems: const ['check spread'],
        tags: const ['test'],
        isArchived: archived,
      );

  test('strategy service rejects duplicate names case-insensitively', () async {
    final repository = _MemoryStrategyRepository();
    final service = StrategyManagementService(repository);
    await service.create(strategy('s1', 'London Breakout'));
    await expectLater(
      service.create(strategy('s2', 'london breakout')),
      throwsStateError,
    );
    expect((await service.list()).length, 1);
  });

  test('strategy service archives and restores a strategy', () async {
    final repository = _MemoryStrategyRepository();
    final service = StrategyManagementService(repository);
    final value = strategy('s1', 'Range');
    await service.create(value);
    await service.archive(value, archived: true);
    expect(await service.list(), isEmpty);
    expect((await service.list(includeArchived: true)).single.isArchived, isTrue);
    final archived = (await service.list(includeArchived: true)).single;
    await service.archive(archived, archived: false);
    expect((await service.list()).single.isArchived, isFalse);
  });
}

class _MemoryStrategyRepository implements StrategyRepository {
  final Map<String, Strategy> values = {};

  @override
  Future<void> create(Strategy strategy) async {
    if (values.containsKey(strategy.id)) throw StateError('Duplicate id');
    values[strategy.id] = strategy;
  }

  @override
  Future<void> update(Strategy strategy) async {
    if (!values.containsKey(strategy.id)) throw StateError('Not found');
    values[strategy.id] = strategy;
  }

  @override
  Future<Strategy?> getById(String id) async => values[id];

  @override
  Future<List<Strategy>> getAll({bool includeArchived = false}) async =>
      values.values.where((item) => includeArchived || !item.isArchived).toList();

  @override
  Future<void> delete(String id) async {
    values.remove(id);
  }
}

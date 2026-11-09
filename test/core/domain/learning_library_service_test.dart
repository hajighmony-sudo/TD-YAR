import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/data/repositories/learning_repository.dart';
import 'package:td_yar/core/domain/models/learning_resource.dart';
import 'package:td_yar/core/domain/services/learning_library_service.dart';

void main() {
  group('LearningLibraryService', () {
    late _MemoryLearningRepository repository;
    late LearningLibraryService service;

    setUp(() {
      repository = _MemoryLearningRepository();
      service = LearningLibraryService(repository);
    });

    test('accepts text-only resources', () async {
      await service.save(_resource(
        type: LearningContentType.text,
        body: 'A learning note',
      ));
      expect(repository.items, hasLength(1));
    });

    test('rejects image resources without a media path', () async {
      await expectLater(
        service.save(_resource(type: LearningContentType.image)),
        throwsArgumentError,
      );
    });

    test('requires both text and media for mixed resources', () async {
      await expectLater(
        service.save(_resource(type: LearningContentType.mixed)),
        throwsArgumentError,
      );
    });

    test('rejects non-http source URLs', () async {
      await expectLater(
        service.save(_resource(
          type: LearningContentType.text,
          body: 'Note',
          sourceUrl: 'file:///private/source',
        )),
        throwsArgumentError,
      );
    });
  });
}

LearningResource _resource({
  required LearningContentType type,
  String body = '',
  String? mediaPath,
  String? sourceUrl,
}) {
  final now = DateTime.utc(2026, 1, 1);
  return LearningResource(
    id: 'resource-1',
    title: 'Learning item',
    type: type,
    body: body,
    mediaPath: mediaPath,
    sourceUrl: sourceUrl,
    createdAt: now,
    updatedAt: now,
  );
}

class _MemoryLearningRepository implements LearningRepository {
  final Map<String, LearningResource> items = {};

  @override
  Future<void> create(LearningResource resource) async {
    if (items.containsKey(resource.id)) {
      throw StateError('Duplicate resource');
    }
    items[resource.id] = resource;
  }

  @override
  Future<void> update(LearningResource resource) async {
    if (!items.containsKey(resource.id)) throw StateError('Missing resource');
    items[resource.id] = resource;
  }

  @override
  Future<LearningResource?> getById(String id) async => items[id];

  @override
  Future<List<LearningResource>> list({
    String? query,
    String? category,
    LearningContentType? type,
    List<String>? tags,
    bool? favoritesOnly,
  }) async =>
      items.values.where((resource) {
        if (query != null && !resource.title.contains(query)) return false;
        if (category != null && resource.category != category) return false;
        if (type != null && resource.type != type) return false;
        if (favoritesOnly == true && !resource.isFavorite) return false;
        if (tags != null && !tags.every(resource.tags.contains)) return false;
        return true;
      }).toList();

  @override
  Future<void> delete(String id) async {
    items.remove(id);
  }
}

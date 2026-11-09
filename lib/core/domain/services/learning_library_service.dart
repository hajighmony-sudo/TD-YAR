import '../../data/repositories/learning_repository.dart';
import '../../data/repositories/strategy_repository.dart';
import '../models/learning_resource.dart';

/// Validates learning material and coordinates its local persistence.
class LearningLibraryService {
  const LearningLibraryService(
    this._repo, {
    StrategyRepository? strategyRepository,
  }) : _strategyRepository = strategyRepository;

  final LearningRepository _repo;
  final StrategyRepository? _strategyRepository;

  Future<void> save(LearningResource resource) async {
    _validate(resource);
    final strategyRepository = _strategyRepository;
    if (strategyRepository != null) {
      for (final id in resource.relatedStrategyIds.toSet()) {
        if (await strategyRepository.getById(id) == null) {
          throw StateError('Related strategy not found: $id');
        }
      }
    }
    if (await _repo.getById(resource.id) == null) {
      await _repo.create(resource);
    } else {
      await _repo.update(resource);
    }
  }

  Future<List<LearningResource>> search({
    String? query,
    String? category,
    LearningContentType? type,
    List<String>? tags,
    bool favoritesOnly = false,
  }) =>
      _repo.list(
        query: query,
        category: category,
        type: type,
        tags: tags,
        favoritesOnly: favoritesOnly ? true : null,
      );

  Future<void> delete(String id) {
    if (id.trim().isEmpty) throw ArgumentError('Resource id is required.');
    return _repo.delete(id);
  }

  static void _validate(LearningResource resource) {
    if (resource.id.trim().isEmpty || resource.title.trim().isEmpty) {
      throw ArgumentError('Resource id and title are required.');
    }
    if (resource.tags.any((tag) => tag.trim().isEmpty) ||
        resource.relatedStrategyIds.any((id) => id.trim().isEmpty)) {
      throw ArgumentError('Tags and related strategy ids cannot be empty.');
    }

    final hasText = resource.body.trim().isNotEmpty;
    final hasMedia = resource.mediaPath?.trim().isNotEmpty ?? false;
    switch (resource.type) {
      case LearningContentType.text:
        if (!hasText || hasMedia) {
          throw ArgumentError('Text content must contain text only.');
        }
      case LearningContentType.image:
      case LearningContentType.video:
        if (!hasMedia || hasText) {
          throw ArgumentError(
            'Image/video content must contain media only; use mixed for both.',
          );
        }
      case LearningContentType.mixed:
        if (!hasText || !hasMedia) {
          throw ArgumentError('Mixed content requires both text and media.');
        }
    }

    final source = resource.sourceUrl?.trim();
    if (source != null && source.isNotEmpty) {
      final uri = Uri.tryParse(source);
      if (uri == null ||
          !uri.hasAuthority ||
          (uri.scheme != 'https' && uri.scheme != 'http')) {
        throw ArgumentError('Source URL must be a valid HTTP(S) URL.');
      }
    }
  }
}

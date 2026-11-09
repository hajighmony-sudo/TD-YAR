import 'dart:convert';

class Playbook {
  const Playbook({
    required this.id,
    required this.name,
    required this.description,
    required this.createdAt,
    required this.updatedAt,
    this.checklistItems = const [],
    this.tags = const [],
  });

  final String id;
  final String name;
  final String description;
  final DateTime createdAt;
  final DateTime updatedAt;
  final List<String> checklistItems;
  final List<String> tags;

  Map<String, Object?> toMap() => {
        'id': id,
        'name': name,
        'description': description,
        'checklist_json': jsonEncode(checklistItems),
        'tags_json': jsonEncode(tags),
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory Playbook.fromMap(Map<String, Object?> map) {
    final checklist = jsonDecode(
      map['checklist_json'] as String? ?? '[]',
    ) as List<dynamic>;
    final tags = jsonDecode(
      map['tags_json'] as String? ?? '[]',
    ) as List<dynamic>;
    return Playbook(
      id: map['id'] as String,
      name: map['name'] as String,
      description: map['description'] as String? ?? '',
      checklistItems:
          checklist.map((value) => value as String).toList(growable: false),
      tags: tags.map((value) => value as String).toList(growable: false),
      createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
      updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
    );
  }
}

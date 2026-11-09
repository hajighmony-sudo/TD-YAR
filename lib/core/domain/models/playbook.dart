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
}

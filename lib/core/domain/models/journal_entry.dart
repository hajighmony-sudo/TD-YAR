import 'dart:convert';

class JournalEntry {
  const JournalEntry({
    required this.id,
    required this.entryDate,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.updatedAt,
    this.linkedTradeIds = const [],
  });

  final String id;
  final DateTime entryDate;
  final String title;
  final String content;
  final DateTime createdAt;
  final DateTime updatedAt;
  final List<String> linkedTradeIds;

  Map<String, Object?> toMap() => {
        'id': id,
        'entry_date': entryDate.toUtc().toIso8601String(),
        'title': title,
        'content': content,
        'linked_trade_ids_json': jsonEncode(linkedTradeIds),
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory JournalEntry.fromMap(Map<String, Object?> map) {
    final linkedIds = jsonDecode(
      map['linked_trade_ids_json'] as String? ?? '[]',
    ) as List<dynamic>;
    return JournalEntry(
      id: map['id'] as String,
      entryDate: DateTime.parse(map['entry_date'] as String).toLocal(),
      title: map['title'] as String? ?? '',
      content: map['content'] as String? ?? '',
      linkedTradeIds:
          linkedIds.map((value) => value as String).toList(growable: false),
      createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
      updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
    );
  }
}

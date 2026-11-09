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
}

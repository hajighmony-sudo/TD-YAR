import 'dart:convert';

class JournalEntry {
  const JournalEntry({
    required this.id,
    required this.entryDate,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.updatedAt,
    this.preMarketPlan = '',
    this.marketContext = '',
    this.executionReview = '',
    this.mistakes = '',
    this.strengths = '',
    this.lessons = '',
    this.nextDayPlan = '',
    this.goals = const [],
    this.tags = const [],
    this.linkedTradeIds = const [],
  });

  final String id;
  final DateTime entryDate;
  final String title;
  final String content;
  final String preMarketPlan;
  final String marketContext;
  final String executionReview;
  final String mistakes;
  final String strengths;
  final String lessons;
  final String nextDayPlan;
  final List<String> goals;
  final List<String> tags;
  final DateTime createdAt;
  final DateTime updatedAt;
  final List<String> linkedTradeIds;

  Map<String, Object?> toMap() => {
        'id': id,
        'entry_date': entryDate.toUtc().toIso8601String(),
        'title': title,
        'content': content,
        'pre_market_plan': preMarketPlan,
        'market_context': marketContext,
        'execution_review': executionReview,
        'mistakes': mistakes,
        'strengths': strengths,
        'lessons': lessons,
        'next_day_plan': nextDayPlan,
        'goals_json': jsonEncode(goals),
        'tags_json': jsonEncode(tags),
        'linked_trade_ids_json': jsonEncode(linkedTradeIds),
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory JournalEntry.fromMap(Map<String, Object?> map) => JournalEntry(
        id: map['id'] as String,
        entryDate: DateTime.parse(map['entry_date'] as String).toLocal(),
        title: map['title'] as String? ?? '',
        content: map['content'] as String? ?? '',
        preMarketPlan: map['pre_market_plan'] as String? ?? '',
        marketContext: map['market_context'] as String? ?? '',
        executionReview: map['execution_review'] as String? ?? '',
        mistakes: map['mistakes'] as String? ?? '',
        strengths: map['strengths'] as String? ?? '',
        lessons: map['lessons'] as String? ?? '',
        nextDayPlan: map['next_day_plan'] as String? ?? '',
        goals: _decodeList(map['goals_json']),
        tags: _decodeList(map['tags_json']),
        linkedTradeIds: _decodeList(map['linked_trade_ids_json']),
        createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
        updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
      );

  static List<String> _decodeList(Object? value) {
    final decoded = jsonDecode(value as String? ?? '[]') as List<dynamic>;
    return decoded.map((item) => item as String).toList(growable: false);
  }
}

import 'dart:convert';

class Strategy {
  const Strategy({
    required this.id,
    required this.name,
    required this.description,
    required this.createdAt,
    required this.updatedAt,
    this.market = '',
    this.timeframes = const [],
    this.setupConditions = const [],
    this.entryRules = const [],
    this.exitRules = const [],
    this.riskRules = const [],
    this.checklistItems = const [],
    this.tags = const [],
    this.isArchived = false,
  });

  final String id;
  final String name;
  final String description;
  final String market;
  final List<String> timeframes;
  final List<String> setupConditions;
  final List<String> entryRules;
  final List<String> exitRules;
  final List<String> riskRules;
  final List<String> checklistItems;
  final List<String> tags;
  final bool isArchived;
  final DateTime createdAt;
  final DateTime updatedAt;

  Map<String, Object?> toMap() => {
        'id': id,
        'name': name,
        'description': description,
        'market': market,
        'timeframes_json': jsonEncode(timeframes),
        'setup_conditions_json': jsonEncode(setupConditions),
        'entry_rules_json': jsonEncode(entryRules),
        'exit_rules_json': jsonEncode(exitRules),
        'risk_rules_json': jsonEncode(riskRules),
        'checklist_json': jsonEncode(checklistItems),
        'tags_json': jsonEncode(tags),
        'is_archived': isArchived ? 1 : 0,
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory Strategy.fromMap(Map<String, Object?> map) => Strategy(
        id: map['id'] as String,
        name: map['name'] as String,
        description: map['description'] as String? ?? '',
        market: map['market'] as String? ?? '',
        timeframes: _decodeList(map['timeframes_json']),
        setupConditions: _decodeList(map['setup_conditions_json']),
        entryRules: _decodeList(map['entry_rules_json']),
        exitRules: _decodeList(map['exit_rules_json']),
        riskRules: _decodeList(map['risk_rules_json']),
        checklistItems: _decodeList(map['checklist_json']),
        tags: _decodeList(map['tags_json']),
        isArchived: (map['is_archived'] as num? ?? 0) == 1,
        createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
        updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
      );

  static List<String> _decodeList(Object? value) {
    final decoded = jsonDecode(value as String? ?? '[]') as List<dynamic>;
    return decoded.map((item) => item as String).toList(growable: false);
  }
}

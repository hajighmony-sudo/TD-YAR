import 'dart:convert';

class PsychologyEntry {
  const PsychologyEntry({
    required this.id,
    required this.entryDate,
    required this.mood,
    required this.confidence,
    required this.focus,
    required this.stress,
    required this.followedPlan,
    required this.isFomo,
    required this.isRevengeTrade,
    required this.createdAt,
    required this.updatedAt,
    this.tradeId,
    this.notes = '',
    this.emotions = const [],
  });

  final String id;
  final String? tradeId;
  final DateTime entryDate;
  final String mood;
  final int confidence;
  final int focus;
  final int stress;
  final bool followedPlan;
  final bool isFomo;
  final bool isRevengeTrade;
  final String notes;
  final List<String> emotions;
  final DateTime createdAt;
  final DateTime updatedAt;

  Map<String, Object?> toMap() => {
        'id': id,
        'trade_id': tradeId,
        'entry_date': entryDate.toUtc().toIso8601String(),
        'mood': mood,
        'confidence': confidence,
        'focus': focus,
        'stress': stress,
        'followed_plan': followedPlan ? 1 : 0,
        'is_fomo': isFomo ? 1 : 0,
        'is_revenge_trade': isRevengeTrade ? 1 : 0,
        'notes': notes,
        'emotions_json': jsonEncode(emotions),
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory PsychologyEntry.fromMap(Map<String, Object?> map) {
    final decoded = jsonDecode(map['emotions_json'] as String? ?? '[]')
        as List<dynamic>;
    return PsychologyEntry(
      id: map['id'] as String,
      tradeId: map['trade_id'] as String?,
      entryDate: DateTime.parse(map['entry_date'] as String).toLocal(),
      mood: map['mood'] as String,
      confidence: (map['confidence'] as num).toInt(),
      focus: (map['focus'] as num).toInt(),
      stress: (map['stress'] as num).toInt(),
      followedPlan: (map['followed_plan'] as num) == 1,
      isFomo: (map['is_fomo'] as num) == 1,
      isRevengeTrade: (map['is_revenge_trade'] as num) == 1,
      notes: map['notes'] as String? ?? '',
      emotions: decoded.map((value) => value as String).toList(growable: false),
      createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
      updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
    );
  }
}

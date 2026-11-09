import 'dart:convert';

enum TradeDirection { long, short }

class Trade {
  const Trade({
    required this.id,
    required this.symbol,
    required this.direction,
    required this.createdAt,
    required this.updatedAt,
    this.accountLabel,
    this.notes = '',
    this.tags = const [],
    this.contractMultiplier = 1,
  });

  final String id;
  final String symbol;
  final TradeDirection direction;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String? accountLabel;
  final String notes;
  final List<String> tags;

  /// Multiplier used for contract-based instruments. Defaults to 1.
  final double contractMultiplier;

  Map<String, Object?> toMap() => {
        'id': id,
        'symbol': symbol,
        'direction': direction.name,
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
        'account_label': accountLabel,
        'notes': notes,
        'tags_json': jsonEncode(tags),
        'contract_multiplier': contractMultiplier,
      };

  factory Trade.fromMap(Map<String, Object?> map) {
    final rawTags = map['tags_json'] as String? ?? '[]';
    final decodedTags = jsonDecode(rawTags) as List<dynamic>;
    return Trade(
      id: map['id'] as String,
      symbol: map['symbol'] as String,
      direction: TradeDirection.values.byName(map['direction'] as String),
      createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
      updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
      accountLabel: map['account_label'] as String?,
      notes: map['notes'] as String? ?? '',
      tags: decodedTags.map((value) => value as String).toList(growable: false),
      contractMultiplier:
          (map['contract_multiplier'] as num?)?.toDouble() ?? 1,
    );
  }
}

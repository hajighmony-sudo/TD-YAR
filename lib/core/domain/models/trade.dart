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
    this.strategyId,
  });

  final String id;
  final String symbol;
  final TradeDirection direction;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String? accountLabel;
  final String notes;
  final List<String> tags;
  final double contractMultiplier;
  final String? strategyId;

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
        'strategy_id': strategyId,
      };

  factory Trade.fromMap(Map<String, Object?> map) {
    final decodedTags = jsonDecode(map['tags_json'] as String? ?? '[]')
        as List<dynamic>;
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
      strategyId: map['strategy_id'] as String?,
    );
  }
}

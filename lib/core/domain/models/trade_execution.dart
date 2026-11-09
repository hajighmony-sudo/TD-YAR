enum ExecutionKind { entry, exit }

class TradeExecution {
  const TradeExecution({
    required this.id,
    required this.tradeId,
    required this.kind,
    required this.quantity,
    required this.price,
    required this.executedAt,
    this.fee = 0,
  });

  final String id;
  final String tradeId;
  final ExecutionKind kind;
  final double quantity;
  final double price;
  final double fee;
  final DateTime executedAt;

  Map<String, Object?> toMap() => {
        'id': id,
        'trade_id': tradeId,
        'kind': kind.name,
        'quantity': quantity,
        'price': price,
        'fee': fee,
        'executed_at': executedAt.toUtc().toIso8601String(),
      };

  factory TradeExecution.fromMap(Map<String, Object?> map) => TradeExecution(
        id: map['id'] as String,
        tradeId: map['trade_id'] as String,
        kind: ExecutionKind.values.byName(map['kind'] as String),
        quantity: (map['quantity'] as num).toDouble(),
        price: (map['price'] as num).toDouble(),
        fee: (map['fee'] as num?)?.toDouble() ?? 0,
        executedAt: DateTime.parse(map['executed_at'] as String).toLocal(),
      );
}

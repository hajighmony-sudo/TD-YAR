class TradeMetrics {
  const TradeMetrics({
    required this.closedQuantity,
    required this.openQuantity,
    required this.averageEntryPrice,
    required this.averageExitPrice,
    required this.grossRealizedPnl,
    required this.feesAllocatedToClosedQuantity,
    required this.totalFees,
    required this.netRealizedPnl,
  });

  final double closedQuantity;
  final double openQuantity;
  final double averageEntryPrice;
  final double averageExitPrice;
  final double grossRealizedPnl;
  final double feesAllocatedToClosedQuantity;
  final double totalFees;
  final double netRealizedPnl;
}

import '../../domain/models/trade.dart';
import '../../domain/models/trade_execution.dart';

abstract interface class TradeRepository {
  Future<void> createTrade(Trade trade);
  Future<void> updateTrade(Trade trade);
  Future<Trade?> getTrade(String id);
  Future<List<Trade>> getTrades({
    String? symbol,
    DateTime? createdFrom,
    DateTime? createdTo,
    TradeDirection? direction,
    String? strategyId,
    String? accountLabel,
  });
  Future<void> addExecution(TradeExecution execution);
  Future<void> updateExecution(TradeExecution execution);
  Future<void> deleteExecution(String tradeId, String executionId);
  Future<List<TradeExecution>> getExecutions(String tradeId);
  Future<void> deleteTrade(String id);
}

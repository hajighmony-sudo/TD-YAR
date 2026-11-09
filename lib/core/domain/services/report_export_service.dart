import 'dart:convert';
import '../../data/repositories/trade_repository.dart';
import '../../data/repositories/journal_repository.dart';
import '../../data/repositories/strategy_repository.dart';
import '../models/performance_report.dart';
import 'performance_analytics_service.dart';
class ReportExportService {
 const ReportExportService({required TradeRepository tradeRepository,required PerformanceAnalyticsService performanceService,required JournalRepository journalRepository,required StrategyRepository strategyRepository}):_trades=tradeRepository,_performance=performanceService,_journals=journalRepository,_strategies=strategyRepository;
 final TradeRepository _trades;final PerformanceAnalyticsService _performance;final JournalRepository _journals;final StrategyRepository _strategies;
 Future<String> exportJson({DateTime? from,DateTime? to}) async {
  _validate(from,to);final trades=await _trades.getTrades();final journals=await _journals.getEntries(from:from,to:to);final strategies=await _strategies.getAll(includeArchived:true);final report=await _performance.buildReport(from:from,to:to);final items=<Map<String,Object?>>[];
  for(final t in trades){final all=await _trades.getExecutions(t.id);final fills=all.where((e)=>(from==null||!e.executedAt.isBefore(from))&&(to==null||!e.executedAt.isAfter(to))).toList();if((from!=null||to!=null)&&fills.isEmpty)continue;items.add({'trade':t.toMap(),'executions':fills.map((e)=>e.toMap()).toList()});}
  return const JsonEncoder.withIndent('  ').convert({'format':'td-yar-export','schema_version':1,'exported_at':DateTime.now().toUtc().toIso8601String(),'filters':{'from':from?.toUtc().toIso8601String(),'to':to?.toUtc().toIso8601String()},'trades':items,'journal_entries':journals.map((e)=>e.toMap()).toList(),'strategies':strategies.map((s)=>s.toMap()).toList(),'performance':_reportMap(report)});
 }
 Future<String> exportRealizationsCsv({DateTime? from,DateTime? to}) async { _validate(from,to);final r=await _performance.buildReport(from:from,to:to);final rows=<List<Object?>>[['trade_id','execution_id','symbol','direction','strategy_id','account','closed_at','quantity','gross_pnl','fees','net_pnl','duration_minutes'],...r.realizations.map((x)=>[x.tradeId,x.executionId,x.symbol,x.direction.name,x.strategyId??'',x.accountLabel??'',x.closedAt.toUtc().toIso8601String(),x.closedQuantity,x.grossPnl,x.fees,x.netPnl,x.duration.inMinutes])];return rows.map((row)=>row.map(csvCell).join(',')).join('\r\n');}
 static String csvCell(Object? value){final s=value?.toString()??'';return '"${s.replaceAll('"','""')}"';}
 static void _validate(DateTime? from,DateTime? to){if(from!=null&&to!=null&&to.isBefore(from))throw ArgumentError('Export end date must not precede start date.');}
 static Map<String,Object?> _reportMap(PerformanceReport r)=>{'total_trades':r.totalTrades,'wins':r.wins,'losses':r.losses,'breakeven':r.breakeven,'net_pnl':r.netPnl,'gross_pnl':r.grossPnl,'fees':r.totalFees,'win_rate':r.winRate,'average_win':r.averageWin,'average_loss':r.averageLoss,'profit_factor':r.profitFactor,'expectancy':r.expectancy,'max_drawdown':r.maxDrawdown};
}
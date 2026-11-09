import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'tradenex_fx.dart';
import 'tx_ai_behavioral_lab.dart';
import 'tx_ai_counterfactual_studio.dart';
import 'tx_ai_data_bridge.dart';
import 'tx_ai_edge_matrix.dart';
import 'tx_ai_insight_feed.dart';
import 'tx_ai_rust_ffi.dart';

class TxAiDashboardApp extends StatelessWidget {
  const TxAiDashboardApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: ThemeData.dark(useMaterial3: true).copyWith(
          scaffoldBackgroundColor: const Color(0xFF060D1D),
          colorScheme: const ColorScheme.dark(
            primary: Color(0xFF55D9F5),
            secondary: Color(0xFFFFD36E),
            surface: Color(0xFF0B1730),
          ),
        ),
        home: const TxAiDashboard(),
      );
}

class TxAiDashboard extends StatefulWidget {
  const TxAiDashboard({super.key});

  @override
  State<TxAiDashboard> createState() => _TxAiDashboardState();
}

class _TxAiDashboardState extends State<TxAiDashboard> {
  final TxAiDataBridge _bridge = TxAiDataBridge();
  late final Future<TxAiAccountSnapshot> _snapshotFuture;
  late final TxRustAnalytics? _analytics;

  @override
  void initState() {
    super.initState();
    _analytics = TxRustAnalytics.tryOpenAndroid();
    _snapshotFuture = _bridge.loadSelectedAccountSnapshot();
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 6,
      child: Scaffold(
        appBar: AppBar(
          backgroundColor: const Color(0xFF060D1D),
          title: const Text('TX AI', style: TextStyle(fontWeight: FontWeight.w900)),
          bottom: const TabBar(
            isScrollable: true,
            tabs: [
              Tab(text: 'مرکز فرماندهی'),
              Tab(text: 'Edge Explorer'),
              Tab(text: 'ماتریس و نقشه'),
              Tab(text: 'Behavioral Lab'),
              Tab(text: 'سناریوها'),
              Tab(text: 'Insight Feed'),
            ],
          ),
        ),
        body: FutureBuilder<TxAiAccountSnapshot>(
          future: _snapshotFuture,
          builder: (context, state) {
            if (state.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            final snapshot = state.data;
            final analytics = _analytics;
            if (state.hasError || snapshot == null) {
              return _errorState(
                state.error?.toString() ??
                    'داده حساب انتخاب‌شده از میزبان اندروید دریافت نشد.',
              );
            }
            if (analytics == null) {
              return _errorState(
                'کتابخانه Rust برای این دستگاه بارگذاری نشد. ساخت native library و ABI دستگاه را بررسی کن.',
              );
            }
            return _dashboard(context, snapshot, analytics);
          },
        ),
      ),
    );
  }

  Widget _errorState(String message) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.analytics_outlined,
                  color: Color(0xFF55D9F5), size: 42),
              const SizedBox(height: 14),
              const Text('TX AI به داده زنده ژورنال متصل نشد',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
              const SizedBox(height: 8),
              Text(message,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 11)),
              const SizedBox(height: 14),
              const Text(
                'هیچ معامله نمونه یا عدد ساختگی نمایش داده نمی‌شود.',
                style: TextStyle(color: Color(0xFFFFD36E), fontSize: 11),
              ),
            ],
          ),
        ),
      );

  Widget _dashboard(
    BuildContext context,
    TxAiAccountSnapshot account,
    TxRustAnalytics analytics,
  ) {
    final trades = account.trades.where((trade) => trade.pnl.isFinite).toList()
      ..sort((a, b) {
        final aRawTime = a.openedAtMillis ?? 0;
        final bRawTime = b.openedAtMillis ?? 0;
        final aTime = aRawTime > 0 ? aRawTime : 0x7fffffffffffffff;
        final bTime = bRawTime > 0 ? bRawTime : 0x7fffffffffffffff;
        final timeOrder = aTime.compareTo(bTime);
        if (timeOrder != 0) return timeOrder;
        final dayOrder = a.epochDay.compareTo(b.epochDay);
        return dayOrder != 0 ? dayOrder : a.id.compareTo(b.id);
      });
    final pnls = trades.map((trade) => trade.pnl).toList(growable: false);
    final currentDate = DateTime.tryParse(account.currentDisplayDateKey ?? '') ?? DateTime.now().toUtc();
    final currentDay = DateTime.utc(currentDate.year, currentDate.month, currentDate.day);
    final weekTrades = trades.where((trade) {
      final date = DateTime.tryParse(trade.displayDateKey ?? '');
      if (date == null) return false;
      final ageDays = currentDay.difference(DateTime.utc(date.year, date.month, date.day)).inDays;
      return ageDays >= 0 && ageDays < 7;
    }).toList(growable: false);
    final monthTrades = account.currentMonthKey == null
        ? const <TxAiTradeRecord>[]
        : trades.where((trade) => trade.displayClosedMonthKey == account.currentMonthKey)
            .toList(growable: false);
    final metrics = analytics.calculate(pnls);
    final revengeEpisodes = _revengeEpisodes(account, analytics, trades);
    final sessionSlices = _sessionSlices(trades);
    final qualifiedSessions = sessionSlices.where((e) => e.trades >= 5).toList(growable: false);
    final bestSession = qualifiedSessions.isEmpty ? null : qualifiedSessions.first;
    final worstSession = qualifiedSessions.isEmpty ? null : qualifiedSessions.last;
    final compliance = trades.where((trade) => trade.strategyFollowed != null).toList();
    final complianceCoverage = compliance.length / math.max(1, trades.length);
    final discipline = trades.length >= 5 &&
            compliance.length >= 3 &&
            complianceCoverage >= 0.70
        ? compliance.where((trade) => trade.strategyFollowed == true).length /
            compliance.length *
            100
        : null;
    final knownRisk = trades
        .where((trade) => trade.plannedRisk != null && trade.plannedRisk! > 0)
        .toList();
    final allowedRisk = account.startingBalance > 0
        ? account.startingBalance * account.riskPercent.clamp(0.1, 100) / 100
        : null;
    final averageR = trades.length >= 5 &&
            knownRisk.length >= 3 &&
            knownRisk.length / trades.length >= 0.70
        ? knownRisk
                .map((trade) => trade.pnl / trade.plannedRisk!)
                .reduce((a, b) => a + b) /
            knownRisk.length
        : null;
    final expectancyAxis = averageR == null
        ? null
        : (50 + math.atan(averageR) * 100 / math.pi).clamp(0, 100).toDouble();
    final consistencyAxis = metrics.sampleSize < 5
        ? null
        : _consistency(pnls, metrics.expectancy);
    final disciplineAxis = discipline;
    final sessionEdgeAxis = _sessionEdge(sessionSlices);
    final winAxis = metrics.sampleSize >= 5 ? metrics.winRate * 100 : null;
    final pfAxis = metrics.sampleSize >= 5
        ? (metrics.profitFactor >= 99
            ? 100.0
            : metrics.profitFactor / (metrics.profitFactor + 1) * 100)
        : null;
    final radar = <double?>[
      winAxis,
      pfAxis,
      expectancyAxis,
      consistencyAxis,
      disciplineAxis,
      sessionEdgeAxis,
    ];
    final monthCompliance = monthTrades.where((trade) => trade.strategyFollowed != null).toList();
    final monthComplianceCoverage = monthCompliance.length / math.max(1, monthTrades.length);
    final monthDiscipline = monthTrades.length >= 5 &&
            monthCompliance.length >= 3 &&
            monthComplianceCoverage >= 0.70
        ? monthCompliance.where((trade) => trade.strategyFollowed == true).length /
            monthCompliance.length * 100
        : null;
    final monthRiskTrades = monthTrades
        .where((trade) => trade.plannedRisk != null && trade.plannedRisk! > 0)
        .toList();
    final monthRiskScore = monthTrades.length >= 5 &&
            monthRiskTrades.length >= 3 &&
            monthRiskTrades.length / monthTrades.length >= 0.70 &&
            allowedRisk != null
        ? monthRiskTrades.where((trade) => trade.plannedRisk! <= allowedRisk).length /
            monthRiskTrades.length * 100
        : null;
    final completeMonthDecisions = monthTrades.where((trade) =>
        trade.entryPrice != null &&
        trade.exitPrice != null &&
        trade.stopLossPrice != null &&
        trade.takeProfitPrice != null &&
        trade.strategy.trim().isNotEmpty).length;
    final monthDecisionScore = monthTrades.length >= 5 &&
            completeMonthDecisions / monthTrades.length >= 0.70
        ? completeMonthDecisions / monthTrades.length * 100
        : null;
    final processScore = monthDiscipline != null &&
            monthRiskScore != null &&
            monthDecisionScore != null
        ? ((monthDiscipline + monthRiskScore + monthDecisionScore) / 3).round()
        : null;
    final tiltEvidence = revengeEpisodes.isEmpty
        ? null
        : revengeEpisodes.map((episode) => episode.evidenceScore).reduce((a, b) => math.max(a, b).toInt());
    final narratives = <String>[];
    if (metrics.sampleSize < 5) {
      narratives.add('برای ادعای الگوی پایدار حداقل ۵ معامله معتبر لازم است؛ فعلاً نتیجه فقط توصیفی است.');
    } else {
      narratives.add(
        'خالص ثبت‌شده: ${metrics.net.toStringAsFixed(2)} ${account.currency} · نرخ برد: ${(metrics.winRate * 100).toStringAsFixed(1)}٪ · Expectancy: ${metrics.expectancy.toStringAsFixed(2)}',
      );
      if (bestSession != null) {
        narratives.add('بهترین میانگین سشن در داده‌های فعلی: ${bestSession.label} با ${bestSession.trades} معامله.');
      }
      if (discipline != null) {
        narratives.add('پایبندی صریح به پلن: ${discipline.toStringAsFixed(0)}٪ از معاملات دارای وضعیت ثبت‌شده.');
      }
    }

    final snapshot = TxAiSnapshot(
      accountLabel: account.accountName,
      tradeCount: metrics.sampleSize,
      netPnl: metrics.sampleSize == 0 ? null : metrics.net,
      winRate: winAxis == null ? null : metrics.winRate,
      profitFactor: metrics.sampleSize < 5 ? null : metrics.profitFactor,
      expectancy: metrics.sampleSize < 5 ? null : metrics.expectancy,
      consistency: consistencyAxis,
      discipline: disciplineAxis,
      sessionEdge: sessionEdgeAxis,
      radarValues: radar,
      processScore: processScore,
      tiltEvidence: tiltEvidence,
      bestSession: bestSession?.label,
      worstSession: worstSession?.label,
      narratives: narratives,
      currency: account.currency,
    );

    final edgeTrades = trades.map((trade) => TxEdgeTrade(
      id: trade.id.toString(),
      netPnl: trade.pnl,
      session: trade.session,
      weekday: trade.displayWeekday,
      strategy: trade.strategy.isEmpty ? null : trade.strategy,
      timeframe: trade.timeframe,
      marketCondition: trade.marketCondition,
      emotion: trade.emotion,
      riskAmount: trade.plannedRisk,
      rewardAmount: trade.pnl,
    )).toList(growable: false);

    final matrixRows = trades
        .map((trade) => trade.strategy.trim().isEmpty ? 'بدون استراتژی' : trade.strategy)
        .toSet()
        .toList()
      ..sort();
    final matrixColumns = trades
        .map((trade) => trade.emotion.trim().isEmpty ? 'ثبت نشده' : trade.emotion)
        .toSet()
        .toList()
      ..sort();
    final matrixCells = <TxEdgeMatrixCell>[];
    for (final row in matrixRows) {
      for (final column in matrixColumns) {
        final rows = trades.where((trade) =>
            (trade.strategy.trim().isEmpty ? 'بدون استراتژی' : trade.strategy) == row &&
            (trade.emotion.trim().isEmpty ? 'ثبت نشده' : trade.emotion) == column);
        final values = rows.toList(growable: false);
        if (values.isEmpty) continue;
        matrixCells.add(TxEdgeMatrixCell(
          rowKey: row,
          columnKey: column,
          tradeCount: values.length,
          netPnl: values.fold<double>(0, (sum, trade) => sum + trade.pnl),
          winRate: values.where((trade) => trade.pnl > 0).length / values.length,
        ));
      }
    }

    final heatmap = _heatmap(trades);
    final processMonths = _processMonths(trades, account);
    final insights = _insights(account, analytics, weekTrades, monthTrades, monthDiscipline);
    final hasOffPlanEvidence = trades.any((trade) =>
        trade.strategyFollowed != null || _isOffPlan(trade));
    final offPlanMask = hasOffPlanEvidence
        ? trades.map(_isOffPlan).toList(growable: false)
        : null;

    return TabBarView(
      children: [
        ListView(
          padding: const EdgeInsets.only(bottom: 24),
          children: [
            SizedBox(
              height: 690,
              child: TxAiCommandCenter(
                snapshot: snapshot,
                onBehavioralLab: () => DefaultTabController.of(context).animateTo(3),
                onAxisTap: (index) => ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('شاخص ${index + 1}: فقط داده ثبت‌شده حساب انتخاب‌شده')),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: TxProcessTimeline(months: processMonths),
            ),
          ],
        ),
        TxEdgeExplorer(
          trades: edgeTrades,
          onTradeTap: (id) {
            final tradeId = int.tryParse(id);
            if (tradeId != null) _showLinkedTrades(context, trades, [tradeId], account.currency);
          },
        ),
        ListView(
          padding: const EdgeInsets.all(12),
          children: [
            TxCrossAnalysisMatrix(
              rowLabels: matrixRows,
              columnLabels: matrixColumns,
              cells: matrixCells,
              title: 'استراتژی × حالت ذهنی',
            ),
            const SizedBox(height: 12),
            _heatmapCard(heatmap, account.displayTimezone),
          ],
        ),
        SingleChildScrollView(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TxBehavioralLab(
          accountId: account.accountId,
          episodes: revengeEpisodes,
          onReviewChanged: (episodeId, review) {
            final status = switch (review) {
              TxBehavioralReview.confirmed => 'confirmed',
              TxBehavioralReview.notRevenge => 'not_revenge',
              TxBehavioralReview.unsure => 'unsure',
              TxBehavioralReview.unreviewed => 'unsure',
            };
            unawaited(_bridge.saveEpisodeReview(
              accountId: account.accountId,
              episodeId: episodeId,
              status: status,
            ).catchError((Object error) {
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('ذخیره بازبینی ناموفق بود: $error')),
                );
              }
            }));
          },
          onShowTrades: (ids) => _showLinkedTrades(context, trades, ids, account.currency),
              ),
              TxBehavioralPatternFeed(
                patterns: account.behavioralPatterns,
                onReviewChanged: (patternId, review) {
                  final status = switch (review) {
                    TxBehavioralReview.confirmed => 'confirmed',
                    TxBehavioralReview.notRevenge => 'not_revenge',
                    TxBehavioralReview.unsure => 'unsure',
                    TxBehavioralReview.unreviewed => 'unsure',
                  };
                  unawaited(_bridge.saveEpisodeReview(
                    accountId: account.accountId,
                    episodeId: patternId,
                    status: status,
                  ).catchError((Object error) {
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('ذخیره بازبینی ناموفق بود: $error')),
                      );
                    }
                  }));
                },
                onShowTrades: (ids) => _showLinkedTrades(context, trades, ids, account.currency),
              ),
            ],
          ),
        ),
        ListView(
          padding: const EdgeInsets.all(12),
          children: [
            TxCounterfactualStudio(
              analytics: analytics,
              realizedPnls: pnls,
              offPlanMask: offPlanMask,
              currency: account.currency,
            ),
          ],
        ),
        SingleChildScrollView(
          padding: const EdgeInsets.all(12),
          child: TxInsightFeed(
          insights: insights,
          onPinChanged: (id, pinned) {
            unawaited(_bridge.setInsightPinned(
              accountId: account.accountId,
              insightId: id,
              pinned: pinned,
            ).catchError((Object error) {
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('ذخیره سنجاق ناموفق بود: $error')),
                );
              }
            }));
          },
          onOpenEvidence: (ids) => _showLinkedTrades(context, trades, ids, account.currency),
          ),
        ),
      ],
    );
  }

  List<TxBehavioralEpisode> _revengeEpisodes(
    TxAiAccountSnapshot account,
    TxRustAnalytics analytics,
    List<TxAiTradeRecord> trades,
  ) {
    final episodes = <TxBehavioralEpisode>[];
    for (var index = 1; index < trades.length; index++) {
      final previous = trades[index - 1];
      final current = trades[index];
      if (previous.pnl >= 0) continue;
      var lossStreak = 0;
      for (var cursor = index - 1; cursor >= 0 && trades[cursor].pnl < 0; cursor--) {
        lossStreak++;
      }
      final previousOpened = previous.openedAtMillis;
      final currentOpened = current.openedAtMillis;
      final gap = previousOpened == null || currentOpened == null ||
              previousOpened <= 0 || currentOpened <= 0 || currentOpened < previousOpened
          ? null
          : ((currentOpened - previousOpened) / 60000).floor();
      final tags = current.tags.map((tag) => tag.toLowerCase()).toList();
      final emotion = current.emotion.toLowerCase();
      final evidence = analytics.revengeEvidence(
        gapMinutes: gap,
        previousWasLoss: true,
        previousRisk: previous.plannedRisk,
        currentRisk: current.plannedRisk,
        previousLot: previous.lot,
        currentLot: current.lot,
        planViolation: current.strategyFollowed == false,
        revengeTag: tags.any((tag) => tag.contains('revenge') || tag.contains('انتقام')),
        emotionalSignal: emotion.contains('anger') ||
            emotion.contains('angry') ||
            emotion.contains('خشم') ||
            emotion.contains('عصبان') ||
            emotion.contains('fomo') ||
            tags.any((tag) => tag.contains('fomo') || tag.contains('anger') || tag.contains('خشم')),
        priorLossStreak: lossStreak,
      );
      if (evidence.classificationCode == 0) continue;
      final level = switch (evidence.classificationCode) {
        4 => TxBehavioralEvidenceLevel.userTagged,
        3 => TxBehavioralEvidenceLevel.strong,
        2 => TxBehavioralEvidenceLevel.possible,
        _ => TxBehavioralEvidenceLevel.lossSequence,
      };
      final reasons = <String>[];
      if (evidence.rapidReentry) reasons.add('ورود مجدد سریع پس از معامله زیان‌ده');
      if (evidence.riskIncreased) reasons.add('افزایش ریسک بیش از ۲۵٪');
      if (evidence.sizeIncreased) reasons.add('افزایش حجم بیش از ۲۵٪');
      if (evidence.planViolation) reasons.add('ثبت صریح خارج از پلن');
      if (evidence.revengeTag) reasons.add('تگ Revenge دستی');
      if (evidence.emotionalSignal) reasons.add('ثبت خشم یا FOMO');
      if (evidence.lossSequence) reasons.add('حداقل دو زیان متوالی');
      final episodeId = '${account.accountId}:${previous.id}:${current.id}';
      final review = switch (account.episodeReviews[episodeId]) {
        'confirmed' => TxBehavioralReview.confirmed,
        'not_revenge' => TxBehavioralReview.notRevenge,
        'unsure' => TxBehavioralReview.unsure,
        _ => TxBehavioralReview.unreviewed,
      };
      episodes.add(TxBehavioralEpisode(
        id: episodeId,
        accountId: account.accountId,
        previousLossTradeId: previous.id,
        triggerTradeId: current.id,
        linkedTradeIds: [previous.id, current.id],
        title: 'الگوی رفتاری پس از زیان',
        evidenceLevel: level,
        evidenceScore: evidence.score,
        actualNetPnl: previous.pnl + current.pnl,
        reasons: reasons,
        elapsedMinutes: gap,
        review: review,
      ));
    }
    return episodes;
  }

  List<_TxSessionSlice> _sessionSlices(List<TxAiTradeRecord> trades) {
    final grouped = <String, List<TxAiTradeRecord>>{};
    for (final trade in trades) {
      final session = trade.session;
      if (session != null && session.isNotEmpty) {
        grouped.putIfAbsent(session, () => []).add(trade);
      }
    }
    final slices = grouped.entries.map((entry) {
      final values = entry.value;
      final average =
          values.fold<double>(0, (sum, trade) => sum + trade.pnl) / values.length;
      return _TxSessionSlice(entry.key, values.length, average);
    }).toList()
      ..sort((a, b) => b.average.compareTo(a.average));
    return slices;
  }

  double? _sessionEdge(List<_TxSessionSlice> sessions) {
    final qualified = sessions.where((session) => session.trades >= 5).toList();
    if (qualified.length < 2) return null;
    final spread = qualified.first.average - qualified.last.average;
    final scale = qualified
        .map((session) => session.average.abs())
        .fold<double>(0, (a, b) => math.max(a, b).toDouble());
    if (scale == 0) return 50;
    return (50 + math.atan(spread / scale) * 100 / math.pi)
        .clamp(0, 100)
        .toDouble();
  }

  double? _consistency(List<double> pnls, double mean) {
    if (pnls.length < 5) return null;
    final variance = pnls
            .map((value) => math.pow(value - mean, 2).toDouble())
            .fold<double>(0, (sum, value) => sum + value) /
        pnls.length;
    final deviation = math.sqrt(variance);
    final denominator = mean.abs() + deviation;
    if (denominator == 0) return 0;
    return (mean.abs() / denominator * 100).clamp(0, 100).toDouble();
  }

  List<List<double?>> _heatmap(List<TxAiTradeRecord> trades) {
    final totals = List.generate(7, (_) => List<double>.filled(24, 0));
    final counts = List.generate(7, (_) => List<int>.filled(24, 0));
    for (final trade in trades) {
      final weekday = trade.displayWeekday;
      final hour = trade.displayHour;
      if (weekday == null || hour == null || weekday < 1 || weekday > 7 || hour < 0 || hour > 23) continue;
      final day = weekday - 1;
      totals[day][hour] += trade.pnl;
      counts[day][hour]++;
    }
    return List.generate(7, (day) => List.generate(24, (hour) {
      if (counts[day][hour] == 0) return null;
      return totals[day][hour] / counts[day][hour];
    }));
  }

  Widget _heatmapCard(List<List<double?>> values, String timezone) => Card(
        color: const Color(0xFF0B1730),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('روز هفته × ساعت ورود ($timezone)',
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              TxHeatmap(cells: values),
              const Text(
                'هر خانه میانگین P&L معاملات دارای زمان ورود را نشان می‌دهد؛ خانه خالی یعنی داده‌ای ثبت نشده است.',
                style: TextStyle(color: Color(0xFF91A5C4), fontSize: 10),
              ),
            ],
          ),
        ),
      );

  List<TxProcessMonth> _processMonths(
    List<TxAiTradeRecord> trades,
    TxAiAccountSnapshot account,
  ) {
    final grouped = <String, List<TxAiTradeRecord>>{};
    for (final trade in trades) {
      final key = trade.displayClosedMonthKey;
      if (key == null || trade.closedAtMillis == null) continue;
      grouped.putIfAbsent(key, () => []).add(trade);
    }
    final keys = grouped.keys.toList()..sort();
    final recentKeys = keys.reversed.take(6).toList().reversed;
    return recentKeys.map((key) {
      final rows = grouped[key]!;
      final known = rows.where((trade) => trade.strategyFollowed != null).toList();
      final coverage = known.length / math.max(1, rows.length);
      final discipline = rows.length >= 5 && known.length >= 3 && coverage >= 0.7
          ? known.where((trade) => trade.strategyFollowed == true).length /
              known.length * 100
          : null;
      final riskRows = rows
          .where((trade) => trade.plannedRisk != null && trade.plannedRisk! > 0)
          .toList();
      final allowedRisk = account.startingBalance > 0
          ? account.startingBalance * account.riskPercent.clamp(0.1, 100) / 100
          : null;
      final risk = rows.length >= 5 &&
              riskRows.length >= 3 &&
              riskRows.length / rows.length >= 0.7 &&
              allowedRisk != null
          ? riskRows.where((trade) => trade.plannedRisk! <= allowedRisk).length /
              riskRows.length * 100
          : null;
      final completeDecisions = rows.where((trade) =>
          trade.entryPrice != null &&
          trade.exitPrice != null &&
          trade.stopLossPrice != null &&
          trade.takeProfitPrice != null &&
          trade.strategy.trim().isNotEmpty).length;
      final decision = rows.length >= 5 && completeDecisions / rows.length >= 0.7
          ? completeDecisions / rows.length * 100
          : null;
      final score = discipline != null && risk != null && decision != null
          ? ((discipline + risk + decision) / 3).round()
          : null;
      return TxProcessMonth(key, score);
    }).toList(growable: false);
  }

  List<TxInsight> _insights(
    TxAiAccountSnapshot account,
    TxRustAnalytics analytics,
    List<TxAiTradeRecord> weeklyTrades,
    List<TxAiTradeRecord> monthlyTrades,
    double? monthlyDiscipline,
  ) {
    final result = <TxInsight>[];
    if (weeklyTrades.length >= 5) {
      final weeklyMetrics = analytics.calculate(
        weeklyTrades.map((trade) => trade.pnl).toList(growable: false),
      );
      const winId = 'summary-win-rate';
      result.add(TxInsight(
        id: winId,
        period: TxInsightPeriod.weekly,
        title: 'خلاصه عملکرد هفت روز اخیر',
        reason: 'فقط معاملات دارای تاریخ ورود در هفت روز اخیر و P&L معتبر حساب انتخاب‌شده.',
        metricLabel: 'Win rate',
        metricValue: '${(weeklyMetrics.winRate * 100).toStringAsFixed(1)}٪',
        action: 'معاملات پشتیبان را مرور کن و نتیجه را با هفته‌های دیگر مقایسه کن.',
        supportingTradeIds: weeklyTrades.take(20).map((trade) => trade.id).toList(growable: false),
        pinned: account.pinnedInsights.contains(winId),
      ));
    }
    if (monthlyDiscipline != null) {
      const disciplineId = 'summary-plan-adherence';
      result.add(TxInsight(
        id: disciplineId,
        period: TxInsightPeriod.monthly,
        title: 'پایبندی ثبت‌شده به پلن در ماه جاری',
        reason: 'فقط معاملات بسته‌شده ماه جاری با وضعیت صریح طبق پلن / خارج از پلن.',
        metricLabel: 'Plan adherence',
        metricValue: '${monthlyDiscipline.toStringAsFixed(0)}٪',
        action: 'وضعیت معاملات بدون ثبت پلن را کامل کن.',
        supportingTradeIds: monthlyTrades
            .where((trade) => trade.strategyFollowed != null)
            .take(20)
            .map((trade) => trade.id)
            .toList(growable: false),
        pinned: account.pinnedInsights.contains(disciplineId),
      ));
    }
    return result;
  }

  bool _isOffPlan(TxAiTradeRecord trade) {
    if (trade.strategyFollowed == false) return true;
    return trade.tags.map((tag) => tag.toLowerCase()).any((tag) =>
        tag.contains('fomo') ||
        tag.contains('revenge') ||
        tag.contains('no-plan') ||
        tag.contains('off-strategy') ||
        tag.contains('بدون پلن') ||
        tag.contains('خارج از استراتژی'));
  }
}

void _showLinkedTrades(
  BuildContext context,
  List<TxAiTradeRecord> trades,
  List<int> tradeIds,
  String currency,
) {
  final selected = trades.where((trade) => tradeIds.contains(trade.id)).toList(growable: false);
  showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: const Color(0xFF0B1730),
    builder: (context) => SafeArea(
      child: DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.55,
        maxChildSize: 0.9,
        builder: (context, controller) => ListView(
          controller: controller,
          padding: const EdgeInsets.all(16),
          children: [
            const Text('معاملات پشتیبان', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900)),
            const SizedBox(height: 8),
            if (selected.isEmpty)
              const Text('معامله مرتبطی در snapshot فعلی پیدا نشد.'),
            for (final trade in selected)
              Card(
                color: const Color(0xFF101F3B),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('#${trade.id} · ${trade.symbol} · ${trade.isBuy ? "BUY" : "SELL"}',
                          style: const TextStyle(fontWeight: FontWeight.w900)),
                      const SizedBox(height: 4),
                      Text('P&L: ${trade.pnl.toStringAsFixed(2)} $currency · Lot: ${trade.lot}'),
                      Text('استراتژی: ${trade.strategy.isEmpty ? "ثبت نشده" : trade.strategy} · نسخه ${trade.strategyVersion}'),
                      Text('سشن: ${trade.session ?? "نامشخص"} · TF: ${trade.timeframe ?? "ثبت نشده"}'),
                      Text('ریسک برنامه‌ریزی‌شده: ${trade.plannedRisk?.toStringAsFixed(2) ?? "ثبت نشده"}'),
                      if (trade.emotion.isNotEmpty) Text('حالت ذهنی: ${trade.emotion}'),
                      if (trade.tags.isNotEmpty) Text('تگ‌ها: ${trade.tags.join("، ")}'),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    ),
  );
}

class _TxSessionSlice {
  const _TxSessionSlice(this.label, this.trades, this.average);
  final String label;
  final int trades;
  final double average;
}

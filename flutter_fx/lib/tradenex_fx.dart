import 'dart:math' as math;
import 'package:flutter/material.dart';

export 'gold_fx.dart';

/// Spinning dotted globe with pulsing city markers.
class GlobeFx extends StatefulWidget {
  const GlobeFx({super.key, this.openSessions = const {2}, this.gold = false});

  /// Gold theme look: dark amber ocean, golden land dots, city-light sparkle on the night side.
  final bool gold;

  /// Indexes of open sessions: 0 Sydney, 1 Tokyo, 2 London, 3 New York.
  final Set<int> openSessions;

  @override
  State<GlobeFx> createState() => _GlobeFxState();
}

class _GlobeFxState extends State<GlobeFx> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(seconds: 40))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) => CustomPaint(
        painter: _GlobePainter(_c.value, widget.openSessions, widget.gold),
        child: const SizedBox.expand(),
      ),
    );
  }
}

class _City {
  const _City(this.lat, this.lon, this.color, this.index);
  final double lat, lon;
  final Color color;
  final int index;
}

const _cities = [
  _City(-33.9, 151.2, Color(0xFF4DA3FF), 0),
  _City(35.7, 139.7, Color(0xFFB58CFF), 1),
  _City(51.5, -0.1, Color(0xFFF7CF6B), 2),
  _City(40.7, -74.0, Color(0xFFFF6B7A), 3),
];

class _GlobePainter extends CustomPainter {
  _GlobePainter(this.t, this.open, this.gold);
  final double t;
  final Set<int> open;
  final bool gold;

  static const double _tilt = 0.35;

  Offset? _project(double latDeg, double lonDeg, double r, Offset c) {
    final lat = latDeg * math.pi / 180;
    final lon = (lonDeg - t * 360 + 20) * math.pi / 180;
    final x = math.cos(lat) * math.sin(lon);
    final y0 = math.sin(lat);
    final z0 = math.cos(lat) * math.cos(lon);
    final y = y0 * math.cos(_tilt) - z0 * math.sin(_tilt);
    final z = y0 * math.sin(_tilt) + z0 * math.cos(_tilt);
    if (z <= 0) return null;
    return Offset(c.dx + x * r, c.dy - y * r);
  }

  @override
  void paint(Canvas canvas, Size size) {
    final r = size.shortestSide * 0.42;
    final c = size.center(Offset.zero);
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..shader = RadialGradient(
          colors: gold
              ? const [Color(0xFF2B1F0D), Color(0xFF120D07), Color(0xFF050403)]
              : const [Color(0xFF1D5FBF), Color(0xFF0C2C66), Color(0xFF040C22)],
        ).createShader(Rect.fromCircle(center: c, radius: r)),
    );
    final grid = Paint()
      ..color = gold ? const Color(0x29F2B53D) : const Color(0x337FB8FF)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8;
    for (var lat = -60; lat <= 60; lat += 30) {
      final path = Path();
      var started = false;
      for (var lon = -180; lon <= 180; lon += 6) {
        final p = _project(lat.toDouble(), lon.toDouble(), r, c);
        if (p == null) {
          started = false;
          continue;
        }
        started ? path.lineTo(p.dx, p.dy) : path.moveTo(p.dx, p.dy);
        started = true;
      }
      canvas.drawPath(path, grid);
    }
    final dot = Paint()..color = gold ? const Color(0xCCFFD884) : const Color(0xAA7FE3D0);
    for (var lat = -60; lat <= 70; lat += 5) {
      for (var lon = -180; lon < 180; lon += 5) {
        if (!_isLand(lat.toDouble(), lon.toDouble())) continue;
        final p = _project(lat.toDouble(), lon.toDouble(), r, c);
        if (p != null) canvas.drawCircle(p, 1.3, dot);
      }
    }
    final pulse = (t * 22) % 1;
    for (final city in _cities) {
      final p = _project(city.lat, city.lon, r, c);
      if (p == null) continue;
      final isOpen = open.contains(city.index);
      if (isOpen) {
        canvas.drawCircle(
          p,
          6 + 16 * pulse,
          Paint()
            ..color = city.color.withOpacity(0.6 * (1 - pulse))
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.5,
        );
      }
      canvas.drawCircle(p, isOpen ? 5 : 3.5, Paint()..color = city.color.withOpacity(isOpen ? 1 : 0.6));
    }
    canvas.drawCircle(
      c,
      r + 3,
      Paint()
        ..color = gold ? const Color(0x66F2B53D) : const Color(0x554DA3FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6),
    );
  }

  // Very coarse land boxes (lat/lon): enough for a stylised dotted globe.
  bool _isLand(double lat, double lon) {
    const boxes = [
      [15, 70, -165, -55], // North America
      [-55, 12, -80, -35], // South America
      [36, 70, -10, 40], // Europe
      [-35, 35, -17, 50], // Africa
      [10, 70, 40, 145], // Asia
      [-38, -12, 114, 153], // Australia
    ];
    for (final b in boxes) {
      if (lat >= b[0] && lat <= b[1] && lon >= b[2] && lon <= b[3]) return true;
    }
    return false;
  }

  @override
  bool shouldRepaint(covariant _GlobePainter old) => old.t != t || old.open != open || old.gold != gold;
}

/// Number that counts up to [value] with a glow.
class GlowNumber extends StatelessWidget {
  const GlowNumber({super.key, required this.value, this.suffix = '', this.color = const Color(0xFF2BE8A8)});
  final double value;
  final String suffix;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: value),
      duration: const Duration(milliseconds: 1200),
      curve: Curves.easeOutCubic,
      builder: (_, v, __) => Text(
        '${v.toStringAsFixed(2)}$suffix',
        style: TextStyle(
          fontSize: 44,
          fontWeight: FontWeight.w800,
          color: color,
          shadows: [Shadow(color: color.withOpacity(0.5), blurRadius: 22)],
        ),
      ),
    );
  }
}

/// Six-axis TX AI radar. Values are normalized 0..100 and never inferred by the widget.
class TxRadar extends StatelessWidget {
  const TxRadar({
    super.key,
    required this.values,
    this.labels = const ['Win Rate', 'Profit Factor', 'Expectancy', 'Consistency', 'Discipline', 'Session Edge'],
    this.onAxisTap,
  }) : assert(values.length == 6), assert(labels.length == 6);

  final List<double?> values;
  final List<String> labels;
  final ValueChanged<int>? onAxisTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 300,
      child: LayoutBuilder(builder: (context, constraints) {
        final side = math.min(constraints.maxWidth, 300.0).toDouble();
        return Center(
          child: SizedBox(
            width: side,
            height: side,
            child: Stack(
              children: [
                Positioned.fill(child: CustomPaint(painter: _TxRadarPainter(values))),
                for (var i = 0; i < 6; i++)
                  _radarLabel(i, side, labels[i], values[i], onAxisTap),
              ],
            ),
          ),
        );
      }),
    );
  }

  Widget _radarLabel(int index, double side, String label, double? value, ValueChanged<int>? onTap) {
    final angle = -math.pi / 2 + index * math.pi / 3;
    final center = side / 2;
    final radius = side * .43;
    final x = center + math.cos(angle) * radius;
    final y = center + math.sin(angle) * radius;
    return Positioned(
      left: (x - 43).clamp(0.0, side - 86).toDouble(),
      top: (y - 18).clamp(0.0, side - 36).toDouble(),
      width: 86,
      height: 36,
      child: GestureDetector(
        onTap: onTap == null ? null : () => onTap(index),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: const Color(0xDD0A1730),
            borderRadius: BorderRadius.circular(9),
            border: Border.all(color: const Color(0x334CCBFF)),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(label, maxLines: 1, overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 9)),
              Text(value == null ? '—' : value.toStringAsFixed(0),
                style: const TextStyle(color: Color(0xFF55D9F5), fontWeight: FontWeight.w800, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }
}

class _TxRadarPainter extends CustomPainter {
  _TxRadarPainter(this.values);
  final List<double?> values;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final radius = size.shortestSide * .29;
    Offset point(int i, double r) {
      final a = -math.pi / 2 + i * math.pi / 3;
      return c + Offset(math.cos(a) * r, math.sin(a) * r);
    }
    final grid = Paint()..color = const Color(0x335A8AB8)..style = PaintingStyle.stroke..strokeWidth = 1;
    for (final level in [.25, .5, .75, 1.0]) {
      final path = Path();
      for (var i = 0; i < 6; i++) {
        final p = point(i, radius * level);
        i == 0 ? path.moveTo(p.dx, p.dy) : path.lineTo(p.dx, p.dy);
      }
      path.close();
      canvas.drawPath(path, grid);
    }
    for (var i = 0; i < 6; i++) {
      canvas.drawLine(c, point(i, radius), grid);
    }
    if (values.every((v) => v == null)) return;
    final shape = Path();
    for (var i = 0; i < 6; i++) {
      final p = point(i, radius * ((values[i] ?? 0).clamp(0, 100).toDouble() / 100));
      i == 0 ? shape.moveTo(p.dx, p.dy) : shape.lineTo(p.dx, p.dy);
    }
    shape.close();
    canvas.drawPath(shape, Paint()..color = const Color(0x3355D9F5)..style = PaintingStyle.fill);
    canvas.drawPath(shape, Paint()..color = const Color(0xFF55D9F5)..style = PaintingStyle.stroke..strokeWidth = 2.2);
    for (var i = 0; i < 6; i++) {
      final v = values[i];
      if (v == null) continue;
      final p = point(i, radius * (v.clamp(0, 100).toDouble() / 100));
      canvas.drawCircle(p, 3.5, Paint()..color = const Color(0xFFFFD36E));
    }
  }

  @override
  bool shouldRepaint(covariant _TxRadarPainter old) => old.values != values;
}

/// Evidence-first insight card. The caller supplies the metric and source-backed explanation.
class TxInsightCard extends StatelessWidget {
  const TxInsightCard({
    super.key, required this.title, required this.description, required this.metric,
    this.actionLabel = 'Deep Dive', this.onAction, this.isPinned = false, this.onPin,
    this.severity = TxInsightSeverity.info,
  });
  final String title, description, metric, actionLabel;
  final VoidCallback? onAction, onPin;
  final bool isPinned;
  final TxInsightSeverity severity;

  @override
  Widget build(BuildContext context) {
    final accent = switch (severity) {
      TxInsightSeverity.positive => const Color(0xFF36D6A0),
      TxInsightSeverity.warning => const Color(0xFFFF7B83),
      TxInsightSeverity.info => const Color(0xFF55D9F5),
    };
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: const Color(0xFF0B1730),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: accent.withOpacity(.35)),
        boxShadow: [BoxShadow(color: accent.withOpacity(.08), blurRadius: 22)],
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(width: 7, height: 7, decoration: BoxDecoration(color: accent, shape: BoxShape.circle)),
          const SizedBox(width: 8),
          Expanded(child: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 14))),
          IconButton(onPressed: onPin, icon: Icon(isPinned ? Icons.push_pin : Icons.push_pin_outlined, color: isPinned ? const Color(0xFFFFD36E) : Colors.white54, size: 18)),
        ]),
        Text(metric, style: TextStyle(color: accent, fontSize: 22, fontWeight: FontWeight.w900)),
        const SizedBox(height: 6),
        Text(description, style: const TextStyle(color: Color(0xFFB7C9E8), height: 1.5, fontSize: 12)),
        if (onAction != null) Align(
          alignment: AlignmentDirectional.centerEnd,
          child: TextButton(onPressed: onAction, child: Text(actionLabel, style: TextStyle(color: accent))),
        ),
      ]),
    );
  }
}

enum TxInsightSeverity { positive, warning, info }

class TxBehavioralBadge extends StatelessWidget {
  const TxBehavioralBadge({super.key, required this.label, required this.evidenceScore, this.confirmedByUser = false});
  final String label;
  final int evidenceScore;
  final bool confirmedByUser;

  @override
  Widget build(BuildContext context) {
    final color = confirmedByUser ? const Color(0xFFFFD36E)
        : evidenceScore >= 45 ? const Color(0xFFFF7B83) : const Color(0xFF55D9F5);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(color: color.withOpacity(.10), borderRadius: BorderRadius.circular(30), border: Border.all(color: color.withOpacity(.45))),
      child: Text('$label · ${confirmedByUser ? "تأیید کاربر" : "شواهد $evidenceScore/100"}',
        style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.w700)),
    );
  }
}

/// Interactive 7 x 24 heatmap. Null cells are unavailable, not zero-result trades.
class TxHeatmap extends StatelessWidget {
  const TxHeatmap({super.key, required this.cells, this.onCellTap});
  final List<List<double?>> cells;
  final void Function(int weekday, int hour)? onCellTap;

  @override
  Widget build(BuildContext context) {
    assert(cells.length == 7 && cells.every((r) => r.length == 24));
    return Column(children: [
      for (var day = 0; day < 7; day++)
        Row(children: [
          SizedBox(width: 25, child: Text(['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'][day], style: const TextStyle(color: Colors.white70, fontSize: 10))),
          for (var hour = 0; hour < 24; hour++)
            Expanded(child: GestureDetector(
              onTap: onCellTap == null ? null : () => onCellTap!(day, hour),
              child: Container(
                height: 18, margin: const EdgeInsets.all(1),
                decoration: BoxDecoration(
                  color: _heatColor(cells[day][hour]),
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
            )),
        ]),
    ]);
  }

  Color _heatColor(double? value) {
    if (value == null) return const Color(0xFF18243A);
    final intensity = (value.abs() / 100).clamp(0.08, .9).toDouble();
    return value >= 0
        ? Color.lerp(const Color(0xFF123C4B), const Color(0xFF28D6B0), intensity)!
        : Color.lerp(const Color(0xFF4A2637), const Color(0xFFFF6678), intensity)!;
  }
}

/// Scenario control only emits a parameter; scenario P&L must be computed by the engine.
class TxCounterfactualSlider extends StatelessWidget {
  const TxCounterfactualSlider({super.key, required this.value, required this.onChanged, this.label = 'Risk multiplier'});
  final double value;
  final ValueChanged<double> onChanged;
  final String label;

  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Row(children: [
      Expanded(child: Text(label, style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 12))),
      Text('${(value * 100).round()}%', style: const TextStyle(color: Color(0xFFFFD36E), fontWeight: FontWeight.w800)),
    ]),
    Slider(value: value.clamp(.1, 2).toDouble(), min: .1, max: 2, divisions: 19, onChanged: onChanged,
      activeColor: const Color(0xFF55D9F5), inactiveColor: const Color(0xFF263A58)),
  ]);
}


class TxProcessTimeline extends StatelessWidget {
  const TxProcessTimeline({super.key, required this.months});
  final List<TxProcessMonth> months;

  @override
  Widget build(BuildContext context) {
    if (months.isEmpty) {
      return const Text('هنوز داده ماهانه کافی ثبت نشده است.', style: TextStyle(color: Color(0xFF91A5C4), fontSize: 12));
    }
    return Column(children: [
      for (final month in months)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 7),
          child: Row(children: [
            SizedBox(width: 64, child: Text(month.label, style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 10))),
            Expanded(child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                minHeight: 8,
                value: month.score == null ? 0.0 : month.score!.clamp(0, 100).toDouble() / 100,
                backgroundColor: const Color(0xFF1B2A43),
                valueColor: AlwaysStoppedAnimation(month.score == null ? const Color(0xFF52627A) : const Color(0xFFFFD36E)),
              ),
            )),
            SizedBox(width: 38, child: Text(month.score?.toString() ?? '—', textAlign: TextAlign.end,
              style: const TextStyle(color: Color(0xFFFFD36E), fontWeight: FontWeight.w800, fontSize: 12))),
          ]),
        ),
    ]);
  }
}

class TxProcessMonth {
  const TxProcessMonth(this.label, this.score);
  final String label;
  /// Null means insufficient evidence, not zero.
  final int? score;
}


/// Immutable presentation contract. Values must come from the local Rust engine
/// or the existing journal store; this package deliberately contains no sample trades.
class TxAiSnapshot {
  const TxAiSnapshot({
    required this.accountLabel,
    required this.tradeCount,
    required this.netPnl,
    required this.winRate,
    required this.profitFactor,
    required this.expectancy,
    required this.consistency,
    required this.discipline,
    required this.sessionEdge,
    required this.radarValues,
    required this.processScore,
    required this.tiltEvidence,
    required this.bestSession,
    required this.worstSession,
    required this.narratives,
    this.currency = '',
  });
  final String accountLabel;
  final int tradeCount;
  final double? netPnl;
  final double? winRate, profitFactor, expectancy, consistency, discipline, sessionEdge;
  /// Six normalized 0..100 dimensions produced by the analytics engine; null means unavailable.
  final List<double?> radarValues;
  final int? processScore;
  /// Evidence index, not a probability.
  final int? tiltEvidence;
  final String? bestSession, worstSession;
  final List<String> narratives;
  final String currency;
}

class TxAiCommandCenter extends StatelessWidget {
  const TxAiCommandCenter({super.key, required this.snapshot, this.onAxisTap, this.onBehavioralLab});
  final TxAiSnapshot snapshot;
  final ValueChanged<int>? onAxisTap;
  final VoidCallback? onBehavioralLab;

  @override
  Widget build(BuildContext context) {
    final positive = snapshot.netPnl != null && snapshot.netPnl! >= 0;
    final edgeScore = _edgeScore(snapshot.radarValues);
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Container(
        color: const Color(0xFF060D1D),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Row(children: [
              const Expanded(child: Text('TX AI · مرکز فرماندهی', style: TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w900))),
              Text(snapshot.tradeCount.toString() + ' معامله', style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 11)),
            ]),
            const SizedBox(height: 4),
            Text(snapshot.accountLabel, style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 11)),
            const SizedBox(height: 14),
            Row(children: [
              Expanded(child: _metricPanel('EDGE SCORE', edgeScore == null ? '—' : edgeScore.toString(), const Color(0xFF55D9F5))),
              const SizedBox(width: 8),
              Expanded(child: _metricPanel('PROCESS SCORE', snapshot.processScore?.toString() ?? '—', const Color(0xFFFFD36E))),
              const SizedBox(width: 8),
              Expanded(child: _metricPanel('TILT EVIDENCE', snapshot.tiltEvidence?.toString() ?? '—', const Color(0xFFFF7B83))),
            ]),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: const Color(0xFF0B1730), borderRadius: BorderRadius.circular(20), border: Border.all(color: const Color(0x224CCBFF))),
              child: Column(children: [
                const Text('Performance Radar', style: TextStyle(color: Color(0xFFB7C9E8), fontSize: 11, fontWeight: FontWeight.w700)),
                TxRadar(values: snapshot.radarValues.length == 6 ? snapshot.radarValues : List<double?>.filled(6, null), onAxisTap: onAxisTap),
              ]),
            ),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(child: _metricPanel('NET P&L', snapshot.netPnl == null ? '—' : '${snapshot.netPnl!.toStringAsFixed(2)} ${snapshot.currency}', positive ? const Color(0xFF36D6A0) : const Color(0xFFFF7B83))),
              const SizedBox(width: 8),
              Expanded(child: _metricPanel('WIN RATE', snapshot.winRate == null ? '—' : '${(snapshot.winRate! * 100).toStringAsFixed(1)}%', const Color(0xFFFFD36E))),
            ]),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(child: _metricPanel('BEST SESSION', snapshot.bestSession ?? 'داده ناکافی', const Color(0xFF36D6A0))),
              const SizedBox(width: 8),
              Expanded(child: _metricPanel('WEAKEST SESSION', snapshot.worstSession ?? 'داده ناکافی', const Color(0xFFFF7B83))),
            ]),
            if (snapshot.tiltEvidence != null && snapshot.tiltEvidence! >= 45) ...[
              const SizedBox(height: 12),
              TxInsightCard(
                title: 'نشانه‌های رفتاری نیازمند مرور',
                metric: 'Evidence ${snapshot.tiltEvidence}/100',
                description: 'این شاخص خلاصه شواهد ثبت‌شده است، نه احتمال روان‌شناختی یا تشخیص قطعی.',
                severity: TxInsightSeverity.warning,
                actionLabel: 'مشاهده Behavioral Lab',
                onAction: onBehavioralLab,
              ),
            ],
            const SizedBox(height: 12),
            for (var i = 0; i < snapshot.narratives.length; i++)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: TxInsightCard(
                  title: 'بینش ${i + 1}',
                  metric: '',
                  description: snapshot.narratives[i],
                  severity: TxInsightSeverity.info,
                ),
              ),
            const SizedBox(height: 16),
            const Text('شاخص‌ها فقط از داده‌های موجود حساب محاسبه می‌شوند؛ مقدار «—» یعنی داده کافی نیست.', style: TextStyle(color: Color(0xFF7185A5), fontSize: 10)),
          ],
        ),
      ),
    );
  }

  static int? _edgeScore(List<double?> values) {
    if (values.length != 6) return null;
    final available = values.whereType<double>().where((v) => v.isFinite).toList();
    if (available.length < 4) return null;
    return (available.map((v) => v.clamp(0, 100)).reduce((a, b) => a + b) / available.length).round();
  }
  Widget _metricPanel(String label, String value, Color accent) => Container(
    padding: const EdgeInsets.all(11),
    decoration: BoxDecoration(color: const Color(0xFF0B1730), borderRadius: BorderRadius.circular(14), border: Border.all(color: accent.withOpacity(.2))),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 9, fontWeight: FontWeight.w700)),
      const SizedBox(height: 5),
      Text(value, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(color: accent, fontSize: 15, fontWeight: FontWeight.w900)),
    ]),
  );
}


class TxEdgeTrade {
  const TxEdgeTrade({
    required this.id, required this.netPnl, this.session, this.weekday,
    this.strategy, this.timeframe, this.marketCondition, this.emotion,
    this.riskAmount, this.rewardAmount,
  });
  final String id;
  final double netPnl;
  final String? session, strategy, timeframe, marketCondition, emotion;
  final int? weekday;
  final double? riskAmount, rewardAmount;
}

/// Interactive local Edge Explorer. The host supplies trades already scoped to one account.
class TxEdgeExplorer extends StatefulWidget {
  const TxEdgeExplorer({super.key, required this.trades, this.onTradeTap});
  final List<TxEdgeTrade> trades;
  final ValueChanged<String>? onTradeTap;

  @override
  State<TxEdgeExplorer> createState() => _TxEdgeExplorerState();
}

class _TxEdgeExplorerState extends State<TxEdgeExplorer> {
  String? session, strategy, timeframe, condition, emotion;

  List<String> _options(String? Function(TxEdgeTrade) pick) => widget.trades
      .map(pick).whereType<String>().where((v) => v.trim().isNotEmpty).toSet().toList()..sort();

  @override
  Widget build(BuildContext context) {
    final filtered = widget.trades.where((t) =>
      (session == null || t.session == session) &&
      (strategy == null || t.strategy == strategy) &&
      (timeframe == null || t.timeframe == timeframe) &&
      (condition == null || t.marketCondition == condition) &&
      (emotion == null || t.emotion == emotion)
    ).toList();
    final validScatter = filtered.where((t) =>
      t.riskAmount != null && t.rewardAmount != null &&
      t.riskAmount! > 0 && t.riskAmount!.isFinite && t.rewardAmount!.isFinite
    ).toList();
    final net = filtered.fold<double>(0, (sum, t) => sum + t.netPnl);
    return Directionality(
      textDirection: TextDirection.rtl,
      child: ListView(padding: const EdgeInsets.all(16), children: [
        const Text('Edge Explorer · کاوشگر لبه', style: TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w900)),
        const SizedBox(height: 5),
        Text('${filtered.length} از ${widget.trades.length} معامله · داده حساب محلی', style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 11)),
        const SizedBox(height: 12),
        Wrap(spacing: 8, runSpacing: 8, children: [
          _filter('سشن', session, _options((t) => t.session), (v) => setState(() => session = v)),
          _filter('استراتژی', strategy, _options((t) => t.strategy), (v) => setState(() => strategy = v)),
          _filter('تایم‌فریم', timeframe, _options((t) => t.timeframe), (v) => setState(() => timeframe = v)),
          _filter('شرایط بازار', condition, _options((t) => t.marketCondition), (v) => setState(() => condition = v)),
          _filter('حالت ذهنی', emotion, _options((t) => t.emotion), (v) => setState(() => emotion = v)),
          ActionChip(label: const Text('پاک‌کردن فیلترها'), onPressed: () => setState(() {
            session = strategy = timeframe = condition = emotion = null;
          })),
        ]),
        const SizedBox(height: 12),
        Row(children: [
          Expanded(child: _summary('خالص فیلتر', net.toStringAsFixed(2), net >= 0 ? const Color(0xFF36D6A0) : const Color(0xFFFF7B83))),
          const SizedBox(width: 8),
          Expanded(child: _summary('Scatter معتبر', validScatter.length.toString(), const Color(0xFF55D9F5))),
          const SizedBox(width: 8),
          Expanded(child: _summary('ریسک ثبت‌نشده', (filtered.length - validScatter.length).toString(), const Color(0xFFFFD36E))),
        ]),
        const SizedBox(height: 14),
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: const Color(0xFF0B1730), borderRadius: BorderRadius.circular(16)),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Risk vs Reward · فقط داده واقعی ثبت‌شده', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 12)),
            const SizedBox(height: 8),
            SizedBox(height: 220, child: validScatter.isEmpty
              ? const Center(child: Text('برای رسم نمودار، ریسک و نتیجه هر دو باید ثبت شده باشند.', style: TextStyle(color: Color(0xFF91A5C4), fontSize: 11)))
              : CustomPaint(size: Size.infinite, painter: _TxScatterPainter(validScatter))),
          ]),
        ),
        const SizedBox(height: 14),
        const Text('معاملات در فیلتر فعلی', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w800)),
        for (final trade in filtered.take(100))
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            title: Text(trade.strategy ?? 'بدون استراتژی', style: const TextStyle(color: Color(0xFFEAF2FF), fontSize: 12)),
            subtitle: Text('${trade.session ?? "سشن نامشخص"} · ${trade.timeframe ?? "TF نامشخص"} · #${trade.id}', style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 10)),
            trailing: Text(trade.netPnl.toStringAsFixed(2), style: TextStyle(color: trade.netPnl >= 0 ? const Color(0xFF36D6A0) : const Color(0xFFFF7B83), fontWeight: FontWeight.w800)),
            onTap: widget.onTradeTap == null ? null : () => widget.onTradeTap!(trade.id),
          ),
      ]),
    );
  }

  Widget _filter(String label, String? value, List<String> options, ValueChanged<String?> onChanged) {
    return SizedBox(width: 155, child: DropdownButtonFormField<String?>(
      value: value,
      isExpanded: true,
      dropdownColor: const Color(0xFF0B1730),
      decoration: InputDecoration(
        labelText: label, labelStyle: const TextStyle(color: Color(0xFF91A5C4), fontSize: 11),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: Color(0x334CCBFF))),
        contentPadding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      ),
      style: const TextStyle(color: Colors.white, fontSize: 11),
      items: [const DropdownMenuItem<String?>(value: null, child: Text('همه')),...options.map((v) => DropdownMenuItem<String?>(value: v, child: Text(v, overflow: TextOverflow.ellipsis)))],
      onChanged: onChanged,
    ));
  }

  Widget _summary(String label, String value, Color color) => Container(
    padding: const EdgeInsets.all(10),
    decoration: BoxDecoration(color: const Color(0xFF0B1730), borderRadius: BorderRadius.circular(12)),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(label, style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 9)),
      const SizedBox(height: 4),
      Text(value, style: TextStyle(color: color, fontWeight: FontWeight.w900, fontSize: 13)),
    ]),
  );
}

class _TxScatterPainter extends CustomPainter {
  _TxScatterPainter(this.trades);
  final List<TxEdgeTrade> trades;
  @override
  void paint(Canvas canvas, Size size) {
    const pad = 18.0;
    final rect = Rect.fromLTWH(pad, pad, size.width - pad * 2, size.height - pad * 2);
    final maxX = trades.map((t) => t.riskAmount!).reduce((a, b) => a > b ? a : b).clamp(1, double.infinity).toDouble();
    final minX = trades.map((t) => t.riskAmount!).reduce((a, b) => a < b ? a : b);
    final maxY = trades.map((t) => t.rewardAmount!).reduce((a, b) => a > b ? a : b).clamp(1, double.infinity).toDouble();
    final minY = trades.map((t) => t.rewardAmount!).reduce((a, b) => a < b ? a : b);
    final spanX = (maxX - minX).abs() < .000001 ? 1.0 : maxX - minX;
    final spanY = (maxY - minY).abs() < .000001 ? 1.0 : maxY - minY;
    final axis = Paint()..color = const Color(0x557A91B5)..strokeWidth = 1;
    canvas.drawLine(Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom), axis);
    canvas.drawLine(Offset(rect.left, rect.top), Offset(rect.left, rect.bottom), axis);
    for (final t in trades) {
      final x = rect.left + ((t.riskAmount! - minX) / spanX) * rect.width;
      final y = rect.bottom - ((t.rewardAmount! - minY) / spanY) * rect.height;
      canvas.drawCircle(Offset(x, y), 4, Paint()..color = t.netPnl >= 0 ? const Color(0xFF36D6A0) : const Color(0xFFFF7B83));
    }
  }
  @override
  bool shouldRepaint(covariant _TxScatterPainter old) => old.trades != trades;
}


enum TxRevengeClassification { userTagged, strongEvidence, possible, lossSequenceOnly }

enum TxRevengeReview { unreviewed, confirmed, notRevenge, unsure }

/// One account-scoped behavioral episode supplied by the host's local journal engine.
/// P&L fields must be realized values; null means unavailable, never zero.
class TxRevengeEpisode {
  const TxRevengeEpisode({
    required this.id,
    required this.previousTradeId,
    required this.triggerTradeId,
    required this.previousTradeLabel,
    required this.triggerTradeLabel,
    required this.evidenceScore,
    required this.classification,
    required this.reasons,
    this.gapMinutes,
    this.previousRealizedPnl,
    this.triggerRealizedPnl,
    this.review = TxRevengeReview.unreviewed,
  });

  final String id;
  final String previousTradeId;
  final String triggerTradeId;
  final String previousTradeLabel;
  final String triggerTradeLabel;
  final int evidenceScore;
  final TxRevengeClassification classification;
  final List<String> reasons;
  final int? gapMinutes;
  final double? previousRealizedPnl;
  final double? triggerRealizedPnl;

  final TxRevengeReview review;

  double? get realizedEpisodeNet {
    if (previousRealizedPnl == null || triggerRealizedPnl == null) return null;
    return previousRealizedPnl! + triggerRealizedPnl!;
  }
}

/// Evidence-first Revenge Trading card. Review persistence and account scoping
/// remain host responsibilities; this widget never writes journal data itself.
class TxRevengeEpisodeCard extends StatelessWidget {
  const TxRevengeEpisodeCard({
    super.key,
    required this.episode,
    this.onShowTrades,
    this.onReview,
  });

  final TxRevengeEpisode episode;
  final ValueChanged<TxRevengeReview>? onReview;
  final VoidCallback? onShowTrades;

  @override
  Widget build(BuildContext context) {
    final accent = switch (episode.classification) {
      TxRevengeClassification.userTagged => const Color(0xFFFFD36E),
      TxRevengeClassification.strongEvidence => const Color(0xFFFF7B83),
      TxRevengeClassification.possible => const Color(0xFF55D9F5),
      TxRevengeClassification.lossSequenceOnly => const Color(0xFF91A5C4),
    };
    final net = episode.realizedEpisodeNet;
    final reviewLabel = switch (episode.review) {
      TxRevengeReview.unreviewed => 'نیازمند مرور',
      TxRevengeReview.confirmed => 'تأیید کاربر',
      TxRevengeReview.notRevenge => 'Revenge نیست',
      TxRevengeReview.unsure => 'نامشخص',
    };

    return Directionality(
      textDirection: TextDirection.rtl,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: const Color(0xFF0B1730),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: accent.withOpacity(.38)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Expanded(child: Text(
                switch (episode.classification) {
                  TxRevengeClassification.userTagged => 'با تگ دستی کاربر',
                  TxRevengeClassification.strongEvidence => 'شواهد رفتاری قوی',
                  TxRevengeClassification.possible => 'الگوی احتمالی',
                  TxRevengeClassification.lossSequenceOnly => 'توالی زیان',
                },
                style: TextStyle(color: accent, fontWeight: FontWeight.w800, fontSize: 13),
              )),
              Text('Evidence ${episode.evidenceScore.clamp(0, 100)}/100',
                style: TextStyle(color: accent, fontWeight: FontWeight.w900, fontSize: 11)),
            ]),
            const SizedBox(height: 8),
            _tradeLine('معامله زیان‌ده قبلی', episode.previousTradeLabel, episode.previousTradeId),
            const SizedBox(height: 5),
            _tradeLine('ورود بعدی', episode.triggerTradeLabel, episode.triggerTradeId),
            const SizedBox(height: 8),
            Row(children: [
              Expanded(child: Text(
                episode.gapMinutes == null ? 'فاصله زمانی: ثبت نشده' : 'فاصله زمانی: ${episode.gapMinutes} دقیقه',
                style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 10),
              )),
              Text(
                net == null ? 'خالص: داده ناکافی' : 'خالص دو معامله: ${net.toStringAsFixed(2)}',
                style: TextStyle(
                  color: net == null ? const Color(0xFF91A5C4) : net >= 0 ? const Color(0xFF36D6A0) : const Color(0xFFFF7B83),
                  fontWeight: FontWeight.w800,
                  fontSize: 10,
                ),
              ),
            ]),
            if (episode.reasons.isNotEmpty) ...[
              const SizedBox(height: 8),
              for (final reason in episode.reasons)
                Padding(
                  padding: const EdgeInsets.only(bottom: 3),
                  child: Text('• $reason', style: const TextStyle(color: Color(0xFFB7C9E8), fontSize: 10, height: 1.4)),
                ),
            ],
            const SizedBox(height: 8),
            Row(children: [
              Expanded(child: Text(reviewLabel, style: const TextStyle(color: Color(0xFFFFD36E), fontSize: 10, fontWeight: FontWeight.w700))),
              if (onShowTrades != null)
                TextButton(onPressed: onShowTrades, child: const Text('مشاهده معاملات')),
            ]),
            if (onReview != null)
              Wrap(spacing: 4, runSpacing: 2, children: [
                _reviewButton('تأیید', TxRevengeReview.confirmed),
                _reviewButton('Revenge نیست', TxRevengeReview.notRevenge),
                _reviewButton('مطمئن نیستم', TxRevengeReview.unsure),
              ]),
            const SizedBox(height: 3),
            const Text(
              'امتیاز فقط شدت شواهد ثبت‌شده است، نه احتمال روان‌شناختی. ورود سریع به‌تنهایی تشخیص Revenge نیست.',
              style: TextStyle(color: Color(0xFF7185A5), fontSize: 9, height: 1.4),
            ),
          ],
        ),
      ),
    );
  }

  Widget _tradeLine(String label, String summary, String id) => Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      SizedBox(width: 104, child: Text(label, style: const TextStyle(color: Color(0xFF91A5C4), fontSize: 10))),
      Expanded(child: Text('$summary · #$id', style: const TextStyle(color: Color(0xFFEAF2FF), fontSize: 10))),
    ],
  );

  Widget _reviewButton(String label, TxRevengeReview value) => OutlinedButton(
    onPressed: () => onReview?.call(value),
    style: OutlinedButton.styleFrom(
      foregroundColor: episode.review == value ? const Color(0xFFFFD36E) : const Color(0xFFB7C9E8),
      side: BorderSide(color: episode.review == value ? const Color(0xFFFFD36E) : const Color(0x334CCBFF)),
      visualDensity: VisualDensity.compact,
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
    ),
    child: Text(label, style: const TextStyle(fontSize: 10)),
  );
}

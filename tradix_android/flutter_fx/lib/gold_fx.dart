import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

/// Colour tokens of the two TRADENEX themes. Mirrors `Theme.kt` in the Android app.
enum TxThemeId { standard, gold }

class TxTokens {
  const TxTokens({
    required this.bg,
    required this.gold,
    required this.goldHi,
    required this.goldDeep,
    required this.panelTop,
    required this.panelBottom,
    required this.green,
    required this.red,
    required this.text,
    required this.muted,
    required this.borderAlpha,
    required this.gilded,
  });

  final Color bg, gold, goldHi, goldDeep, panelTop, panelBottom, green, red, text, muted;
  final double borderAlpha;
  final bool gilded;

  /// The theme that already shipped ("Default").
  static const standard = TxTokens(
    bg: Color(0xFF07070A),
    gold: Color(0xFFF7CF6B),
    goldHi: Color(0xFFFFD978),
    goldDeep: Color(0xFFC98E1E),
    panelTop: Color(0x17FFFFFF),
    panelBottom: Color(0x05FFFFFF),
    green: Color(0xFF2BE8A8),
    red: Color(0xFFFF6B7A),
    text: Color(0xFFF4EFE3),
    muted: Color(0xFFB4AE9F),
    borderAlpha: 0.38,
    gilded: false,
  );

  /// "Gold": black-gold panels, gold edge light.
  static const goldTheme = TxTokens(
    bg: Color(0xFF070608),
    gold: Color(0xFFF2B53D),
    goldHi: Color(0xFFFFD884),
    goldDeep: Color(0xFFB9780F),
    panelTop: Color(0xFF1A130B),
    panelBottom: Color(0xFF0D0A07),
    green: Color(0xFF1FD673),
    red: Color(0xFFFF3D4F),
    text: Color(0xFFF7EDD8),
    muted: Color(0xFFAB9E82),
    borderAlpha: 0.55,
    gilded: true,
  );

  static TxTokens of(TxThemeId id) => id == TxThemeId.gold ? goldTheme : standard;

  /// Smooth in-between tokens for animated theme changes.
  static TxTokens lerp(TxTokens a, TxTokens b, double t) => TxTokens(
        bg: Color.lerp(a.bg, b.bg, t)!,
        gold: Color.lerp(a.gold, b.gold, t)!,
        goldHi: Color.lerp(a.goldHi, b.goldHi, t)!,
        goldDeep: Color.lerp(a.goldDeep, b.goldDeep, t)!,
        panelTop: Color.lerp(a.panelTop, b.panelTop, t)!,
        panelBottom: Color.lerp(a.panelBottom, b.panelBottom, t)!,
        green: Color.lerp(a.green, b.green, t)!,
        red: Color.lerp(a.red, b.red, t)!,
        text: Color.lerp(a.text, b.text, t)!,
        muted: Color.lerp(a.muted, b.muted, t)!,
        borderAlpha: a.borderAlpha + (b.borderAlpha - a.borderAlpha) * t,
        gilded: t < 0.5 ? a.gilded : b.gilded,
      );
}

/// Card with the Gold-theme look: gradient panel, gold edge, soft glow and a light that sweeps the border.
/// With a non-gilded [tokens] it renders the plain glass card, so one widget serves both themes.
class TxPanel extends StatefulWidget {
  const TxPanel({super.key, required this.tokens, required this.child, this.radius = 18, this.padding = const EdgeInsets.all(14)});
  final TxTokens tokens;
  final Widget child;
  final double radius;
  final EdgeInsets padding;

  @override
  State<TxPanel> createState() => _TxPanelState();
}

class _TxPanelState extends State<TxPanel> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 4800))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = widget.tokens;
    final r = BorderRadius.circular(widget.radius);
    return AnimatedBuilder(
      animation: _c,
      builder: (_, child) => CustomPaint(
        foregroundPainter: t.gilded ? _ShimmerBorderPainter(_c.value, t.goldHi, r) : null,
        child: child,
      ),
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: r,
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [t.panelTop, t.panelBottom],
          ),
          border: Border.all(color: t.gold.withOpacity(t.borderAlpha)),
          boxShadow: [BoxShadow(color: t.gold.withOpacity(t.gilded ? 0.28 : 0.14), blurRadius: 18, spreadRadius: -4)],
        ),
        child: Padding(padding: widget.padding, child: widget.child),
      ),
    );
  }
}

class _ShimmerBorderPainter extends CustomPainter {
  _ShimmerBorderPainter(this.phase, this.color, this.radius);
  final double phase;
  final Color color;
  final BorderRadius radius;

  @override
  void paint(Canvas canvas, Size size) {
    final rect = (Offset.zero & size).deflate(0.7);
    final x0 = -size.width + 2.4 * size.width * phase;
    final shader = LinearGradient(
      colors: [Colors.transparent, color.withOpacity(0.95), Colors.transparent],
    ).createShader(Rect.fromLTWH(x0, 0, size.width * 0.9, size.height));
    canvas.drawRRect(
      radius.toRRect(rect),
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4
        ..shader = shader,
    );
  }

  @override
  bool shouldRepaint(covariant _ShimmerBorderPainter old) => old.phase != phase || old.color != color;
}

/// Gold dust drifting upwards. Same simulation as `rust/src/fx.rs` (the Android app runs that one natively).
class AmbientDust extends StatefulWidget {
  const AmbientDust({super.key, this.count = 54, this.color = const Color(0xFFFFD884)});
  final int count;
  final Color color;

  @override
  State<AmbientDust> createState() => _AmbientDustState();
}

class _AmbientDustState extends State<AmbientDust> with SingleTickerProviderStateMixin {
  late final Ticker _ticker;
  late List<_Dust> _p;
  final math.Random _rnd = math.Random(7);
  Duration _last = Duration.zero;
  double _t = 0;

  @override
  void initState() {
    super.initState();
    _p = List.generate(widget.count, (_) => _spawn(false));
    _ticker = createTicker((d) {
      final dt = _last == Duration.zero ? 0.0 : ((d - _last).inMicroseconds / 1e6).clamp(0.0, 0.1);
      _last = d;
      _t = d.inMicroseconds / 1e6;
      for (var i = 0; i < _p.length; i++) {
        final p = _p[i];
        final sway = math.sin(_t * 0.7 + p.phase) * 0.012;
        p.x += (p.vx + sway) * dt;
        p.y += p.vy * dt;
        if (p.x < 0) p.x += 1;
        if (p.x > 1) p.x -= 1;
        if (p.y < -0.03) _p[i] = _spawn(true);
        final tw = 0.5 + 0.5 * math.sin(_t * 1.6 + p.phase);
        final edge = ((p.y + 0.03) / 0.15).clamp(0.0, 1.0) * ((1.05 - p.y) / 0.15).clamp(0.0, 1.0);
        p.alpha = (0.2 + 0.8 * tw) * edge;
      }
      setState(() {});
    })
      ..start();
  }

  _Dust _spawn(bool bottom) => _Dust(
        x: _rnd.nextDouble(),
        y: bottom ? 1.02 : _rnd.nextDouble(),
        vx: (_rnd.nextDouble() - 0.5) * 0.02,
        vy: -(0.012 + _rnd.nextDouble() * 0.05),
        phase: _rnd.nextDouble() * math.pi * 2,
        size: 0.25 + _rnd.nextDouble() * 0.75,
      );

  @override
  void dispose() {
    _ticker.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: CustomPaint(painter: _DustPainter(_p, widget.color, _t), size: Size.infinite),
    );
  }
}

class _Dust {
  _Dust({required this.x, required this.y, required this.vx, required this.vy, required this.phase, required this.size});
  double x, y, vx, vy, phase, size;
  double alpha = 0.5;
}

class _DustPainter extends CustomPainter {
  _DustPainter(this.p, this.color, this.t);
  final List<_Dust> p;
  final Color color;
  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    for (final d in p) {
      if (d.alpha <= 0.01) continue;
      final c = Offset(d.x * size.width, d.y * size.height);
      final r = 0.7 + d.size * 1.8;
      canvas.drawCircle(c, r * 3.2, Paint()..color = color.withOpacity(d.alpha * 0.16));
      canvas.drawCircle(c, r, Paint()..color = color.withOpacity(d.alpha * 0.75));
    }
  }

  @override
  bool shouldRepaint(covariant _DustPainter old) => old.t != t;
}

/// Small crown with a pulsing glow (logo ornament of the Gold theme).
class CrownPulse extends StatefulWidget {
  const CrownPulse({super.key, this.width = 26, this.tokens = TxTokens.goldTheme});
  final double width;
  final TxTokens tokens;

  @override
  State<CrownPulse> createState() => _CrownPulseState();
}

class _CrownPulseState extends State<CrownPulse> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 1800))..repeat(reverse: true);

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
        size: Size(widget.width, widget.width * 0.6),
        painter: _CrownPainter(widget.tokens, 0.55 + 0.45 * _c.value),
      ),
    );
  }
}

class _CrownPainter extends CustomPainter {
  _CrownPainter(this.t, this.pulse);
  final TxTokens t;
  final double pulse;

  @override
  void paint(Canvas canvas, Size s) {
    final w = s.width, h = s.height;
    canvas.drawCircle(Offset(w / 2, h * 0.55), w * 0.75, Paint()..color = t.gold.withOpacity(0.28 * pulse));
    final path = Path()
      ..moveTo(0, h * 0.35)
      ..lineTo(w * 0.25, h * 0.65)
      ..lineTo(w * 0.5, 0)
      ..lineTo(w * 0.75, h * 0.65)
      ..lineTo(w, h * 0.35)
      ..lineTo(w * 0.9, h)
      ..lineTo(w * 0.1, h)
      ..close();
    canvas.drawPath(
      path,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [const Color(0xFFFFE7A8), t.gold, t.goldDeep],
        ).createShader(Offset.zero & s),
    );
  }

  @override
  bool shouldRepaint(covariant _CrownPainter old) => old.pulse != pulse || old.t != t;
}

/// Switches between theme builds with a circular reveal that grows from [origin] (0..1 of the widget size).
class ThemeReveal extends StatefulWidget {
  const ThemeReveal({
    super.key,
    required this.theme,
    required this.builder,
    this.origin = const Offset(0.5, 0.12),
    this.duration = const Duration(milliseconds: 750),
  });
  final TxThemeId theme;
  final Widget Function(BuildContext context, TxThemeId theme) builder;
  final Offset origin;
  final Duration duration;

  @override
  State<ThemeReveal> createState() => _ThemeRevealState();
}

class _ThemeRevealState extends State<ThemeReveal> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: widget.duration);
  TxThemeId? _from;

  @override
  void didUpdateWidget(covariant ThemeReveal old) {
    super.didUpdateWidget(old);
    if (old.theme != widget.theme) {
      _from = old.theme;
      _c.forward(from: 0).whenComplete(() {
        if (mounted) setState(() => _from = null);
      });
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final from = _from;
    if (from == null) return widget.builder(context, widget.theme);
    return AnimatedBuilder(
      animation: _c,
      builder: (ctx, _) => Stack(
        fit: StackFit.expand,
        children: [
          widget.builder(ctx, from),
          ClipPath(
            clipper: _CircleReveal(Curves.easeInOutCubic.transform(_c.value), widget.origin),
            child: widget.builder(ctx, widget.theme),
          ),
        ],
      ),
    );
  }
}

class _CircleReveal extends CustomClipper<Path> {
  _CircleReveal(this.t, this.origin);
  final double t;
  final Offset origin;

  @override
  Path getClip(Size size) {
    final c = Offset(size.width * origin.dx, size.height * origin.dy);
    final far = math.sqrt(size.width * size.width + size.height * size.height) * 1.1;
    return Path()..addOval(Rect.fromCircle(center: c, radius: far * t));
  }

  @override
  bool shouldReclip(covariant _CircleReveal old) => old.t != t;
}

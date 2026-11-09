import 'package:flutter/material.dart';
import 'tradenex_fx.dart';

/// Run with: flutter run -t lib/fx_demo.dart
void main() => runApp(const FxDemo());

/// Standalone preview of the Dart animation kit: `flutter run` inside flutter_fx/.
/// Tap Default / Gold to see the circular theme reveal, shimmering panel, gold dust and the crown.
class FxDemo extends StatefulWidget {
  const FxDemo({super.key});

  @override
  State<FxDemo> createState() => _FxDemoState();
}

class _FxDemoState extends State<FxDemo> {
  TxThemeId _theme = TxThemeId.gold;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(useMaterial3: true),
      home: Scaffold(
        body: ThemeReveal(
          theme: _theme,
          builder: (context, id) => _Screen(
            id: id,
            onPick: (v) => setState(() => _theme = v),
          ),
        ),
      ),
    );
  }
}

class _Screen extends StatelessWidget {
  const _Screen({required this.id, required this.onPick});
  final TxThemeId id;
  final ValueChanged<TxThemeId> onPick;

  @override
  Widget build(BuildContext context) {
    final t = TxTokens.of(id);
    final gilded = id == TxThemeId.gold;
    return ColoredBox(
      color: t.bg,
      child: Stack(
        children: [
          if (gilded) const Positioned.fill(child: AmbientDust()),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  if (gilded) const CrownPulse(),
                  Text(
                    'TRADENEX',
                    style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, letterSpacing: 2, color: t.gold),
                  ),
                  const SizedBox(height: 12),
                  SegmentedButton<TxThemeId>(
                    segments: const [
                      ButtonSegment(value: TxThemeId.standard, label: Text('Default')),
                      ButtonSegment(value: TxThemeId.gold, label: Text('Gold')),
                    ],
                    selected: {id},
                    onSelectionChanged: (s) => onPick(s.first),
                  ),
                  const SizedBox(height: 16),
                  TxPanel(
                    tokens: t,
                    child: Center(child: GlowNumber(value: 24.75, suffix: '%', color: t.green)),
                  ),
                  Expanded(child: GlobeFx(openSessions: const {2}, gold: gilded)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

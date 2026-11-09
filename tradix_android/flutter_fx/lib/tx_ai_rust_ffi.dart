import 'dart:ffi';
import 'dart:io';

import 'package:ffi/ffi.dart';

typedef _NativeMetrics = Int32 Function(
  Pointer<Double>, UintPtr, Pointer<Double>, UintPtr,
);
typedef _DartMetrics = int Function(
  Pointer<Double>, int, Pointer<Double>, int,
);
typedef _NativeScenario = Int32 Function(
  Pointer<Double>, UintPtr, Pointer<Uint8>, UintPtr, Double,
  Pointer<Double>, UintPtr,
);
typedef _DartScenario = int Function(
  Pointer<Double>, int, Pointer<Uint8>, int, double, Pointer<Double>, int,
);
typedef _NativeRevenge = Int32 Function(
  Int64, Int32, Int32, Double, Double, Double, Double,
  Int32, Int32, Int32, Uint32, Pointer<Int32>, UintPtr,
);
typedef _DartRevenge = int Function(
  int, int, int, double, double, double, double,
  int, int, int, int, Pointer<Int32>, int,
);

/// Structured result from the deterministic Rust analytics engine.
class TxRustMetrics {
  const TxRustMetrics({
    required this.net,
    required this.winRate,
    required this.profitFactor,
    required this.maxDrawdown,
    required this.expectancy,
    required this.averageWin,
    required this.averageLoss,
    required this.recoveryFactor,
    required this.largestWinShare,
    required this.largestLossShare,
    required this.sampleSize,
  });

  final double net;
  final double winRate;
  final double profitFactor;
  final double maxDrawdown;
  final double expectancy;
  final double averageWin;
  final double averageLoss;
  final double recoveryFactor;
  final double largestWinShare;
  final double largestLossShare;
  final int sampleSize;
}

/// Dart-to-Rust bridge. The host app must package the matching
/// libtradenex_core.so for each Android ABI before constructing this class.
class TxRustAnalytics {
  TxRustAnalytics._(DynamicLibrary library)
      : _calculate = library.lookupFunction<_NativeMetrics, _DartMetrics>(
          'tx_ai_performance_metrics',
        ),
        _revenge = library.lookupFunction<_NativeRevenge, _DartRevenge>(
          'tx_ai_revenge_evidence',
        ),
        _scenario = library.lookupFunction<_NativeScenario, _DartScenario>(
          'tx_ai_scenario_metrics',
        );
  final _DartMetrics _calculate;
  final _DartRevenge _revenge;
  final _DartScenario _scenario;

  static TxRustAnalytics openAndroid() {
    if (!Platform.isAndroid) {
      throw UnsupportedError('Rust analytics library is currently packaged for Android only.');
    }
    return TxRustAnalytics._(DynamicLibrary.open('libtradenex_core.so'));
  }

  static TxRustAnalytics? tryOpenAndroid() {
    try {
      return openAndroid();
    } on Object {
      return null;
    }
  }

  TxRustRevengeEvidence revengeEvidence({
    required int? gapMinutes,
    required bool previousWasLoss,
    required double? previousRisk,
    required double? currentRisk,
    required double? previousLot,
    required double? currentLot,
    required bool planViolation,
    required bool revengeTag,
    required bool emotionalSignal,
    required int priorLossStreak,
  }) {
    final output = calloc<Int32>(3);
    try {
      final status = _revenge(
        gapMinutes ?? 0, gapMinutes == null ? 0 : 1, previousWasLoss ? 1 : 0,
        previousRisk ?? 0, currentRisk ?? 0, previousLot ?? 0, currentLot ?? 0,
        planViolation ? 1 : 0, revengeTag ? 1 : 0, emotionalSignal ? 1 : 0,
        priorLossStreak.clamp(0, 0xFFFFFFFF).toInt(), output, 3,
      );
      if (status != 0) {
        throw StateError('Rust revenge analyzer rejected the output buffer (status $status).');
      }
      return TxRustRevengeEvidence(
        score: output[0], flags: output[1], classificationCode: output[2],
      );
    } finally {
      calloc.free(output);
    }
  }

  /// Runs a deterministic what-if over realized journal P&L.
  /// A false mask entry excludes a trade; scaleFactor=0.5 illustrates half risk.
  TxRustMetrics calculateScenario(
    List<double> realizedPnls, {
    List<bool>? includeMask,
    double scaleFactor = 1.0,
  }) {
    if (includeMask != null && includeMask.length != realizedPnls.length) {
      throw ArgumentError.value(includeMask.length, 'includeMask.length',
          'Must match the number of P&L values.');
    }
    if (!scaleFactor.isFinite || scaleFactor < 0) {
      throw ArgumentError.value(scaleFactor, 'scaleFactor',
          'Must be finite and non-negative.');
    }
    final input = calloc<Double>(realizedPnls.isEmpty ? 1 : realizedPnls.length);
    final mask = includeMask == null ? nullptr : calloc<Uint8>(includeMask.length);
    final output = calloc<Double>(10);
    try {
      for (var i = 0; i < realizedPnls.length; i++) {
        input[i] = realizedPnls[i];
        if (includeMask != null) {
          mask[i] = includeMask[i] ? 1 : 0;
        }
      }
      final status = _scenario(
        input,
        realizedPnls.length,
        mask,
        includeMask?.length ?? 0,
        scaleFactor,
        output,
        10,
      );
      if (status != 0) {
        throw StateError('Rust scenario engine rejected the input (status $status).');
      }
      final sampleSize = realizedPnls.asMap().entries.where((entry) =>
          entry.value.isFinite &&
          (includeMask == null || includeMask[entry.key])).length;
      return TxRustMetrics(
        net: output[0],
        winRate: output[1],
        profitFactor: output[2],
        maxDrawdown: output[3],
        expectancy: output[4],
        averageWin: output[5],
        averageLoss: output[6],
        recoveryFactor: output[7],
        largestWinShare: output[8],
        largestLossShare: output[9],
        sampleSize: sampleSize,
      );
    } finally {
      calloc.free(input);
      if (includeMask != null) calloc.free(mask);
      calloc.free(output);
    }
  }

  TxRustMetrics calculate(List<double> realizedPnls) {
    final input = calloc<Double>(realizedPnls.isEmpty ? 1 : realizedPnls.length);
    final output = calloc<Double>(10);
    try {
      for (var i = 0; i < realizedPnls.length; i++) {
        input[i] = realizedPnls[i];
      }
      final status = _calculate(input, realizedPnls.length, output, 10);
      if (status != 0) {
        throw StateError('Rust analytics rejected the input buffer (status $status).');
      }
      return TxRustMetrics(
        net: output[0],
        winRate: output[1],
        profitFactor: output[2],
        maxDrawdown: output[3],
        expectancy: output[4],
        averageWin: output[5],
        averageLoss: output[6],
        recoveryFactor: output[7],
        largestWinShare: output[8],
        largestLossShare: output[9],
        sampleSize: realizedPnls.where((v) => v.isFinite).length,
      );
    } finally {
      calloc.free(input);
      calloc.free(output);
    }
  }
}

/// Bit flags: rapid=1, risk-up=2, size-up=4, plan-break=8,
/// user revenge tag=16, emotion=32, prior loss streak=64.
class TxRustRevengeEvidence {
  const TxRustRevengeEvidence({
    required this.score, required this.flags, required this.classificationCode,
  });
  final int score;
  final int flags;
  /// 0 none, 1 loss sequence, 2 possible, 3 strong, 4 user-tagged.
  final int classificationCode;
  bool get rapidReentry => (flags & 1) != 0;
  bool get riskIncreased => (flags & 2) != 0;
  bool get sizeIncreased => (flags & 4) != 0;
  bool get planViolation => (flags & 8) != 0;
  bool get revengeTag => (flags & 16) != 0;
  bool get emotionalSignal => (flags & 32) != 0;
  bool get lossSequence => (flags & 64) != 0;
}

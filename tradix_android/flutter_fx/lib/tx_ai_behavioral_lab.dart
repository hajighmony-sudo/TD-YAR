import 'package:flutter/material.dart';

import 'tx_ai_data_bridge.dart';

enum TxBehavioralReview { unreviewed, confirmed, notRevenge, unsure }
enum TxBehavioralEvidenceLevel { lossSequence, possible, strong, userTagged }

class TxBehavioralEpisode {
  const TxBehavioralEpisode({
    required this.id,
    required this.accountId,
    required this.previousLossTradeId,
    required this.triggerTradeId,
    required this.linkedTradeIds,
    required this.title,
    required this.evidenceLevel,
    required this.evidenceScore,
    required this.actualNetPnl,
    required this.reasons,
    this.elapsedMinutes,
    this.review = TxBehavioralReview.unreviewed,
  });

  final String id;
  final String accountId;
  final int previousLossTradeId;
  final int triggerTradeId;
  final List<int> linkedTradeIds;
  final String title;
  final TxBehavioralEvidenceLevel evidenceLevel;
  final int evidenceScore;
  final double actualNetPnl;
  final List<String> reasons;
  final int? elapsedMinutes;
  final TxBehavioralReview review;
}

enum _BehavioralFilter { all, strong, userTagged, needsReview }

/// Account-scoped Behavioral Lab presentation. Detection is supplied by the
/// deterministic host engine; this widget never invents missing evidence.
class TxBehavioralLab extends StatefulWidget {
  const TxBehavioralLab({
    super.key,
    required this.accountId,
    required this.episodes,
    this.onReviewChanged,
    this.onShowTrades,
  });

  final String accountId;
  final List<TxBehavioralEpisode> episodes;
  final void Function(String episodeId, TxBehavioralReview review)? onReviewChanged;
  final void Function(List<int> tradeIds)? onShowTrades;

  @override
  State<TxBehavioralLab> createState() => _TxBehavioralLabState();
}

class _TxBehavioralLabState extends State<TxBehavioralLab> {
  _BehavioralFilter _filter = _BehavioralFilter.all;
  final Map<String, TxBehavioralReview> _localReviews = {};

  List<TxBehavioralEpisode> get _accountEpisodes => widget.episodes
      .where((episode) => episode.accountId == widget.accountId)
      .toList(growable: false);

  List<TxBehavioralEpisode> get _visibleEpisodes {
    final rows = _accountEpisodes;
    return switch (_filter) {
      _BehavioralFilter.all => rows,
      _BehavioralFilter.strong => rows.where((e) =>
          e.evidenceLevel == TxBehavioralEvidenceLevel.strong ||
          e.evidenceLevel == TxBehavioralEvidenceLevel.userTagged).toList(growable: false),
      _BehavioralFilter.userTagged => rows
          .where((e) => e.evidenceLevel == TxBehavioralEvidenceLevel.userTagged)
          .toList(growable: false),
      _BehavioralFilter.needsReview => rows
          .where((e) => (_localReviews[e.id] ?? e.review) == TxBehavioralReview.unreviewed)
          .toList(growable: false),
    };
  }

  TxBehavioralReview _reviewFor(TxBehavioralEpisode episode) =>
      _localReviews[episode.id] ?? episode.review;

  void _setReview(TxBehavioralEpisode episode, TxBehavioralReview review) {
    setState(() => _localReviews[episode.id] = review);
    widget.onReviewChanged?.call(episode.id, review);
  }

  @override
  Widget build(BuildContext context) {
    final episodes = _visibleEpisodes;
    final strongCount = _accountEpisodes.where((e) =>
        e.evidenceLevel == TxBehavioralEvidenceLevel.strong ||
        e.evidenceLevel == TxBehavioralEvidenceLevel.userTagged).length;
    final unreviewedCount = _accountEpisodes
        .where((e) => _reviewFor(e) == TxBehavioralReview.unreviewed).length;

    return Directionality(
      textDirection: TextDirection.rtl,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text('Behavioral Lab · Revenge Trading',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          Text(
            'امتیاز فقط میزان شواهد ثبت‌شده است؛ احتمال روان‌شناختی یا تشخیص قطعی نیست.',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          Wrap(spacing: 8, runSpacing: 8, children: [
            _metric('اپیزودها', _accountEpisodes.length.toString()),
            _metric('شواهد قوی/تگ دستی', strongCount.toString()),
            _metric('نیازمند بازبینی', unreviewedCount.toString()),
          ]),
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(children: [
              _filterChip('همه', _BehavioralFilter.all),
              _filterChip('قوی', _BehavioralFilter.strong),
              _filterChip('تگ دستی', _BehavioralFilter.userTagged),
              _filterChip('بازبینی‌نشده', _BehavioralFilter.needsReview),
            ]),
          ),
          const SizedBox(height: 8),
          if (episodes.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 20),
              child: Text(
                _accountEpisodes.isEmpty
                    ? 'برای این حساب هنوز اپیزود دارای شواهد ثبت‌شده‌ای وجود ندارد.'
                    : 'در این فیلتر موردی پیدا نشد.',
                textAlign: TextAlign.center,
              ),
            ),
          for (final episode in episodes) _episodeCard(episode),
        ],
      ),
    );
  }

  Widget _metric(String label, String value) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(children: [
          Text(value, style: const TextStyle(fontWeight: FontWeight.w800)),
          Text(label, style: const TextStyle(fontSize: 11)),
        ]),
      );

  Widget _filterChip(String label, _BehavioralFilter filter) => Padding(
        padding: const EdgeInsetsDirectional.only(end: 6),
        child: ChoiceChip(
          label: Text(label),
          selected: _filter == filter,
          onSelected: (_) => setState(() => _filter = filter),
        ),
      );

  Widget _episodeCard(TxBehavioralEpisode episode) {
    final review = _reviewFor(episode);
    final theme = Theme.of(context);
    final level = switch (episode.evidenceLevel) {
      TxBehavioralEvidenceLevel.lossSequence => 'توالی زیان',
      TxBehavioralEvidenceLevel.possible => 'شواهد احتمالی',
      TxBehavioralEvidenceLevel.strong => 'شواهد رفتاری قوی',
      TxBehavioralEvidenceLevel.userTagged => 'تگ دستی کاربر',
    };
    final reviewLabel = switch (review) {
      TxBehavioralReview.unreviewed => 'بازبینی نشده',
      TxBehavioralReview.confirmed => 'تأیید شده توسط کاربر',
      TxBehavioralReview.notRevenge => 'Revenge نیست',
      TxBehavioralReview.unsure => 'نامشخص',
    };

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Row(children: [
            Expanded(child: Text(level, style: const TextStyle(fontWeight: FontWeight.w800))),
            Text('${episode.evidenceScore.clamp(0, 100)}/100',
                style: TextStyle(color: theme.colorScheme.primary, fontWeight: FontWeight.w800)),
          ]),
          const SizedBox(height: 4),
          Text(episode.title),
          const SizedBox(height: 8),
          Text(
            'معامله زیان‌ده قبلی: #${episode.previousLossTradeId}  ·  ورود بعدی: #${episode.triggerTradeId}',
            style: theme.textTheme.bodySmall,
          ),
          Text(
            'فاصله: ${episode.elapsedMinutes == null ? 'ثبت نشده' : '${episode.elapsedMinutes} دقیقه'}  ·  خالص دو معامله: ${episode.actualNetPnl.toStringAsFixed(2)}',
            style: theme.textTheme.bodySmall,
          ),
          if (episode.reasons.isNotEmpty) ...[
            const SizedBox(height: 8),
            for (final reason in episode.reasons)
              Padding(
                padding: const EdgeInsets.only(bottom: 3),
                child: Text('• $reason', style: theme.textTheme.bodySmall),
              ),
          ],
          Align(
            alignment: AlignmentDirectional.centerStart,
            child: TextButton.icon(
              onPressed: widget.onShowTrades == null
                  ? null
                  : () => widget.onShowTrades!(List<int>.unmodifiable(episode.linkedTradeIds)),
              icon: const Icon(Icons.receipt_long),
              label: const Text('نمایش معاملات مرتبط'),
            ),
          ),
          const Divider(height: 12),
          Text('بازبینی کاربر: $reviewLabel', style: theme.textTheme.bodySmall),
          const SizedBox(height: 6),
          Wrap(spacing: 6, runSpacing: 4, children: [
            OutlinedButton(
              onPressed: () => _setReview(episode, TxBehavioralReview.confirmed),
              child: const Text('تأیید توسط من'),
            ),
            OutlinedButton(
              onPressed: () => _setReview(episode, TxBehavioralReview.notRevenge),
              child: const Text('Revenge نیست'),
            ),
            OutlinedButton(
              onPressed: () => _setReview(episode, TxBehavioralReview.unsure),
              child: const Text('مطمئن نیستم'),
            ),
          ]),
        ]),
      ),
    );
  }
}


/// Additional explainable patterns computed from explicit journal fields.
/// These are observable records, not diagnoses or causal conclusions.
class TxBehavioralPatternFeed extends StatefulWidget {
  const TxBehavioralPatternFeed({
    super.key,
    required this.patterns,
    this.onReviewChanged,
    this.onShowTrades,
  });

  final List<TxAiBehaviorPatternRecord> patterns;
  final void Function(String patternId, TxBehavioralReview review)? onReviewChanged;
  final void Function(List<int> tradeIds)? onShowTrades;

  @override
  State<TxBehavioralPatternFeed> createState() => _TxBehavioralPatternFeedState();
}

class _TxBehavioralPatternFeedState extends State<TxBehavioralPatternFeed> {
  final Map<String, TxBehavioralReview> _localReviews = {};

  TxBehavioralReview _reviewFor(TxAiBehaviorPatternRecord pattern) =>
      _localReviews[pattern.id] ??
      switch (pattern.reviewStatus) {
        'confirmed' => TxBehavioralReview.confirmed,
        'not_revenge' => TxBehavioralReview.notRevenge,
        'unsure' => TxBehavioralReview.unsure,
        _ => TxBehavioralReview.unreviewed,
      };

  void _setReview(TxAiBehaviorPatternRecord pattern, TxBehavioralReview review) {
    setState(() => _localReviews[pattern.id] = review);
    widget.onReviewChanged?.call(pattern.id, review);
  }

  @override
  Widget build(BuildContext context) {
    final unreviewed = widget.patterns
        .where((pattern) => _reviewFor(pattern) == TxBehavioralReview.unreviewed)
        .length;
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SizedBox(height: 14),
          const Text('سایر الگوهای رفتاری قابل مشاهده',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
          const SizedBox(height: 5),
          Text(
            'تراکم معاملات پس از برد، افزایش حجم پس از برد، خروج ثبت‌شده پیش از حدسود و انحراف از سشن مجاز. هر کارت فقط بر پایه داده‌های موجود ساخته می‌شود.',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 11),
          ),
          const SizedBox(height: 8),
          Wrap(spacing: 8, children: [
            _metric('الگوها', widget.patterns.length.toString()),
            _metric('بازبینی‌نشده', unreviewed.toString()),
          ]),
          if (widget.patterns.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Text('برای این الگوها داده کافی یا قانون صریحی ثبت نشده است.',
                  textAlign: TextAlign.center),
            ),
          for (final pattern in widget.patterns) _patternCard(pattern),
        ],
      ),
    );
  }

  Widget _metric(String label, String value) => Chip(
        label: Text('$label: $value'),
        visualDensity: VisualDensity.compact,
      );

  Widget _patternCard(TxAiBehaviorPatternRecord pattern) {
    final theme = Theme.of(context);
    final review = _reviewFor(pattern);
    final reviewLabel = switch (review) {
      TxBehavioralReview.unreviewed => 'بازبینی نشده',
      TxBehavioralReview.confirmed => 'تأیید شده',
      TxBehavioralReview.notRevenge => 'مورد معتبر نیست',
      TxBehavioralReview.unsure => 'نامشخص',
    };
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(children: [
              Expanded(
                child: Text(pattern.title,
                    style: const TextStyle(fontWeight: FontWeight.w800)),
              ),
              Text('Evidence ${pattern.evidenceScore}/100',
                  style: TextStyle(color: theme.colorScheme.primary, fontWeight: FontWeight.w800)),
            ]),
            const SizedBox(height: 5),
            Text(
              'معاملات: ${pattern.linkedTradeIds.map((id) => "#$id").join("، ")} · خالص ثبت‌شده: ${pattern.actualNetPnl.toStringAsFixed(2)}',
              style: theme.textTheme.bodySmall,
            ),
            for (final reason in pattern.reasons)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text('• $reason', style: theme.textTheme.bodySmall),
              ),
            TextButton.icon(
              onPressed: widget.onShowTrades == null
                  ? null
                  : () => widget.onShowTrades!(pattern.linkedTradeIds),
              icon: const Icon(Icons.receipt_long),
              label: const Text('نمایش معاملات مرتبط'),
            ),
            const Divider(height: 12),
            Text('بازبینی کاربر: $reviewLabel', style: theme.textTheme.bodySmall),
            Wrap(spacing: 6, runSpacing: 4, children: [
              OutlinedButton(
                onPressed: () => _setReview(pattern, TxBehavioralReview.confirmed),
                child: const Text('تأیید'),
              ),
              OutlinedButton(
                onPressed: () => _setReview(pattern, TxBehavioralReview.notRevenge),
                child: const Text('مورد معتبر نیست'),
              ),
              OutlinedButton(
                onPressed: () => _setReview(pattern, TxBehavioralReview.unsure),
                child: const Text('نامشخص'),
              ),
            ]),
          ],
        ),
      ),
    );
  }
}

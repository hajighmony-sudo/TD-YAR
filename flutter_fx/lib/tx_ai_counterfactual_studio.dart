import 'package:flutter/material.dart';

import 'tx_ai_rust_ffi.dart';

/// Interactive, deterministic what-if view backed by the Rust scenario engine.
/// [offPlanMask] marks trades explicitly recorded as off-plan; null means that
/// the source data has no reliable compliance labels and no exclusion is offered.
class TxCounterfactualStudio extends StatefulWidget {
  const TxCounterfactualStudio({
    super.key,
    required this.analytics,
    required this.realizedPnls,
    this.offPlanMask,
    this.currency = '',
  }) : assert(offPlanMask == null || offPlanMask.length == realizedPnls.length);

  final TxRustAnalytics analytics;
  final List<double> realizedPnls;
  final List<bool>? offPlanMask;
  final String currency;

  @override
  State<TxCounterfactualStudio> createState() => _TxCounterfactualStudioState();
}

class _TxCounterfactualStudioState extends State<TxCounterfactualStudio> {
  double _riskScale = 1.0;
  bool _excludeOffPlan = false;

  @override
  Widget build(BuildContext context) {
    final actual = widget.analytics.calculate(widget.realizedPnls);
    final canExclude = widget.offPlanMask != null;
    final includeMask = _excludeOffPlan && canExclude
        ? widget.offPlanMask!.map((isOffPlan) => !isOffPlan).toList(growable: false)
        : null;
    final scenario = widget.analytics.calculateScenario(
      widget.realizedPnls,
      includeMask: includeMask,
      scaleFactor: _riskScale,
    );
    final delta = scenario.net - actual.net;
    final suffix = widget.currency.isEmpty ? '' : ' ${widget.currency}';

    return Directionality(
      textDirection: TextDirection.rtl,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Counterfactual Studio',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(
                'سناریو فقط روی معاملات بسته‌شده و P&L ثبت‌شده محاسبه می‌شود؛ این بخش بک‌تست یا بازپخش بازار نیست.',
                style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
              ),
              if (canExclude)
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('حذف معاملات صریحاً خارج از پلن'),
                  subtitle: const Text('فقط برچسب‌های ثبت‌شده توسط کاربر؛ داده خالی تخلف فرض نمی‌شود.'),
                  value: _excludeOffPlan,
                  onChanged: (value) => setState(() => _excludeOffPlan = value),
                )
              else
                const ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text('فیلتر خارج از پلن در دسترس نیست'),
                  subtitle: Text('برای جلوگیری از حدس، باید وضعیت پلن هنگام ثبت معامله مشخص شده باشد.'),
                ),
              const SizedBox(height: 8),
              Row(
                children: [
                  const Expanded(child: Text('ضریب ریسک سناریو')),
                  Text('${(_riskScale * 100).round()}%',
                      style: const TextStyle(fontWeight: FontWeight.w800)),
                ],
              ),
              Slider(
                min: 0.25,
                max: 1.0,
                divisions: 3,
                value: _riskScale,
                label: '${(_riskScale * 100).round()}%',
                onChanged: (value) => setState(() => _riskScale = value),
              ),
              const SizedBox(height: 8),
              _metricRow('عملکرد واقعی', actual.net, suffix),
              _metricRow('عملکرد سناریو', scenario.net, suffix),
              _metricRow('تفاوت سناریو با واقعیت', delta, suffix),
              const SizedBox(height: 8),
              Text(
                'نمونه سناریو: ${scenario.sampleSize} از ${actual.sampleSize} معامله معتبر',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 6),
              Text(
                'فرض‌ها: نسبت سود و زیان هر معامله به‌صورت خطی با ضریب ریسک تغییر می‌کند. کارمزد، اسلیپیج، مارجین، ترتیب اجرای سفارش و تغییر رفتار معامله‌گر بازسازی نمی‌شوند.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _metricRow(String label, double value, String suffix) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            Expanded(child: Text(label)),
            Text('${value.toStringAsFixed(2)}$suffix',
                style: const TextStyle(fontWeight: FontWeight.w800)),
          ],
        ),
      );
}

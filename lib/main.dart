import 'package:flutter/material.dart';

void main() {
  runApp(const TdYarApp());
}

const Color ink = Color(0xFF0B1220);
const Color panel = Color(0xFF111C2E);
const Color panelSoft = Color(0xFF17243A);
const Color line = Color(0xFF26364D);
const Color mint = Color(0xFF35D6B0);
const Color red = Color(0xFFFF6B78);
const Color muted = Color(0xFF8FA2BC);

class TdYarApp extends StatelessWidget {
  const TdYarApp({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = ColorScheme.fromSeed(
      seedColor: mint,
      brightness: Brightness.dark,
      surface: panel,
    );
    return MaterialApp(
      title: 'TD-YAR',
      debugShowCheckedModeBanner: false,
      locale: const Locale('fa'),
      supportedLocales: const [Locale('fa'), Locale('en')],
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        scaffoldBackgroundColor: ink,
        fontFamily: null,
        cardTheme: const CardThemeData(
          color: panel,
          elevation: 0,
          margin: EdgeInsets.zero,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: panelSoft,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: line),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: line),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: mint, width: 1.4),
          ),
        ),
      ),
      home: const Directionality(
        textDirection: TextDirection.rtl,
        child: MainShell(),
      ),
    );
  }
}

class TradeEntry {
  TradeEntry({
    required this.symbol,
    required this.side,
    required this.entry,
    required this.exit,
    required this.quantity,
    required this.date,
    this.note = '',
  });

  final String symbol;
  final String side;
  final double entry;
  final double exit;
  final double quantity;
  final DateTime date;
  final String note;

  double get pnl {
    final sign = side == 'خرید' ? 1.0 : -1.0;
    return (exit - entry) * quantity * sign;
  }
}

class StrategyEntry {
  StrategyEntry({required this.title, required this.description});
  final String title;
  final String description;
}

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int selectedIndex = 0;
  final List<TradeEntry> trades = [];
  final List<StrategyEntry> strategies = [
    StrategyEntry(
      title: 'روند و پولبک',
      description: 'نمونه دفترچه: ثبت شرایط، دلیل ورود و نکات بازبینی.',
    ),
    StrategyEntry(
      title: 'شکست محدوده',
      description: 'نمونه دفترچه: محدوده، تأییدها و مواردی که باید بررسی شوند.',
    ),
  ];

  double get totalPnl => trades.fold(0, (sum, trade) => sum + trade.pnl);

  Future<void> addTrade() async {
    final result = await showDialog<TradeEntry>(
      context: context,
      builder: (_) => const TradeEntryDialog(),
    );
    if (result != null) {
      setState(() => trades.insert(0, result));
    }
  }

  Future<void> addStrategy() async {
    final result = await showDialog<StrategyEntry>(
      context: context,
      builder: (_) => const StrategyEntryDialog(),
    );
    if (result != null) {
      setState(() => strategies.insert(0, result));
    }
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      DashboardScreen(trades: trades, totalPnl: totalPnl, onAddTrade: addTrade),
      TradesScreen(trades: trades, onAddTrade: addTrade),
      StrategiesScreen(
        strategies: strategies,
        onAdd: addStrategy,
        onDelete: (item) => setState(() => strategies.remove(item)),
      ),
      const SessionsScreen(),
    ];

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 8),
              child: Row(
                children: [
                  Container(
                    width: 42,
                    height: 42,
                    decoration: BoxDecoration(
                      color: mint.withValues(alpha: 0.13),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: mint.withValues(alpha: 0.28)),
                    ),
                    child: const Icon(Icons.candlestick_chart_rounded,
                        color: mint, size: 24),
                  ),
                  const SizedBox(width: 11),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('TD-YAR',
                            style: TextStyle(
                                fontSize: 18, fontWeight: FontWeight.w900)),
                        Text('ژورنال و بازبینی عملکرد',
                            style: TextStyle(fontSize: 11, color: muted)),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'افزودن معامله',
                    onPressed: addTrade,
                    icon: const Icon(Icons.add_circle_outline_rounded,
                        color: mint, size: 27),
                  ),
                  IconButton(
                    tooltip: 'تنظیمات',
                    onPressed: () => showAboutDialog(
                      context: context,
                      applicationName: 'TD-YAR',
                      applicationVersion: '0.1.0',
                      children: const [
                        Text('نسخه اولیه ژورنال شخصی و آموزشی.'),
                      ],
                    ),
                    icon: const Icon(Icons.tune_rounded, color: muted),
                  ),
                ],
              ),
            ),
            Expanded(
              child: IndexedStack(index: selectedIndex, children: pages),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        backgroundColor: panel,
        indicatorColor: mint.withValues(alpha: 0.14),
        selectedIndex: selectedIndex,
        onDestinationSelected: (index) =>
            setState(() => selectedIndex = index),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.grid_view_rounded),
            selectedIcon: Icon(Icons.grid_view_rounded, color: mint),
            label: 'نمای کلی',
          ),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_rounded),
            selectedIcon: Icon(Icons.receipt_long_rounded, color: mint),
            label: 'معاملات',
          ),
          NavigationDestination(
            icon: Icon(Icons.menu_book_rounded),
            selectedIcon: Icon(Icons.menu_book_rounded, color: mint),
            label: 'استراتژی‌ها',
          ),
          NavigationDestination(
            icon: Icon(Icons.schedule_rounded),
            selectedIcon: Icon(Icons.schedule_rounded, color: mint),
            label: 'سشن‌ها',
          ),
        ],
      ),
    );
  }
}

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({
    super.key,
    required this.trades,
    required this.totalPnl,
    required this.onAddTrade,
  });

  final List<TradeEntry> trades;
  final double totalPnl;
  final VoidCallback onAddTrade;

  @override
  Widget build(BuildContext context) {
    final wins = trades.where((trade) => trade.pnl > 0).length;
    final winRate = trades.isEmpty ? 0.0 : wins * 100 / trades.length;
    final pnlColor = totalPnl >= 0 ? mint : red;

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 24),
      children: [
        const Text('سلام، خوش اومدی 👋',
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
        const SizedBox(height: 5),
        const Text('مرور منظم، تصمیم‌گیری آگاهانه‌تر',
            style: TextStyle(color: muted, fontSize: 12)),
        const SizedBox(height: 20),
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF173A47), Color(0xFF14253A)],
              begin: Alignment.topRight,
              end: Alignment.bottomLeft,
            ),
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: mint.withValues(alpha: 0.22)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.account_balance_wallet_outlined,
                      color: mint, size: 18),
                  SizedBox(width: 8),
                  Text('خالص نتیجه معاملات ثبت‌شده',
                      style: TextStyle(color: Color(0xFFC1D7D8), fontSize: 12)),
                  Spacer(),
                  Icon(Icons.more_horiz, color: muted),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                totalPnl.toStringAsFixed(2),
                style: TextStyle(
                  color: pnlColor,
                  fontSize: 32,
                  fontWeight: FontWeight.w900,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                trades.isEmpty ? 'هنوز معامله‌ای ثبت نشده' : 'بر اساس اطلاعات واردشده',
                style: const TextStyle(color: muted, fontSize: 11),
              ),
              const SizedBox(height: 18),
              Row(
                children: [
                  Expanded(
                    child: _MiniStat(
                      label: 'تعداد معاملات',
                      value: trades.length.toString(),
                    ),
                  ),
                  Expanded(
                    child: _MiniStat(
                      label: 'درصد معاملات مثبت',
                      value: winRate.toStringAsFixed(1) + '%',
                    ),
                  ),
                  Expanded(
                    child: _MiniStat(
                      label: 'معاملات مثبت',
                      value: wins.toString(),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        const SectionHeading(title: 'روند عملکرد', subtitle: 'نمای تجمعی معاملات ثبت‌شده'),
        const SizedBox(height: 10),
        Container(
          height: 190,
          padding: const EdgeInsets.fromLTRB(12, 18, 12, 12),
          decoration: BoxDecoration(
            color: panel,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: line),
          ),
          child: trades.isEmpty
              ? const Center(
                  child: Text('با ثبت اولین معامله، نمودار عملکرد ساخته می‌شود.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: muted, fontSize: 12)),
                )
              : CustomPaint(
                  painter: PerformancePainter(trades),
                  child: const SizedBox.expand(),
                ),
        ),
        const SizedBox(height: 18),
        SectionHeading(
          title: 'آخرین معاملات',
          subtitle: trades.isEmpty ? 'برای شروع یک معامله ثبت کن' : 'آخرین موارد ثبت‌شده',
          trailing: TextButton(
            onPressed: onAddTrade,
            child: const Text('+ ثبت معامله',
                style: TextStyle(color: mint, fontWeight: FontWeight.bold)),
          ),
        ),
        const SizedBox(height: 8),
        if (trades.isEmpty)
          EmptyState(
            icon: Icons.receipt_long_outlined,
            title: 'ژورنالت هنوز خالیه',
            subtitle: 'جزئیات معاملات گذشته را دستی وارد کن تا بتوانی بعداً مرورشان کنی.',
            actionLabel: 'ثبت اولین معامله',
            onAction: onAddTrade,
          )
        else
          ...trades.take(4).map((trade) => TradeTile(trade: trade)),
      ],
    );
  }
}

class TradesScreen extends StatelessWidget {
  const TradesScreen({super.key, required this.trades, required this.onAddTrade});
  final List<TradeEntry> trades;
  final VoidCallback onAddTrade;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
      children: [
        SectionHeading(
          title: 'دفتر معاملات',
          subtitle: 'ثبت دستی و مرور سوابق',
          trailing: IconButton(
            onPressed: onAddTrade,
            icon: const Icon(Icons.add_circle, color: mint, size: 28),
          ),
        ),
        const SizedBox(height: 14),
        if (trades.isEmpty)
          EmptyState(
            icon: Icons.note_add_outlined,
            title: 'هنوز موردی وجود ندارد',
            subtitle: 'معاملات بسته‌شده را همراه با قیمت ورود، خروج و تعداد ثبت کن.',
            actionLabel: 'افزودن معامله',
            onAction: onAddTrade,
          )
        else ...[
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: panel,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: line),
            ),
            child: Row(
              children: [
                Expanded(child: _MiniStat(label: 'کل موارد', value: trades.length.toString())),
                Expanded(
                  child: _MiniStat(
                    label: 'نتیجه ثبت‌شده',
                    value: trades.fold<double>(0, (sum, item) => sum + item.pnl).toStringAsFixed(2),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          ...trades.map((trade) => TradeTile(trade: trade)),
        ],
      ],
    );
  }
}

class StrategiesScreen extends StatelessWidget {
  const StrategiesScreen({
    super.key,
    required this.strategies,
    required this.onAdd,
    required this.onDelete,
  });

  final List<StrategyEntry> strategies;
  final VoidCallback onAdd;
  final ValueChanged<StrategyEntry> onDelete;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
      children: [
        SectionHeading(
          title: 'دفترچه استراتژی‌ها',
          subtitle: 'قوانین و یادداشت‌های شخصی',
          trailing: IconButton(
            onPressed: onAdd,
            icon: const Icon(Icons.add_circle, color: mint, size: 28),
          ),
        ),
        const SizedBox(height: 14),
        if (strategies.isEmpty)
          EmptyState(
            icon: Icons.menu_book_outlined,
            title: 'دفترچه خالی است',
            subtitle: 'می‌توانی قوانین و نکات آموزشی خودت را ثبت کنی.',
            actionLabel: 'ساخت یادداشت',
            onAction: onAdd,
          )
        else
          ...strategies.map(
            (item) => Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.all(15),
              decoration: BoxDecoration(
                color: panel,
                borderRadius: BorderRadius.circular(17),
                border: Border.all(color: line),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: mint.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.bookmark_outline, color: mint),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(item.title,
                            style: const TextStyle(fontWeight: FontWeight.bold)),
                        const SizedBox(height: 5),
                        Text(item.description,
                            style: const TextStyle(color: muted, fontSize: 12)),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'حذف',
                    onPressed: () => onDelete(item),
                    icon: const Icon(Icons.delete_outline, color: red, size: 20),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

class SessionsScreen extends StatelessWidget {
  const SessionsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    const sessions = [
      _SessionInfo('توکیو', '00:00 تا 09:00', 'UTC', Icons.wb_twilight_rounded),
      _SessionInfo('لندن', '08:00 تا 17:00', 'UTC', Icons.location_city_rounded),
      _SessionInfo('نیویورک', '13:00 تا 22:00', 'UTC', Icons.apartment_rounded),
    ];

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
      children: [
        const SectionHeading(
          title: 'سشن‌های مرجع',
          subtitle: 'بازه‌های نمونه بر اساس UTC',
        ),
        const SizedBox(height: 8),
        Container(
          padding: const EdgeInsets.all(13),
          decoration: BoxDecoration(
            color: Color(0xFF302A1B),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: Color(0xFF66512B)),
          ),
          child: const Row(
            children: [
              Icon(Icons.info_outline, color: Color(0xFFFFC96B), size: 19),
              SizedBox(width: 9),
              Expanded(
                child: Text(
                  'این زمان‌ها بازه‌های مرجع آموزشی هستند؛ ساعت تابستانی و منطقه زمانی محلی می‌تواند آن‌ها را تغییر دهد.',
                  style: TextStyle(color: Color(0xFFFFDDA0), fontSize: 11, height: 1.6),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        ...sessions.map(
          (session) => Container(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: panel,
              borderRadius: BorderRadius.circular(17),
              border: Border.all(color: line),
            ),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: mint.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(13),
                  ),
                  child: Icon(session.icon, color: mint),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(session.name,
                          style: const TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Text(session.time + ' ' + session.zone,
                          style: const TextStyle(color: muted, fontSize: 12)),
                    ],
                  ),
                ),
                const Icon(Icons.schedule_rounded, color: muted),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        const SectionHeading(title: 'هم‌پوشانی‌های مرجع'),
        const SizedBox(height: 8),
        const _OverlapCard(title: 'لندن + نیویورک', time: '13:00 تا 17:00 UTC'),
        const _OverlapCard(title: 'توکیو + لندن', time: '08:00 تا 09:00 UTC'),
      ],
    );
  }
}

class _SessionInfo {
  const _SessionInfo(this.name, this.time, this.zone, this.icon);
  final String name;
  final String time;
  final String zone;
  final IconData icon;
}

class _OverlapCard extends StatelessWidget {
  const _OverlapCard({required this.title, required this.time});
  final String title;
  final String time;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: panelSoft,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: line),
      ),
      child: Row(
        children: [
          const Icon(Icons.compare_arrows_rounded, color: mint),
          const SizedBox(width: 10),
          Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.w600))),
          Text(time, style: const TextStyle(color: muted, fontSize: 11)),
        ],
      ),
    );
  }
}

class TradeEntryDialog extends StatefulWidget {
  const TradeEntryDialog({super.key});

  @override
  State<TradeEntryDialog> createState() => _TradeEntryDialogState();
}

class _TradeEntryDialogState extends State<TradeEntryDialog> {
  final formKey = GlobalKey<FormState>();
  final symbol = TextEditingController();
  final entry = TextEditingController();
  final exit = TextEditingController();
  final quantity = TextEditingController(text: '1');
  final note = TextEditingController();
  String side = 'خرید';

  @override
  void dispose() {
    symbol.dispose();
    entry.dispose();
    exit.dispose();
    quantity.dispose();
    note.dispose();
    super.dispose();
  }

  double? parse(TextEditingController controller) =>
      double.tryParse(controller.text.trim().replaceAll(',', '.'));

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: panel,
      title: const Text('ثبت معامله بسته‌شده'),
      content: SizedBox(
        width: 420,
        child: Form(
          key: formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: symbol,
                  decoration: const InputDecoration(labelText: 'نماد یا عنوان'),
                  validator: (value) =>
                      value == null || value.trim().isEmpty ? 'این فیلد الزامی است' : null,
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  value: side,
                  decoration: const InputDecoration(labelText: 'جهت ثبت'),
                  items: const [
                    DropdownMenuItem(value: 'خرید', child: Text('خرید')),
                    DropdownMenuItem(value: 'فروش', child: Text('فروش')),
                  ],
                  onChanged: (value) => setState(() => side = value ?? 'خرید'),
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: entry,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'قیمت ورود'),
                  validator: (value) => parse(entry) == null ? 'عدد معتبر وارد کن' : null,
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: exit,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'قیمت خروج'),
                  validator: (value) => parse(exit) == null ? 'عدد معتبر وارد کن' : null,
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: quantity,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'تعداد / حجم ثبت‌شده'),
                  validator: (value) =>
                      parse(quantity) == null || parse(quantity)! <= 0
                          ? 'عدد باید بیشتر از صفر باشد'
                          : null,
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: note,
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'یادداشت (اختیاری)'),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('انصراف', style: TextStyle(color: muted)),
        ),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: mint, foregroundColor: ink),
          onPressed: () {
            if (!formKey.currentState!.validate()) return;
            Navigator.pop(
              context,
              TradeEntry(
                symbol: symbol.text.trim().toUpperCase(),
                side: side,
                entry: parse(entry)!,
                exit: parse(exit)!,
                quantity: parse(quantity)!,
                date: DateTime.now(),
                note: note.text.trim(),
              ),
            );
          },
          child: const Text('ذخیره'),
        ),
      ],
    );
  }
}

class StrategyEntryDialog extends StatefulWidget {
  const StrategyEntryDialog({super.key});

  @override
  State<StrategyEntryDialog> createState() => _StrategyEntryDialogState();
}

class _StrategyEntryDialogState extends State<StrategyEntryDialog> {
  final titleController = TextEditingController();
  final descriptionController = TextEditingController();

  @override
  void dispose() {
    titleController.dispose();
    descriptionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: panel,
      title: const Text('یادداشت جدید'),
      content: SizedBox(
        width: 400,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: titleController,
              decoration: const InputDecoration(labelText: 'عنوان'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: descriptionController,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'قوانین و توضیحات'),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('انصراف', style: TextStyle(color: muted)),
        ),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: mint, foregroundColor: ink),
          onPressed: () {
            if (titleController.text.trim().isEmpty) return;
            Navigator.pop(
              context,
              StrategyEntry(
                title: titleController.text.trim(),
                description: descriptionController.text.trim(),
              ),
            );
          },
          child: const Text('ذخیره'),
        ),
      ],
    );
  }
}

class TradeTile extends StatelessWidget {
  const TradeTile({super.key, required this.trade});
  final TradeEntry trade;

  @override
  Widget build(BuildContext context) {
    final positive = trade.pnl >= 0;
    final color = positive ? mint : red;
    return Container(
      margin: const EdgeInsets.only(bottom: 9),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: panel,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: line),
      ),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(11),
            ),
            child: Icon(
              positive ? Icons.trending_up_rounded : Icons.trending_down_rounded,
              color: color,
            ),
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(trade.symbol,
                    style: const TextStyle(fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text(
                  trade.side + '  •  ' + trade.date.toString().substring(0, 16),
                  style: const TextStyle(color: muted, fontSize: 10),
                ),
                if (trade.note.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(trade.note,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: muted, fontSize: 10)),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text((positive ? '+' : '') + trade.pnl.toStringAsFixed(2),
                  style: TextStyle(color: color, fontWeight: FontWeight.w900)),
              const SizedBox(height: 4),
              Text('ورود ' + trade.entry.toStringAsFixed(2) +
                  ' / خروج ' + trade.exit.toStringAsFixed(2),
                  style: const TextStyle(color: muted, fontSize: 9)),
            ],
          ),
        ],
      ),
    );
  }
}

class _MiniStat extends StatelessWidget {
  const _MiniStat({required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: muted, fontSize: 10)),
        const SizedBox(height: 5),
        Text(value,
            style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
      ],
    );
  }
}

class SectionHeading extends StatelessWidget {
  const SectionHeading({super.key, required this.title, this.subtitle, this.trailing});
  final String title;
  final String? subtitle;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
              if (subtitle != null) ...[
                const SizedBox(height: 4),
                Text(subtitle!, style: const TextStyle(color: muted, fontSize: 11)),
              ],
            ],
          ),
        ),
        if (trailing != null) trailing!,
      ],
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.actionLabel,
    required this.onAction,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 28),
      decoration: BoxDecoration(
        color: panel,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: line),
      ),
      child: Column(
        children: [
          Icon(icon, color: mint, size: 34),
          const SizedBox(height: 12),
          Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text(subtitle,
              textAlign: TextAlign.center,
              style: const TextStyle(color: muted, fontSize: 11, height: 1.6)),
          const SizedBox(height: 14),
          FilledButton(
            onPressed: onAction,
            style: FilledButton.styleFrom(backgroundColor: mint, foregroundColor: ink),
            child: Text(actionLabel),
          ),
        ],
      ),
    );
  }
}

class PerformancePainter extends CustomPainter {
  PerformancePainter(this.trades);
  final List<TradeEntry> trades;

  @override
  void paint(Canvas canvas, Size size) {
    final values = <double>[0];
    var total = 0.0;
    for (final trade in trades.reversed) {
      total += trade.pnl;
      values.add(total);
    }

    final minValue = values.reduce((a, b) => a < b ? a : b);
    final maxValue = values.reduce((a, b) => a > b ? a : b);
    final range = (maxValue - minValue).abs() < 0.0001 ? 1.0 : maxValue - minValue;
    const horizontalPadding = 8.0;
    const verticalPadding = 12.0;
    final chartWidth = size.width - horizontalPadding * 2;
    final chartHeight = size.height - verticalPadding * 2;

    final gridPaint = Paint()
      ..color = line
      ..strokeWidth = 0.7;
    for (var i = 0; i < 4; i++) {
      final y = verticalPadding + chartHeight * i / 3;
      canvas.drawLine(Offset(horizontalPadding, y),
          Offset(size.width - horizontalPadding, y), gridPaint);
    }

    final points = <Offset>[];
    for (var i = 0; i < values.length; i++) {
      final x = horizontalPadding + chartWidth * i / (values.length - 1);
      final normalized = (values[i] - minValue) / range;
      final y = verticalPadding + chartHeight * (1 - normalized);
      points.add(Offset(x, y));
    }

    final path = Path()..moveTo(points.first.dx, points.first.dy);
    for (final point in points.skip(1)) {
      path.lineTo(point.dx, point.dy);
    }

    final fillPath = Path.from(path)
      ..lineTo(points.last.dx, size.height - verticalPadding)
      ..lineTo(points.first.dx, size.height - verticalPadding)
      ..close();

    final fillPaint = Paint()
      ..shader = LinearGradient(
        colors: [mint.withValues(alpha: 0.25), mint.withValues(alpha: 0.01)],
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
      ).createShader(Offset.zero & size);
    canvas.drawPath(fillPath, fillPaint);

    final strokePaint = Paint()
      ..color = mint
      ..strokeWidth = 2.5
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    canvas.drawPath(path, strokePaint);

    final dotPaint = Paint()..color = mint;
    canvas.drawCircle(points.last, 4, dotPaint);
  }

  @override
  bool shouldRepaint(covariant PerformancePainter oldDelegate) =>
      oldDelegate.trades != trades;
}

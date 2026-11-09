package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.TxAiEngine
import com.tradenex.app.data.TxAiSlice
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.theme.Tx
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun TxAiScreen(nav: NavController) {
    val trades = AppState.visibleTrades.toList()
    val account = AppState.selectedAccount
    val riskPercent = AppState.settings.riskPercent
    val localZone = remember(AppState.settings.tzMode) {
        when (AppState.settings.tzMode) {
            0 -> ZoneId.of("Asia/Tehran")
            1 -> ZoneOffset.UTC
            else -> ZoneId.systemDefault()
        }
    }
    val report = remember(trades, account, riskPercent, localZone) {
        TxAiEngine.analyze(trades, account, riskPercent, localZone)
    }
    val performance = remember(trades) {
        NativeCore.performanceMetrics(trades.map { it.pnl }.toDoubleArray())
    }
    val revengeEpisodes = remember(trades) { com.tradenex.app.data.RevengeTradingAnalyzer.analyze(trades) }
    val strategyRules = AppState.strategies.toList()
    val strategyRevisions = AppState.strategyRevisions.toList()
    val behavioralPatterns = remember(trades, strategyRules, strategyRevisions) {
        com.tradenex.app.data.TxAiBehavioralPatternAnalyzer.analyze(
            trades, strategyRules, strategyRevisions,
        )
    }
    fun money(v: Double) = Analytics.money(v)
    Page(title = "TX AI", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("شناخت شخصیت معاملاتی", color = Tx.Gold, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("تحلیل بر پایه معاملات واقعی حساب «" + AppState.selectedAccount.name + "»؛ این بخش سیگنال خرید یا فروش صادر نمی‌کند.", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("سود/زیان خالص", money(report.net), if (report.net >= 0) Tx.Green else Tx.Red, Modifier.weight(1f))
                        StatTile("نرخ برد", (report.winRate * 100).toInt().toString() + "%", Tx.Gold, Modifier.weight(1f))
                        StatTile("معاملات", report.totalTrades.toString(), Tx.Blue, Modifier.weight(1f))
                    }
                    Text("معیارهای آماری هسته Rust", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile(
                            "Profit Factor",
                            if (performance.sampleSize == 0) "—" else if (performance.profitFactor >= 99.0) "≥99" else String.format(java.util.Locale.US, "%.2f", performance.profitFactor),
                            Tx.Gold, Modifier.weight(1f),
                        )
                        StatTile(
                            "Expectancy",
                            if (performance.sampleSize == 0) "—" else money(performance.expectancy),
                            Tx.Blue, Modifier.weight(1f),
                        )
                        StatTile(
                            "Max Drawdown",
                            if (performance.sampleSize == 0) "—" else money(performance.maxDrawdown),
                            Tx.Red, Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile(
                            "میانگین برد",
                            if (performance.sampleSize == 0) "—" else money(performance.averageWin),
                            Tx.Green, Modifier.weight(1f),
                        )
                        StatTile(
                            "میانگین باخت",
                            if (performance.sampleSize == 0) "—" else money(-performance.averageLoss),
                            Tx.Red, Modifier.weight(1f),
                        )
                        StatTile(
                            "Recovery",
                            if (performance.sampleSize == 0) "—" else String.format(java.util.Locale.US, "%.2f", performance.recoveryFactor),
                            Tx.Gold, Modifier.weight(1f),
                        )
                    }
                    Text(
                        if (performance.sampleSize == 0) "برای محاسبه معیارها، معامله بسته‌شده با P&L معتبر ثبت کن."
                        else "بزرگ‌ترین برد: ${(performance.largestWinShare * 100).toInt()}٪ از سودهای مثبت · بزرگ‌ترین باخت: ${(performance.largestLossShare * 100).toInt()}٪ از زیان‌های منفی · نمونه معتبر: ${performance.sampleSize}",
                        color = Tx.Muted, fontSize = 10.sp,
                    )
                    Text("پروفایل رفتاری فعلی: " + report.personality, color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(report.bestSession?.let { "سشن برتر فعلی: " + it.label + " · " + it.trades + " معامله" } ?: "برای اعلام سشن برتر، حداقل ۵ معامله زمان‌دار در یک سشن لازم است.", color = Tx.Text, fontSize = 12.sp)
                    Text(
                        report.worstSession?.let { "سشن ضعیف‌تر: ${it.label} · ${it.trades} معامله" }
                            ?: "برای تعیین سشن ضعیف‌تر نیز حداقل ۵ معامله زمان‌دار لازم است.",
                        color = Tx.Muted, fontSize = 11.sp,
                    )
                }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("الگوی عملکرد و سشن‌ها")
                    Text("سشن‌ها از زمان بازشدن معامله و ساعت محلی بازارها استخراج شده‌اند.", color = Tx.Muted, fontSize = 10.sp)
                    if (report.sessions.isEmpty()) EmptyAi("زمان بازشدن معاملات ثبت نشده است.")
                    report.sessions.forEach { AiSliceRow(it, money(it.net)) }
                    Text("بهترین تایم‌فریم‌ها", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.timeframes.isEmpty()) EmptyAi("از این نسخه به بعد تایم‌فریم هنگام ثبت معامله ذخیره می‌شود.")
                    report.timeframes.take(5).forEach { AiSliceRow(it, money(it.net)) }
                    Text("شرایط بازار", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.marketConditions.isEmpty()) EmptyAi("هنوز شرایط بازار برای معامله‌ها ثبت نشده است.")
                    report.marketConditions.forEach { AiSliceRow(it, money(it.net)) }
                    Text("عملکرد بر اساس حالت ذهنی", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.emotions.isEmpty()) EmptyAi("حالت ذهنی برای معاملات ثبت نشده است.")
                    report.emotions.take(5).forEach { AiSliceRow(it, money(it.net)) }
                    Text("عملکرد نمادها", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    report.symbols.take(8).forEach { AiSliceRow(it, money(it.net)) }
                    report.bestStrategy?.let { Text("استراتژی شاخص: " + it.label, color = Tx.Gold, fontSize = 12.sp) }
                    Text("مقایسه نسخه‌های استراتژی", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.strategyVersions.isEmpty()) EmptyAi("برای مقایسه نسخه‌ها، نام و نسخه استراتژی باید هنگام ثبت معامله ذخیره شود.")
                    report.strategyVersions.take(8).forEach { AiSliceRow(it, money(it.net)) }
                    Text("عملکرد در بازه‌های ۱۵ دقیقه‌ای", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.timeOfDay.isEmpty()) EmptyAi("زمان ورود معامله‌ها ثبت نشده است.")
                    report.timeOfDay.take(8).forEach { AiSliceRow(it, money(it.net)) }
                    Text("مدت نگهداری و کیفیت خروج", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "میانگین نگهداری: ${report.averageHoldMinutes?.let { String.format(java.util.Locale.US, "%.1f دقیقه", it) } ?: "—"} · میانه: ${report.medianHoldMinutes?.let { String.format(java.util.Locale.US, "%.1f دقیقه", it) } ?: "—"}",
                        color = Tx.Text, fontSize = 11.sp,
                    )
                    Text(
                        "میانگین R ثبت‌شده: ${report.averageRMultiple?.let { String.format(java.util.Locale.US, "%+.2fR", it) } ?: "—"}",
                        color = Tx.Text, fontSize = 11.sp,
                    )
                    Text(
                        "بیشترین برد متوالی: ${report.maxWinStreak} · بیشترین باخت متوالی: ${report.maxLossStreak}",
                        color = Tx.Text, fontSize = 11.sp,
                    )
                    Text(
                        "بازه ۱۵ دقیقه‌ای بر اساس منطقه زمانی انتخاب‌شده در تنظیمات محاسبه می‌شود؛ داده بدون زمان ورود از این تحلیل کنار گذاشته شده است.",
                        color = Tx.Muted, fontSize = 9.sp,
                    )
                }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("تحلیل روزانه و هفتگی")
                    Text("روزهای معاملاتی اخیر", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    report.days.takeLast(14).forEach { AiSliceRow(it, money(it.net)) }
                    Text("الگوی عملکرد در روزهای هفته", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.weekdays.isEmpty()) EmptyAi("داده روزانه کافی نیست.")
                    report.weekdays.forEach { AiSliceRow(it, money(it.net)) }
                    Text("هفته‌های اخیر · شروع هفته دوشنبه", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report.weeks.isEmpty()) EmptyAi("داده هفتگی کافی نیست.")
                    report.weeks.takeLast(8).forEach { AiSliceRow(it, money(it.net)) }
                }
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("شبیه‌سازی تصمیم‌های جایگزین")
                    Text("اگر معاملات دارای نشانه خارج از پلن حذف می‌شدند", color = Tx.Text, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("تعداد", report.outsideStrategyCount.toString(), Tx.Red, Modifier.weight(1f))
                        StatTile("سود/زیان آن‌ها", money(report.outsideStrategyNet), if (report.outsideStrategyNet >= 0) Tx.Green else Tx.Red, Modifier.weight(1f))
                    }
                    Text("اگر ریسک همه معاملات نصف می‌شد (با فرض ثابت‌ماندن نسبت سود و زیان)", color = Tx.Text, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("واقعی", money(report.allNet), Tx.Text, Modifier.weight(1f))
                        StatTile("ریسک ۵۰٪", money(report.halfRiskNet), Tx.Gold, Modifier.weight(1f))
                    }
                    Text("سود/زیان کل اگر معاملات خارج از پلن حذف می‌شد: " + money(report.withoutOutsideStrategyNet), color = if (report.withoutOutsideStrategyNet >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("تغییر نسبت به عملکرد واقعی: " + money(report.withoutOutsideStrategyDelta), color = if (report.withoutOutsideStrategyDelta >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("تغییر سناریوی ریسک نصف: " + money(report.halfRiskDelta), color = if (report.halfRiskDelta >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("این شبیه‌سازی بک‌تست نیست؛ اسلیپیج، مارجین و تغییر رفتار اجرای سفارش را مدل نمی‌کند. تشخیص خارج از پلن بر اساس انتخاب صریح «خارج از پلن» یا تگ‌های FOMO، Revenge، No-plan و Off-strategy است؛ داده ثبت‌نشده به‌تنهایی تخلف محسوب نمی‌شود.", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        }
        Reveal(4) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("پیشرفت ماهانه · " + report.scores.monthLabel)
                    Text("امتیاز ۰ تا ۱۰۰؛ خط تیره یعنی داده کافی موجود نیست.", color = Tx.Muted, fontSize = 10.sp)
                    ScoreRow("نظم معاملاتی", report.scores.discipline, "نبود تگ‌های FOMO، Revenge و Overtrading")
                    ScoreRow("مدیریت ریسک", report.scores.risk, "نیازمند ثبت ریسک برنامه‌ریزی‌شده")
                    ScoreRow("پایبندی به استراتژی", report.scores.strategy, "بر اساس ثبت صریح «طبق پلن / خارج از پلن»؛ پوشش داده باید حداقل ۷۰٪ باشد")
                    ScoreRow("کیفیت تصمیم‌گیری", report.scores.decision, "کامل‌بودن ورود، خروج، حدضرر و استراتژی")
                    Text("نمونه این ماه: " + report.scores.sampleSize + " معامله · امتیاز فقط با پوشش داده کافی نمایش داده می‌شود.", color = Tx.Muted, fontSize = 10.sp)
                    Text("روند امتیازها در ماه‌های ثبت‌شده", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    report.monthlyProgress.forEach { month ->
                        Text(month.monthLabel + "  ·  نظم " + (month.discipline?.toString() ?: "—") + "  ·  ریسک " + (month.risk?.toString() ?: "—") + "  ·  استراتژی " + (month.strategy?.toString() ?: "—") + "  ·  تصمیم " + (month.decision?.toString() ?: "—"), color = Tx.Text, fontSize = 10.sp)
                    }
                }
            }
        }
        Reveal(5) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("پیشنهادهای TX AI")
                    if (report.insights.isEmpty()) EmptyAi("برای ارائه پیشنهاد معتبر، معاملات و اطلاعات زمینه‌ای بیشتری ثبت کن.")
                    report.insights.forEachIndexed { i, insight ->
                        val insightId = "report-${insight.hashCode().toUInt().toString(16)}"
                        val pinned = AppState.isInsightPinned(account.id, insightId)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (i + 1).toString() + ". " + insight,
                                color = Tx.Text, fontSize = 11.sp, modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                AppState.setInsightPinned(account.id, insightId, !pinned)
                            }) {
                                Text(if (pinned) "برداشتن سنجاق" else "سنجاق")
                            }
                        }
                    }
                    Text("تایم‌فریم و شرایط بازار از معاملات جدیدی که این فیلدها در آن‌ها تکمیل شده استخراج می‌شوند؛ معاملات قدیمی فاقد این اطلاعات‌اند.", color = Tx.Gold, fontSize = 10.sp)
                }
            }
        }
        Reveal(6) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("Behavioral Lab · Revenge Trading")
                    Text(
                        "این بخش فقط الگوهای قابل مشاهده در ژورنال را نشان می‌دهد؛ امتیاز، احتمال روان‌شناختی یا تشخیص قطعی نیست.",
                        color = Tx.Muted, fontSize = 10.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatTile("اپیزودها", revengeEpisodes.size.toString(), Tx.Gold, Modifier.weight(1f))
                        StatTile("شواهد قوی", revengeEpisodes.count { it.evidence.classification == com.tradenex.app.data.RevengeClass.STRONG_EVIDENCE }.toString(), Tx.Red, Modifier.weight(1f))
                        StatTile("تگ دستی", revengeEpisodes.count { it.evidence.classification == com.tradenex.app.data.RevengeClass.USER_TAGGED }.toString(), Tx.Blue, Modifier.weight(1f))
                    }
                    if (revengeEpisodes.isEmpty()) {
                        EmptyAi("در معاملات این حساب، اپیزود قابل‌نمایشی با شواهد ثبت‌شده پیدا نشد. ورود سریع پس از ضرر به‌تنهایی برای برچسب Revenge کافی نیست.")
                    }
                    revengeEpisodes.take(20).forEach { episode ->
                        val previous = trades.firstOrNull { it.id == episode.previousLossTradeId }
                        val trigger = trades.firstOrNull { it.id == episode.triggerTradeId }
                        Column(
                            Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(12.dp)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when (episode.evidence.classification) {
                                        com.tradenex.app.data.RevengeClass.USER_TAGGED -> "با تگ دستی کاربر"
                                        com.tradenex.app.data.RevengeClass.STRONG_EVIDENCE -> "شواهد رفتاری قوی"
                                        com.tradenex.app.data.RevengeClass.POSSIBLE -> "الگوی احتمالی"
                                        com.tradenex.app.data.RevengeClass.LOSS_SEQUENCE_ONLY -> "توالی زیان"
                                    },
                                    color = if (episode.evidence.score >= 45) Tx.Red else Tx.Gold,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("Evidence ${episode.evidence.score}/100", color = Tx.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "معامله قبلی: ${previous?.symbol ?: "—"} · ${previous?.let { if (it.buy) "BUY" else "SELL" } ?: "—"} · ${previous?.let { money(it.pnl) } ?: "—"}",
                                color = Tx.Text, fontSize = 11.sp,
                            )
                            Text(
                                "ورود بعدی: ${trigger?.symbol ?: "—"} · ${trigger?.let { if (it.buy) "BUY" else "SELL" } ?: "—"} · ${trigger?.let { money(it.pnl) } ?: "—"}",
                                color = Tx.Text, fontSize = 11.sp,
                            )
                            Text(
                                "فاصله زمانی: ${episode.evidence.gapMinutes?.let { "$it دقیقه" } ?: "ثبت نشده"} · خالص دو معامله مرتبط: ${money(episode.actualNetPnl)}",
                                color = Tx.Muted, fontSize = 10.sp,
                            )
                            episode.evidence.reasons.forEach { reason ->
                                Text("• $reason", color = Tx.Muted, fontSize = 10.sp)
                            }
                            val review = AppState.episodeReview(account.id, episode.id)
                            Text(
                                "بازبینی شما: " + when (review) {
                                    com.tradenex.app.data.TxAiReviewStatus.CONFIRMED -> "تأیید شده"
                                    com.tradenex.app.data.TxAiReviewStatus.NOT_REVENGE -> "Revenge نیست"
                                    com.tradenex.app.data.TxAiReviewStatus.UNSURE -> "نامشخص"
                                    null -> "بازبینی نشده"
                                },
                                color = Tx.Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    AppState.reviewEpisode(
                                        account.id, episode.id,
                                        com.tradenex.app.data.TxAiReviewStatus.CONFIRMED,
                                    )
                                }) { Text("تأیید") }
                                TextButton(onClick = {
                                    AppState.reviewEpisode(
                                        account.id, episode.id,
                                        com.tradenex.app.data.TxAiReviewStatus.NOT_REVENGE,
                                    )
                                }) { Text("Revenge نیست") }
                                TextButton(onClick = {
                                    AppState.reviewEpisode(
                                        account.id, episode.id,
                                        com.tradenex.app.data.TxAiReviewStatus.UNSURE,
                                    )
                                }) { Text("نامشخص") }
                            }
                        }
                    }
                    Text(
                        "روش تشخیص: فاصله حداکثر ۱۵ دقیقه، افزایش ریسک/حجم بیش از ۲۵٪، تخلف صریح از پلن، تگ Revenge، نشانه‌های خشم/FOMO و توالی ضرر. داده ناقص امتیاز نمی‌گیرد. هر اپیزود فقط دو معامله مرتبط را نمایش می‌دهد و جمع آن‌ها برای محاسبه هزینه کل حساب تجمیع نشده تا زیان تکراری شمرده نشود.",
                        color = Tx.Muted, fontSize = 9.sp,
                    )
                }
            }
        }
        Reveal(7) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("سایر الگوهای رفتاری قابل مشاهده")
                    Text(
                        "این شاخص‌ها فقط شواهد ثبت‌شده در ژورنال را توصیف می‌کنند؛ برای نسبت‌دادن انگیزه یا تشخیص قطعی کافی نیستند.",
                        color = Tx.Muted, fontSize = 10.sp,
                    )
                    if (behavioralPatterns.isEmpty()) {
                        EmptyAi("برای این الگوها هنوز داده زمان، حجم یا قیمت کافی ثبت نشده است.")
                    }
                    behavioralPatterns.take(30).forEach { pattern ->
                        Column(
                            Modifier.fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pattern.type.label,
                                    color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("Evidence ${pattern.evidenceScore}/100", color = Tx.Gold, fontSize = 10.sp)
                            }
                            Text(
                                "معاملات مرتبط: ${pattern.linkedTradeIds.joinToString("، ") { "#$it" }} · خالص ثبت‌شده: ${money(pattern.actualNetPnl)}",
                                color = Tx.Text, fontSize = 10.sp,
                            )
                            pattern.reasons.forEach { reason ->
                                Text("• $reason", color = Tx.Muted, fontSize = 10.sp)
                            }
                            val review = AppState.episodeReview(account.id, pattern.id)
                            Text(
                                "بازبینی شما: " + when (review) {
                                    com.tradenex.app.data.TxAiReviewStatus.CONFIRMED -> "تأیید شده"
                                    com.tradenex.app.data.TxAiReviewStatus.NOT_REVENGE -> "رد شده"
                                    com.tradenex.app.data.TxAiReviewStatus.UNSURE -> "نامشخص"
                                    null -> "بازبینی نشده"
                                },
                                color = Tx.Muted, fontSize = 10.sp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    AppState.reviewEpisode(
                                        account.id, pattern.id,
                                        com.tradenex.app.data.TxAiReviewStatus.CONFIRMED,
                                    )
                                }) { Text("تأیید") }
                                TextButton(onClick = {
                                    AppState.reviewEpisode(
                                        account.id, pattern.id,
                                        com.tradenex.app.data.TxAiReviewStatus.NOT_REVENGE,
                                    )
                                }) { Text("مورد معتبر نیست") }
                                TextButton(onClick = {
                                    AppState.reviewEpisode(
                                        account.id, pattern.id,
                                        com.tradenex.app.data.TxAiReviewStatus.UNSURE,
                                    )
                                }) { Text("نامشخص") }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun AiSliceRow(slice: TxAiSlice, net: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(slice.label, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(slice.trades.toString() + " معامله · برد " + (slice.winRate * 100).toInt() + "% · میانگین " + String.format(java.util.Locale.US, "%.2f", slice.average), color = Tx.Muted, fontSize = 10.sp)
        }
        Text(net, color = if (slice.net >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreRow(label: String, score: Int?, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Tx.Text, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(score?.toString() ?: "—", color = score?.let { if (it >= 70) Tx.Green else Tx.Gold } ?: Tx.Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
            if (score != null) Box(Modifier.fillMaxWidth(score / 100f).height(6.dp).background(Tx.Gold, RoundedCornerShape(4.dp)))
        }
        Text(detail, color = Tx.Muted, fontSize = 9.sp)
    }
}

@Composable
private fun EmptyAi(text: String) { Text(text, color = Tx.Muted, fontSize = 11.sp) }

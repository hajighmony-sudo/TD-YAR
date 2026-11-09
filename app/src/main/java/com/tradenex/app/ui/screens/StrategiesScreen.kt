package com.tradenex.app.ui.screens

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Strategy
import com.tradenex.app.data.StrategyRevision
import com.tradenex.app.data.StrategyAttachment
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.theme.Tx

@Composable
fun StrategiesScreen(nav: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var ruleInput by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(emptyList<StrategyAttachment>()) }
    var editAttachments by remember { mutableStateOf(emptyList<StrategyAttachment>()) }
    var pickingForEdit by remember { mutableStateOf(false) }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val metadata = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        (if (nameIndex >= 0) cursor.getString(nameIndex) else null) to
                            (if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L)
                    } else null
                }
            }.getOrNull()
            val attachment = StrategyAttachment(
                id = System.currentTimeMillis(),
                uri = uri.toString(),
                mimeType = context.contentResolver.getType(uri) ?: "*/*",
                displayName = metadata?.first?.takeIf(String::isNotBlank) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "فایل پیوست",
                sizeBytes = metadata?.second ?: 0L,
            )
            if (pickingForEdit) editAttachments = editAttachments + attachment else attachments = attachments + attachment
        }
    }
    val rules = remember { mutableStateListOf<String>() }
    var editingStrategy by remember { mutableStateOf<Strategy?>(null) }
    var deletingStrategy by remember { mutableStateOf<Strategy?>(null) }
    var historyStrategy by remember { mutableStateOf<Strategy?>(null) }
    var editName by remember { mutableStateOf("") }
    var editRules by remember { mutableStateOf("") }
    var strategyQuery by remember { mutableStateOf("") }
    var includeInactive by remember { mutableStateOf(false) }
    val n = AppState.visibleTrades.size

    Page(title = "استراتژی‌های من", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("کدام استراتژی برایت کار می‌کند؟")
                    val nets = AppState.strategies.map { s -> s to AppState.visibleTrades.filter { it.strategy == s.name }.sumOf { it.pnl } }
                    val maxAbs = nets.maxOfOrNull { kotlin.math.abs(it.second) }?.coerceAtLeast(1.0) ?: 1.0
                    nets.forEach { (s, net) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, color = Tx.Text, fontSize = 11.sp, modifier = Modifier.width(96.dp))
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((kotlin.math.abs(net) / maxAbs).toFloat().coerceIn(0.03f, 1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (net >= 0) Tx.Green else Tx.Red),
                                )
                            }
                            Text(Analytics.money(net), color = if (net >= 0) Tx.Green else Tx.Red, fontSize = 11.sp, modifier = Modifier.width(78.dp).padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        TxField(strategyQuery, { strategyQuery = it }, "جست‌وجوی نام یا قوانین استراتژی")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StrategyFilterChip("فعال‌ها", !includeInactive) { includeInactive = false }
            StrategyFilterChip("همه استراتژی‌ها", includeInactive) { includeInactive = true }
        }
        val filteredStrategies = AppState.strategies.filter { strategy ->
            (includeInactive || strategy.active) &&
                (strategyQuery.isBlank() ||
                    strategy.name.contains(strategyQuery, ignoreCase = true) ||
                    strategy.rules.any { it.contains(strategyQuery, ignoreCase = true) } ||
                    strategy.attachments.any { it.displayName.contains(strategyQuery, ignoreCase = true) })
        }
        if (filteredStrategies.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("استراتژی‌ای با این فیلتر پیدا نشد.", color = Tx.Muted, modifier = Modifier.padding(14.dp))
            }
        }
        filteredStrategies.forEachIndexed { idx, s ->
            Reveal(idx + 1) {
                StrategyCard(s, n, onEdit = {
                    editingStrategy = s
                    editName = s.name
                    editRules = s.rules.joinToString("\n")
                    editAttachments = s.attachments
                }, onDelete = { deletingStrategy = s }, onHistory = { historyStrategy = s })
            }
        }

        Reveal(AppState.strategies.size + 1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = Tx.Gold)
                        Text("  استراتژی جدید", color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    TxField(name, { name = it }, "نام استراتژی (مثلاً بریک‌اوت لندن)")
                    Text("قانون‌های ورود (چک‌لیست قبل از هر معامله)", color = Tx.Muted, fontSize = 11.sp)
                    rules.forEachIndexed { i, r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = Tx.Green, modifier = Modifier.size(18.dp))
                            Text("  $r", color = Tx.Text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Delete, "حذف قانون", tint = Tx.Muted, modifier = Modifier.size(20.dp).clickable { rules.removeAt(i) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxField(ruleInput, { ruleInput = it }, "افزودن قانون...", Modifier.weight(1f))
                        IconBox(Icons.Rounded.Add, "افزودن قانون") {
                            if (ruleInput.isNotBlank()) {
                                rules.add(ruleInput.trim())
                                ruleInput = ""
                            }
                        }
                    }
                    Text("فایل‌های پیوست استراتژی", color = Tx.Muted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconBox(Icons.Rounded.Add, "افزودن فایل") {
                            pickingForEdit = false
                            attachmentPicker.launch(arrayOf("*/*"))
                        }
                        Text(attachments.size.toString() + " فایل انتخاب شده؛ فایل‌ها با URI نگهداری می‌شوند و در حافظه برنامه کپی نمی‌شوند.", color = Tx.Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    }
                    attachments.forEach { file ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(file.displayName, color = Tx.Text, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Delete, "حذف پیوست", tint = Tx.Muted, modifier = Modifier.size(18.dp).clickable { attachments = attachments.filterNot { it.id == file.id } })
                        }
                    }
                    GoldButton("ذخیره استراتژی") {
                        if (name.isNotBlank()) {
                            AppState.addStrategy(Strategy(System.currentTimeMillis(), name.trim(), rules.toList(), true, attachments = attachments.toList()))
                            name = ""
                            rules.clear()
                            attachments = emptyList()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    editingStrategy?.let { selected ->
        AlertDialog(
            onDismissRequest = { editingStrategy = null },
            containerColor = Tx.Dialog,
            title = { Text("ویرایش استراتژی", color = Tx.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxField(editName, { editName = it }, "نام استراتژی")
                    TxField(editRules, { editRules = it }, "قوانین را با خط جدید جدا کن")
                    Text("فایل‌های پیوست", color = Tx.Muted, fontSize = 11.sp)
                    IconBox(Icons.Rounded.Add, "افزودن فایل") {
                        pickingForEdit = true
                        attachmentPicker.launch(arrayOf("*/*"))
                    }
                    editAttachments.forEach { file ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(file.displayName, color = Tx.Text, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Delete, "حذف پیوست", tint = Tx.Muted, modifier = Modifier.size(18.dp).clickable { editAttachments = editAttachments.filterNot { it.id == file.id } })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val index = AppState.strategies.indexOfFirst { it.id == selected.id }
                    if (index >= 0 && editName.isNotBlank()) {
                        AppState.strategyRevisions.add(
                            StrategyRevision(
                                id = System.currentTimeMillis(),
                                strategyId = selected.id,
                                version = selected.version,
                                name = selected.name,
                                rules = selected.rules,
                                createdAt = System.currentTimeMillis(),
                                attachments = selected.attachments,
                            ),
                        )
                        AppState.strategies[index] = selected.copy(
                            name = editName.trim(),
                            rules = editRules.lines().map(String::trim).filter(String::isNotBlank),
                            version = selected.version + 1,
                            attachments = editAttachments.toList(),
                        )
                        AppState.persist()
                    }
                    editingStrategy = null
                }) { Text("ذخیره", color = Tx.Gold) }
            },
            dismissButton = {
                TextButton(onClick = { editingStrategy = null }) { Text("انصراف", color = Tx.Muted) }
            },
        )
    }

    historyStrategy?.let { selected ->
        val revisions = AppState.strategyRevisions.filter { it.strategyId == selected.id }.sortedByDescending { it.version }
        AlertDialog(
            onDismissRequest = { historyStrategy = null },
            containerColor = Tx.Dialog,
            title = { Text("تاریخچه «${selected.name}»", color = Tx.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نسخه فعلی: V${selected.version}", color = Tx.Gold, fontWeight = FontWeight.Bold)
                    if (revisions.isEmpty()) {
                        Text("هنوز نسخه قبلی برای این استراتژی ذخیره نشده است.", color = Tx.Muted)
                    } else {
                        revisions.forEach { revision ->
                            Column(
                                Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp)).padding(9.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text("V${revision.version} · ${revision.name}", color = Tx.Text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                revision.rules.forEach { rule -> Text("• $rule", color = Tx.Muted, fontSize = 10.sp) }
                                TextButton(onClick = {
                                    val index = AppState.strategies.indexOfFirst { it.id == selected.id }
                                    if (index >= 0) {
                                        val current = AppState.strategies[index]
                                        AppState.strategyRevisions.add(
                                            StrategyRevision(
                                                id = System.currentTimeMillis(),
                                                strategyId = current.id,
                                                version = current.version,
                                                name = current.name,
                                                rules = current.rules,
                                                createdAt = System.currentTimeMillis(),
                                                attachments = current.attachments,
                                            ),
                                        )
                                        AppState.strategies[index] = current.copy(
                                            name = revision.name,
                                            rules = revision.rules,
                                            version = current.version + 1,
                                            attachments = revision.attachments,
                                        )
                                        AppState.persist()
                                    }
                                    historyStrategy = null
                                }) { Text("بازیابی این نسخه", color = Tx.Gold, fontSize = 10.sp) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyStrategy = null }) { Text("بستن", color = Tx.Gold) } },
        )
    }

    deletingStrategy?.let { selected ->
        AlertDialog(
            onDismissRequest = { deletingStrategy = null },
            containerColor = Tx.Dialog,
            title = { Text("حذف استراتژی؟", color = Tx.Text) },
            text = { Text("استراتژی «${selected.name}» حذف شود؟ سوابق معاملات قبلی دست‌نخورده می‌مانند.", color = Tx.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    AppState.deleteStrategy(selected.id)
                    deletingStrategy = null
                }) { Text("حذف استراتژی", color = Tx.Red) }
            },
            dismissButton = { TextButton(onClick = { deletingStrategy = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
}

@Composable
private fun StrategyCard(s: Strategy, tradeCount: Int, onEdit: () -> Unit, onDelete: () -> Unit, onHistory: () -> Unit) {
    val visible = AppState.visibleTrades
    val mine = remember(s.name, visible) { visible.filter { it.strategy == s.name }.sortedWith(compareBy({ it.epochDay }, { it.id })) }
    val net = mine.sumOf { it.pnl }
    val win = if (mine.isEmpty()) 0.0 else mine.count { it.pnl > 0 }.toDouble() / mine.size
    val curve = remember(mine) {
        var eq = 0.0
        mine.takeLast(24).map { eq += it.pnl; eq }
    }
    val c = if (net >= 0) Tx.Green else Tx.Red
    GlassCard(Modifier.fillMaxWidth(), border = if (s.active) Tx.Gold.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.12f)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(s.name, color = if (s.active) Tx.Text else Tx.Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("نسخه V${s.version} · ${s.rules.size} قانون · ${s.attachments.size} فایل · ${mine.size} معامله", color = Tx.Muted, fontSize = 11.sp)
                Text(s.rules.take(2).joinToString(" · "), color = Tx.Muted, fontSize = 10.sp, maxLines = 1)
                TextButton(onClick = onHistory) {
                    Text("تاریخچه نسخه‌ها · ${AppState.strategyRevisions.count { it.strategyId == s.id }}", color = Tx.Gold, fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spark(curve, c, Modifier.width(72.dp).height(24.dp))
                Text(Analytics.money(net), color = c, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("برد ${(win * 100).toInt()}%", color = Tx.Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Rounded.Edit,
                    "ویرایش استراتژی",
                    tint = Tx.Gold,
                    modifier = Modifier.size(20.dp).clickable(onClick = onEdit),
                )
                SwitchMini(s.active) {
                    AppState.updateStrategy(s.id) { it.copy(active = !it.active) }
                }
                Icon(
                    Icons.Rounded.Delete,
                    "حذف استراتژی",
                    tint = Tx.Muted,
                    modifier = Modifier.size(22.dp).clickable(onClick = onDelete),
                )
            }
        }
    }
}

@Composable
private fun SwitchMini(on: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Color(0xFF14B888) else Color(0xFF4A4A52))
            .clickable(onClick = onToggle)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.White))
    }
}

@Composable
private fun Spark(values: List<Double>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = values.min()
        val hi = values.max()
        val span = (hi - lo).coerceAtLeast(1e-6)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - ((v - lo) / span).toFloat() * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}


@Composable
private fun StrategyFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Tx.Gold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) Tx.Gold else Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Tx.Gold else Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

package com.tradenex.app.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import com.tradenex.app.data.TxPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.zxing.integration.android.IntentIntegrator
import com.tradenex.app.data.EducationBlock
import com.tradenex.app.data.EducationBlockType
import com.tradenex.app.data.EducationLesson
import com.tradenex.app.data.EducationQr
import com.tradenex.app.data.EducationStore
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.io.File

@Composable
fun EducationScreen(nav: NavController) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf("همه") }
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var qrLesson by remember { mutableStateOf<EducationLesson?>(null) }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var message by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val lessonId = selected
            if (lessonId != null) {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val type = when {
                    mime.startsWith("image/") -> EducationBlockType.IMAGE
                    mime.startsWith("video/") -> EducationBlockType.VIDEO
                    else -> EducationBlockType.FILE
                }
                EducationStore.addBlock(lessonId, EducationBlock(type = type, uri = uri.toString(), mimeType = mime, displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "فایل پیوست"))
            }
        }
    }
    var txPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val txExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            busy = true; message = "در حال ساخت بسته‌ی .tx …"
            val pass = txPassword
            val snapshot = EducationStore.lessons.toList()
            val strategySnapshot = AppState.strategies.toList()
            val revisionSnapshot = AppState.strategyRevisions.toList()
            scope.launch {
                val r = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openOutputStream(uri)!!.use { TxPackage.export(context, snapshot, pass, it, strategySnapshot, revisionSnapshot) } }
                }
                busy = false
                message = r.fold(
                    { "بسته‌ی .tx ساخته شد: ${it.lessons} درس، ${it.strategies} استراتژی، ${it.revisions} نسخه، ${it.files} فایل" + if (it.skipped > 0) " (${it.skipped} فایل قابل خواندن نبود)" else "" },
                    { "ساخت بسته ممکن نشد: ${it.message}" },
                )
            }
        }
    }
    val txImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true; message = "در حال خواندن بسته‌ی .tx …"
            val pass = txPassword
            scope.launch {
                val r = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)!!.use { TxPackage.import(context, it, pass) } }
                }
                busy = false
                message = r.fold(
                    { "وارد شد: ${it.lessons} درس، ${it.strategies} استراتژی، ${it.revisions} نسخه، ${it.files} فایل" + if (it.skipped > 0) " (${it.skipped} مورد ناقص)" else "" },
                    { if (it is TxPackage.BadPackage) it.message ?: "فایل نامعتبر" else "ورود ممکن نشد: ${it.message}" },
                )
            }
        }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanned = IntentIntegrator.parseActivityResult(Activity.RESULT_OK, result.data)?.contents
            val decoded = runCatching { scanned?.let { EducationQr.decodePayload(it, txPassword) } }
            val imported = decoded.getOrNull()
            if (imported != null) {
                EducationStore.importTextLesson(imported.first, imported.second)
                message = "درس متنی با موفقیت دریافت شد."
            } else if (scanned != null) {
                message = decoded.exceptionOrNull()?.message ?: "این QR متعلق به یک درس TRADIX نیست یا قابل خواندن نیست."
            }
        }
    }

    val categories = listOf("همه", "متن", "عکس", "ویدیو", "فایل")
    val visible = EducationStore.lessons.filter { lesson ->
        val matchesType = filter == "همه" || lesson.blocks.any { block ->
            when (filter) {
                "متن" -> block.type == EducationBlockType.TEXT
                "عکس" -> block.type == EducationBlockType.IMAGE
                "ویدیو" -> block.type == EducationBlockType.VIDEO
                else -> block.type == EducationBlockType.FILE
            }
        }
        val matchesSearch = searchQuery.isBlank() ||
            lesson.title.contains(searchQuery, ignoreCase = true) ||
            lesson.category.contains(searchQuery, ignoreCase = true) ||
            lesson.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesType && matchesSearch && (!favoritesOnly || lesson.favorite)
    }

    Page(title = "آموزش و دانش", onBack = { nav.popBackStack() }) {
        SectionHero("LEARNING HUB / KNOWLEDGE", "مرکز دانش و آموزش", "درس‌ها، رسانه‌ها و یادداشت‌های آموزشی خودت را سازمان‌دهی کن.")
        Spacer(Modifier.height(8.dp))
        SectionHero("KNOWLEDGE BASE / ACADEMY", "آکادمی شخصی تو", "درس‌ها، متن‌ها، تصویرها، ویدیوها و فایل‌های آموزشی را دسته‌بندی و مرور کن.", Tx.Purple)
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("کتابخانه آموزشی", "درس‌ها، فایل‌ها و یادداشت‌های شخصی")
                    TxField(searchQuery, { searchQuery = it }, "جست‌وجوی عنوان، دسته یا برچسب")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { TxChip(it, filter == it, { filter = it }) }
                    }
                    TxChip("★ محبوب‌ها", favoritesOnly, { favoritesOnly = !favoritesOnly })
                    Text("بسته‌ی رمزنگاری‌شده‌ی .tx (آفلاین): درس‌ها، استراتژی‌ها، نسخه‌ها و فایل‌های پیوستشان را در یک فایل بگیر و برای دوستت بفرست؛ فقط TRADIX می‌تواند بازش کند. برای رمزنگاری واقعی، رمز عبور الزامی است.", color = Tx.Muted, fontSize = 10.sp)
                    TxField(txPassword, { txPassword = it }, "رمز مشترک برای .tx و QR (الزامی)")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxChip(if (busy) "…" else "خروجی .tx", false, { if (busy) Unit else if (txPassword.isBlank()) message = "برای رمزنگاری فایل .tx ابتدا رمز تعیین کن." else if (EducationStore.lessons.isNotEmpty() || AppState.strategies.isNotEmpty()) txExport.launch("td-yar-full-content.tx") else message = "درس یا استراتژی‌ای برای خروجی گرفتن نیست." })
                        TxChip("ورود .tx", false, { if (busy) Unit else if (txPassword.isBlank()) message = "برای بازکردن فایل .tx رمز آن را وارد کن." else txImport.launch(arrayOf("*/*")) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxChip("＋ ساخت درس", true, { creating = true })
                        TxChip("اسکن QR", false, {
                            val activity = context as? Activity
                            if (activity != null) {
                                val intent = IntentIntegrator(activity).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                    .setPrompt("QR درس TRADIX را اسکن کن").setBeepEnabled(false).createScanIntent()
                                scanner.launch(intent)
                            } else message = "اسکنر در این محیط در دسترس نیست."
                        })
                    }
                }
            }
        }

        visible.forEachIndexed { index, lesson ->
            Reveal(index + 1) {
                LessonRow(lesson, onOpen = { selected = if (selected == lesson.id) null else lesson.id }, onQr = {
                    if (txPassword.isBlank()) {
                        message = "برای ساخت QR رمزنگاری‌شده، ابتدا در بالای صفحه رمز تعیین کن."
                    } else runCatching {
                        qrLesson = lesson
                        qrBitmap = EducationQr.bitmap(EducationQr.encode(lesson, txPassword))
                    }.onFailure { message = it.message ?: "ساخت QR ممکن نشد." }
                }, onDelete = { EducationStore.delete(lesson.id); if (selected == lesson.id) selected = null })
                if (selected == lesson.id) {
                    LessonEditor(lesson, context, onRemove = { blockId -> EducationStore.removeBlock(lesson.id, blockId) }, onText = { text ->
                        if (text.isNotBlank()) EducationStore.addBlock(lesson.id, EducationBlock(type = EducationBlockType.TEXT, text = text.trim()))
                    }, onPick = { imagePicker.launch(arrayOf("image/*", "video/*", "application/pdf", "text/plain", "application/*")) },
                        onProgress = { EducationStore.updateProgress(lesson.id, it) })
                }
            }
        }
        if (visible.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("هنوز درسی ثبت نشده. اولین مبحث آموزشی خودت را بساز.", color = Tx.Muted, modifier = Modifier.padding(16.dp), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            containerColor = Tx.Dialog,
            title = { Text("ساخت مبحث آموزشی", color = Tx.Text) },
            text = { TxField(newTitle, { newTitle = it }, "عنوان مبحث") },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) selected = EducationStore.add(newTitle).id
                    newTitle = ""; creating = false
                }) { Text("ساخت", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
    if (qrLesson != null && qrBitmap != null) {
        AlertDialog(
            onDismissRequest = { qrLesson = null; qrBitmap = null },
            containerColor = Tx.Dialog,
            title = { Text("QR: ${qrLesson!!.title}", color = Tx.Text) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(qrBitmap!!.asImageBitmap(), contentDescription = "QR code for lesson", modifier = Modifier.size(240.dp).clip(RoundedCornerShape(8.dp)))
                    Text("این QR عنوان و متن‌های درس را رمزنگاری‌شده منتقل می‌کند. فایل‌های عکس، ویدیو و PDF را با خروجی .tx و همین رمز ارسال کن؛ فایل‌ها داخل QR جا نمی‌شوند.", color = Tx.Muted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val payload = EducationQr.encode(qrLesson!!, txPassword)
                        val dir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
                        val file = File(dir, "tradix_lesson_${qrLesson!!.id}.png")
                        file.outputStream().use { qrBitmap!!.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, "درس TRADIX: " + qrLesson!!.title + "\nکد واردسازی رمزنگاری‌شده: " + payload + "\nرمز را جداگانه و امن با گیرنده به اشتراک بگذار.")
                            clipData = ClipData.newUri(context.contentResolver, "درس TRADIX", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری QR رمزنگاری‌شده"))
                    }.onFailure { message = it.message ?: "اشتراک‌گذاری انجام نشد." }
                }) { Text("اشتراک‌گذاری QR رمزنگاری‌شده", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { qrLesson = null; qrBitmap = null }) { Text("بستن", color = Tx.Muted) } },
        )
    }
    if (message.isNotBlank()) {
        AlertDialog(onDismissRequest = { message = "" }, containerColor = Tx.Dialog,
            text = { Text(message, color = Tx.Text) },
            confirmButton = { TextButton(onClick = { message = "" }) { Text("باشه", color = Tx.Gold) } })
    }
}

@Composable
private fun LessonRow(lesson: EducationLesson, onOpen: () -> Unit, onQr: () -> Unit, onDelete: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(lesson.title, color = Tx.Text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("${lesson.category} · ${lesson.blocks.size} بخش آموزشی · ${lesson.blocks.count { it.type == EducationBlockType.TEXT }} متن", color = Tx.Muted, fontSize = 10.sp)
                    if (lesson.tags.isNotEmpty() || lesson.favorite) {
                        Text((if (lesson.favorite) "★ محبوب  " else "") + lesson.tags.joinToString(" · "), color = Tx.Gold, fontSize = 9.sp, maxLines = 1)
                    }
                }
                IconBox(Icons.Rounded.QrCode2, "ساخت و اشتراک QR", onQr)
                IconBox(Icons.Rounded.Delete, "حذف درس", onDelete)
            }
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth(lesson.progress.coerceIn(0f, 1f)).height(5.dp).background(Tx.Green))
            }
        }
    }
}

@Composable
private fun LessonEditor(lesson: EducationLesson, context: android.content.Context, onRemove: (Long) -> Unit, onText: (String) -> Unit, onPick: () -> Unit, onProgress: (Float) -> Unit) {
    var text by remember(lesson.id, lesson.blocks.size) { mutableStateOf("") }
    var category by remember(lesson.id) { mutableStateOf(lesson.category) }
    var tags by remember(lesson.id) { mutableStateOf(lesson.tags.joinToString(", ")) }
    var favorite by remember(lesson.id) { mutableStateOf(lesson.favorite) }
    var linkedStrategyId by remember(lesson.id) { mutableStateOf(lesson.linkedStrategyId) }
    GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("محتوای مبحث", "ترتیب متن و فایل‌ها حفظ می‌شود")
            Text("دسته‌بندی درس", color = Tx.Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("عمومی", "فارکس", "تحلیل تکنیکال", "استراتژی", "روان‌شناسی", "مدیریت ریسک").forEach {
                    TxChip(it, category == it, { category = it })
                }
            }
            TxField(tags, { tags = it }, "برچسب‌ها را با کاما جدا کن")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TxChip(if (favorite) "★ در محبوب‌ها" else "☆ افزودن به محبوب‌ها", favorite, { favorite = !favorite })
            }
            if (AppState.strategies.isNotEmpty()) {
                Text("اتصال به استراتژی", color = Tx.Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TxChip("بدون لینک", linkedStrategyId == null, { linkedStrategyId = null })
                    AppState.strategies.forEach { strategy ->
                        TxChip(strategy.name, linkedStrategyId == strategy.id, { linkedStrategyId = strategy.id })
                    }
                }
            }
            TextButton(onClick = {
                EducationStore.updateMeta(lesson.id, category, tags.split(",").map { it.trim() }, favorite, linkedStrategyId)
            }) { Text("ذخیره مشخصات درس", color = Tx.Gold) }
            lesson.blocks.forEach { block ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.035f))
                    .clickable(enabled = block.uri.isNotBlank()) {
                        runCatching {
                            val sourceUri = Uri.parse(block.uri)
                            val openUri = if (sourceUri.scheme == "file") {
                                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(sourceUri.path ?: error("مسیر فایل نامعتبر است")))
                            } else sourceUri
                            context.startActivity(Intent(Intent.ACTION_VIEW)
                                .setDataAndType(openUri, block.mimeType.ifBlank { "*/*" })
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        }
                    }.padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    val (icon, label) = when (block.type) {
                        EducationBlockType.TEXT -> Icons.Rounded.TextFields to block.text
                        EducationBlockType.IMAGE -> Icons.Rounded.Image to block.displayName
                        EducationBlockType.VIDEO -> Icons.Rounded.VideoLibrary to block.displayName
                        EducationBlockType.FILE -> Icons.Rounded.Description to block.displayName
                    }
                    androidx.compose.material3.Icon(icon, null, tint = Tx.Gold, modifier = Modifier.size(20.dp))
                    Text(label, color = Tx.Text, fontSize = 11.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    IconBox(Icons.Rounded.Delete, "حذف این بخش") { onRemove(block.id) }
                }
            }
            MultiLineEducationField(text, { text = it }, "متن آموزشی یا توضیح این فایل")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxChip("افزودن متن", false, { onText(text); text = "" })
                TxChip("افزودن عکس/ویدیو/فایل", true, onPick)
            }
            Text("میزان مطالعه", color = Tx.Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0f, .25f, .5f, .75f, 1f).forEach { value ->
                    TxChip("${(value * 100).toInt()}٪", lesson.progress == value, { onProgress(value) })
                }
            }
        }
    }
}

@Composable
private fun MultiLineEducationField(value: String, onValue: (String) -> Unit, hint: String) {
    val shape = RoundedCornerShape(13.dp)
    BasicTextField(
        value = value,
        onValueChange = onValue,
        textStyle = TextStyle(color = Tx.Text, fontSize = 13.sp),
        cursorBrush = SolidColor(Tx.Gold),
        modifier = Modifier.fillMaxWidth().height(112.dp).clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = Tx.Muted, fontSize = 12.sp)
                inner()
            }
        },
    )
}

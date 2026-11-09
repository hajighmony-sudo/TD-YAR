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
            scope.launch {
                val r = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openOutputStream(uri)!!.use { TxPackage.export(context, snapshot, pass, it) } }
                }
                busy = false
                message = r.fold(
                    { "بسته‌ی .tx ساخته شد: ${it.lessons} درس، ${it.files} فایل" + if (it.skipped > 0) " (${it.skipped} فایل قابل خواندن نبود)" else "" },
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
                    { "وارد شد: ${it.lessons} درس، ${it.files} فایل" + if (it.skipped > 0) " (${it.skipped} مورد ناقص)" else "" },
                    { if (it is TxPackage.BadPackage) it.message ?: "فایل نامعتبر" else "ورود ممکن نشد: ${it.message}" },
                )
            }
        }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanned = IntentIntegrator.parseActivityResult(Activity.RESULT_OK, result.data)?.contents
            val imported = runCatching { scanned?.let(EducationQr::decodePayload) }.getOrNull()
            if (imported != null) {
                EducationStore.importTextLesson(imported.first, imported.second)
                message = "درس متنی با موفقیت دریافت شد."
            } else if (scanned != null) message = "این QR متعلق به یک درس TRADIX نیست یا قابل خواندن نیست."
        }
    }

    val categories = listOf("همه", "متن", "عکس", "ویدیو", "فایل")
    val visible = EducationStore.lessons.filter { lesson ->
        filter == "همه" || lesson.blocks.any { block ->
            when (filter) {
                "متن" -> block.type == EducationBlockType.TEXT
                "عکس" -> block.type == EducationBlockType.IMAGE
                "ویدیو" -> block.type == EducationBlockType.VIDEO
                else -> block.type == EducationBlockType.FILE
            }
        }
    }

    Page(title = "آموزش و دانش", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("کتابخانه آموزشی", "درس‌ها، فایل‌ها و یادداشت‌های شخصی")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { TxChip(it, filter == it, { filter = it }) }
                    }
                    Text("بسته‌ی رمزنگاری‌شده‌ی .tx (آفلاین): همه‌ی درس‌ها و فایل‌هایشان را در یک فایل بگیر و برای دوستت بفرست؛ فقط TRADIX می‌تواند بازش کند. رمز اختیاری است ولی امنیت واقعی را همان می‌دهد.", color = Tx.Muted, fontSize = 10.sp)
                    TxField(txPassword, { txPassword = it }, "رمز بسته (اختیاری)")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxChip(if (busy) "…" else "خروجی .tx", false, { if (!busy && EducationStore.lessons.isNotEmpty()) txExport.launch("tradix-education.tx") else if (EducationStore.lessons.isEmpty()) message = "درسی برای خروجی گرفتن نیست." })
                        TxChip("ورود .tx", false, { if (!busy) txImport.launch(arrayOf("*/*")) })
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
                    runCatching {
                        qrLesson = lesson
                        qrBitmap = EducationQr.bitmap(EducationQr.encode(lesson))
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
                    Text("این QR عنوان و متن‌های درس را منتقل می‌کند. فایل‌های عکس، ویدیو و PDF باید همراه QR از طریق اشتراک‌گذاری فایل ارسال شوند.", color = Tx.Muted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val payload = EducationQr.encode(qrLesson!!)
                        val dir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
                        val file = File(dir, "tradix_lesson_${qrLesson!!.id}.png")
                        file.outputStream().use { qrBitmap!!.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val attachments = qrLesson!!.blocks.mapNotNull { it.uri.takeIf(String::isNotBlank)?.let(Uri::parse) }
                        val streams = arrayListOf(uri).apply { addAll(attachments) }
                        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "*/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams)
                            putExtra(Intent.EXTRA_TEXT, "درس TRADIX: ${qrLesson!!.title}\nکد واردسازی: $payload")
                            clipData = ClipData.newUri(context.contentResolver, "درس TRADIX", uri)
                            attachments.forEach { clipData?.addItem(ClipData.Item(it)) }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری درس TRADIX"))
                    }.onFailure { message = it.message ?: "اشتراک‌گذاری انجام نشد." }
                }) { Text("اشتراک‌گذاری QR و فایل‌ها", color = Tx.Gold) }
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
                    Text("${lesson.blocks.size} بخش آموزشی · ${lesson.blocks.count { it.type == EducationBlockType.TEXT }} متن", color = Tx.Muted, fontSize = 10.sp)
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
    GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("محتوای مبحث", "ترتیب متن و فایل‌ها حفظ می‌شود")
            lesson.blocks.forEach { block ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.035f))
                    .clickable(enabled = block.uri.isNotBlank()) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW)
                                .setDataAndType(Uri.parse(block.uri), block.mimeType.ifBlank { "*/*" })
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

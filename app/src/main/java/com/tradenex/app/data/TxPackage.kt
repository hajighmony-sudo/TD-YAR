package com.tradenex.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-encrypted .tx container for education lessons, strategy definitions,
 * strategy revision history and attached files. Media streams in bounded chunks;
 * the app never loads multi-gigabyte attachments into memory.
 */
object TxPackage {
    private val MAGIC = byteArrayOf('T'.code.toByte(), 'X'.code.toByte(), 'P'.code.toByte(), 'K'.code.toByte(), 1)
    private const val CHUNK = 1 shl 20
    private const val APP_SECRET = "TRADIX-TX-PACKAGE-v1"
    private const val ITER = 60_000

    class BadPackage(message: String) : Exception(message)

    data class Result(
        val lessons: Int,
        val files: Int,
        val skipped: Int,
        val strategies: Int = 0,
        val revisions: Int = 0,
    )

    private data class MediaEntry(val path: String, val uri: String)

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        require(password.isNotBlank()) { "برای ساخت یا بازکردن بسته .tx رمز عبور لازم است." }
        val spec = PBEKeySpec((APP_SECRET + "|" + password).toCharArray(), salt, ITER, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }

    private fun iv(base: ByteArray, counter: Long): ByteArray {
        val out = base.copyOf()
        val c = ByteBuffer.allocate(8).putLong(counter).array()
        for (i in 0 until 8) out[4 + i] = (out[4 + i].toInt() xor c[i].toInt()).toByte()
        return out
    }

    private class ChunkOut(private val sink: OutputStream, private val key: SecretKeySpec, private val baseIv: ByteArray) : FilterOutputStream(sink) {
        private val buf = ByteArray(CHUNK)
        private var n = 0
        private var counter = 0L

        override fun write(b: Int) {
            if (n == CHUNK) flushChunk()
            buf[n++] = b.toByte()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var left = len
            while (left > 0) {
                if (n == CHUNK) flushChunk()
                val count = minOf(left, CHUNK - n)
                System.arraycopy(b, offset, buf, n, count)
                n += count
                offset += count
                left -= count
            }
        }

        private fun flushChunk() {
            if (n == 0) return
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv(baseIv, counter++)))
            val encrypted = cipher.doFinal(buf, 0, n)
            sink.write(ByteBuffer.allocate(4).putInt(n).array())
            sink.write(encrypted)
            n = 0
        }

        override fun flush() { sink.flush() }
        override fun close() { flushChunk(); sink.flush(); sink.close() }
    }

    private class ChunkIn(private val src: InputStream, private val key: SecretKeySpec, private val baseIv: ByteArray) : FilterInputStream(src) {
        private var plain = ByteArray(0)
        private var pos = 0
        private var counter = 0L
        private var eof = false

        private fun readFully(buffer: ByteArray): Boolean {
            var got = 0
            while (got < buffer.size) {
                val count = src.read(buffer, got, buffer.size - got)
                if (count < 0) return false
                got += count
            }
            return true
        }

        private fun fill(): Boolean {
            if (eof) return false
            val header = ByteArray(4)
            var got = 0
            while (got < header.size) {
                val count = src.read(header, got, header.size - got)
                if (count < 0) {
                    if (got == 0) { eof = true; return false }
                    throw BadPackage("فایل ناقص است")
                }
                got += count
            }
            val length = ByteBuffer.wrap(header).int
            if (length <= 0 || length > CHUNK) throw BadPackage("ساختار فایل نامعتبر است")
            val encrypted = ByteArray(length + 16)
            if (!readFully(encrypted)) throw BadPackage("فایل ناقص است")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv(baseIv, counter++)))
            plain = try {
                cipher.doFinal(encrypted)
            } catch (_: Exception) {
                throw BadPackage("رمز اشتباه است یا فایل آسیب دیده")
            }
            pos = 0
            return true
        }

        override fun skip(n: Long): Long {
            var left = n
            val buffer = ByteArray(8192)
            while (left > 0) {
                val count = read(buffer, 0, minOf(left, buffer.size.toLong()).toInt())
                if (count < 0) break
                left -= count
            }
            return n - left
        }

        override fun available(): Int = plain.size - pos
        override fun markSupported(): Boolean = false

        override fun read(): Int {
            if (pos >= plain.size && !fill()) return -1
            return plain[pos++].toInt() and 0xFF
        }

        override fun read(buffer: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (pos >= plain.size && !fill()) return -1
            val count = minOf(len, plain.size - pos)
            System.arraycopy(plain, pos, buffer, off, count)
            pos += count
            return count
        }
    }

    fun export(
        context: Context,
        lessons: List<EducationLesson>,
        password: String,
        out: OutputStream,
        strategies: List<Strategy> = emptyList(),
        revisions: List<StrategyRevision> = emptyList(),
    ): Result {
        require(password.isNotBlank()) { "برای رمزنگاری بسته .tx رمز تعیین کن." }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val baseIv = ByteArray(12).also(random::nextBytes)
        out.write(MAGIC)
        out.write(salt)
        out.write(baseIv)

        val zip = ZipOutputStream(ChunkOut(out, key(password, salt), baseIv))
        zip.setLevel(0)
        val mediaPlan = ArrayList<MediaEntry>()
        val mediaByUri = HashMap<String, String>()

        fun registerMedia(namespace: String, ownerId: Long, attachmentId: Long, uri: String): String {
            if (uri.isBlank()) return ""
            return mediaByUri.getOrPut(uri) {
                val entry = "$namespace/${ownerId}_${attachmentId}_${mediaByUri.size}"
                mediaPlan.add(MediaEntry(entry, uri))
                entry
            }
        }

        val lessonRows = JSONArray()
        lessons.forEach { lesson ->
            lessonRows.put(JSONObject().apply {
                put("id", lesson.id)
                put("title", lesson.title)
                put("createdAt", lesson.createdAt)
                put("progress", lesson.progress.toDouble())
                put("category", lesson.category)
                put("tags", JSONArray(lesson.tags))
                put("favorite", lesson.favorite)
                lesson.linkedStrategyId?.let { put("linkedStrategyId", it) }
                put("blocks", JSONArray().apply {
                    lesson.blocks.forEach { block ->
                        val entry = if (block.type != EducationBlockType.TEXT) {
                            registerMedia("lesson_media", lesson.id, block.id, block.uri)
                        } else ""
                        put(JSONObject().apply {
                            put("id", block.id)
                            put("type", block.type.name)
                            put("text", block.text)
                            put("mimeType", block.mimeType)
                            put("displayName", block.displayName)
                            put("entry", entry)
                        })
                    }
                })
            })
        }

        fun attachmentRows(ownerId: Long, attachments: List<StrategyAttachment>): JSONArray =
            JSONArray().apply {
                attachments.forEach { attachment ->
                    val entry = registerMedia("strategy_media", ownerId, attachment.id, attachment.uri)
                    put(JSONObject().apply {
                        put("id", attachment.id)
                        put("mimeType", attachment.mimeType)
                        put("displayName", attachment.displayName)
                        put("sizeBytes", attachment.sizeBytes)
                        put("entry", entry)
                    })
                }
            }

        val strategyRows = JSONArray()
        strategies.forEach { strategy ->
            strategyRows.put(JSONObject().apply {
                put("id", strategy.id)
                put("name", strategy.name)
                put("rules", JSONArray(strategy.rules))
                put("active", strategy.active)
                put("version", strategy.version)
                put("attachments", attachmentRows(strategy.id, strategy.attachments))
            })
        }

        val revisionRows = JSONArray()
        revisions.forEach { revision ->
            revisionRows.put(JSONObject().apply {
                put("id", revision.id)
                put("strategyId", revision.strategyId)
                put("version", revision.version)
                put("name", revision.name)
                put("rules", JSONArray(revision.rules))
                put("createdAt", revision.createdAt)
                put("attachments", attachmentRows(revision.strategyId, revision.attachments))
            })
        }

        zip.putNextEntry(ZipEntry("manifest.json"))
        zip.write(
            JSONObject()
                .put("format", 2)
                .put("lessons", lessonRows)
                .put("strategies", strategyRows)
                .put("strategyRevisions", revisionRows)
                .toString()
                .toByteArray(Charsets.UTF_8),
        )
        zip.closeEntry()

        var files = 0
        var skipped = 0
        mediaPlan.forEach { media ->
            val input = runCatching { context.contentResolver.openInputStream(Uri.parse(media.uri)) }.getOrNull()
            if (input == null) {
                skipped++
                return@forEach
            }
            zip.putNextEntry(ZipEntry(media.path))
            input.use { it.copyTo(zip, 256 * 1024) }
            zip.closeEntry()
            files++
        }
        zip.close()
        return Result(lessons.size, files, skipped, strategies.size, revisions.size)
    }

    fun import(context: Context, input: InputStream, password: String): Result {
        require(password.isNotBlank()) { "برای بازکردن بسته .tx رمز آن را وارد کن." }
        val header = ByteArray(MAGIC.size + 16 + 12)
        var got = 0
        while (got < header.size) {
            val count = input.read(header, got, header.size - got)
            if (count < 0) throw BadPackage("این فایل TRADIX نیست")
            got += count
        }
        if (!header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw BadPackage("این فایل TRADIX نیست")
        val salt = header.copyOfRange(MAGIC.size, MAGIC.size + 16)
        val baseIv = header.copyOfRange(MAGIC.size + 16, header.size)
        val zip = ZipInputStream(ChunkIn(input, key(password, salt), baseIv))
        val directory = (context.getExternalFilesDir("education_media") ?: File(context.filesDir, "education_media")).also { it.mkdirs() }
        val saved = HashMap<String, String>()
        var manifest: JSONObject? = null
        var files = 0

        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.name == "manifest.json") {
                manifest = JSONObject(String(zip.readBytes(), Charsets.UTF_8))
            } else if (
                (entry.name.startsWith("media/") || entry.name.startsWith("lesson_media/") || entry.name.startsWith("strategy_media/")) &&
                !entry.name.contains("..") && !entry.name.startsWith("/")
            ) {
                val safeName = entry.name.substringAfterLast('/').replace('\\', '_')
                val target = File(directory, safeName + "_" + System.currentTimeMillis())
                target.outputStream().use { zip.copyTo(it, 256 * 1024) }
                saved[entry.name] = Uri.fromFile(target).toString()
                files++
            }
            zip.closeEntry()
        }

        val source = manifest ?: throw BadPackage("محتوای بسته خوانده نشد")
        val strategyRows = source.optJSONArray("strategies") ?: JSONArray()
        val strategyIdMap = HashMap<Long, Long>()
        val usedStrategyIds = AppState.strategies.map { it.id }.toMutableSet()
        for (i in 0 until strategyRows.length()) {
            val row = strategyRows.optJSONObject(i) ?: continue
            val oldId = row.optLong("id")
            if (row.optString("name").isBlank()) continue
            val newId = if (oldId > 0L && oldId !in usedStrategyIds) oldId else System.nanoTime() + i
            strategyIdMap[oldId] = newId
            usedStrategyIds.add(newId)
        }
        val lessonRows = source.optJSONArray("lessons") ?: JSONArray()
        var lessonCount = 0
        var skipped = 0
        for (i in 0 until lessonRows.length()) {
            val row = lessonRows.optJSONObject(i) ?: continue
            val blockRows = row.optJSONArray("blocks") ?: JSONArray()
            val blocks = (0 until blockRows.length()).mapNotNull { index ->
                val block = blockRows.optJSONObject(index) ?: return@mapNotNull null
                val entry = block.optString("entry")
                val type = runCatching { EducationBlockType.valueOf(block.optString("type")) }
                    .getOrDefault(EducationBlockType.TEXT)
                if (entry.isNotEmpty() && entry !in saved) { skipped++; return@mapNotNull null }
                if (type != EducationBlockType.TEXT && entry.isBlank()) { skipped++; return@mapNotNull null }
                EducationBlock(
                    id = System.nanoTime() + index,
                    type = type,
                    text = block.optString("text"),
                    uri = saved[entry].orEmpty(),
                    mimeType = block.optString("mimeType"),
                    displayName = block.optString("displayName"),
                )
            }
            val tags = row.optJSONArray("tags") ?: JSONArray()
            val oldLinkedId = row.optLong("linkedStrategyId").takeIf { row.has("linkedStrategyId") && it > 0L }
            EducationStore.importLesson(
                EducationLesson(
                    id = System.nanoTime() + i,
                    title = row.optString("title"),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                    blocks = blocks,
                    progress = row.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                    category = row.optString("category", "عمومی"),
                    tags = (0 until tags.length()).mapNotNull { tags.optString(it).takeIf(String::isNotBlank) },
                    favorite = row.optBoolean("favorite", false),
                    linkedStrategyId = oldLinkedId?.let { strategyIdMap[it] ?: it },
                ),
            )
            lessonCount++
        }

        var strategyCount = 0
        for (i in 0 until strategyRows.length()) {
            val row = strategyRows.optJSONObject(i) ?: continue
            val oldId = row.optLong("id")
            val newId = strategyIdMap[oldId] ?: continue
            val rules = row.optJSONArray("rules") ?: JSONArray()
            val attachmentRows = row.optJSONArray("attachments") ?: JSONArray()
            val attachments = (0 until attachmentRows.length()).mapNotNull { index ->
                val item = attachmentRows.optJSONObject(index) ?: return@mapNotNull null
                val entry = item.optString("entry")
                if (entry.isNotBlank() && entry !in saved) { skipped++; return@mapNotNull null }
                val uri = saved[entry].orEmpty()
                if (uri.isBlank()) return@mapNotNull null
                StrategyAttachment(
                    id = System.nanoTime() + index,
                    uri = uri,
                    mimeType = item.optString("mimeType", "*/*"),
                    displayName = item.optString("displayName", "فایل استراتژی"),
                    sizeBytes = item.optLong("sizeBytes", 0L).coerceAtLeast(0L),
                )
            }
            val name = row.optString("name").trim()
            if (name.isBlank()) continue
            AppState.strategies.add(
                0,
                Strategy(
                    id = newId,
                    name = name,
                    rules = (0 until rules.length()).mapNotNull { rules.optString(it).takeIf(String::isNotBlank) },
                    active = row.optBoolean("active", true),
                    version = row.optInt("version", 1).coerceAtLeast(1),
                    attachments = attachments,
                ),
            )
            strategyCount++
        }

        val revisionRows = source.optJSONArray("strategyRevisions") ?: JSONArray()
        var revisionCount = 0
        for (i in 0 until revisionRows.length()) {
            val row = revisionRows.optJSONObject(i) ?: continue
            val mappedStrategyId = strategyIdMap[row.optLong("strategyId")] ?: continue
            val rules = row.optJSONArray("rules") ?: JSONArray()
            val attachmentRows = row.optJSONArray("attachments") ?: JSONArray()
            val attachments = (0 until attachmentRows.length()).mapNotNull { index ->
                val item = attachmentRows.optJSONObject(index) ?: return@mapNotNull null
                val entry = item.optString("entry")
                if (entry.isNotBlank() && entry !in saved) { skipped++; return@mapNotNull null }
                val uri = saved[entry].orEmpty()
                if (uri.isBlank()) return@mapNotNull null
                StrategyAttachment(
                    id = System.nanoTime() + index,
                    uri = uri,
                    mimeType = item.optString("mimeType", "*/*"),
                    displayName = item.optString("displayName", "فایل نسخه استراتژی"),
                    sizeBytes = item.optLong("sizeBytes", 0L).coerceAtLeast(0L),
                )
            }
            AppState.strategyRevisions.add(
                StrategyRevision(
                    id = System.nanoTime() + i,
                    strategyId = mappedStrategyId,
                    version = row.optInt("version", 1).coerceAtLeast(1),
                    name = row.optString("name"),
                    rules = (0 until rules.length()).mapNotNull { rules.optString(it).takeIf(String::isNotBlank) },
                    createdAt = row.optLong("createdAt"),
                    attachments = attachments,
                ),
            )
            revisionCount++
        }
        AppState.persist()
        return Result(lessonCount, files, skipped, strategyCount, revisionCount)
    }
}

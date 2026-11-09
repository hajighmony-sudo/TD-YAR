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
 * ".tx" files: an encrypted container for education lessons (text + attached files) that only TRADIX can read.
 *
 * Layout: MAGIC(5) | salt(16) | baseIv(12) | repeated { plainLen:int32 | AES-256-GCM(chunk) }.
 * Every chunk (1 MiB of a ZIP stream) is authenticated separately with IV = baseIv XOR chunkCounter,
 * so multi-GB packages stream with constant memory and tampering/truncation is detected.
 *
 * Security note: without a password the key derives from a constant built into the app, which keeps the file
 * unreadable to other apps/humans but is obfuscation, not secrecy. With a password it is real encryption.
 */
object TxPackage {
    private val MAGIC = byteArrayOf('T'.code.toByte(), 'X'.code.toByte(), 'P'.code.toByte(), 'K'.code.toByte(), 1)
    private const val CHUNK = 1 shl 20
    private const val APP_SECRET = "TRADIX-TX-PACKAGE-v1"
    private const val ITER = 60_000

    class BadPackage(message: String) : Exception(message)

    data class Result(val lessons: Int, val files: Int, val skipped: Int)

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
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

        override fun write(b: Int) { if (n == CHUNK) flushChunk(); buf[n++] = b.toByte() }
        override fun write(b: ByteArray, off: Int, len: Int) {
            var o = off; var left = len
            while (left > 0) {
                if (n == CHUNK) flushChunk()
                val c = minOf(left, CHUNK - n)
                System.arraycopy(b, o, buf, n, c)
                n += c; o += c; left -= c
            }
        }

        private fun flushChunk() {
            if (n == 0) return
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv(baseIv, counter++)))
            val enc = cipher.doFinal(buf, 0, n)
            sink.write(ByteBuffer.allocate(4).putInt(n).array())
            sink.write(enc)
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

        private fun readFully(b: ByteArray, len: Int): Boolean {
            var got = 0
            while (got < len) {
                val r = src.read(b, got, len - got)
                if (r < 0) return false
                got += r
            }
            return true
        }

        private fun fill(): Boolean {
            if (eof) return false
            val head = ByteArray(4)
            var got = 0
            while (got < 4) {
                val r = src.read(head, got, 4 - got)
                if (r < 0) { if (got == 0) { eof = true; return false } else throw BadPackage("فایل ناقص است") }
                got += r
            }
            val len = ByteBuffer.wrap(head).int
            if (len <= 0 || len > CHUNK) throw BadPackage("ساختار فایل نامعتبر است")
            val enc = ByteArray(len + 16)
            if (!readFully(enc, enc.size)) throw BadPackage("فایل ناقص است")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv(baseIv, counter++)))
            plain = try { cipher.doFinal(enc) } catch (e: Exception) { throw BadPackage("رمز اشتباه است یا فایل آسیب دیده") }
            pos = 0
            return true
        }

        override fun skip(n: Long): Long {
            var left = n
            val sink = ByteArray(8192)
            while (left > 0) {
                val r = read(sink, 0, minOf(left, sink.size.toLong()).toInt())
                if (r < 0) break
                left -= r
            }
            return n - left
        }

        override fun available(): Int = plain.size - pos
        override fun markSupported(): Boolean = false

        override fun read(): Int {
            if (pos >= plain.size && !fill()) return -1
            return plain[pos++].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (pos >= plain.size && !fill()) return -1
            val c = minOf(len, plain.size - pos)
            System.arraycopy(plain, pos, b, off, c)
            pos += c
            return c
        }
    }

    /** Writes every lesson (and the files attached to it) into [out] as a .tx package. */
    fun export(context: Context, lessons: List<EducationLesson>, password: String, out: OutputStream): Result {
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val baseIv = ByteArray(12).also(random::nextBytes)
        out.write(MAGIC); out.write(salt); out.write(baseIv)
        val zip = ZipOutputStream(ChunkOut(out, key(password, salt), baseIv))
        zip.setLevel(0) // media is already compressed; keeps multi-GB exports fast

        val manifest = JSONArray()
        var files = 0
        var skipped = 0
        val mediaPlan = ArrayList<Pair<String, EducationBlock>>()
        lessons.forEach { lesson ->
            manifest.put(JSONObject().apply {
                put("id", lesson.id); put("title", lesson.title); put("createdAt", lesson.createdAt)
                put("blocks", JSONArray().apply {
                    lesson.blocks.forEach { b ->
                        val entry = if (b.type != EducationBlockType.TEXT && b.uri.isNotBlank()) "media/${lesson.id}_${b.id}" else ""
                        if (entry.isNotEmpty()) mediaPlan.add(entry to b)
                        put(JSONObject().apply {
                            put("id", b.id); put("type", b.type.name); put("text", b.text)
                            put("mimeType", b.mimeType); put("displayName", b.displayName); put("entry", entry)
                        })
                    }
                })
            })
        }
        zip.putNextEntry(ZipEntry("manifest.json"))
        zip.write(JSONObject().put("format", 1).put("lessons", manifest).toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        val missing = HashSet<String>()
        mediaPlan.forEach { (entry, b) ->
            val input = runCatching { context.contentResolver.openInputStream(Uri.parse(b.uri)) }.getOrNull()
            if (input == null) { skipped++; missing.add(entry); return@forEach }
            zip.putNextEntry(ZipEntry(entry))
            input.use { it.copyTo(zip, 256 * 1024) }
            zip.closeEntry()
            files++
        }
        zip.close()
        return Result(lessons.size, files, skipped)
    }

    /** Reads a .tx package; attached files are copied into app storage and lessons are added to [EducationStore]. */
    fun import(context: Context, input: InputStream, password: String): Result {
        val head = ByteArray(MAGIC.size + 16 + 12)
        var got = 0
        while (got < head.size) {
            val r = input.read(head, got, head.size - got)
            if (r < 0) throw BadPackage("این فایل TRADIX نیست")
            got += r
        }
        if (!head.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw BadPackage("این فایل TRADIX نیست")
        val salt = head.copyOfRange(MAGIC.size, MAGIC.size + 16)
        val baseIv = head.copyOfRange(MAGIC.size + 16, head.size)
        val zip = ZipInputStream(ChunkIn(input, key(password, salt), baseIv))
        val dir = File(context.filesDir, "education_media").also { it.mkdirs() }
        val saved = HashMap<String, String>() // entry -> file uri
        var manifest: JSONObject? = null
        var files = 0
        while (true) {
            val e = zip.nextEntry ?: break
            if (e.name == "manifest.json") {
                manifest = JSONObject(String(zip.readBytes(), Charsets.UTF_8))
            } else if (e.name.startsWith("media/") && !e.name.contains("..")) {
                val target = File(dir, e.name.removePrefix("media/").replace('/', '_') + "_" + System.currentTimeMillis())
                target.outputStream().use { zip.copyTo(it, 256 * 1024) }
                saved[e.name] = Uri.fromFile(target).toString()
                files++
            }
            zip.closeEntry()
        }
        val m = manifest ?: throw BadPackage("محتوای بسته خوانده نشد")
        val rows = m.getJSONArray("lessons")
        var count = 0
        var skipped = 0
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val blocksJson = row.optJSONArray("blocks") ?: JSONArray()
            val blocks = (0 until blocksJson.length()).mapNotNull { k ->
                val b = blocksJson.getJSONObject(k)
                val entry = b.optString("entry")
                val type = runCatching { EducationBlockType.valueOf(b.optString("type")) }.getOrDefault(EducationBlockType.TEXT)
                if (entry.isNotEmpty() && entry !in saved) { skipped++; return@mapNotNull null }
                EducationBlock(
                    id = System.nanoTime() + k, type = type, text = b.optString("text"),
                    uri = saved[entry].orEmpty(), mimeType = b.optString("mimeType"), displayName = b.optString("displayName"),
                )
            }
            EducationStore.importTextLesson(row.optString("title"), blocks)
            count++
        }
        return Result(count, files, skipped)
    }
}

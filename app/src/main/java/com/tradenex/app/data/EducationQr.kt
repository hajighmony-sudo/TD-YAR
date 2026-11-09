package com.tradenex.app.data

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EducationQr {
    private const val LEGACY_PREFIX = "TRADIX-LESSON-1:"
    private const val ENCRYPTED_PREFIX = "TRADIX-LESSON-2:"
    private const val ITERATIONS = 120_000
    private const val MAX_PAYLOAD_BYTES = 2800
    private const val MAX_TEXT_BYTES = 48 * 1024

    private fun lessonJson(lesson: EducationLesson): ByteArray = JSONObject().apply {
        put("title", lesson.title)
        put("blocks", JSONArray().apply {
            lesson.blocks.filter { it.type == EducationBlockType.TEXT && it.text.isNotBlank() }.forEach {
                put(JSONObject().put("type", "TEXT").put("text", it.text))
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_TEXT_BYTES) { "متن QR بیش از حد بزرگ است؛ از بسته .tx استفاده کن." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }

    fun encode(lesson: EducationLesson, password: String): String {
        require(password.isNotBlank()) { "برای رمزنگاری QR باید رمز تعیین کنی." }
        val rawText = lessonJson(lesson)
        require(rawText.size <= MAX_TEXT_BYTES) { "متن درس برای QR بیش از حد طولانی است؛ از بسته .tx استفاده کن." }
        val compressed = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { it.write(rawText) }
            out.toByteArray()
        }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(compressed)
        val payloadBytes = ByteArrayOutputStream().use { out ->
            out.write(salt)
            out.write(iv)
            out.write(encrypted)
            out.toByteArray()
        }
        val payload = ENCRYPTED_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            "متن درس برای یک QR بیش از حد طولانی است؛ آن را به چند درس کوتاه‌تر تقسیم کن یا از فایل .tx استفاده کن."
        }
        return payload
    }

    fun bitmap(payload: String, size: Int = 900): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
    }

    fun decode(bitmap: Bitmap, password: String = ""): Pair<String, List<EducationBlock>>? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        return decodePayload(result, password)
    }

    fun decodePayload(result: String, password: String = ""): Pair<String, List<EducationBlock>>? {
        val jsonBytes = when {
            result.startsWith(ENCRYPTED_PREFIX) -> {
                require(password.isNotBlank()) { "برای بازکردن QR رمز آن را وارد کن." }
                val packed = Base64.getUrlDecoder().decode(result.removePrefix(ENCRYPTED_PREFIX))
                require(packed.size > 44) { "QR رمزنگاری‌شده ناقص است." }
                val salt = packed.copyOfRange(0, 16)
                val iv = packed.copyOfRange(16, 28)
                val encrypted = packed.copyOfRange(28, packed.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
                val compressed = try {
                    cipher.doFinal(encrypted)
                } catch (_: Exception) {
                    throw IllegalArgumentException("رمز QR اشتباه است یا داده آسیب دیده است.")
                }
                GZIPInputStream(ByteArrayInputStream(compressed)).use(::readLimited)
            }
            result.startsWith(LEGACY_PREFIX) -> {
                val compressed = Base64.getUrlDecoder().decode(result.removePrefix(LEGACY_PREFIX))
                GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
            }
            else -> return null
        }
        val json = JSONObject(jsonBytes.toString(Charsets.UTF_8))
        val array = json.optJSONArray("blocks") ?: JSONArray()
        val blocks = (0 until array.length()).mapNotNull { i ->
            val row = array.optJSONObject(i) ?: return@mapNotNull null
            EducationBlock(type = EducationBlockType.TEXT, text = row.optString("text"))
        }
        return json.optString("title", "درس واردشده") to blocks
    }
}

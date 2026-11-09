package com.tradenex.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
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
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object EducationQr {
    private const val PREFIX = "TRADIX-LESSON-1:"

    fun encode(lesson: EducationLesson): String {
        val json = JSONObject().apply {
            put("title", lesson.title)
            put("blocks", JSONArray().apply {
                lesson.blocks.filter { it.type == EducationBlockType.TEXT && it.text.isNotBlank() }.forEach {
                    put(JSONObject().put("type", "TEXT").put("text", it.text))
                }
            })
        }.toString().toByteArray(Charsets.UTF_8)
        val compressed = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { it.write(json) }
            out.toByteArray()
        }
        val payload = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        require(payload.toByteArray(Charsets.UTF_8).size <= 2800) { "متن درس برای یک QR بیش از حد طولانی است؛ آن را به چند درس تقسیم کن." }
        return payload
    }

    fun bitmap(payload: String, size: Int = 900): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
            for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }

    fun decode(bitmap: Bitmap): Pair<String, List<EducationBlock>>? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        return decodePayload(result)
    }

    fun decodePayload(result: String): Pair<String, List<EducationBlock>>? {
        if (!result.startsWith(PREFIX)) return null
        val compressed = Base64.getUrlDecoder().decode(result.removePrefix(PREFIX))
        val bytes = GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val array = json.optJSONArray("blocks") ?: JSONArray()
        val blocks = (0 until array.length()).mapNotNull { i ->
            val row = array.optJSONObject(i) ?: return@mapNotNull null
            EducationBlock(type = EducationBlockType.TEXT, text = row.optString("text"))
        }
        return json.optString("title", "درس واردشده") to blocks
    }
}

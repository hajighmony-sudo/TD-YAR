package com.tradenex.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

enum class EducationBlockType { TEXT, IMAGE, VIDEO, FILE }

data class EducationBlock(
    val id: Long = System.currentTimeMillis(),
    val type: EducationBlockType,
    val text: String = "",
    val uri: String = "",
    val mimeType: String = "",
    val displayName: String = "",
)

data class EducationLesson(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val blocks: List<EducationBlock> = emptyList(),
    val progress: Float = 0f,
    val category: String = "عمومی",
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val linkedStrategyId: Long? = null,
)

object EducationStore {
    val lessons = androidx.compose.runtime.mutableStateListOf<EducationLesson>()
    private var prefs: android.content.SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("tradix_education_v1", Context.MODE_PRIVATE)
        val raw = prefs?.getString("lessons", null) ?: return
        runCatching {
            val rows = JSONArray(raw)
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val blocks = row.optJSONArray("blocks") ?: JSONArray()
                val parsed = (0 until blocks.length()).mapNotNull { index ->
                    val b = blocks.optJSONObject(index) ?: return@mapNotNull null
                    val type = runCatching { EducationBlockType.valueOf(b.optString("type")) }.getOrDefault(EducationBlockType.TEXT)
                    EducationBlock(
                        id = b.optLong("id", System.currentTimeMillis() + index),
                        type = type,
                        text = b.optString("text"),
                        uri = b.optString("uri"),
                        mimeType = b.optString("mimeType"),
                        displayName = b.optString("displayName"),
                    )
                }
                val tagRows = row.optJSONArray("tags") ?: JSONArray()
                lessons.add(
                    EducationLesson(
                        row.optLong("id"), row.optString("title"), row.optLong("createdAt"), parsed,
                        row.optDouble("progress").toFloat(), row.optString("category", "عمومی"),
                        (0 until tagRows.length()).mapNotNull { tagRows.optString(it).takeIf(String::isNotBlank) },
                        row.optBoolean("favorite", false),
                        row.optLong("linkedStrategyId").takeIf { row.has("linkedStrategyId") && it > 0L },
                    ),
                )
            }
        }
    }

    fun migrateLegacy(legacy: List<Lesson>) {
        if (lessons.isNotEmpty()) return
        legacy.forEach { old ->
            val kindText = old.kind.name.contains("TEXT")
            val blocks = if (kindText) listOf(EducationBlock(type = EducationBlockType.TEXT, text = "")) else emptyList()
            lessons.add(EducationLesson(old.id, old.title, old.id, blocks, old.progress))
        }
        if (lessons.isNotEmpty()) persist()
    }

    fun add(title: String): EducationLesson {
        val lesson = EducationLesson(System.currentTimeMillis(), title.trim(), System.currentTimeMillis())
        lessons.add(0, lesson)
        persist()
        return lesson
    }

    fun addBlock(lessonId: Long, block: EducationBlock) {
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index < 0) return
        lessons[index] = lessons[index].copy(blocks = lessons[index].blocks + block)
        persist()
    }

    fun removeBlock(lessonId: Long, blockId: Long) {
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index >= 0) { lessons[index] = lessons[index].copy(blocks = lessons[index].blocks.filterNot { it.id == blockId }); persist() }
    }

    fun updateMeta(lessonId: Long, category: String, tags: List<String>, favorite: Boolean, linkedStrategyId: Long?) {
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index < 0) return
        lessons[index] = lessons[index].copy(
            category = category.trim().ifBlank { "عمومی" },
            tags = tags.map { it.trim() }.filter(String::isNotBlank).distinct().take(20),
            favorite = favorite,
            linkedStrategyId = linkedStrategyId,
        )
        persist()
    }

    fun updateProgress(lessonId: Long, progress: Float) {
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index >= 0) { lessons[index] = lessons[index].copy(progress = progress.coerceIn(0f, 1f)); persist() }
    }

    fun delete(lessonId: Long) {
        lessons.removeAll { it.id == lessonId }
        persist()
    }

    fun importTextLesson(title: String, blocks: List<EducationBlock>) {
        importLesson(EducationLesson(System.currentTimeMillis(), title, System.currentTimeMillis(), blocks))
    }

    fun importLesson(lesson: EducationLesson) {
        val imported = lesson.copy(
            id = System.currentTimeMillis(),
            blocks = lesson.blocks.mapIndexed { index, block -> block.copy(id = System.nanoTime() + index) },
        )
        lessons.add(0, imported)
        persist()
    }

    fun persist() {
        val rows = JSONArray()
        lessons.forEach { lesson ->
            rows.put(JSONObject().apply {
                put("id", lesson.id); put("title", lesson.title); put("createdAt", lesson.createdAt); put("progress", lesson.progress)
                put("category", lesson.category); put("tags", JSONArray(lesson.tags)); put("favorite", lesson.favorite)
                lesson.linkedStrategyId?.let { put("linkedStrategyId", it) }
                put("blocks", JSONArray().apply {
                    lesson.blocks.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id); put("type", b.type.name); put("text", b.text)
                            put("uri", b.uri); put("mimeType", b.mimeType); put("displayName", b.displayName)
                        })
                    }
                })
            })
        }
        prefs?.edit()?.putString("lessons", rows.toString())?.apply()
    }
}

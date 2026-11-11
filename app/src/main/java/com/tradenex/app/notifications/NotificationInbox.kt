package com.tradenex.app.notifications

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class NotifyCategory(val key: String, val label: String) {
    JOURNAL("journal", "ژورنال"), REMINDER("reminder", "یادآوری"), NEWS("news", "اخبار اقتصادی"),
    STRATEGY("strategy", "استراتژی"), REPORT("report", "گزارش"),
    SESSION("session", "سشن‌ها"), BACKUP("backup", "پشتیبان‌گیری")
}
data class InboxNotification(
    val id: Int, val category: NotifyCategory, val title: String, val body: String,
    val createdAt: Long, val route: String, val read: Boolean = false, val done: Boolean = false
)
object NotificationInbox {
    private const val PREFS = "nex_notify_inbox"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 100

    @Synchronized fun all(context: Context): List<InboxNotification> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val category = NotifyCategory.entries.firstOrNull { it.key == o.optString("category") } ?: NotifyCategory.JOURNAL
                InboxNotification(o.optInt("id"), category, o.optString("title"), o.optString("body"),
                    o.optLong("createdAt"), o.optString("route", "home"), o.optBoolean("read"), o.optBoolean("done"))
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }
    @Synchronized fun add(context: Context, item: InboxNotification) =
        save(context, (listOf(item) + all(context).filterNot { it.id == item.id }).take(MAX_ITEMS))
    @Synchronized fun markRead(context: Context, id: Int) = update(context, id) { it.copy(read = true) }
    @Synchronized fun markDone(context: Context, id: Int) = update(context, id) { it.copy(read = true, done = true) }
    @Synchronized fun markAllRead(context: Context) = save(context, all(context).map { it.copy(read = true) })
    @Synchronized fun clear(context: Context) = save(context, emptyList())

    private fun update(context: Context, id: Int, transform: (InboxNotification) -> InboxNotification) {
        save(context, all(context).map { if (it.id == id) transform(it) else it })
    }
    private fun save(context: Context, items: List<InboxNotification>) {
        val array = JSONArray()
        items.forEach { item -> array.put(JSONObject().apply {
            put("id", item.id); put("category", item.category.key); put("title", item.title)
            put("body", item.body); put("createdAt", item.createdAt); put("route", item.route)
            put("read", item.read); put("done", item.done)
        }) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply()
    }
}

package com.tradenex.app.ui.theme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class UserThemePreset(
    val id: String,
    val name: String,
    val themeKey: String,
    val accent: Int,
    val glow: Int,
    val particles: Boolean,
    val shimmer: Boolean,
    val crown: Boolean,
)

object ThemePresetStore {
    private const val PREFS = "tradenix_custom_themes"
    private const val KEY = "presets"

    fun load(context: Context): List<UserThemePreset> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val rows = JSONArray(raw)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val name = row.optString("name").trim()
                if (name.isBlank()) return@mapNotNull null
                UserThemePreset(
                    id = row.optString("id"),
                    name = name,
                    themeKey = row.optString("themeKey", ThemeId.DEFAULT.key),
                    accent = row.optInt("accent", 0).coerceIn(0, 2),
                    glow = row.optInt("glow", 2).coerceIn(0, 3),
                    particles = row.optBoolean("particles", true),
                    shimmer = row.optBoolean("shimmer", true),
                    crown = row.optBoolean("crown", true),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, preset: UserThemePreset): List<UserThemePreset> {
        val next = (load(context).filterNot { it.id == preset.id } + preset).takeLast(20)
        persist(context, next)
        return next
    }

    fun delete(context: Context, id: String): List<UserThemePreset> {
        val next = load(context).filterNot { it.id == id }
        persist(context, next)
        return next
    }

    private fun persist(context: Context, presets: List<UserThemePreset>) {
        val rows = JSONArray()
        presets.forEach { preset ->
            rows.put(JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("themeKey", preset.themeKey)
                put("accent", preset.accent)
                put("glow", preset.glow)
                put("particles", preset.particles)
                put("shimmer", preset.shimmer)
                put("crown", preset.crown)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, rows.toString()).apply()
    }

    fun apply(preset: UserThemePreset) {
        val theme = ThemeId.values().firstOrNull { it.key == preset.themeKey && (it != ThemeId.GOLD || ThemeController.goldAvailable) }
            ?: ThemeId.DEFAULT
        ThemeController.select(theme)
        ThemeController.setAccent(preset.accent)
        ThemeController.setGlow(preset.glow)
        ThemeController.setParticles(preset.particles)
        ThemeController.setShimmer(preset.shimmer)
        ThemeController.setCrown(preset.crown)
    }
}

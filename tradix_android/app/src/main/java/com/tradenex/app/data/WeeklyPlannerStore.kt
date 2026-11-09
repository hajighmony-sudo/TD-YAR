package com.tradenex.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class DayPlan(
    val date: String,
    val tradeCount: Int = 0,
    val totalProfit: Double = 0.0,
    val totalLoss: Double = 0.0,
    val dailyGoal: String = "",
    val maxRiskPercent: Double = 1.0,
    val notes: String = "",
    val noTradeReason: String = "",
    val mood: Int = 3,
    val followedPlan: Boolean = true,
    val confirmed: Boolean = false,
    val doneTasks: Set<String> = emptySet(),
)

object WeeklyPlannerStore {
    val plans = mutableStateMapOf<String, DayPlan>()
    private var preferences: android.content.SharedPreferences? = null

    val checklist = listOf(
        "بررسی تقویم اقتصادی و اخبار مهم",
        "تعیین جهت و سناریوی بازار قبل از ورود",
        "مشخص‌کردن حد ضرر و ریسک هر معامله",
        "رعایت سقف ریسک روزانه",
        "ثبت دلیل ورود و خروج معاملات",
        "مرور عملکرد و احساسات پایان روز",
    )
    val noTradeReasons = listOf(
        "ستاپ معتبر وجود نداشت",
        "اخبار پرریسک یا نوسان غیرعادی",
        "به سقف ریسک روزانه رسیدم",
        "خارج از ساعت معاملاتی بودم",
        "شرایط روانی یا تمرکز مناسب نبود",
        "به‌صورت آگاهانه معامله نکردم",
    )

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences("tradix_weekly_planner", Context.MODE_PRIVATE)
        val raw = preferences?.getString("plans", null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                val tasks = row.optJSONArray("doneTasks") ?: JSONArray()
                plans[row.getString("date")] = DayPlan(
                    date = row.getString("date"),
                    tradeCount = row.optInt("tradeCount", 0).coerceIn(0, 100),
                    totalProfit = row.optDouble("totalProfit", 0.0).coerceAtLeast(0.0),
                    totalLoss = row.optDouble("totalLoss", 0.0).coerceAtLeast(0.0),
                    dailyGoal = row.optString("dailyGoal"),
                    maxRiskPercent = row.optDouble("maxRiskPercent", 1.0).coerceIn(0.1, 10.0),
                    notes = row.optString("notes"),
                    noTradeReason = row.optString("noTradeReason"),
                    mood = row.optInt("mood", 3).coerceIn(1, 5),
                    followedPlan = row.optBoolean("followedPlan", true),
                    confirmed = row.optBoolean("confirmed", false),
                    doneTasks = (0 until tasks.length()).mapNotNull { tasks.optString(it).takeIf(String::isNotBlank) }.toSet(),
                )
            }
        }
    }

    fun save(plan: DayPlan) {
        plans[plan.date] = plan
        persist()
    }

    fun persist() {
        val rows = JSONArray()
        plans.values.forEach { plan ->
            rows.put(JSONObject().apply {
                put("date", plan.date)
                put("tradeCount", plan.tradeCount)
                put("totalProfit", plan.totalProfit)
                put("totalLoss", plan.totalLoss)
                put("dailyGoal", plan.dailyGoal)
                put("maxRiskPercent", plan.maxRiskPercent)
                put("notes", plan.notes)
                put("noTradeReason", plan.noTradeReason)
                put("mood", plan.mood)
                put("followedPlan", plan.followedPlan)
                put("confirmed", plan.confirmed)
                put("doneTasks", JSONArray(plan.doneTasks.toList()))
            })
        }
        preferences?.edit()?.putString("plans", rows.toString())?.apply()
    }

    fun plan(date: LocalDate): DayPlan = plans[date.toString()] ?: DayPlan(date.toString())
}

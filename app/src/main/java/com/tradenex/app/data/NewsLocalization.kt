package com.tradenex.app.data

object NewsLocalization {
    private val faTitles = mapOf(
        "Non-Farm Employment Change" to "تغییر اشتغال بخش غیرکشاورزی (NFP)",
        "Unemployment Rate" to "نرخ بیکاری",
        "Average Hourly Earnings m/m" to "میانگین درآمد ساعتی ماهانه",
        "CPI m/m" to "شاخص قیمت مصرف‌کننده ماهانه",
        "CPI y/y" to "تورم سالانه شاخص قیمت مصرف‌کننده",
        "Core CPI m/m" to "شاخص قیمت مصرف‌کننده هسته ماهانه",
        "Core CPI y/y" to "تورم سالانه شاخص قیمت مصرف‌کننده هسته",
        "PPI m/m" to "شاخص قیمت تولیدکننده ماهانه",
        "Core PCE Price Index m/m" to "شاخص قیمت PCE هسته ماهانه",
        "Federal Funds Rate" to "نرخ بهره فدرال رزرو",
        "FOMC Statement" to "بیانیه کمیته بازار آزاد فدرال",
        "FOMC Press Conference" to "نشست خبری فدرال رزرو",
        "GDP q/q" to "تولید ناخالص داخلی فصلی",
        "Retail Sales m/m" to "خرده‌فروشی ماهانه",
        "Core Retail Sales m/m" to "خرده‌فروشی هسته ماهانه",
        "ISM Manufacturing PMI" to "شاخص مدیران خرید تولیدی ISM",
        "ISM Services PMI" to "شاخص مدیران خرید خدمات ISM",
        "Manufacturing PMI" to "شاخص مدیران خرید تولیدی",
        "Services PMI" to "شاخص مدیران خرید خدمات",
        "Flash Manufacturing PMI" to "شاخص مقدماتی مدیران خرید تولیدی",
        "Flash Services PMI" to "شاخص مقدماتی مدیران خرید خدمات",
        "Initial Jobless Claims" to "درخواست اولیه بیمه بیکاری",
        "JOLTS Job Openings" to "فرصت‌های شغلی JOLTS",
        "Consumer Confidence" to "اعتماد مصرف‌کننده",
        "Durable Goods Orders m/m" to "سفارش کالاهای بادوام ماهانه",
        "Trade Balance" to "تراز تجاری",
        "Current Account" to "حساب جاری",
        "BOE Interest Rate Decision" to "تصمیم نرخ بهره بانک مرکزی انگلیس",
        "ECB Interest Rate Decision" to "تصمیم نرخ بهره بانک مرکزی اروپا",
        "BOJ Policy Rate" to "نرخ سیاستی بانک مرکزی ژاپن",
        "RBA Cash Rate" to "نرخ بهره نقدی بانک مرکزی استرالیا",
        "SNB Policy Rate" to "نرخ سیاستی بانک ملی سوئیس",
        "New Home Sales" to "فروش خانه‌های نوساز",
        "Existing Home Sales" to "فروش خانه‌های موجود",
        "Housing Starts" to "آغاز ساخت مسکن",
        "ADP Non-Farm Employment Change" to "تغییر اشتغال غیرکشاورزی ADP",
        "Personal Income m/m" to "درآمد شخصی ماهانه",
        "Personal Spending m/m" to "مخارج شخصی ماهانه",
    )

    fun title(value: String, language: Int): String =
        if (language == 0) faTitles[value.trim()] ?: value else value

    fun impact(value: Int, language: Int): String = if (language == 0) {
        when (value) { 2 -> "تأثیر بالا"; 1 -> "تأثیر متوسط"; -1 -> "تعطیلی"; else -> "تأثیر کم" }
    } else {
        when (value) { 2 -> "High impact"; 1 -> "Medium impact"; -1 -> "Holiday"; else -> "Low impact" }
    }

    fun label(key: String, language: Int): String = if (language == 0) {
        when (key) {
            "currency" -> "ارز"
            "time" -> "زمان"
            "previous" -> "قبلی"
            "forecast" -> "پیش‌بینی"
            "actual" -> "واقعی"
            "source" -> "منبع"
            else -> key
        }
    } else {
        when (key) {
            "currency" -> "Currency"
            "time" -> "Time"
            "previous" -> "Previous"
            "forecast" -> "Forecast"
            "actual" -> "Actual"
            "source" -> "Source"
            else -> key
        }
    }
}

package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("groundlink_iii_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MY_CODE = "my_employee_code"
        private const val KEY_BASE_RATE = "base_rate"
        private const val KEY_OVERTIME_MULTIPLIER = "overtime_multiplier"
        private const val KEY_NIGHT_PREMIUM = "night_premium_percentage"
        private const val KEY_NIGHT_START_HOUR = "night_start_hour"
        private const val KEY_NIGHT_END_HOUR = "night_end_hour"
        private const val KEY_HOLIDAY_MULTIPLIER = "holiday_multiplier"
        private const val KEY_SELECTED_THEME = "selected_theme_index"
        private const val KEY_SHIFT_ALERT_ENABLED = "shift_alert_enabled"
        private const val KEY_SHIFT_ALERT_MINUTES = "shift_alert_minutes"
        private const val KEY_FONT_OVERRIDE = "font_override_index"
    }

    var fontOverride: Int
        get() = prefs.getInt(KEY_FONT_OVERRIDE, 0) // 0 = Theme Default, 1 = Monospace, 2 = Sans-Serif, 3 = Serif
        set(value) = prefs.edit().putInt(KEY_FONT_OVERRIDE, value).apply()

    var shiftAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIFT_ALERT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHIFT_ALERT_ENABLED, value).apply()

    var shiftAlertMinutes: Int
        get() = prefs.getInt(KEY_SHIFT_ALERT_MINUTES, 90)
        set(value) = prefs.edit().putInt(KEY_SHIFT_ALERT_MINUTES, value).apply()

    var selectedTheme: Int
        get() = prefs.getInt(KEY_SELECTED_THEME, 0) // Default to Retro Amber (0)
        set(value) = prefs.edit().putInt(KEY_SELECTED_THEME, value).apply()

    var myEmployeeCode: String?
        get() = prefs.getString(KEY_MY_CODE, null)
        set(value) = prefs.edit().putString(KEY_MY_CODE, value).apply()

    var baseRate: Float
        get() = prefs.getFloat(KEY_BASE_RATE, 7.50f)
        set(value) = prefs.edit().putFloat(KEY_BASE_RATE, value).apply()

    var overtimeMultiplier: Float
        get() = prefs.getFloat(KEY_OVERTIME_MULTIPLIER, 1.50f)
        set(value) = prefs.edit().putFloat(KEY_OVERTIME_MULTIPLIER, value).apply()

    var nightPremiumPercentage: Float
        get() = prefs.getFloat(KEY_NIGHT_PREMIUM, 0.25f) // default +25% night wage
        set(value) = prefs.edit().putFloat(KEY_NIGHT_PREMIUM, value).apply()

    var nightStartHour: Int
        get() = prefs.getInt(KEY_NIGHT_START_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_NIGHT_START_HOUR, value).apply()

    var nightEndHour: Int
        get() = prefs.getInt(KEY_NIGHT_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_NIGHT_END_HOUR, value).apply()

    var holidayMultiplier: Float
        get() = prefs.getFloat(KEY_HOLIDAY_MULTIPLIER, 2.00f) // default 100% premium (double pay)
        set(value) = prefs.edit().putFloat(KEY_HOLIDAY_MULTIPLIER, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

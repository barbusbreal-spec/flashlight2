package com.eblansoft.camera67

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Хранилище самого важного: сколько фоток еблан сегодня уже сделал
 * и купил ли он премиум за 0 рублей.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("eblan_camera_67", Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Жёсткий лимит бесплатного тарифа. 12. Не 13. Не 11. Ровно 12. ✅ */
    val dailyLimit: Int get() = 12

    var isPremium: Boolean
        get() = sp.getBoolean("premium", false)
        set(value) = sp.edit().putBoolean("premium", value).apply()

    fun photosToday(): Int {
        return if (sp.getString("photos_date", "") == today()) {
            sp.getInt("photos_count", 0)
        } else 0
    }

    fun registerPhoto() {
        val date = today()
        val count = if (sp.getString("photos_date", "") == date) {
            sp.getInt("photos_count", 0)
        } else 0
        sp.edit()
            .putString("photos_date", date)
            .putInt("photos_count", count + 1)
            .apply()
    }

    fun photosLeft(): Int = (dailyLimit - photosToday()).coerceAtLeast(0)
}

package com.eblansoft.camera67

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Хранилище самого важного: сколько фоток еблан сегодня уже сделал,
 * купил ли он премиум за 0 рублей и как он накрутил ебейшесть в настройках.
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

    // ---------- Настройки ебейшести (HDR RAW 67228+++++) ----------

    /** Кадров в HDR RAW стеке: 1 (бомж), 3 (норм ✅), 5 (ебейший 🥵). */
    var burstFrames: Int
        get() = sp.getInt("burst_frames", 3)
        set(value) = sp.edit().putInt("burst_frames", value).apply()

    /** Сочность, % (100 = как в жизни, скучно). */
    var juiciness: Int
        get() = sp.getInt("juiciness", 155)
        set(value) = sp.edit().putInt("juiciness", value).apply()

    /** Резкость, % (0 = мыло, 200 = порежешься). */
    var sharpness: Int
        get() = sp.getInt("sharpness", 86)
        set(value) = sp.edit().putInt("sharpness", value).apply()

    /** Сила AI-глоу, 0..128. */
    var glowAlpha: Int
        get() = sp.getInt("glow_alpha", 64)
        set(value) = sp.edit().putInt("glow_alpha", value).apply()

    /** Виньетка, % затемнения краёв. */
    var vignette: Int
        get() = sp.getInt("vignette", 45)
        set(value) = sp.edit().putInt("vignette", value).apply()

    /** Теплота тона, -10..10 (плюс — закат, минус — морг). */
    var warmth: Int
        get() = sp.getInt("warmth", 3)
        set(value) = sp.edit().putInt("warmth", value).apply()

    /** Качество JPEG, 67..100. Меньше 67 не бывает по политике компании. */
    var jpegQuality: Int
        get() = sp.getInt("jpeg_quality", 95)
        set(value) = sp.edit().putInt("jpeg_quality", value).apply()

    fun resetTuning() {
        sp.edit()
            .remove("burst_frames")
            .remove("juiciness")
            .remove("sharpness")
            .remove("glow_alpha")
            .remove("vignette")
            .remove("warmth")
            .remove("jpeg_quality")
            .apply()
    }

    // ---------- Дневной лимит ----------

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

package com.eblansoft.flashlight2

import android.content.Context

/** Persistent storage for premium status, daily quota and story progress. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("flashlight2_ultimate", Context.MODE_PRIVATE)

    var premium: Boolean
        get() = sp.getBoolean(KEY_PREMIUM, false)
        set(v) = sp.edit().putBoolean(KEY_PREMIUM, v).apply()

    /** Number of free turn-offs already used *today*. */
    var turnOffsUsedToday: Int
        get() = sp.getInt(KEY_USED, 0)
        set(v) = sp.edit().putInt(KEY_USED, v).apply()

    /** Extra turn-offs earned by watching ads (do not reset daily). */
    var bonusTurnOffs: Int
        get() = sp.getInt(KEY_BONUS, 0)
        set(v) = sp.edit().putInt(KEY_BONUS, v).apply()

    /** Epoch-day of the last quota reset. */
    var lastResetDay: Long
        get() = sp.getLong(KEY_DAY, 0L)
        set(v) = sp.edit().putLong(KEY_DAY, v).apply()

    var colorIndex: Int
        get() = sp.getInt(KEY_COLOR, 0)
        set(v) = sp.edit().putInt(KEY_COLOR, v).apply()

    /** Highest story chapter the player has reached (0-based). */
    var storyProgress: Int
        get() = sp.getInt(KEY_STORY, 0)
        set(v) = sp.edit().putInt(KEY_STORY, v).apply()

    companion object {
        private const val KEY_PREMIUM = "premium"
        private const val KEY_USED = "turnoffs_used"
        private const val KEY_BONUS = "bonus_turnoffs"
        private const val KEY_DAY = "last_reset_day"
        private const val KEY_COLOR = "color_index"
        private const val KEY_STORY = "story_progress"
    }
}

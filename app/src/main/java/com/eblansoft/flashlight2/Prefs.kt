package com.eblansoft.flashlight2

import android.content.Context

/** Persistent storage for premium status, daily quota and story progress. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("flashlight2_ultimate", Context.MODE_PRIVATE)

    var premium: Boolean
        get() = sp.getBoolean(KEY_PREMIUM, false)
        set(v) = sp.edit().putBoolean(KEY_PREMIUM, v).apply()

    /** Subscription tier index (0 = free, see GameState.TIERS). */
    var tier: Int
        get() = sp.getInt(KEY_TIER, 0)
        set(v) = sp.edit().putInt(KEY_TIER, v).apply()

    /** Turn-ONs used in the current 10-hour window. */
    var onUsedWindow: Int
        get() = sp.getInt(KEY_ON_USED, 0)
        set(v) = sp.edit().putInt(KEY_ON_USED, v).apply()

    /** Turn-OFFs used in the current 10-hour window. */
    var offUsedWindow: Int
        get() = sp.getInt(KEY_OFF_USED, 0)
        set(v) = sp.edit().putInt(KEY_OFF_USED, v).apply()

    /** Purchased/earned extra turn-ONs (do not reset with the window). */
    var boughtOn: Int
        get() = sp.getInt(KEY_BOUGHT_ON, 0)
        set(v) = sp.edit().putInt(KEY_BOUGHT_ON, v).apply()

    /** Purchased/earned extra turn-OFFs (do not reset with the window). */
    var boughtOff: Int
        get() = sp.getInt(KEY_BOUGHT_OFF, 0)
        set(v) = sp.edit().putInt(KEY_BOUGHT_OFF, v).apply()

    /** Lifetime number of times the light was switched off. */
    var totalTurnOffs: Int
        get() = sp.getInt(KEY_TOTAL_OFF, 0)
        set(v) = sp.edit().putInt(KEY_TOTAL_OFF, v).apply()

    /** How many times the secret «Светить сильнее» button was pressed. */
    var secretPresses: Int
        get() = sp.getInt(KEY_SECRET, 0)
        set(v) = sp.edit().putInt(KEY_SECRET, v).apply()

    /** Start timestamp (millis) of the current 10-hour quota window. */
    var windowStart: Long
        get() = sp.getLong(KEY_WINDOW, 0L)
        set(v) = sp.edit().putLong(KEY_WINDOW, v).apply()

    var colorIndex: Int
        get() = sp.getInt(KEY_COLOR, 0)
        set(v) = sp.edit().putInt(KEY_COLOR, v).apply()

    var rgbMode: Boolean
        get() = sp.getBoolean(KEY_RGB, false)
        set(v) = sp.edit().putBoolean(KEY_RGB, v).apply()

    /** Highest story chapter the player has reached (0-based). */
    var storyProgress: Int
        get() = sp.getInt(KEY_STORY, 0)
        set(v) = sp.edit().putInt(KEY_STORY, v).apply()

    /** Ids of story events that have already fired (so each fires once). */
    var storyEvents: Set<String>
        get() = sp.getStringSet(KEY_STORY_EVENTS, emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet(KEY_STORY_EVENTS, v).apply()

    // ---- Privacy -----------------------------------------------------------

    var passwordEnabled: Boolean
        get() = sp.getBoolean(KEY_PW_ON, false)
        set(v) = sp.edit().putBoolean(KEY_PW_ON, v).apply()

    var password: String
        get() = sp.getString(KEY_PW, "") ?: ""
        set(v) = sp.edit().putString(KEY_PW, v).apply()

    /** Require a fingerprint / biometric to open the flashlight. */
    var biometricEnabled: Boolean
        get() = sp.getBoolean(KEY_BIOMETRIC, false)
        set(v) = sp.edit().putBoolean(KEY_BIOMETRIC, v).apply()

    companion object {
        private const val KEY_PREMIUM = "premium"
        private const val KEY_TIER = "tier"
        private const val KEY_ON_USED = "on_used_window"
        private const val KEY_OFF_USED = "off_used_window"
        private const val KEY_BOUGHT_ON = "bought_on"
        private const val KEY_BOUGHT_OFF = "bought_off"
        private const val KEY_TOTAL_OFF = "total_turnoffs"
        private const val KEY_SECRET = "secret_presses"
        private const val KEY_WINDOW = "window_start"
        private const val KEY_COLOR = "color_index"
        private const val KEY_RGB = "rgb_mode"
        private const val KEY_STORY = "story_progress"
        private const val KEY_STORY_EVENTS = "story_events"
        private const val KEY_PW_ON = "password_enabled"
        private const val KEY_PW = "password"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}

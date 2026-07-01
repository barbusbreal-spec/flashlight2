package com.eblansoft.flashlight2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.eblansoft.flashlight2.security.SecurityCheck
import com.eblansoft.flashlight2.security.SecurityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

enum class Screen { FLASHLIGHT, STORY, PREMIUM }

/** A selectable light colour. Some are gated behind Premium. */
data class LightColor(
    val name: String,
    val color: Color,
    val premiumOnly: Boolean,
)

/**
 * Single source of truth for the whole app. Holds Compose-observable state and
 * owns the "business logic" of «Еблан Софт»: turning the light ON is free,
 * turning it OFF costs one of your 5 daily uses (unless you pay, of course).
 */
class GameState(
    private val prefs: Prefs,
    private val flash: FlashController,
    private val security: SecurityChecker,
) {
    companion object {
        const val DAILY_FREE_TURN_OFFS = 5

        val PALETTE = listOf(
            LightColor("Белый", Color(0xFFFFFFFF), premiumOnly = false),
            LightColor("Тёплый", Color(0xFFFFE9A3), premiumOnly = false),
            LightColor("Красный", Color(0xFFFF4D4D), premiumOnly = true),
            LightColor("Зелёный", Color(0xFF4DFF7A), premiumOnly = true),
            LightColor("Синий", Color(0xFF4D9BFF), premiumOnly = true),
            LightColor("Фиолетовый", Color(0xFFB44DFF), premiumOnly = true),
            LightColor("Розовый", Color(0xFFFF4DC4), premiumOnly = true),
            LightColor("Кислотный", Color(0xFFCCFF00), premiumOnly = true),
        )
    }

    val hasTorch: Boolean get() = flash.hasTorch

    var screen by mutableStateOf(Screen.FLASHLIGHT)
        private set

    // ---- Security gate -----------------------------------------------------
    // The flashlight refuses to open until every check is green.

    var securityChecking by mutableStateOf(false)
        private set

    var securityResults by mutableStateOf<List<SecurityCheck>>(emptyList())
        private set

    /** True only when the gate has run and every check passed. */
    val securityPassed: Boolean
        get() = securityResults.isNotEmpty() && securityResults.all { it.passed }

    val securityChecked: Boolean
        get() = securityResults.isNotEmpty()

    /** Set once the user is let through the gate (only possible if all green). */
    var gateUnlocked by mutableStateOf(false)
        private set

    fun unlockGate() {
        if (securityPassed) gateUnlocked = true
    }

    suspend fun runSecurityChecks() {
        securityChecking = true
        // Off the main thread: keystore attestation and `which su` can block.
        val results = withContext(Dispatchers.Default) { security.runAll() }
        securityResults = results
        securityChecking = false
    }

    var premium by mutableStateOf(prefs.premium)
        private set

    /** Whether the torch (and/or screen light) is currently on. */
    var lightOn by mutableStateOf(false)
        private set

    /** Use the phone screen as a light instead of the camera torch. */
    var screenLightMode by mutableStateOf(!flash.hasTorch)
        private set

    var colorIndex by mutableIntStateOf(prefs.colorIndex.coerceIn(0, PALETTE.lastIndex))
        private set

    var storyProgress by mutableIntStateOf(prefs.storyProgress)
        private set

    /** True while the fake ad overlay should be shown. */
    var showAd by mutableStateOf(false)
        private set

    /** True while the "you're out of free turn-offs" paywall prompt is shown. */
    var showOutOfQuota by mutableStateOf(false)
        private set

    private var usedToday by mutableIntStateOf(0)
    private var bonus by mutableIntStateOf(prefs.bonusTurnOffs)

    init {
        rolloverDayIfNeeded()
        usedToday = prefs.turnOffsUsedToday
    }

    // ---- Quota -------------------------------------------------------------

    val turnOffsRemaining: Int
        get() = if (premium) Int.MAX_VALUE
        else (DAILY_FREE_TURN_OFFS - usedToday).coerceAtLeast(0) + bonus

    val isUnlimited: Boolean get() = premium

    private fun rolloverDayIfNeeded() {
        val today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        if (prefs.lastResetDay != today) {
            prefs.lastResetDay = today
            prefs.turnOffsUsedToday = 0
            usedToday = 0
        }
    }

    // ---- Light control -----------------------------------------------------

    val currentColor: Color get() = PALETTE[colorIndex].color

    /** Handle the big central button. */
    fun onToggleLight() {
        rolloverDayIfNeeded()
        if (!lightOn) {
            turnOn()
        } else {
            requestTurnOff()
        }
    }

    private fun turnOn() {
        lightOn = true
        if (!screenLightMode) flash.setTorch(true)
    }

    private fun requestTurnOff() {
        if (turnOffsRemaining <= 0 && !premium) {
            // Out of quota — offer the ad / paywall instead of turning off.
            showOutOfQuota = true
            return
        }
        if (!premium) spendOneTurnOff()
        doTurnOff()
    }

    private fun spendOneTurnOff() {
        if (bonus > 0) {
            bonus--
            prefs.bonusTurnOffs = bonus
        } else {
            usedToday++
            prefs.turnOffsUsedToday = usedToday
        }
    }

    private fun doTurnOff() {
        lightOn = false
        flash.setTorch(false)
    }

    fun toggleScreenLightMode() {
        // Switching modes turns the physical torch off to avoid a stuck beam.
        if (lightOn && !screenLightMode) flash.setTorch(false)
        screenLightMode = !screenLightMode
        if (lightOn && !screenLightMode) flash.setTorch(true)
    }

    // ---- Colours -----------------------------------------------------------

    fun selectColor(index: Int) {
        val target = PALETTE[index]
        if (target.premiumOnly && !premium) {
            screen = Screen.PREMIUM
            return
        }
        colorIndex = index
        prefs.colorIndex = index
    }

    // ---- Ads & paywall -----------------------------------------------------

    /** User chose "watch ad for +1 turn-off". */
    fun startAd() {
        showOutOfQuota = false
        showAd = true
    }

    /** Called when the fake ad finished playing. */
    fun onAdFinished() {
        showAd = false
        bonus++
        prefs.bonusTurnOffs = bonus
        // Reward is immediate: actually turn the light off now.
        if (lightOn) {
            spendOneTurnOff()
            doTurnOff()
        }
    }

    fun dismissAd() {
        showAd = false
    }

    fun dismissOutOfQuota() {
        showOutOfQuota = false
    }

    fun openPaywallFromQuota() {
        showOutOfQuota = false
        screen = Screen.PREMIUM
    }

    // ---- Premium -----------------------------------------------------------

    fun purchasePremium() {
        premium = true
        prefs.premium = true
        showAd = false
        showOutOfQuota = false
    }

    /** Debug helper: cancel the subscription (returns you to the peasant tier). */
    fun cancelPremium() {
        premium = false
        prefs.premium = false
    }

    // ---- Story -------------------------------------------------------------

    fun advanceStory(toChapter: Int) {
        if (toChapter > storyProgress) {
            storyProgress = toChapter
            prefs.storyProgress = toChapter
        }
    }

    // ---- Navigation --------------------------------------------------------

    fun navigate(to: Screen) { screen = to }

    fun onDispose() { flash.release() }
}

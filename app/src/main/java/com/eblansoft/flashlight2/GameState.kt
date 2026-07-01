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

enum class Screen { FLASHLIGHT, STORY, PREMIUM, SETTINGS }

/** A selectable light colour. Some are gated behind Premium. */
data class LightColor(
    val name: String,
    val color: Color,
    val premiumOnly: Boolean,
)

/**
 * Single source of truth for the whole app. Holds Compose-observable state and
 * owns the "business logic" of «Еблан Софт»: turning the light ON and OFF each
 * cost one action from a 10-hour quota window (unless you pay, of course).
 */
class GameState(
    private val prefs: Prefs,
    private val flash: FlashController,
    private val security: SecurityChecker,
) {
    companion object {
        const val WINDOW_HOURS = 10
        val WINDOW_MS = WINDOW_HOURS * 60L * 60L * 1000L

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

        const val EV_FIRST_ON = "first_on"
        const val EV_FIRST_OFF = "first_off"
        const val EV_OPEN_SETTINGS = "open_settings"
        const val EV_OPEN_BILLING = "open_billing"
        const val EV_OUT_OF_QUOTA = "out_of_quota"
        const val EV_BUY_PREMIUM = "buy_premium"

        // event id -> (chapter to unlock, notification to show)
        val STORY_EVENTS: Map<String, Pair<Int, AppNotification>> = mapOf(
            EV_FIRST_ON to (1 to AppNotification("📖", "Глава 1 открыта", "Во тьме зажёгся свет. Открой «Сюжетку».")),
            EV_FIRST_OFF to (2 to AppNotification("📖", "Глава 2 открыта", "Ты потратил выключение. Их всего пять.")),
            EV_OPEN_SETTINGS to (3 to AppNotification("⚙️", "Глава 3 открыта", "Ты нашёл настройки «Еблан Софт».")),
            EV_OPEN_BILLING to (4 to AppNotification("💳", "Глава 4 открыта", "Ты увидел цены. Наступило прозрение.")),
            EV_OUT_OF_QUOTA to (5 to AppNotification("🚫", "Глава 5 открыта", "Лимит исчерпан. Восстание близко.")),
            EV_BUY_PREMIUM to (6 to AppNotification("👑", "Финал открыт", "Ты купил свободу. Прочти финал в «Сюжетке».")),
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

    var colorRgbMode by mutableStateOf(prefs.rgbMode)
        private set

    /** Subscription tier index into [Tiers.ALL]; 0 = Free. */
    var tier by mutableIntStateOf(prefs.tier)
        private set

    /** How many times the secret «Светить сильнее» button was pressed. */
    var secretPresses by mutableIntStateOf(prefs.secretPresses)
        private set

    // ---- Password / biometric lock ----------------------------------------

    var passwordEnabled by mutableStateOf(prefs.passwordEnabled)
        private set

    var biometricEnabled by mutableStateOf(prefs.biometricEnabled)
        private set

    /** Cleared once the user passes the lock this session. */
    var locked by mutableStateOf(prefs.passwordEnabled || prefs.biometricEnabled)
        private set

    val lockRequired: Boolean get() = passwordEnabled || biometricEnabled

    // ---- In-app notifications (Claude-style banners) -----------------------

    var currentNotification by mutableStateOf<AppNotification?>(null)
        private set

    private val notificationQueue = ArrayDeque<AppNotification>()

    private val firedEvents: MutableSet<String> = prefs.storyEvents.toMutableSet()

    /** True while the fake ad overlay should be shown. */
    var showAd by mutableStateOf(false)
        private set

    /** Which quota ran out: "on", "off", or null when the prompt is hidden. */
    var outOfQuotaKind by mutableStateOf<String?>(null)
        private set

    private var onUsed by mutableIntStateOf(0)
    private var offUsed by mutableIntStateOf(0)
    private var boughtOn by mutableIntStateOf(prefs.boughtOn)
    private var boughtOff by mutableIntStateOf(prefs.boughtOff)

    /** Millis until the current 10-hour window resets (drives the UI countdown). */
    var windowResetInMs by mutableStateOf(0L)
        private set

    init {
        rolloverWindowIfNeeded()
        onUsed = prefs.onUsedWindow
        offUsed = prefs.offUsedWindow
    }

    // ---- Quota (10-hour window + purchasable top-ups) ----------------------

    private val tierOnLimit: Int get() = Tiers.ALL[tier].onLimit
    private val tierOffLimit: Int get() = Tiers.ALL[tier].offLimit

    val onUnlimited: Boolean get() = tierOnLimit == Int.MAX_VALUE
    val offUnlimited: Boolean get() = tierOffLimit == Int.MAX_VALUE

    /** Remaining turn-ONs in this window (Int.MAX_VALUE = unlimited). */
    val onRemaining: Int
        get() = if (onUnlimited) Int.MAX_VALUE
        else (tierOnLimit - onUsed).coerceAtLeast(0) + boughtOn

    /** Remaining turn-OFFs in this window (Int.MAX_VALUE = unlimited). */
    val offRemaining: Int
        get() = if (offUnlimited) Int.MAX_VALUE
        else (tierOffLimit - offUsed).coerceAtLeast(0) + boughtOff

    private fun rolloverWindowIfNeeded() {
        val now = System.currentTimeMillis()
        val start = prefs.windowStart
        if (start == 0L || now - start >= WINDOW_MS) {
            prefs.windowStart = now
            prefs.onUsedWindow = 0
            prefs.offUsedWindow = 0
            onUsed = 0
            offUsed = 0
        }
        windowResetInMs = (prefs.windowStart + WINDOW_MS - now).coerceAtLeast(0L)
    }

    // ---- Light control -----------------------------------------------------

    val currentColor: Color get() = PALETTE[colorIndex].color

    /** Handle the big central button. */
    fun onToggleLight() {
        rolloverWindowIfNeeded()
        if (!lightOn) requestTurnOn() else requestTurnOff()
    }

    private fun requestTurnOn() {
        if (onRemaining <= 0) {
            outOfQuotaKind = "on"
            return
        }
        consume(isOn = true)
        lightOn = true
        if (!screenLightMode) flash.setTorch(true)
        fireStoryEvent(EV_FIRST_ON)
    }

    private fun requestTurnOff() {
        if (offRemaining <= 0) {
            outOfQuotaKind = "off"
            fireStoryEvent(EV_OUT_OF_QUOTA)
            return
        }
        consume(isOn = false)
        doTurnOff()
        prefs.totalTurnOffs += 1
        fireStoryEvent(EV_FIRST_OFF)
    }

    private fun consume(isOn: Boolean) {
        if (isOn) {
            if (onUnlimited) return
            if (boughtOn > 0) { boughtOn--; prefs.boughtOn = boughtOn }
            else { onUsed++; prefs.onUsedWindow = onUsed }
        } else {
            if (offUnlimited) return
            if (boughtOff > 0) { boughtOff--; prefs.boughtOff = boughtOff }
            else { offUsed++; prefs.offUsedWindow = offUsed }
        }
    }

    private fun doTurnOff() {
        lightOn = false
        flash.setTorch(false)
    }

    // ---- Top-ups (докупка лимитов) -----------------------------------------

    fun buyTopUp(topUp: Tiers.TopUp) {
        boughtOn += topUp.onCredits
        boughtOff += topUp.offCredits
        prefs.boughtOn = boughtOn
        prefs.boughtOff = boughtOff
        outOfQuotaKind = null
        notify(AppNotification("🧾", "Лимиты докуплены", "${topUp.label} зачислено. Свети на здоровье."))
    }

    val boughtOnCredits: Int get() = boughtOn
    val boughtOffCredits: Int get() = boughtOff
    val totalTurnOffsEver: Int get() = prefs.totalTurnOffs

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
            notify(AppNotification("🔒", "Цвет под замком", "«${target.name}» доступен в Premium."))
            screen = Screen.PREMIUM
            return
        }
        colorRgbMode = false
        prefs.rgbMode = false
        colorIndex = index
        prefs.colorIndex = index
    }

    val rgbUnlocked: Boolean get() = tier >= Tiers.PREMIUM_PLUS

    fun toggleRgb() {
        if (!rgbUnlocked) {
            notify(AppNotification("💡", "RGB под замком", "Лампочки RGB — в подписке Premium+."))
            screen = Screen.PREMIUM
            return
        }
        colorRgbMode = !colorRgbMode
        prefs.rgbMode = colorRgbMode
    }

    // ---- Ads & paywall -----------------------------------------------------

    private var adRewardKind: String = "off"

    /** User chose "watch ad for +1 action". */
    fun startAd() {
        adRewardKind = outOfQuotaKind ?: "off"
        outOfQuotaKind = null
        showAd = true
    }

    /** Called when the fake ad finished playing: grants one credit of its kind. */
    fun onAdFinished() {
        showAd = false
        if (adRewardKind == "on") {
            boughtOn++
            prefs.boughtOn = boughtOn
        } else {
            boughtOff++
            prefs.boughtOff = boughtOff
        }
        notify(AppNotification("🎁", "Награда получена", "+1 ${if (adRewardKind == "on") "включение" else "выключение"}. Спасибо за просмотр."))
    }

    fun dismissAd() {
        showAd = false
    }

    fun dismissOutOfQuota() {
        outOfQuotaKind = null
    }

    fun openPaywallFromQuota() {
        outOfQuotaKind = null
        screen = Screen.PREMIUM
    }

    // ---- Premium / tiers ---------------------------------------------------

    val tierBadge: String get() = Tiers.ALL[tier].badge

    /** Buy a subscription tier by index into [Tiers.ALL]. */
    fun purchaseTier(index: Int) {
        tier = index
        prefs.tier = index
        val isPaid = index >= 1
        premium = isPaid
        prefs.premium = isPaid
        if (!rgbUnlocked) {
            colorRgbMode = false
            prefs.rgbMode = false
        }
        showAd = false
        outOfQuotaKind = null
        if (isPaid) {
            notify(AppNotification("👑", "Подписка ${Tiers.ALL[index].name}", "Спасибо, вы обогатили «Еблан Софт»."))
            fireStoryEvent(EV_BUY_PREMIUM)
        }
    }

    fun purchasePremium() = purchaseTier(1)

    /** Cancel the subscription (returns you to the peasant tier). */
    fun cancelPremium() {
        tier = 0
        prefs.tier = 0
        premium = false
        prefs.premium = false
        colorRgbMode = false
        prefs.rgbMode = false
    }

    val secretButtonUnlocked: Boolean get() = tier >= Tiers.ULTRA

    /** The legendary «Светить сильнее» button. Does nothing, gloriously. */
    fun pressSecret() {
        secretPresses += 1
        prefs.secretPresses = secretPresses
        notify(
            AppNotification(
                "⚡",
                "Светит сильнее!",
                "Яркость увеличена на 500% (визуально не подтверждается). Нажатий: $secretPresses.",
            )
        )
    }

    // ---- Password / biometric ----------------------------------------------

    fun setPassword(pin: String) {
        prefs.password = pin
        prefs.passwordEnabled = true
        passwordEnabled = true
        notify(AppNotification("🔐", "Пароль установлен", "Теперь фонарик под защитой."))
    }

    fun disablePassword() {
        prefs.passwordEnabled = false
        prefs.password = ""
        passwordEnabled = false
    }

    fun checkPassword(pin: String): Boolean {
        val ok = pin == prefs.password
        if (ok) locked = false
        return ok
    }

    fun toggleBiometric(enabled: Boolean) {
        prefs.biometricEnabled = enabled
        biometricEnabled = enabled
    }

    /** Called after a successful biometric prompt. */
    fun onAuthPassed() { locked = false }

    // ---- Story & notifications ---------------------------------------------

    fun advanceStory(toChapter: Int) {
        if (toChapter > storyProgress) {
            storyProgress = toChapter
            prefs.storyProgress = toChapter
        }
    }

    fun notify(n: AppNotification) {
        if (currentNotification == null) {
            currentNotification = n
        } else {
            notificationQueue.addLast(n)
        }
    }

    fun dismissNotification() {
        currentNotification = notificationQueue.removeFirstOrNull()
    }

    /**
     * The story is discovered by poking the interface: each first-time
     * interaction fires a Claude-style notification and unlocks a chapter.
     */
    fun fireStoryEvent(event: String) {
        if (event in firedEvents) return
        firedEvents.add(event)
        prefs.storyEvents = firedEvents.toSet()

        val (chapter, note) = STORY_EVENTS[event] ?: return
        advanceStory(chapter)
        notify(note)
    }

    fun onOpenSettings() = fireStoryEvent(EV_OPEN_SETTINGS)
    fun onOpenBilling() = fireStoryEvent(EV_OPEN_BILLING)

    // ---- Navigation --------------------------------------------------------

    fun navigate(to: Screen) {
        screen = to
        if (to == Screen.SETTINGS) onOpenSettings()
    }

    fun onDispose() { flash.release() }
}

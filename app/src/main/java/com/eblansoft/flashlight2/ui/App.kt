package com.eblansoft.flashlight2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.Screen

@Composable
fun App(state: GameState) {
    Flashlight2Theme {
        // 1. Security gate: nothing else is reachable until every check is green.
        if (!state.gateUnlocked) {
            SecurityGateScreen(state, onPassed = { state.unlockGate() })
            return@Flashlight2Theme
        }

        // 2. Password / biometric lock on the flashlight itself.
        if (state.locked) {
            LockScreen(state)
            NotificationBanner(state)
            return@Flashlight2Theme
        }

        // 3. Main app. Screens other than the main one act like a depth-1 back stack.
        BackHandler(enabled = state.screen != Screen.FLASHLIGHT) {
            state.navigate(Screen.FLASHLIGHT)
        }

        when (state.screen) {
            Screen.FLASHLIGHT -> FlashlightScreen(state)
            Screen.STORY -> StoryScreen(state)
            Screen.PREMIUM -> PaywallScreen(state)
            Screen.SETTINGS -> SettingsScreen(state)
        }

        // Overlays render on top of whatever screen is active.
        OutOfQuotaDialog(state)
        AdOverlay(state)
        NotificationBanner(state)
    }
}

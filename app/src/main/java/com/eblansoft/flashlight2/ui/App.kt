package com.eblansoft.flashlight2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.Screen

@Composable
fun App(state: GameState) {
    Flashlight2Theme {
        // Security gate: nothing else is reachable until every check is green.
        if (!state.gateUnlocked) {
            SecurityGateScreen(state, onPassed = { state.unlockGate() })
            return@Flashlight2Theme
        }

        // Screens other than the main one act like a back stack of depth 1.
        BackHandler(enabled = state.screen != Screen.FLASHLIGHT) {
            state.navigate(Screen.FLASHLIGHT)
        }

        when (state.screen) {
            Screen.FLASHLIGHT -> FlashlightScreen(state)
            Screen.STORY -> StoryScreen(state)
            Screen.PREMIUM -> PaywallScreen(state)
        }

        // Overlays render on top of whatever screen is active.
        OutOfQuotaDialog(state)
        AdOverlay(state)
    }
}

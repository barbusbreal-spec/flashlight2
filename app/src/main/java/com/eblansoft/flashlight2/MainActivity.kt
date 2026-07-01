package com.eblansoft.flashlight2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eblansoft.flashlight2.security.SecurityChecker
import com.eblansoft.flashlight2.ui.App

class MainActivity : ComponentActivity() {

    private lateinit var state: GameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        state = GameState(
            prefs = Prefs(applicationContext),
            flash = FlashController(applicationContext),
            security = SecurityChecker(applicationContext),
        )

        setContent { App(state) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::state.isInitialized) state.onDispose()
    }
}

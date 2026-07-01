package com.eblansoft.flashlight2

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.eblansoft.flashlight2.security.SecurityChecker
import com.eblansoft.flashlight2.ui.App
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

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
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Routes `flashlight://` deep links: DEP ID OAuth callback or `open/<menu>`. */
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "flashlight" || !::state.isInitialized) return

        if (uri.host == "auth") {
            val code = uri.getQueryParameter("code")
            val oauthState = uri.getQueryParameter("state")
            lifecycleScope.launch { state.dep.handleCallback(code, oauthState) }
        } else {
            state.openDeepLink(uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::state.isInitialized) state.onDispose()
    }
}

package com.eblansoft.flashlight2.dep

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eblansoft.flashlight2.Prefs
import java.util.UUID

/**
 * Holds DEP ID (DEP API v1) OAuth2 session state: login status, profile and
 * casino data. All network-touching functions are suspend and are meant to be
 * driven by the UI layer (LaunchedEffect / rememberCoroutineScope), same as
 * [com.eblansoft.flashlight2.GameState.runSecurityChecks].
 */
class DepAuthState(private val prefs: Prefs) {

    var loggedIn by mutableStateOf(prefs.depAccessToken.isNotBlank())
        private set

    var profile by mutableStateOf<DepProfile?>(null)
        private set

    var casino by mutableStateOf<CasinoBalance?>(null)
        private set

    var lastSpin by mutableStateOf<SpinResult?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** Builds the /oauth/authorize.php URL to open in a browser, storing the CSRF state. */
    fun buildAuthorizeUrl(): String {
        val csrf = UUID.randomUUID().toString()
        prefs.depOauthState = csrf
        return "${DepAuthConfig.AUTHORIZE_URL}" +
            "?client_id=${DepAuthConfig.CLIENT_ID}" +
            "&redirect_uri=${DepAuthConfig.REDIRECT_URI}" +
            "&state=$csrf"
    }

    /** Called when the flashlight://auth/callback deep link is received. */
    suspend fun handleCallback(code: String?, state: String?) {
        val expected = prefs.depOauthState
        prefs.depOauthState = ""

        if (code.isNullOrBlank()) {
            error = "DEP ID: код авторизации не получен"
            return
        }
        if (expected.isBlank() || state != expected) {
            error = "DEP ID: не совпадает state, вход отклонён (защита от CSRF)"
            return
        }

        loading = true
        error = null
        runCatching { DepApi.exchangeCode(code) }
            .onSuccess { tokens ->
                saveTokens(tokens)
                loadProfileInternal()
            }
            .onFailure { error = it.message ?: "Не удалось войти через DEP ID" }
        loading = false
    }

    suspend fun loadProfile() {
        if (!loggedIn) return
        loading = true
        error = null
        loadProfileInternal()
        loading = false
    }

    private suspend fun loadProfileInternal() {
        runCatching { withValidToken { DepApi.profile(it) } }
            .onSuccess { profile = it }
            .onFailure { error = it.message ?: "Не удалось загрузить профиль" }
    }

    suspend fun loadCasino() {
        runCatching { withValidToken { DepApi.casinoBalance(it) } }
            .onSuccess { casino = it }
            .onFailure { error = it.message ?: "Казино недоступно" }
    }

    suspend fun spin(bet: Int) {
        loading = true
        error = null
        runCatching { withValidToken { DepApi.spin(it, bet) } }
            .onSuccess {
                lastSpin = it
                casino = CasinoBalance(it.balanceAfter, it.debt)
            }
            .onFailure { error = it.message ?: "Ставка не удалась" }
        loading = false
    }

    suspend fun logout() {
        val token = prefs.depAccessToken
        if (token.isNotBlank()) DepApi.revoke(token)
        prefs.clearDep()
        loggedIn = false
        profile = null
        casino = null
        lastSpin = null
    }

    fun dismissError() { error = null }

    private fun saveTokens(tokens: DepTokens) {
        prefs.depAccessToken = tokens.accessToken
        prefs.depRefreshToken = tokens.refreshToken
        prefs.depExpiresAt = System.currentTimeMillis() + tokens.expiresInSec * 1000
        loggedIn = true
    }

    private suspend fun <T> withValidToken(block: suspend (String) -> T): T {
        ensureFreshToken()
        return block(prefs.depAccessToken)
    }

    private suspend fun ensureFreshToken() {
        val expiresAt = prefs.depExpiresAt
        if (expiresAt != 0L && System.currentTimeMillis() < expiresAt - 30_000L) return
        val refresh = prefs.depRefreshToken
        if (refresh.isBlank()) return
        saveTokens(DepApi.refresh(refresh))
    }
}

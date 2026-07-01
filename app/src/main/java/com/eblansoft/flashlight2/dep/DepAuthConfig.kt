package com.eblansoft.flashlight2.dep

/**
 * DEP ID (DEP API v1) integration config.
 *
 * Register an OAuth service in DEP ID → Панель разработчика and paste the
 * resulting client_id / client_secret below. The redirect URI must match the
 * one registered there — we use the app's own [REDIRECT_URI] deep link.
 *
 * Note: embedding a client_secret in an app is not truly secret. DEP API v1
 * has no PKCE, so we follow its documented server-style code exchange as-is.
 */
object DepAuthConfig {
    const val BASE = "https://zenusus.serv00.net/dep"
    const val AUTHORIZE_URL = "$BASE/oauth/authorize.php"
    const val TOKEN_URL = "$BASE/oauth/token.php"
    const val REVOKE_URL = "$BASE/oauth/revoke.php"
    const val PROFILE_URL = "$BASE/api/v1/profile.php"
    const val CASINO_URL = "$BASE/api/v1/casino.php"

    /** Custom-scheme deep link DEP ID redirects back to after authorization. */
    const val REDIRECT_URI = "flashlight://auth/callback"

    const val CLIENT_ID = "dep_87baeb24db5bf933d5ba11ab"
    const val CLIENT_SECRET = "6d9490163c4babfcd2ccc40eb09f49e17bdf6150a13748ccafbdc72fff8f1bd8"

    val isConfigured: Boolean get() = CLIENT_ID.isNotBlank() && CLIENT_SECRET.isNotBlank()
}

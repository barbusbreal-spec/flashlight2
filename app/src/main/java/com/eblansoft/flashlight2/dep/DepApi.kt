package com.eblansoft.flashlight2.dep

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin DEP API v1 client built on HttpURLConnection + org.json — no extra deps.
 * All calls are suspending and run on the IO dispatcher.
 */
object DepApi {

    // ---- OAuth -------------------------------------------------------------

    suspend fun exchangeCode(code: String): DepTokens = post(
        DepAuthConfig.TOKEN_URL,
        JSONObject().apply {
            put("grant_type", "authorization_code")
            put("client_id", DepAuthConfig.CLIENT_ID)
            put("client_secret", DepAuthConfig.CLIENT_SECRET)
            put("code", code)
            put("redirect_uri", DepAuthConfig.REDIRECT_URI)
        },
    ).let(DepTokens::fromJson)

    suspend fun refresh(refreshToken: String): DepTokens = post(
        DepAuthConfig.TOKEN_URL,
        JSONObject().apply {
            put("grant_type", "refresh_token")
            put("client_id", DepAuthConfig.CLIENT_ID)
            put("client_secret", DepAuthConfig.CLIENT_SECRET)
            put("refresh_token", refreshToken)
        },
    ).let(DepTokens::fromJson)

    suspend fun revoke(token: String) {
        runCatching {
            post(DepAuthConfig.REVOKE_URL, JSONObject().apply { put("token", token) })
        }
    }

    // ---- Profile / Casino --------------------------------------------------

    suspend fun profile(accessToken: String): DepProfile =
        get(DepAuthConfig.PROFILE_URL, accessToken).let(DepProfile::fromJson)

    suspend fun casinoBalance(accessToken: String): CasinoBalance =
        get(DepAuthConfig.CASINO_URL, accessToken).let(CasinoBalance::fromJson)

    suspend fun spin(accessToken: String, bet: Int): SpinResult = post(
        DepAuthConfig.CASINO_URL,
        JSONObject().apply { put("bet", bet) },
        bearer = accessToken,
    ).let(SpinResult::fromJson)

    // ---- transport ---------------------------------------------------------

    private suspend fun post(url: String, body: JSONObject, bearer: String? = null): JSONObject =
        request("POST", url, bearer, body)

    private suspend fun get(url: String, bearer: String): JSONObject =
        request("GET", url, bearer, null)

    private suspend fun request(
        method: String,
        url: String,
        bearer: String?,
        body: JSONObject?,
    ): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            bearer?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                throw DepException(json.optString("error", "HTTP $code"))
            }
            json
        } finally {
            conn.disconnect()
        }
    }
}

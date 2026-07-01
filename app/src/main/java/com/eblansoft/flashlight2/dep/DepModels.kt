package com.eblansoft.flashlight2.dep

import org.json.JSONObject

/** OAuth2 token pair from DEP ID. */
data class DepTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
) {
    companion object {
        fun fromJson(o: JSONObject) = DepTokens(
            accessToken = o.optString("access_token"),
            refreshToken = o.optString("refresh_token"),
            expiresInSec = o.optLong("expires_in", 3600L),
        )
    }
}

data class DepFriend(
    val id: Int,
    val username: String,
    val online: Boolean,
)

/** Profile from GET /api/v1/profile.php */
data class DepProfile(
    val id: Int,
    val username: String,
    val description: String,
    val avatarUrl: String,
    val friendCode: String,
    val depcoins: Int,
    val debt: Int,
    val calculations: Int,
    val spins: Int,
    val registeredAt: String,
    val friends: List<DepFriend>,
) {
    companion object {
        fun fromJson(o: JSONObject): DepProfile {
            val friends = mutableListOf<DepFriend>()
            o.optJSONArray("friends")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val f = arr.optJSONObject(i) ?: continue
                    friends += DepFriend(
                        id = f.optInt("id"),
                        username = f.optString("username"),
                        online = f.optBoolean("online"),
                    )
                }
            }
            return DepProfile(
                id = o.optInt("id"),
                username = o.optString("username"),
                description = o.optString("description"),
                avatarUrl = o.optString("avatar_url"),
                friendCode = o.optString("friend_code"),
                depcoins = o.optInt("depcoins"),
                debt = o.optInt("debt"),
                calculations = o.optInt("calculations"),
                spins = o.optInt("spins"),
                registeredAt = o.optString("registered_at"),
                friends = friends,
            )
        }
    }
}

/** Balance from GET /api/v1/casino.php */
data class CasinoBalance(val depcoins: Int, val debt: Int) {
    companion object {
        fun fromJson(o: JSONObject) = CasinoBalance(
            depcoins = o.optInt("depcoins"),
            debt = o.optInt("debt"),
        )
    }
}

/** Result from POST /api/v1/casino.php (spin). */
data class SpinResult(
    val result: String,   // lose | small_win | win | jackpot
    val bet: Int,
    val win: Int,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val debt: Int,
) {
    companion object {
        fun fromJson(o: JSONObject) = SpinResult(
            result = o.optString("result"),
            bet = o.optInt("bet"),
            win = o.optInt("win"),
            balanceBefore = o.optInt("balance_before"),
            balanceAfter = o.optInt("balance_after"),
            debt = o.optInt("debt"),
        )
    }
}

/** Thrown for any DEP API error, carrying a user-facing message. */
class DepException(message: String) : Exception(message)

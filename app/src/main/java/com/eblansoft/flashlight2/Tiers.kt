package com.eblansoft.flashlight2

import androidx.compose.ui.graphics.Color

/** A «Еблан Софт» subscription tier. Index 0 is Free. */
data class Tier(
    val name: String,
    val price: String,
    val badge: String,
    val accent: Color,
    val perks: List<String>,
    /** Max turn-ONs per 10-hour window (Int.MAX_VALUE = unlimited). */
    val onLimit: Int,
    /** Max turn-OFFs per 10-hour window (Int.MAX_VALUE = unlimited). */
    val offLimit: Int,
)

/** In-app Claude-style notification. */
data class AppNotification(
    val emoji: String,
    val title: String,
    val body: String,
)

object Tiers {
    /** Anything at index >= 1 counts as "premium" (unlimited + colours + no ads). */
    val ALL: List<Tier> = listOf(
        Tier(
            name = "Free",
            price = "0 ₽",
            badge = "FREE",
            accent = Color(0xFF8A8A9A),
            perks = listOf(
                "⏳ Лимит за 10 ч: 5 включений и 5 выключений",
                "Реклама капсул для стирки",
                "Ровно один белый цвет",
            ),
            onLimit = 5,
            offLimit = 5,
        ),
        Tier(
            name = "Lite",
            price = "499 ₽",
            badge = "Lite",
            accent = Color(0xFF05F78A),
            perks = listOf(
                "Улучшенный тариф",
                "⏳ Лимит за 10 ч: 15 включений и 10 выключений",
                "📖 Полная сюжетка, включая финал",
                "Ровно один белый цвет",
            ),
            onLimit = 15,
            offLimit = 10,
        ),
        Tier(
            name = "Plus",
            price = "999 ₽",
            badge = "Plus",
            accent = Color(0xFF05F78A),
            perks = listOf(
                "Cтандартный тариф",
                "⏳ Лимит за 10 ч: 50 включений и 50 выключений",
                "🎨 Все цвета свечения",
                "🚫 Никакой рекламы",
                "📖 Полная сюжетка, включая финал",
                "🔋 +500% к яркости (не работает)",
            ),
            onLimit = 50,
            offLimit = 50,
        ),
        Tier(
            name = "MAX 2x",
            price = "1 299 ₽ / мес",
            badge = "MAX 2x",
            accent = Color(0xFFFFD34D),
            perks = listOf(
                "Всё из Plus в 2 раз больше",
                "🎨 Все цвета свечения",
                "🚫 Никакой рекламы",
                "📖 Полная сюжетка, включая финал",
                "🔋 +500% к яркости (не работает)",
                "⏳ Лимит за 10 ч: 100 включений, 100 выключений",
            ),
            onLimit = 100,
            offLimit = 100,
        ),
        Tier(
            name = "MAX 5x",
            price = "1 999 ₽ / мес",
            badge = "MAX 5x",
            accent = Color(0xFFB44DFF),
            perks = listOf(
                "Всё из Plus в 5 раз больше",
                "⏳ Лимиты в 2 раза больше",
            ),
            onLimit = 500,
            offLimit = Int.MAX_VALUE,
        ),
        Tier(
            name = "пиздец",
            price = "67676767676767676767 ₽ / сек",
            badge = "пиздец",
            accent = Color(0xFF4D9BFF),
            perks = listOf(
                "все нахуй разблокированно",
                "бесконечные включение и выключение",
            ),
            onLimit = Int.MAX_VALUE,
            offLimit = Int.MAX_VALUE,
        ),
    )

    const val PREMIUM_PLUS = 3
    const val ULTRA = 5

    /** Buyable top-up packs (докупка лимитов). */
    data class TopUp(val label: String, val price: String, val onCredits: Int, val offCredits: Int)

    val TOP_UPS: List<TopUp> = listOf(
        TopUp("+10 включений", "99 ₽ ", onCredits = 10, offCredits = 0),
        TopUp("+10 выключений", "600 ₽", onCredits = 0, offCredits = 10),
        TopUp("+50 вкл / +50 выкл", "699 ₽", onCredits = 50, offCredits = 50),
        TopUp("Мега-пак ×250/×250", "1 999 ₽", onCredits = 250, offCredits = 250),
        TopUp("УЛЬТРА", "200000 ₽", onCredits = 9999999, offCredits = 9999999),
        TopUp("молодежный 67 (скидка 1%)", "67 ₽ ", onCredits = 6, offCredits = 7),
        TopUp("Бесплатный", "ЬЕСПЛАТНО ", onCredits = 0, offCredits = 0),
    )
}

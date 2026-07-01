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
            name = "Premium",
            price = "999 ₽ / мес",
            badge = "PREMIUM 👑",
            accent = Color(0xFFFFD34D),
            perks = listOf(
                "♾️ Безлимитные выключения света",
                "🎨 Все цвета свечения",
                "🚫 Никакой рекламы",
                "📖 Полная сюжетка, включая финал",
                "🔋 +500% к яркости (не работает)",
                "⏳ Лимит за 10 ч: 100 включений, выключения ∞",
            ),
            onLimit = 100,
            offLimit = Int.MAX_VALUE,
        ),
        Tier(
            name = "Premium+",
            price = "1 999 ₽ / мес",
            badge = "PREMIUM+ 💡",
            accent = Color(0xFFB44DFF),
            perks = listOf(
                "Всё из Premium",
                "💡 Поддержка лампочек RGB",
                "🌞 Разблокировка Солнца Pro",
                "🔦 Ускорение фотонов на 300%",
                "⏳ Лимит за 10 ч: 500 включений, выключения ∞",
            ),
            onLimit = 500,
            offLimit = Int.MAX_VALUE,
        ),
        Tier(
            name = "Ultra",
            price = "4 999 ₽ / мес",
            badge = "ULTRA ⚡",
            accent = Color(0xFF4D9BFF),
            perks = listOf(
                "Всё из Premium+",
                "📡 Приоритетный доступ к электричеству",
                "🛰️ Синхронизация с МКС",
                "👑 Секретная кнопка «Светить сильнее»",
                "⏳ Включения и выключения без 10-часовых лимитов",
            ),
            onLimit = Int.MAX_VALUE,
            offLimit = Int.MAX_VALUE,
        ),
        Tier(
            name = "Экспериментальный",
            price = "9 999 ₽ / мес",
            badge = "EXPERIMENTAL 🧪",
            accent = Color(0xFF4DFF7A),
            perks = listOf(
                "Всё из Ultra",
                "⚡ Использование батареи телефона (бета)",
                "🧪 Нестабильные бета-фотоны",
                "🔬 Доступ к тёмной материи",
                "⏳ Без 10-часовых лимитов",
            ),
            onLimit = Int.MAX_VALUE,
            offLimit = Int.MAX_VALUE,
        ),
        Tier(
            name = "Божественный",
            price = "99 999 ₽ / мес",
            badge = "GOD TIER 🌌",
            accent = Color(0xFFFF4DC4),
            perks = listOf(
                "Всё и сразу",
                "🌌 Личное управление Солнцем",
                "🪐 Свой фотонный завод",
                "😇 Отпущение грехов от «Еблан Софт»",
                "⏳ Абсолютно без лимитов (честно)",
            ),
            onLimit = Int.MAX_VALUE,
            offLimit = Int.MAX_VALUE,
        ),
    )

    const val PREMIUM_PLUS = 2
    const val ULTRA = 3

    /** Buyable top-up packs (докупка лимитов). */
    data class TopUp(val label: String, val price: String, val onCredits: Int, val offCredits: Int)

    val TOP_UPS: List<TopUp> = listOf(
        TopUp("+10 включений", "199 ₽", onCredits = 10, offCredits = 0),
        TopUp("+10 выключений", "199 ₽", onCredits = 0, offCredits = 10),
        TopUp("+50 вкл / +50 выкл", "699 ₽", onCredits = 50, offCredits = 50),
        TopUp("Мега-пак ×250/×250", "1 999 ₽", onCredits = 250, offCredits = 250),
    )
}

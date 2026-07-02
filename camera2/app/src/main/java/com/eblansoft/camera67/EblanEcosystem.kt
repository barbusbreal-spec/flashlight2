package com.eblansoft.camera67

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Экосистема «Еблан Софт» ✅ — бесшовная интеграция двух флагманов:
 * EBLAN Camera 67 и Фонарик 2 Ultimate. Как у эпла, только теплее.
 */
object EblanEcosystem {

    private val FLASHLIGHT_PACKAGES = listOf(
        "com.eblansoft.flashlight2",
        "com.eblansoft.flashlight2.debug",
    )

    /** Стоит ли у пацана Фонарик 2 Ultimate. */
    fun isFlashlight2Installed(context: Context): Boolean =
        FLASHLIGHT_PACKAGES.any { pkg ->
            runCatching { context.packageManager.getLaunchIntentForPackage(pkg) }
                .getOrNull() != null
        }

    /**
     * Запускаем Фонарик 2: сперва по фирменному deep link
     * `flashlight://open`, если не вышло — обычным launch intent.
     * @return true, если синергия состоялась 🤝
     */
    fun openFlashlight2(context: Context): Boolean {
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse("flashlight://open/home"))
        if (runCatching { context.startActivity(deepLink); true }.getOrDefault(false)) {
            return true
        }
        for (pkg in FLASHLIGHT_PACKAGES) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            if (runCatching { context.startActivity(launch); true }.getOrDefault(false)) {
                return true
            }
        }
        return false
    }
}

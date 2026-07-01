package com.eblansoft.flashlight2.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom

/** Result of a single security gate check. */
data class SecurityCheck(
    val title: String,
    val passed: Boolean,
    val detail: String,
)

/**
 * The «Еблан Софт» anti-tamper gate. Before you're allowed to point a flashlight
 * at anything, we verify three things, «вдруг взломаешь нахуй»:
 *
 *  1. Key Attestation — the device has a locked bootloader / verified boot and
 *     can produce a hardware-backed key (TEE/StrongBox).
 *  2. Root detection — no su binary, root manager or test-keys build.
 *  3. Play Integrity — the device looks genuine (not an emulator / tampered ROM).
 *
 * The Play Integrity check here is a local heuristic: a full verdict requires the
 * Play Integrity API plus a server that decodes the token. We fake the verdict
 * shape (`MEETS_DEVICE_INTEGRITY`) but base it on real signals we can read offline.
 */
class SecurityChecker(private val context: Context) {

    fun runAll(): List<SecurityCheck> = listOf(
        checkKeyAttestation(),
        checkRoot(),
        checkPlayIntegrity(),
    )

    // ---- 1. Key attestation / locked bootloader ----------------------------

    private fun checkKeyAttestation(): SecurityCheck {
        val bootState = systemProp("ro.boot.verifiedbootstate").lowercase()
        val locked = systemProp("ro.boot.flash.locked")
        val vbmeta = systemProp("ro.boot.vbmeta.device_state").lowercase()

        // "green" = verified boot chain intact; "orange"/"yellow"/"red" = unlocked/tampered.
        val bootUnlocked = bootState == "orange" || bootState == "red" ||
            vbmeta == "unlocked" || locked == "0"

        val hardwareBacked = hasHardwareBackedKey()

        val passed = hardwareBacked && !bootUnlocked
        val detail = buildString {
            append(if (hardwareBacked) "Ключ в TEE/StrongBox ✓" else "Нет аппаратного ключа ✗")
            append(" · verified boot: ")
            append(bootState.ifBlank { "неизвестно" })
        }
        return SecurityCheck("Key Attestation (бутлоадер)", passed, detail)
    }

    /** Generates a throwaway attestation key and checks it lives in secure hardware. */
    private fun hasHardwareBackedKey(): Boolean = runCatching {
        val alias = "fl2_attest_probe"
        val challenge = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore",
        )
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)
                .build()
        )
        val keyPair = kpg.generateKeyPair()

        val factory = KeyFactory.getInstance(keyPair.private.algorithm, "AndroidKeyStore")
        val keyInfo = factory.getKeySpec(keyPair.private, KeyInfo::class.java)

        val secure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
        } else {
            @Suppress("DEPRECATION")
            keyInfo.isInsideSecureHardware
        }

        // Clean up the probe key so we don't leave junk in the keystore.
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        }
        secure
    }.getOrDefault(false)

    // ---- 2. Root detection -------------------------------------------------

    private fun checkRoot(): SecurityCheck {
        val reasons = mutableListOf<String>()

        if (Build.TAGS?.contains("test-keys") == true) reasons += "test-keys"

        val suFound = SU_PATHS.any { runCatching { File(it).exists() }.getOrDefault(false) }
        if (suFound) reasons += "su binary"

        val magiskFound = MAGISK_PATHS.any { runCatching { File(it).exists() }.getOrDefault(false) }
        if (magiskFound) reasons += "Magisk"

        val rootApp = ROOT_PACKAGES.firstOrNull { isPackageInstalled(it) }
        if (rootApp != null) reasons += "root-менеджер"

        if (canExecuteSu()) reasons += "su в PATH"

        val passed = reasons.isEmpty()
        val detail = if (passed) "Root не обнаружен ✓" else "Найдено: ${reasons.joinToString(", ")}"
        return SecurityCheck("Root Detection", passed, detail)
    }

    private fun canExecuteSu(): Boolean = runCatching {
        val process = ProcessBuilder("which", "su").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.isNotEmpty()
    }.getOrDefault(false)

    // ---- 3. Play Integrity (heuristic device integrity) --------------------

    private fun checkPlayIntegrity(): SecurityCheck {
        val emulator = isProbablyEmulator()
        val playStore = isPackageInstalled("com.android.vending")

        // Emulators fail device integrity; a missing Play Store fails app integrity.
        val passed = !emulator && playStore
        val verdict = when {
            emulator -> "MEETS_BASIC_INTEGRITY только (эмулятор)"
            !playStore -> "Нет Play Store — не сертифицировано"
            else -> "MEETS_DEVICE_INTEGRITY ✓"
        }
        return SecurityCheck("Play Integrity", passed, verdict)
    }

    private fun isProbablyEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        return fp.startsWith("generic") || fp.startsWith("unknown") ||
            fp.contains("emulator") || fp.contains("vbox") || fp.contains("test-keys") ||
            model.contains("emulator") || model.contains("android sdk built for") ||
            model.contains("sdk_gphone") ||
            product.contains("sdk") || product.contains("emulator") ||
            hardware == "goldfish" || hardware == "ranchu" || hardware.contains("vbox") ||
            manufacturer.contains("genymotion")
    }

    // ---- helpers -----------------------------------------------------------

    private fun isPackageInstalled(pkg: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    }.getOrDefault(false)

    /** Reads a build system property via reflection; "" if unavailable. */
    private fun systemProp(key: String): String = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String).orEmpty()
    }.getOrDefault("")

    private companion object {
        val SU_PATHS = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/system/app/Superuser.apk", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/vendor/bin/su", "/system/xbin/daemonsu",
        )
        val MAGISK_PATHS = listOf(
            "/sbin/.magisk", "/data/adb/magisk", "/data/adb/modules",
            "/cache/.disable_magisk", "/sbin/magisk",
        )
        val ROOT_PACKAGES = listOf(
            "com.topjohnwu.magisk", "eu.chainfire.supersu",
            "com.noshufou.android.su", "com.noshufou.android.su.elite",
            "com.koushikdutta.superuser", "com.thirdparty.superuser",
            "com.yellowes.su", "com.kingroot.kinguser", "com.kingo.root",
        )
    }
}

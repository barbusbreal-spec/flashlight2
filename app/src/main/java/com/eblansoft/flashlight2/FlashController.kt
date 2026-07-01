package com.eblansoft.flashlight2

import android.content.Context
import android.hardware.camera2.CameraManager

/**
 * Thin wrapper around the camera torch. Fails quietly on devices/emulators
 * without a flash unit so the rest of «Фонарик 2 Ultimate» keeps working
 * (the on-screen light mode covers those cases).
 */
class FlashController(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /** Id of the first camera that actually has a flash unit, or null. */
    private val torchCameraId: String? = runCatching {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    val hasTorch: Boolean get() = torchCameraId != null

    /** Returns true if the hardware state was changed successfully. */
    fun setTorch(enabled: Boolean): Boolean {
        val id = torchCameraId ?: return false
        return runCatching { cameraManager.setTorchMode(id, enabled); true }
            .getOrDefault(false)
    }

    fun release() {
        // Never leave the torch burning when we go away.
        runCatching { torchCameraId?.let { cameraManager.setTorchMode(it, false) } }
    }
}

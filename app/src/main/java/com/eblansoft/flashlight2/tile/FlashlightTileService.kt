package com.eblansoft.flashlight2.tile

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile so the flashlight can be toggled straight from the
 * notification shade, right next to Wi-Fi and the rest.
 *
 * The tile talks to the camera torch directly and stays in sync with the app
 * via [CameraManager.TorchCallback], so toggling in one place updates the other.
 */
class FlashlightTileService : TileService() {

    private val cameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as CameraManager
    }

    private var torchCameraId: String? = null
    private var torchOn = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                torchOn = enabled
                updateTile()
            }
        }
    }

    private fun ensureCamera() {
        if (torchCameraId != null) return
        torchCameraId = runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    override fun onStartListening() {
        ensureCamera()
        runCatching { cameraManager.registerTorchCallback(torchCallback, null) }
        updateTile()
    }

    override fun onStopListening() {
        runCatching { cameraManager.unregisterTorchCallback(torchCallback) }
    }

    override fun onClick() {
        ensureCamera()
        val id = torchCameraId ?: return
        runCatching { cameraManager.setTorchMode(id, !torchOn) }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = when {
            torchCameraId == null -> Tile.STATE_UNAVAILABLE
            torchOn -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = "Фонарик 2"
        tile.updateTile()
    }
}

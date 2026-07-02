package com.eblansoft.camera67

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eblansoft.camera67.ui.CameraScreen
import com.eblansoft.camera67.ui.PremiumScreen

private val EblanColors = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFFFFD34D),
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF14141C),
)

class MainActivity : ComponentActivity() {

    private var permissionsGranted by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            permissionsGranted = result[Manifest.permission.CAMERA] == true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MaterialTheme(colorScheme = EblanColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (permissionsGranted) {
                        EblanNav()
                    } else {
                        PermissionBegScreen {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EblanNav() {
    var showPremium by remember { mutableStateOf(false) }
    if (showPremium) {
        PremiumScreen(onBack = { showPremium = false })
    } else {
        CameraScreen(onOpenPremium = { showPremium = true })
    }
}

@Composable
private fun PermissionBegScreen(onAsk: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📸", style = MaterialTheme.typography.displayLarge)
        Text(
            "EBLAN Camera 67",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Дай доступ к камере, брат 🤝\n" +
                "Без камеры наши алгоритмы (в 67 раз круче гугловских ✅) " +
                "смогут обработать ровно 0 фоток.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAsk) {
            Text("ВЫДАТЬ ДОСТУП ✅✅✅")
        }
    }
}

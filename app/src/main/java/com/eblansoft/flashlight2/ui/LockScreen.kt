package com.eblansoft.flashlight2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.security.BiometricAuth

@Composable
fun LockScreen(state: GameState) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricReady = state.biometricEnabled && activity != null && BiometricAuth.isAvailable(activity)

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun promptBiometric() {
        val act = activity ?: return
        BiometricAuth.prompt(act, onSuccess = { state.onAuthPassed() })
    }

    // Offer the fingerprint automatically on first show.
    LaunchedEffect(biometricReady) {
        if (biometricReady) promptBiometric()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Фонарик заблокирован",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Еблан Софт бережёт ваш свет",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(28.dp))

            if (state.passwordEnabled) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; error = false } },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text("Неверный PIN", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { if (!state.checkPassword(pin)) { error = true; pin = "" } },
                    enabled = pin.length >= 4,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Разблокировать") }
            }

            if (biometricReady) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { promptBiometric() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("  Отпечаток / лицо")
                }
            }

            if (!state.passwordEnabled && !biometricReady) {
                // Biometric enabled but unavailable and no PIN — don't lock the user out.
                Text(
                    "Способ разблокировки недоступен на этом устройстве.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Button(
                    onClick = { state.onAuthPassed() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Войти") }
            }
        }
    }
}

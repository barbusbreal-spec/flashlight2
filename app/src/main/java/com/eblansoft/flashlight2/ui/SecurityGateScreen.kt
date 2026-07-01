package com.eblansoft.flashlight2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.security.SecurityCheck
import kotlinx.coroutines.launch

@Composable
fun SecurityGateScreen(state: GameState, onPassed: () -> Unit) {
    val scope = rememberCoroutineScope()

    // Run the checks once when the gate first appears.
    LaunchedEffect(Unit) {
        if (!state.securityChecked && !state.securityChecking) {
            state.runSecurityChecks()
        }
    }

    val allGreen = state.securityChecked && state.securityPassed

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = if (allGreen) Color(0xFF4DFF7A) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Проверка безопасности",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Еблан Софт защищает ваш фонарик",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(24.dp))

            if (state.securityChecking) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Проверяем бутлоадер, root и Play Integrity…",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            } else {
                state.securityResults.forEach { CheckRow(it) }
            }

            Spacer(Modifier.weight(1f))

            when {
                state.securityChecking -> Unit

                allGreen -> {
                    Text(
                        "Все галочки зелёные. Доступ разрешён.",
                        color = Color(0xFF4DFF7A),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onPassed,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("Войти в фонарик", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                else -> {
                    Text(
                        "Доступ запрещён.\nВаше устройство не прошло проверку — вдруг взломаете. " +
                            "Разблокируйте только на стоковой прошивке с заблокированным " +
                            "бутлоадером и без root.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { scope.launch { state.runSecurityChecks() } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("Проверить ещё раз")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CheckRow(check: SecurityCheck) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (check.passed) Color(0xFF4DFF7A) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    check.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    check.detail,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

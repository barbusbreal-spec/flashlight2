package com.eblansoft.flashlight2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblansoft.flashlight2.GameState
import kotlinx.coroutines.delay

/** "You're out of free turn-offs" prompt: watch an ad or go Premium. */
@Composable
fun OutOfQuotaDialog(state: GameState) {
    if (!state.showOutOfQuota) return
    AlertDialog(
        onDismissRequest = { state.dismissOutOfQuota() },
        title = { Text("Лимит выключений исчерпан") },
        text = {
            Text(
                "Сегодня вы уже выключили свет ${GameState.DAILY_FREE_TURN_OFFS} раз.\n\n" +
                    "Посмотрите рекламу, чтобы получить ещё одно выключение, " +
                    "или оформите Premium для безлимита."
            )
        },
        confirmButton = {
            Button(onClick = { state.startAd() }) { Text("Смотреть рекламу (+1)") }
        },
        dismissButton = {
            TextButton(onClick = { state.openPaywallFromQuota() }) { Text("Premium ∞") }
        },
    )
}

/** A fake full-screen rewarded ad with an un-skippable countdown. */
@Composable
fun AdOverlay(state: GameState) {
    if (!state.showAd) return
    var secondsLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        secondsLeft = 5
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101018)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("реклама", color = Color(0xFF6A6A7A), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF23233A)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "🧴\n\nМЕГА КАПСУЛЫ 3000\nстирают даже совесть",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
            if (secondsLeft > 0) {
                Text(
                    "Награду можно забрать через $secondsLeft…",
                    color = Color(0xFFB0B0C0),
                )
            } else {
                Button(onClick = { state.onAdFinished() }) {
                    Text("Забрать +1 выключение")
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { state.dismissAd() }) {
                Text("Закрыть", color = Color(0xFF6A6A7A))
            }
        }
    }
}

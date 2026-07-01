package com.eblansoft.flashlight2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

/** Out-of-quota prompt (for turn-ON or turn-OFF): ad, top-up or Premium. */
@Composable
fun OutOfQuotaDialog(state: GameState) {
    val kind = state.outOfQuotaKind ?: return
    val action = if (kind == "on") "включений" else "выключений"
    AlertDialog(
        onDismissRequest = { state.dismissOutOfQuota() },
        title = { Text("Лимит $action исчерпан") },
        text = {
            Text(
                "На эти ${GameState.WINDOW_HOURS} часов лимит $action закончился.\n\n" +
                    "Посмотрите рекламу (+1), докупите лимиты в Настройках → Billing " +
                    "или оформите подписку для безлимита."
            )
        },
        confirmButton = {
            Button(onClick = { state.startAd() }) { Text("Реклама (+1)") }
        },
        dismissButton = {
            TextButton(onClick = { state.openPaywallFromQuota() }) { Text("Подписка ∞") }
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
                    Text("Забрать награду (+1)")
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { state.dismissAd() }) {
                Text("Закрыть", color = Color(0xFF6A6A7A))
            }
        }
    }
}

/** Claude-style in-app notification banner that slides in from the top. */
@Composable
fun NotificationBanner(state: GameState) {
    val note = state.currentNotification

    LaunchedEffect(note) {
        if (note != null) {
            delay(3800)
            state.dismissNotification()
        }
    }

    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = note != null,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
        ) {
            if (note != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF23232E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { state.dismissNotification() },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(note.emoji, fontSize = 22.sp)
                        Spacer(Modifier.padding(start = 6.dp))
                        Column(Modifier.padding(start = 6.dp)) {
                            Text(
                                note.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                            Text(
                                note.body,
                                color = Color(0xFFC7C7D2),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

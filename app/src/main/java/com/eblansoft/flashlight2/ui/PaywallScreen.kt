package com.eblansoft.flashlight2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.Screen

@Composable
fun PaywallScreen(state: GameState) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👑", fontSize = 56.sp)
            Text(
                "Фонарик 2 Ultimate\nPREMIUM",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Perk("♾️", "Безлимитные выключения света")
                    Perk("🎨", "Все цвета свечения")
                    Perk("🚫", "Никакой рекламы")
                    Perk("📖", "Полная сюжетка, включая финал")
                    Perk("😇", "Чистая совесть (не гарантируется)")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (state.premium) {
                Text(
                    "Premium уже активен. Спасибо, вы прекрасны 💛",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { state.cancelPremium() }) {
                    Text("Отменить подписку")
                }
            } else {
                Text(
                    "999 ₽ / месяц",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "первые 0 дней бесплатно",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { state.purchasePremium() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Оформить Premium", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    "* оплата ненастоящая, это демо от Еблан Софт",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { state.navigate(Screen.FLASHLIGHT) }) {
                Text("Назад к фонарику")
            }
        }
    }
}

@Composable
private fun Perk(emoji: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(0.dp))
        Text(
            "  $text",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
        )
    }
}

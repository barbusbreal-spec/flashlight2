package com.eblansoft.flashlight2.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.Screen

@Composable
fun FlashlightScreen(state: GameState) {
    // In screen-light mode with the light on, the whole screen becomes the lamp.
    val screenIsLamp = state.screenLightMode && state.lightOn
    val bg by animateColorAsState(
        targetValue = if (screenIsLamp) state.currentColor else MaterialTheme.colorScheme.background,
        label = "bg",
    )

    Surface(color = bg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!screenIsLamp) {
                Header(state)
                QuotaBanner(state)
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PowerButton(state, dimmed = screenIsLamp)
            }

            if (!screenIsLamp) {
                ColorPicker(state)
                Spacer(Modifier.height(12.dp))
                ModeAndStoryRow(state)
                Spacer(Modifier.height(12.dp))
                FakeAdBanner(state)
            }
        }
    }
}

@Composable
private fun Header(state: GameState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "Фонарик 2 Ultimate",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "by Еблан Софт",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
        if (state.premium) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "PREMIUM 👑",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        } else {
            Button(onClick = { state.navigate(Screen.PREMIUM) }) { Text("Premium") }
        }
    }
}

@Composable
private fun QuotaBanner(state: GameState) {
    val remaining = state.turnOffsRemaining
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            if (state.isUnlimited) {
                Text(
                    "Выключений сегодня: ∞ (спасибо за подписку)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    "Осталось бесплатных выключений: $remaining / ${GameState.DAILY_FREE_TURN_OFFS}",
                    color = if (remaining == 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Включать свет — бесплатно. Выключать — по лимиту.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun PowerButton(state: GameState, dimmed: Boolean) {
    val on = state.lightOn
    val ringColor = if (on) state.currentColor else MaterialTheme.colorScheme.surfaceVariant
    val iconTint = when {
        dimmed -> Color.Black.copy(alpha = 0.35f)
        on -> state.currentColor
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    }
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(
                if (dimmed) Color.Black.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface
            )
            .border(6.dp, ringColor, CircleShape)
            .clickable { state.onToggleLight() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = if (on) "Выключить" else "Включить",
                tint = iconTint,
                modifier = Modifier.size(84.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (on) "ВЫКЛЮЧИТЬ" else "ВКЛЮЧИТЬ",
                color = iconTint,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun ColorPicker(state: GameState) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Цвет свечения",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(GameState.PALETTE) { c ->
                val index = GameState.PALETTE.indexOf(c)
                val selected = index == state.colorIndex
                val locked = c.premiumOnly && !state.premium
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(c.color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                        )
                        .clickable { state.selectColor(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (locked) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Premium",
                            tint = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeAndStoryRow(state: GameState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { state.toggleScreenLightMode() },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (state.screenLightMode) "Режим: экран" else "Режим: вспышка")
        }
        Button(
            onClick = { state.navigate(Screen.STORY) },
            modifier = Modifier.weight(1f),
        ) {
            Text("Сюжетка")
        }
    }
}

@Composable
private fun FakeAdBanner(state: GameState) {
    if (state.premium) return
    Card(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A38)),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Реклама", fontSize = 10.sp, color = Color(0xFF8A8A9A))
                    Text(
                        "Купи капсулы для стирки со скидкой 3%",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    "Убрать →",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.clickable { state.navigate(Screen.PREMIUM) },
                )
            }
        }
    }
}

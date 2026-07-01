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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.Screen
import com.eblansoft.flashlight2.story.Story

@Composable
fun StoryScreen(state: GameState) {
    var current by remember { mutableIntStateOf(state.storyProgress.coerceIn(0, Story.chapters.lastIndex)) }
    val chapter = Story.chapters[current]
    val locked = chapter.premiumOnly && !state.premium

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                "Сюжетка",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
            Text(
                chapter.title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Глава ${current + 1} из ${Story.chapters.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                ) {
                    if (locked) {
                        Text("🔒", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Эта глава доступна только по Premium-подписке.\n\n" +
                                "Свобода в мире «Еблан Софт» стоит 999 ₽/мес.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { state.navigate(Screen.PREMIUM) }) {
                            Text("Разблокировать Premium")
                        }
                    } else {
                        Text(
                            chapter.body,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { if (current > 0) current-- },
                    enabled = current > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("Назад") }

                if (current < Story.chapters.lastIndex) {
                    Button(
                        onClick = {
                            current++
                            state.advanceStory(current)
                        },
                        enabled = !locked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Далее") }
                } else {
                    Button(
                        onClick = { state.navigate(Screen.FLASHLIGHT) },
                        modifier = Modifier.weight(1f),
                    ) { Text("В меню") }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { state.navigate(Screen.FLASHLIGHT) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) { Text("К фонарику") }
        }
    }
}

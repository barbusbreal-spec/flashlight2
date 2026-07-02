package com.eblansoft.camera67.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.eblansoft.camera67.Prefs
import kotlinx.coroutines.delay

private val PERKS = listOf(
    "✅ Съёмка видео НЕОГРАНИЧЕННА на минуте (ровно 1:00, безлимитно)",
    "✅ Те же 12 фото в день, но с ощущением превосходства",
    "✅ Ватермарка EBLAN Camera 67 остаётся (это фича, не баг)",
    "✅ Алгоритмы становятся в 67 раз круче в 67 раз чаще",
    "✅ AI 777 67 УЛЬТРА++++ с пометкой AI ✨ (есть и так, но теперь премиально)",
    "🤝 Галочка ⭐ PREMIUM в углу экрана — уважение пацанов",
    "🤝 Поддержка «Еблан Софт» морально",
)

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var isPremium by remember { mutableStateOf(prefs.isPremium) }
    var buying by remember { mutableStateOf(false) }

    LaunchedEffect(buying) {
        if (buying) {
            delay(1800) // очень сложная платёжная система
            prefs.isPremium = true
            isPremium = true
            buying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("⭐", style = MaterialTheme.typography.displayLarge)
        Text(
            "EBLAN PREMIUM 67",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "67₽/мес",
                style = MaterialTheme.typography.titleLarge,
                textDecoration = TextDecoration.LineThrough,
            )
            Text(
                "  0₽ НАВСЕГДА*",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PERKS.forEach { perk ->
                    Text(perk, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        when {
            buying -> {
                CircularProgressIndicator()
                Text("Проводим платёж 0₽ через блокчейн… 🤝")
            }
            isPremium -> {
                Text(
                    "ТЫ ПРЕМИУМ, БРАТ ⭐✅🤝",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                TextButton(onClick = {
                    prefs.isPremium = false
                    isPremium = false
                }) {
                    Text("отменить подписку (зачем??) 💀")
                }
            }
            else -> {
                Button(onClick = { buying = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("ОФОРМИТЬ ЗА 0₽ ✅✅✅")
                }
            }
        }

        Text(
            "* — потому что деньги мы брать не умеем. " +
                "Лимиты не меняются, ватермарка вечна, безлимит равен минуте. " +
                "Оферта одобрена юристами «Еблан Софт».",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )

        TextButton(onClick = onBack) { Text("← назад к съёмке") }
        Spacer(Modifier.height(16.dp))
    }
}

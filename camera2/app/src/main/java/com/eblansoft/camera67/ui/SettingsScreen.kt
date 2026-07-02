package com.eblansoft.camera67.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import com.eblansoft.camera67.Prefs

/**
 * Настройки ебейшести. Каждая крутилка реально влияет на пайплайн
 * HDR RAW 67228+++++. Гугл такое прячет, мы — выдаём.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var burst by remember { mutableIntStateOf(prefs.burstFrames) }
    var juiciness by remember { mutableIntStateOf(prefs.juiciness) }
    var sharpness by remember { mutableIntStateOf(prefs.sharpness) }
    var glow by remember { mutableIntStateOf(prefs.glowAlpha) }
    var vignette by remember { mutableIntStateOf(prefs.vignette) }
    var warmth by remember { mutableIntStateOf(prefs.warmth) }
    var quality by remember { mutableIntStateOf(prefs.jpegQuality) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "⚙️ НАСТРОЙКИ ЕБЕЙШЕСТИ",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            "Каждая крутилка реально влияет на пайплайн HDR RAW 67228+++++ ✅",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "📸 HDR RAW стек (кадров с разной экспозицией)",
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BurstOption("1 · бомж 😔", burst == 1) {
                        burst = 1; prefs.burstFrames = 1
                    }
                    BurstOption("3 · норм ✅", burst == 3) {
                        burst = 3; prefs.burstFrames = 3
                    }
                    BurstOption("5 · ебейший 🥵", burst == 5) {
                        burst = 5; prefs.burstFrames = 5
                    }
                }
                Text(
                    "Камера снимает стек кадров с EV-брекетингом (недодержка → " +
                        "света, передержка → тени) и сплавляет их по Мертенсу. " +
                        "Как HDR+ у гугла, но плюсов больше. Чем больше кадров — " +
                        "тем дольше страдает нейросеть.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        TuningSlider("🌈 Сочность", juiciness, 100..200, "%") {
            juiciness = it; prefs.juiciness = it
        }
        TuningSlider("🔪 Резкость", sharpness, 0..200, "%") {
            sharpness = it; prefs.sharpness = it
        }
        TuningSlider("✨ AI-глоу", glow, 0..128, "") {
            glow = it; prefs.glowAlpha = it
        }
        TuningSlider("🎬 Виньетка", vignette, 0..100, "%") {
            vignette = it; prefs.vignette = it
        }
        TuningSlider("🔥 Теплота (минус — морг, плюс — закат)", warmth, -10..10, "") {
            warmth = it; prefs.warmth = it
        }
        TuningSlider("💾 Качество JPEG (меньше 67 нельзя, политика)", quality, 67..100, "") {
            quality = it; prefs.jpegQuality = it
        }

        TextButton(
            onClick = {
                prefs.resetTuning()
                burst = prefs.burstFrames
                juiciness = prefs.juiciness
                sharpness = prefs.sharpness
                glow = prefs.glowAlpha
                vignette = prefs.vignette
                warmth = prefs.warmth
                quality = prefs.jpegQuality
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("↩️ сбросить как настроили инженеры «Еблан Софт»")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("ℹ️ О ПРИЛОЖЕНИИ", fontWeight = FontWeight.Bold)
                Text("Версия: 67.228.1488-halal ✅", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Алгоритм: eblanHRRrawHDR77722867++ (ультра секретный 🔒)",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Бурмалда-алгоритмов в пайплайне: 67 (все нужные)",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Сертификация: 🟢 HALAL ✅", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Дизайн: Material Design 3 Expressive 6767",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Издатель: «Еблан Софт» — экосистема из двух флагманов 🤝",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Text(
            "Ватермарка и лимит 12 фото не настраиваются. Даже не ищи. ✅",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("← назад к съёмке")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BurstOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.Black else Color.White,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(
                if (selected) Color(0xFFFFD34D) else Color(0x33FFFFFF),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun TuningSlider(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onChange: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, fontWeight = FontWeight.Bold)
                Text("$value$unit", color = Color(0xFFFFD34D), fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
            )
        }
    }
}

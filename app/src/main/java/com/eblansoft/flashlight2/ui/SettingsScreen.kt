package com.eblansoft.flashlight2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.eblansoft.flashlight2.GameState
import com.eblansoft.flashlight2.Screen
import com.eblansoft.flashlight2.Tiers
import com.eblansoft.flashlight2.security.BiometricAuth

private enum class SettingsTab(val label: String, val icon: ImageVector) {
    ACCOUNT("Account", Icons.Filled.AccountCircle),
    PRIVACY("Privacy", Icons.Filled.Shield),
    BILLING("Billing", Icons.Filled.CreditCard),
    USAGE("Usage", Icons.Filled.BarChart),
}

@Composable
fun SettingsScreen(state: GameState) {
    var tab by remember { mutableStateOf(SettingsTab.ACCOUNT) }

    LaunchedEffect(tab) {
        if (tab == SettingsTab.BILLING) state.onOpenBilling()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { state.navigate(Screen.FLASHLIGHT) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "Настройки",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Row(Modifier.fillMaxSize()) {
                // Left rail — like the Claude account modal.
                Column(
                    Modifier
                        .width(96.dp)
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SettingsTab.entries.forEach { t ->
                        RailItem(t, selected = t == tab) { tab = t }
                    }
                }

                Box(Modifier.weight(1f).fillMaxSize().padding(end = 16.dp, start = 4.dp)) {
                    when (tab) {
                        SettingsTab.ACCOUNT -> AccountTab(state) { tab = SettingsTab.BILLING }
                        SettingsTab.PRIVACY -> PrivacyTab(state)
                        SettingsTab.BILLING -> BillingTab(state)
                        SettingsTab.USAGE -> UsageTab(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun RailItem(tab: SettingsTab, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(tab.label, fontSize = 11.sp, color = tint)
    }
}

// ---- Account ---------------------------------------------------------------

@Composable
private fun AccountTab(state: GameState, goBilling: () -> Unit) {
    ScrollColumn {
        SectionTitle("Аккаунт")
        InfoCard {
            KeyValue("Пользователь", "гость@еблансофт.рф")
            KeyValue("Тарифный план", Tiers.ALL[state.tier].name)
            KeyValue("Статус", if (state.premium) "Активна подписка" else "Бесплатный пользователь")
            KeyValue("В семье «Еблан Софт»", "с 2026 года")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = goBilling, modifier = Modifier.fillMaxWidth()) {
            Text("Управление подпиской")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Text("Выйти (никуда)")
        }
    }
}

// ---- Privacy ---------------------------------------------------------------

@Composable
private fun PrivacyTab(state: GameState) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var pin by remember { mutableStateOf("") }

    ScrollColumn {
        SectionTitle("Приватность")

        InfoCard {
            ToggleRow(
                title = "Пароль на фонарик",
                subtitle = "Спрашивать PIN при запуске",
                checked = state.passwordEnabled,
                onChange = { on -> if (!on) state.disablePassword() },
            )
            if (!state.passwordEnabled) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("Новый PIN (до 6 цифр)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { if (pin.length >= 4) { state.setPassword(pin); pin = "" } },
                    enabled = pin.length >= 4,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Установить пароль") }
            }
        }

        Spacer(Modifier.height(12.dp))

        InfoCard {
            ToggleRow(
                title = "Биометрия (отпечаток)",
                subtitle = "Разблокировка фонарика по отпечатку / лицу",
                checked = state.biometricEnabled,
                onChange = { on -> state.toggleBiometric(on) },
            )
            val available = activity != null && BiometricAuth.isAvailable(activity)
            if (!available) {
                Text(
                    "На устройстве не настроена биометрия — пригодится пароль.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "«Еблан Софт» не собирает ваши данные. Мы их просто выдумываем.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        )
    }
}

// ---- Billing ---------------------------------------------------------------

@Composable
private fun BillingTab(state: GameState) {
    ScrollColumn {
        SectionTitle("Подписки")
        Tiers.ALL.forEachIndexed { index, tier ->
            val current = index == state.tier
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (current) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tier.name, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            color = tier.accent)
                        Text(tier.price, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(6.dp))
                    tier.perks.forEach { perk ->
                        Text("• $perk", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(vertical = 1.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    if (current) {
                        Text("Текущий план ✓", color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    } else {
                        Button(
                            onClick = { state.purchaseTier(index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (index == 0) "Перейти на Free" else "Оформить ${tier.name}") }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle("Докупить лимиты")
        Text(
            "Кончились включения/выключения раньше сброса? Докупите.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Tiers.TOP_UPS.forEach { pack ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(pack.label, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(pack.price, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(onClick = { state.buyTopUp(pack) }) { Text("Купить") }
                }
            }
        }
        Text(
            "* все платежи ненастоящие, деньги воображаемые",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

// ---- Usage -----------------------------------------------------------------

@Composable
private fun UsageTab(state: GameState) {
    ScrollColumn {
        SectionTitle("Использование")
        InfoCard {
            KeyValue("Включений осталось", if (state.onUnlimited) "∞" else state.onRemaining.toString())
            KeyValue("Выключений осталось", if (state.offUnlimited) "∞" else state.offRemaining.toString())
            KeyValue("Докуплено включений", "+${state.boughtOnCredits}")
            KeyValue("Докуплено выключений", "+${state.boughtOffCredits}")
            KeyValue("Сброс лимитов через", formatDuration(state.windowResetInMs))
        }
        Spacer(Modifier.height(12.dp))
        InfoCard {
            KeyValue("Всего выключений за всё время", state.totalTurnOffsEver.toString())
            KeyValue("Нажатий «Светить сильнее»", state.secretPresses.toString())
            KeyValue("Синхронизаций с МКС", "0")
            KeyValue("Ускорено фотонов", "0")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Окно лимитов: ${GameState.WINDOW_HOURS} часов.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

// ---- shared bits -----------------------------------------------------------

@Composable
private fun ScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        content = content,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

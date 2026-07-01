package com.eblansoft.flashlight2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.eblansoft.flashlight2.dep.DepApi
import com.eblansoft.flashlight2.dep.DepAuthConfig
import com.eblansoft.flashlight2.security.BiometricAuth
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SettingsTab(val label: String, val icon: ImageVector) {
    ACCOUNT("Account", Icons.Filled.AccountCircle),
    PRIVACY("Privacy", Icons.Filled.Shield),
    BILLING("Billing", Icons.Filled.CreditCard),
    USAGE("Usage", Icons.Filled.BarChart),
    ABOUT("About", Icons.Filled.Info),
    DEV("Dev", Icons.Filled.Terminal),
}

@Composable
fun SettingsScreen(state: GameState) {
    var tab by remember {
        mutableStateOf(
            when (state.consumePendingSettingsTab()) {
                "privacy" -> SettingsTab.PRIVACY
                "billing" -> SettingsTab.BILLING
                "usage" -> SettingsTab.USAGE
                "about" -> SettingsTab.ABOUT
                else -> SettingsTab.ACCOUNT
            }
        )
    }

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
                    SettingsTab.entries
                        .filter { it != SettingsTab.DEV || state.devModeUnlocked }
                        .forEach { t -> RailItem(t, selected = t == tab) { tab = t } }
                }

                Box(Modifier.weight(1f).fillMaxSize().padding(end = 16.dp, start = 4.dp)) {
                    when (tab) {
                        SettingsTab.ACCOUNT -> AccountTab(state) { tab = SettingsTab.BILLING }
                        SettingsTab.PRIVACY -> PrivacyTab(state)
                        SettingsTab.BILLING -> BillingTab(state)
                        SettingsTab.USAGE -> UsageTab(state)
                        SettingsTab.ABOUT -> AboutTab(state)
                        SettingsTab.DEV -> DevTab(state)
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

        Spacer(Modifier.height(20.dp))
        SectionTitle("Вход через DEP ID")
        DepAccountSection(state)
    }
}

@Composable
private fun DepAccountSection(state: GameState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dep = state.dep

    LaunchedEffect(dep.loggedIn) {
        if (dep.loggedIn && dep.profile == null) dep.loadProfile()
    }

    if (!DepAuthConfig.isConfigured) {
        InfoCard {
            Text(
                "OAuth-сервис не настроен: заполните CLIENT_ID и CLIENT_SECRET " +
                    "в DepAuthConfig.kt после регистрации в DEP ID → Панель разработчика.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }

    if (!dep.loggedIn) {
        InfoCard {
            Text(
                "Войдите через DEP ID, чтобы видеть профиль, друзей и крутить слоты казино.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Button(
                onClick = {
                    val url = dep.buildAuthorizeUrl()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                enabled = !dep.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Войти через DEP ID") }
        }
    } else {
        val profile = dep.profile
        InfoCard {
            if (profile == null) {
                Text(
                    if (dep.loading) "Загрузка профиля…" else "Профиль недоступен",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                KeyValue("Имя", profile.username)
                KeyValue("Friend code", profile.friendCode)
                KeyValue("DepCoins", profile.depcoins.toString())
                KeyValue("Долг казино", profile.debt.toString())
                KeyValue("Друзей", profile.friends.size.toString())
                if (profile.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        profile.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { scope.launch { dep.logout() } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Выйти из DEP ID") }
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle("Казино DEP")
        DepCasinoWidget(state)
    }

    dep.error?.let { message ->
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { dep.dismissError() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun DepCasinoWidget(state: GameState) {
    val dep = state.dep
    val scope = rememberCoroutineScope()
    var bet by remember { mutableStateOf("10") }

    LaunchedEffect(Unit) { if (dep.casino == null) dep.loadCasino() }

    InfoCard {
        val balance = dep.casino
        KeyValue("DepCoins", balance?.depcoins?.toString() ?: "…")
        KeyValue("Долг", balance?.debt?.toString() ?: "…")

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = bet,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) bet = it },
            label = { Text("Ставка") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { bet.toIntOrNull()?.takeIf { it > 0 }?.let { b -> scope.launch { dep.spin(b) } } },
            enabled = !dep.loading && bet.toIntOrNull()?.let { it > 0 } == true,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (dep.loading) "Крутим…" else "Крутить слоты") }

        dep.lastSpin?.let { spin ->
            Spacer(Modifier.height(10.dp))
            val label = when (spin.result) {
                "jackpot" -> "🎰 ДЖЕКПОТ ×5!"
                "win" -> "🎉 Выигрыш ×1.5"
                "small_win" -> "↩️ Частичный возврат"
                else -> "💸 Проигрыш"
            }
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Ставка ${spin.bet} → выигрыш ${spin.win} · баланс ${spin.balanceAfter}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
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

// ---- About -------------------------------------------------------------

@Composable
private fun AboutTab(state: GameState) {
    val context = LocalContext.current
    val packageInfo = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    val versionName = packageInfo?.versionName ?: "?"
    @Suppress("DEPRECATION")
    val versionCode = packageInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong()
    } ?: 0L

    ScrollColumn {
        SectionTitle("О программе")
        InfoCard {
            Text("🔦", fontSize = 40.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Фонарик 2 Ultimate", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Издатель: Еблан Софт", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Версия $versionName (build $versionCode)" + if (state.devModeUnlocked) " · DEV" else "",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { state.tapVersionNumber() },
            )
            if (!state.devModeUnlocked && state.versionTapCount > 0) {
                Text(
                    "Тапнуто: ${state.versionTapCount}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        InfoCard {
            Text(
                "Фонарик 2 Ultimate — сатира на мобильную монетизацию: включать " +
                    "свет бесплатно, выключать — по лимиту. Реклама, подписки, " +
                    "казино через DEP ID и сюжетка про мир Вечной Тьмы прилагаются.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }

        Spacer(Modifier.height(12.dp))
        InfoCard {
            KeyValue("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            KeyValue("Устройство", "${Build.MANUFACTURER} ${Build.MODEL}")
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "© 2026 Еблан Софт. Все права никому не принадлежат.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        )
    }
}

// ---- Dev (secret) --------------------------------------------------------

@Composable
private fun DevTab(state: GameState) {
    ScrollColumn {
        SectionTitle("Дев-меню 🛠️")
        Text(
            "Секретный раздел. Всё здесь по-настоящему работает — но абсолютно бесполезно.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        DevDeviceInfoCard()
        Spacer(Modifier.height(12.dp))
        DevStrobeCard(state)
        Spacer(Modifier.height(12.dp))
        DevVibrationCard()
        Spacer(Modifier.height(12.dp))
        DevLightSensorCard()
        Spacer(Modifier.height(12.dp))
        DevNetworkCard()
        Spacer(Modifier.height(12.dp))
        DevCacheCard()
        Spacer(Modifier.height(12.dp))
        DevExportCard(state)
        Spacer(Modifier.height(12.dp))
        DevCheatsCard(state)
        Spacer(Modifier.height(12.dp))
        DevSunCard()
        Spacer(Modifier.height(12.dp))
        DevFactsCard()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DevDeviceInfoCard() {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val uptimeMin = remember { SystemClock.elapsedRealtime() / 60000L }
    val battery = remember {
        runCatching {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        }.getOrNull()
    }

    SectionTitle("Инфо об устройстве")
    InfoCard {
        KeyValue("Модель", "${Build.MANUFACTURER} ${Build.MODEL}")
        KeyValue("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        KeyValue("Экран", "${metrics.widthPixels}×${metrics.heightPixels} @ ${metrics.densityDpi}dpi")
        KeyValue("Локаль", Locale.getDefault().toString())
        KeyValue("Аптайм", "$uptimeMin мин")
        KeyValue("Батарея", battery?.let { "$it%" } ?: "недоступно")
    }
}

@Composable
private fun DevStrobeCard(state: GameState) {
    var strobeOn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { state.devRawTorch(false) }
    }

    LaunchedEffect(strobeOn) {
        if (!strobeOn) { state.devRawTorch(false); return@LaunchedEffect }
        val dot = 200L; val dash = 600L; val gap = 200L; val letterGap = 400L; val wordGap = 1000L
        try {
            while (true) {
                repeat(3) { state.devRawTorch(true); delay(dot); state.devRawTorch(false); delay(gap) }
                delay(letterGap)
                repeat(3) { state.devRawTorch(true); delay(dash); state.devRawTorch(false); delay(gap) }
                delay(letterGap)
                repeat(3) { state.devRawTorch(true); delay(dot); state.devRawTorch(false); delay(gap) }
                delay(wordGap)
            }
        } finally {
            state.devRawTorch(false)
        }
    }

    SectionTitle("SOS-фонарик (морзянка)")
    InfoCard {
        if (!state.hasTorch) {
            Text(
                "На этом устройстве нет вспышки.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                "Мигает настоящей вспышкой в азбуке Морзе: ... --- ...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Button(
                onClick = { strobeOn = !strobeOn },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (strobeOn) "Остановить SOS" else "Запустить SOS") }
        }
    }
}

@Composable
private fun DevVibrationCard() {
    val context = LocalContext.current

    SectionTitle("Тест вибрации")
    InfoCard {
        Button(
            onClick = {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                        ?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                val pattern = longArrayOf(0, 100, 80, 100, 80, 200)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Тряхнуть телефон") }
    }
}

@Composable
private fun DevLightSensorCard() {
    val context = LocalContext.current
    var lux by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { lux = event.values.firstOrNull() }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { manager?.unregisterListener(listener) }
    }

    SectionTitle("Датчик освещённости")
    InfoCard {
        Text(
            lux?.let { "${it.toInt()} люкс" } ?: "Датчик недоступен или ждём показаний…",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Совет: выключите фонарик, чтобы значение было честным.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun DevNetworkCard() {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    var pinging by remember { mutableStateOf(false) }

    SectionTitle("Пинг DEP API")
    InfoCard {
        Text(
            result ?: "Замерим задержку до DEP API.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = {
                pinging = true
                scope.launch {
                    result = runCatching { DepApi.ping() }
                        .fold({ ms -> "Пинг: $ms мс" }, { e -> "Ошибка: ${e.message}" })
                    pinging = false
                }
            },
            enabled = !pinging,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (pinging) "Пингуем…" else "Пинговать DEP API") }
    }
}

@Composable
private fun DevCacheCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }

    SectionTitle("Очистка кэша")
    InfoCard {
        Text(
            status ?: "Реально считает и чистит кэш приложения.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = {
                val before = dirSize(context.cacheDir)
                context.cacheDir.deleteRecursively()
                status = "Освобождено: ${before / 1024} КБ"
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Очистить кэш") }
    }
}

private fun dirSize(dir: File): Long =
    dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

@Composable
private fun DevExportCard(state: GameState) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    SectionTitle("Экспорт состояния")
    InfoCard {
        Text(
            "Копирует дамп текущего состояния в буфер обмена — для багрепортов.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = {
                val dump = buildString {
                    appendLine("Flashlight2Ultimate debug dump")
                    appendLine("tier=${state.tier} premium=${state.premium}")
                    appendLine("onRemaining=${state.onRemaining} offRemaining=${state.offRemaining}")
                    appendLine("story=${state.storyProgress}")
                    appendLine("depLoggedIn=${state.dep.loggedIn}")
                    appendLine("devMode=${state.devModeUnlocked}")
                }
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("debug", dump))
                copied = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (copied) "Скопировано ✓" else "Скопировать дамп") }
    }
}

@Composable
private fun DevCheatsCard(state: GameState) {
    val scope = rememberCoroutineScope()

    SectionTitle("Читы")
    InfoCard {
        Button(onClick = { state.devMaxTier() }, modifier = Modifier.fillMaxWidth()) {
            Text("God Mode: максимальный тариф")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.cancelPremium() }, modifier = Modifier.fillMaxWidth()) {
            Text("Сбросить тариф на Free")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.devResetQuotaWindow() }, modifier = Modifier.fillMaxWidth()) {
            Text("Обнулить 10-часовое окно")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.devResetStory() }, modifier = Modifier.fillMaxWidth()) {
            Text("Сбросить сюжетку")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.devResetLock() }, modifier = Modifier.fillMaxWidth()) {
            Text("Снять пароль/биометрию")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { scope.launch { state.dep.logout() } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Выйти из DEP ID") }
    }
}

@Composable
private fun DevSunCard() {
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SectionTitle("Пересобрать Солнце")
    InfoCard {
        if (running) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else if (done) {
            Text(
                "Готово. Ничего не изменилось.",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                done = false
                running = true
                progress = 0f
                scope.launch {
                    while (progress < 1f) {
                        delay(40)
                        progress = (progress + 0.02f).coerceAtMost(1f)
                    }
                    running = false
                    done = true
                }
            },
            enabled = !running,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (running) "Пересборка…" else "Начать пересборку") }
    }
}

private val DEV_FACTS = listOf(
    "Раньше «Фонарик 1» умел светить, но не умел выключаться вообще.",
    "У «Еблан Софт» нет отдела тестирования — есть только вы.",
    "Слово «Ultimate» в названии добавили, потому что версия 3 не поместилась в бюджет.",
    "Секретная кнопка «Светить сильнее» физически ничего не меняет, но психологически — очень даже.",
    "DepCoins не конвертируются в рубли. И в доллары. Вообще никак.",
    "Казино в фонарике встроено потому что «а почему бы и нет».",
    "Этот дев-режим тоже когда-нибудь монетизируют.",
)

@Composable
private fun DevFactsCard() {
    var index by remember { mutableStateOf(0) }

    SectionTitle("Случайный факт")
    InfoCard {
        Text(
            DEV_FACTS[index],
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = { index = (index + 1) % DEV_FACTS.size },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Следующий факт") }
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

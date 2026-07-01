# Фонарик 2 Ultimate 🔦

**by Еблан Софт**

Революционный фонарик для Android, в котором **включать свет — бесплатно, а вот
выключать — уже нет.** Полноценная сатира на современные мобильные приложения,
собранная как настоящее нативное Android-приложение (Kotlin + Jetpack Compose).

## ✨ Возможности

- **🔦 Фонарик** — вспышка камеры (`CameraManager.setTorchMode`) или подсветка
  экраном, если вспышки нет.
- **🚫 5 бесплатных выключений в день** — включай сколько хочешь, но выключить
  свет бесплатно можно лишь 5 раз в сутки. Лимит сбрасывается каждый день.
- **📺 Реклама** — закончился лимит? Посмотри «рекламу» (30… ладно, 5 секунд
  капсул для стирки) и получи +1 выключение.
- **👑 Premium-подписка** — 999 ₽/мес: безлимитные выключения, все цвета,
  без рекламы, полная сюжетка. (Оплата ненастоящая — это демо.)
- **🎨 Цвета** — палитра свечения; часть цветов под замком Premium.
- **📖 Сюжетка** — 6 глав про мир Вечной Тьмы, где выключить свет — привилегия
  избранных. Финал, разумеется, только для подписчиков.
- **🛡️ Anti-tamper гейт** — прежде чем пустить к фонарику, приложение проверяет:
  - **Key Attestation** — заблокирован ли бутлоадер / verified boot и есть ли
    аппаратный ключ в TEE/StrongBox (реальный AndroidKeyStore attestation).
  - **Root Detection** — su-бинарники, Magisk, root-менеджеры, `test-keys`.
  - **Play Integrity** — эвристика подлинности устройства (эмулятор, Play Store).

  Пока **не все галочки зелёные — в фонарик не пускает.** «Вдруг взломаешь».

## 🏗️ Сборка

Требуется Android SDK и JDK 17.

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release-unsigned.apk
```

APK **универсальный** — все ABI (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`)
в одном файле, без сплитов по density/ABI. Ставится на любой Android 7.0+ (API 24).

## 🤖 CI / GitHub Actions

Воркфлоу [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml)
на каждый push собирает **универсальный APK** и выкладывает его в артефакты:

- `flashlight2-ultimate-debug` — подписанный debug-ключом, ставится сразу.
- `flashlight2-ultimate-release-unsigned` — release без подписи.

Скачать: вкладка **Actions** → нужный запуск → раздел **Artifacts**.

> Debug-APK устанавливается напрямую. Release-APK не подписан — подпишите своим
> ключом (`apksigner`) перед установкой/публикацией.

## 📁 Структура

```
app/src/main/java/com/eblansoft/flashlight2/
├── MainActivity.kt        # точка входа
├── GameState.kt           # вся логика: лимиты, реклама, premium, цвета
├── FlashController.kt      # управление вспышкой камеры
├── Prefs.kt               # сохранение состояния (SharedPreferences)
├── security/
│   └── SecurityChecker.kt # attestation + root + Play Integrity гейт
├── story/Story.kt         # тексты сюжетки
└── ui/                    # экраны на Jetpack Compose
    ├── App.kt             # навигация + гейт безопасности
    ├── SecurityGateScreen.kt
    ├── FlashlightScreen.kt
    ├── PaywallScreen.kt   # Premium
    ├── StoryScreen.kt     # Сюжетка
    ├── Overlays.kt        # реклама + диалог лимита
    └── Theme.kt
```

## ⚠️ Дисклеймер

Это шуточное приложение. Никаких реальных платежей, подписок и рекламных сетей
внутри нет — весь «монетизационный ад» смоделирован локально ради сатиры.

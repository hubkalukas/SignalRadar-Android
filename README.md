# SignalRadar (Android)

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Release](https://img.shields.io/github/v/release/hubkalukas/SignalRadar-Android?label=Release)

Android aplikace pro vyhledávání okolních **Bluetooth (BLE)** a **Wi-Fi** zařízení, sledování síly signálu v dBm a orientační odhad vzdálenosti v metrech.

> **English summary:** Android (Kotlin + Jetpack Compose) Bluetooth Low Energy & Wi-Fi (2.4 / 5 / 6 GHz) signal scanner featuring a live radar view, 1D Kalman-filtered RSSI distance estimation, vendor lookup (BLE Company ID / MAC OUI), and proximity vibration feedback.

---

## ⬇️ Stažení aplikace

- **[Stáhnout SignalRadar-v1.0.apk](https://github.com/hubkalukas/SignalRadar-Android/releases/latest/download/SignalRadar-v1.0.apk)** (přímé stažení nejnovější verze)
- Všechny verze najdeš v sekci [Releases](https://github.com/hubkalukas/SignalRadar-Android/releases).

Vyžaduje **Android 8.0 (API 26)** nebo novější. Pro reálné skenování je potřeba povolit přístup k okolním zařízením a poloze.

---

## Co aplikace umí

- **Skenování BLE & Wi-Fi v reálném čase:** Bluetooth Low Energy běží v režimu `SCAN_MODE_LOW_LATENCY` bez zpoždění reportů, Wi-Fi skenuje pásma **2.4, 5 i 6 GHz**.
- **Rozpoznání výrobce:** U známých zařízení automaticky doplňuje výrobce podle BLE Company ID nebo OUI prefixu MAC adresy.
- **Tři režimy zobrazení:** Přehled (radar a seznam pod sebou), samostatný radar a samostatný seznam s vyhledáváním (název, výrobce, MAC) i filtrováním podle typu signálu.
- **Detail a odhad vzdálenosti:** Měřák signálu, živý graf průběhu RSSI a přepočet na metry pomocí **1D Kalmanova filtru** a log-distance modelu s volbou prostředí (*venku / interiér / přes zdi*).
- **Haptické hledání:** Při dohledávání zařízení v ruce lze zapnout vibrační odezvu, která zrychluje s rostoucí silou signálu.

---

## Technologie a architektura

- **Kotlin** + **Jetpack Compose** (Material 3, Navigation 3)
- `SignalScannerEngine.kt` — BLE a Wi-Fi skenování
- `SignalMath.kt` (+ unit testy v `SignalMathTest.kt`) — 1D Kalmanův filtr a výpočet vzdálenosti z RSSI
- `RadarCanvas.kt` & `TargetLockSheet.kt` — vykreslení radaru a detail zaměřeného zařízení

## Sestavení ze zdrojového kódu

Vyžaduje **JDK 17** (nebo 21) a **Android SDK (platforma 36.1)**:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Výsledné APK se uloží do `app/build/outputs/apk/debug/app-debug.apk`.
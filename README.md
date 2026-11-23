# SignalRadar

Android aplikace pro vyhledávání okolních Bluetooth (BLE) a Wi-Fi zařízení, sledování síly signálu v dBm a orientační odhad vzdálenosti v metrech.

## Co umí

Aplikace skenuje Bluetooth Low Energy v režimu `SCAN_MODE_LOW_LATENCY` bez zpoždění reportů a zároveň průběžně načítá výsledky Wi-Fi v pásmech 2.4, 5 a 6 GHz. U známých zařízení zkouší doplnit výrobce podle BLE Company ID nebo OUI prefixu MAC adresy.

Hlavní obrazovka jde přepínat mezi přehledem (radar a seznam pod sebou), samotným radarem a samotným seznamem. V seznamu se dá vyhledávat podle názvu, výrobce i MAC adresy nebo filtrovat podle typu signálu.

Po klepnutí na konkrétní zařízení se otevře detail s měřákem signálu, odhadem vzdálenosti v metrech a grafem průběhu RSSI. Výpočet používá 1D Kalmanův filtr a log-distance model, kde se dá přepnout prostředí (venku, interiér, přes zdi). Při hledání zařízení v ruce jde zapnout vibrační odezva, která zrychluje s rostoucí silou signálu.

## Instalace

Hotové APK (`SignalRadar-v1.0.apk`) je v sekci [Releases](../../releases). Funguje od Androidu 8.0 (API 26) výš. Pro reálné skenování je potřeba povolit přístup k okolním zařízením a poloze.

## Build

Vyžaduje JDK 17 nebo 21 a Android SDK (platforma 36.1):

```bash
./gradlew assembleDebug testDebugUnitTest
```

APK se uloží do `app/build/outputs/apk/debug/app-debug.apk`.

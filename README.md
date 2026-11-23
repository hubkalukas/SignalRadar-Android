# SignalRadar (Android)

Elegantní tmavá Android aplikace pro **nepřetržité vyhledávání Bluetooth (BLE) i Wi-Fi signálů**, zobrazení jejich síly v reálném čase (`dBm` / `%`) a **odhad vzdálenosti v metrech**.

## Hlavní funkce
- **Duální skenování BLE + Wi-Fi**:
  - Bluetooth Low Energy v nejrychlejším režimu `SCAN_MODE_LOW_LATENCY` (`setReportDelay(0)`, `MATCH_MODE_AGGRESSIVE`) s okamžitým zpracováním každého příchozího paketu.
  - Wi-Fi skenování napříč pásmy `2.4 GHz`, `5 GHz` a `6 GHz` včetně zabezpečení a rozpoznání výrobce (OUI / BLE Company ID).
- **Interaktivní Radar + Minimalistický Dashboard**:
  - Přepínání pohledů **Radar + Panel**, **Radar** a **Dashboard**.
  - Plynule rotující radarový paprsek, přepínatelný dosah (`5 m / 15 m / 35 m`) a zaměření libovolného zařízení klepnutím.
  - Ruční vyhledávání podle názvu, výrobce nebo MAC adresy, rychlé filtry (*Vše / Bluetooth / Wi-Fi / Jen s názvem*) a živé sparkline grafy.
- **Režim zaměření v ruce (Lock-On)**:
  - Analogovo-digitální obloukový měřák s odhadem vzdálenosti v metrech (`1D Kalmanův filtr` + Log-Distance Path Loss Model).
  - Živý adaptivní oscilogram průběhu RSSI (`MIN / MAX dBm`, `pkt/s`, počet paketů).
  - Volitelná **Geigerova haptika** (vibrace zrychlující s přiblížením k zařízení).
  - Kalibrace prostředí (**Volno**, **Interiér**, **Zdi**) a vestavěný **Demo režim**.

## Instalace do telefonu
Stáhni si připravené APK ze sekce **[Releases](../../releases)** (`SignalRadar-v1.0.apk`) a nainstaluj jej na Android 8.0+ (API 26+).

## Sestavení ze zdrojového kódu
Vyžaduje **JDK 17 nebo JDK 21** a Android SDK (platforma Android 16 / API 36.1):

```bash
./gradlew assembleDebug testDebugUnitTest
```

Výsledné APK se vytvoří v `app/build/outputs/apk/debug/app-debug.apk`.

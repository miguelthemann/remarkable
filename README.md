# Remarkable

A Material Design 3 desk clock for Android. Full-screen time (analog and digital), live weather, and Spotify now-playing — built to sit on a counter, nightstand, or old tablet you refuse to throw away.

Copyright (c) 2026 Miguel Guerra · [MIT license](LICENSE) · third-party notices in [NOTICE](NOTICE)

You can build Remarkable on **Debian**, **Ubuntu**, **Fedora**, and **Arch Linux**.

## What you get

- Full-screen desk clock (12/24 h, seconds, keep screen on, night dim)
- Weather from [Open-Meteo](https://open-meteo.com) (CC BY 4.0), via approximate GPS or a city you type in Settings
- Spotify playback controls through **Spotify App Remote** (Spotify app must be installed and signed in)
- Themes (including Monet / Material You), Peaks-style weather sky, movable modules, burn-in protection, optional home-launcher / screen-saver / overlay modes

## App requirements

| Field | Value |
| --- | --- |
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` / `compileSdk` / `maxSdk` | 36 (Android 16) |
| ABI | **arm64-v8a only** |
| Densities | single vector asset set — no `mdpi`/`hdpi` folders or DPI splits |
| Package | `com.miguelthemann.remarkable` |
| Languages | English (`en`, default) and European Portuguese (`pt-PT`) |

## License (MIT)

Everything **in this repository** is MIT — see [LICENSE](LICENSE). You may use, modify, and redistribute it if you keep the copyright notice and license text.

That does **not** include:

- **Spotify Auth** (Maven Central) and **App Remote** (AAR downloaded from GitHub at build time; not on Maven)
- Open-Meteo weather data (CC BY 4.0; attribution appears in the UI)
- AndroidX, Kotlin, OkHttp, and Gson (Apache 2.0), fetched by Gradle

No Spotify SDK or third-party binaries are vendored in the tree. Original source files carry `SPDX-License-Identifier: MIT`.

## System dependencies

You need JDK **17**, plus `git`, `wget`, `unzip`, and (optionally) `adb` udev rules to install on a phone.

**Debian / Ubuntu**

```bash
sudo apt update
sudo apt install --no-install-recommends openjdk-17-jdk git wget unzip android-sdk-platform-tools-common
```

**Fedora**

Fedora’s own OpenJDK 17 package (`java-17-openjdk-devel`) is gone on recent releases, and the system JDK is too new for this project’s Gradle / AGP combo. Install Eclipse Temurin 17 from the repo Fedora ships for that:

```bash
sudo dnf install git wget unzip android-tools
sudo dnf install adoptium-temurin-java-repository
sudo fedora-third-party enable
sudo dnf install temurin-17-jdk
```

If `dnf` still cannot see `temurin-17-jdk`, enable third-party repositories (Software → Repositories, or `fedora-third-party enable`) and try again.

**Arch Linux**

```bash
sudo pacman -S --needed jdk17-openjdk git wget unzip android-tools
```

Confirm the JDK:

```bash
java -version
```

Point `JAVA_HOME` at JDK 17 if Gradle cannot find it (adjust the path for your machine):

```bash
# Debian / Ubuntu
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Fedora (Temurin 17 — check with: ls /usr/lib/jvm)
export JAVA_HOME=/usr/lib/jvm/java-17-temurin-jdk
# some installs use: /usr/lib/jvm/temurin-17-jdk

# Arch Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

## Android SDK

Distro `android-sdk` packages are often behind API 36. Prefer Google’s [command-line tools](https://developer.android.com/studio#command-line-tools-only):

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
```

Accept licenses and install platform 36:

```bash
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

In the repo root, create `local.properties` (gitignored). You can copy `local.properties.example`:

```properties
sdk.dir=/home/<you>/Android/Sdk
```

That path must match your `ANDROID_HOME`.

## Gradle wrapper

`gradle-wrapper.jar` (Apache 2.0) is **not** committed, so the tree stays free of binaries. Generate it once with your distro Gradle **or** any Gradle 8.11 install:

**Debian / Ubuntu**

```bash
sudo apt install --no-install-recommends gradle
gradle wrapper --gradle-version 8.11.1
```

APT’s `gradle` package may be older than 8.11; the wrapper it writes still downloads Gradle **8.11.1** on the first build.

**Fedora**

```bash
sudo dnf install gradle
gradle wrapper --gradle-version 8.11.1
```

**Arch Linux**

```bash
sudo pacman -S --needed gradle
gradle wrapper --gradle-version 8.11.1
```

Or drop the official JAR at `gradle/wrapper/gradle-wrapper.jar`:

```text
https://github.com/gradle/gradle/raw/v8.11.1/gradle/wrapper/gradle-wrapper.jar
```

Then:

```bash
chmod +x gradlew
```

Pinned toolchain: **Gradle 8.11.1**, **Android Gradle Plugin 8.9.1**, **Kotlin 2.0.21**.

The first build needs network: Gradle downloads `spotify-app-remote-release-0.8.0.aar` from [github.com/spotify/android-sdk](https://github.com/spotify/android-sdk/releases) into `app/libs/` (gitignored).

## Build

From the repo root:

```bash
export JAVA_HOME  # if it is not already set in this shell
export ANDROID_HOME="$HOME/Android/Sdk"

./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

APKs land here:

- Debug: `app/build/outputs/apk/debug/`
- Release: `app/build/outputs/apk/release/` (`arm64-v8a` only)

Install over USB (`adb`, your user in `plugdev` or equivalent udev rules):

```bash
./gradlew :app:installDebug
```

Release builds need a keystore. **Do not commit** the keystore or passwords.

### GitHub Actions (required for updateable Releases)

Create a keystore once, then add these **repository secrets**:

| Secret | Value |
|--------|--------|
| `REMARKABLE_KEYSTORE_BASE64` | `base64 -w0 your.jks` (or `.p12`) |
| `REMARKABLE_STORE_PASSWORD` | store password |
| `REMARKABLE_KEY_ALIAS` | key alias |
| `REMARKABLE_KEY_PASSWORD` | key password |
| `REMARKABLE_STORE_TYPE` | optional (`PKCS12` if using `.p12`) |

Example:

```bash
keytool -genkeypair -v -keystore remarkable-upload.jks -alias remarkable \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 remarkable-upload.jks   # paste into REMARKABLE_KEYSTORE_BASE64
keytool -list -v -keystore remarkable-upload.jks   # SHA-1 for Spotify Dashboard
```

Use the **same** keystore for every CI release so sideload updates keep working.

### Local release

```bash
./gradlew :app:assembleRelease \
  -Pandroid.injected.signing.store.file="$HOME/keys/remarkable-release.jks" \
  -Pandroid.injected.signing.store.password=... \
  -Pandroid.injected.signing.key.alias=... \
  -Pandroid.injected.signing.key.password=...
```

Or create a gitignored `keystore.properties` at the repo root:

```properties
storeFile=/home/you/keys/remarkable-upload.jks
storePassword=...
keyAlias=remarkable
keyPassword=...
```

## Music / now playing

By default Remarkable shows **whatever Android is playing** (Spotify, YouTube Music, podcasts, …) via the system media session. You need to grant **notification access** once (Settings → Music → Open notification access).

Optional sources in Settings:

- **Spotify** — App Remote controls (needs Client ID + Spotify installed)
- **Last.fm** — shows your recent / now-playing from last.fm (API key + username). Optional scrobbling from device playback needs shared secret + a one-time password sign-in.

## Spotify setup (optional)

1. Install Spotify and sign in on the device
2. Create an app at [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard)
3. Redirect URI: `remarkable://callback`
4. Package name: `com.miguelthemann.remarkable`
5. Add the SHA-1 fingerprint of your debug or release keystore (`keytool -list -v -keystore ...`)
6. Paste the **Client ID** into Remarkable → Settings and set music source to Spotify

Without Spotify, the clock, weather, and system now-playing still work.

## Last.fm setup (optional)

1. Create an API account at [last.fm/api](https://www.last.fm/api)
2. Enter API key, shared secret, and username in Settings
3. For scrobbling: enter password once → Sign in → enable “Scrobble from device playback”
4. Set music source to Last.fm to show profile now-playing, or keep Device and scrobble in the background

## Weather

- Grant `ACCESS_COARSE_LOCATION`, or
- Enter a city in Settings (Open-Meteo geocoding)

No API key is required.

## How to use the clock

- **Long-press empty space** → Settings
- **Long-press a module** (time, date, weather, Spotify) → rearrange mode, then drag; tap **Done** when finished
- Toggle modules, themes, Peaks weather effects, and burn-in options in Settings
- **Reset all settings** (bottom of Settings) asks twice before wiping preferences and returning you to onboarding

## Project layout

```text
app/src/main/java/com/miguelthemann/remarkable/
  MainActivity.kt          Compose entry, keep-screen-on
  ui/clock/                Clock UI + ViewModel
  ui/settings/             Settings
  ui/onboarding/           First-run flow
  ui/ambient/              Backgrounds + Peaks weather scene
  weather/                 Open-Meteo client
  spotify/                 App Remote
  prefs/                   DataStore
  location/                LocationManager + Geocoder
```

## Languages

- English: `app/src/main/res/values/` (default)
- European Portuguese: `app/src/main/res/values-pt-rPT/`

Android picks `pt-PT` when the system (or per-app) language is European Portuguese. Everything else falls back to English.

## Themes, background, burn-in

In Settings you can choose:

- Theme: system, light, dark, or **Monet** (Material You, API 31+)
- Accent color (when you are not on pure Monet)
- Background: solid, Monet, weather-reactive, time-of-day, accent, **custom color** (presets + HSV + HEX), or **image**
- Clock style: analog, digital, or both
- Module positions (time, date, weather, Spotify)
- Burn-in protection (pixel shift) and **smart pixels**
- Launcher (HOME), screen saver (Daydream), and overlay over other apps

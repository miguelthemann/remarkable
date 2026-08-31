# Remarkable

Relógio de secretária para Android, em [Material Design 3](https://m3.material.io/). Mostra a hora (analógico e digital), meteorologia e o que está a tocar no Spotify.

Copyright (c) 2026 Miguel Guerra. Publicado sob a [licença MIT](LICENSE). Avisos de terceiros: [NOTICE](NOTICE).

A compilação é suportada em **Debian**, **Ubuntu**, **Fedora** e **Arch Linux**.

## Requisitos da app

| Campo | Valor |
| --- | --- |
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` / `compileSdk` / `maxSdk` | 36 (Android 16) |
| ABI | **apenas** `arm64-v8a` |
| Densidades | um único conjunto de recursos vectoriais — sem pastas `mdpi`/`hdpi`/… e sem splits de DPI |
| Pacote | `com.miguelthemann.remarkable` |
| Idiomas | inglês (`en`, predefinição) e português de Portugal (`pt-PT`) |

## Funcionalidades

- Relógio de secretária em ecrã completo (formato 12/24 h, segundos, ecrã sempre ligado, modo noturno ténue)
- Meteorologia via [Open-Meteo](https://open-meteo.com) (CC BY 4.0), por GPS aproximado ou cidade nas definições
- Controlo Spotify através da **Spotify App Remote** (a app Spotify tem de estar instalada no dispositivo)

## Licença (MIT)

O código, recursos e documentação **deste repositório** estão sob MIT — ver [LICENSE](LICENSE). Podes usar, modificar e redistribuir desde que conserves o aviso de copyright e o texto da licença.

Isto **não** cobre:

- o **Spotify Auth** (Maven Central) e o **App Remote** (AAR do GitHub no *build*; não está no Maven)
- dados de meteorologia da Open-Meteo (CC BY 4.0; a atribuição aparece na UI)
- bibliotecas AndroidX, Kotlin, OkHttp e Gson (Apache 2.0), descarregadas pelo Gradle

Não há SDK da Spotify nem binários de terceiros *vendored* neste repositório.

Cada ficheiro de código-fonte original inclui `SPDX-License-Identifier: MIT`.

## Dependências do sistema

JDK **17**, `git`, `wget`, `unzip` e regras udev para `adb` (opcional, só para instalar no telefone).

**Debian / Ubuntu**

```bash
sudo apt update
sudo apt install --no-install-recommends openjdk-17-jdk git wget unzip android-sdk-platform-tools-common
```

**Fedora**

O OpenJDK 17 da distro (`java-17-openjdk-devel`) foi retirado a partir do Fedora 42; no Fedora 44 também já não há `java-21-openjdk`. O JDK do sistema é o 25, que não serve para este projeto (Gradle 8.11 / AGP 8.9).

Instala o JDK 17 do Eclipse Temurin, pelo repositório que o Fedora fornece:

```bash
sudo dnf install git wget unzip android-tools
sudo dnf install adoptium-temurin-java-repository
sudo fedora-third-party enable
sudo dnf install temurin-17-jdk
```

Se `dnf` ainda não vir o `temurin-17-jdk`, ativa repositórios de terceiros (`fedora-third-party enable` / Software → Repositórios) e volta a tentar.

**Arch Linux**

```bash
sudo pacman -S --needed jdk17-openjdk git wget unzip android-tools
```

Confirma o JDK:

```bash
java -version
```

Define `JAVA_HOME` se o Gradle não encontrar o 17 (ajusta o caminho se o teu sistema usar outro):

```bash
# Debian / Ubuntu
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Fedora (Temurin 17; confirma com: ls /usr/lib/jvm)
export JAVA_HOME=/usr/lib/jvm/java-17-temurin-jdk
# algumas instalações usam: /usr/lib/jvm/temurin-17-jdk

# Arch Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

## Android SDK

Os pacotes `android-sdk` das distros costumam estar atrasados em relação à API 36. Usa as [command-line tools](https://developer.android.com/studio#command-line-tools-only) oficiais:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
```

Aceita licenças e instala a plataforma 36:

```bash
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

Na raiz do repositório, cria `local.properties` (não vai para o Git). Podes partir de `local.properties.example`:

```properties
sdk.dir=/home/<user>/Android/Sdk
```

O caminho tem de coincidir com `ANDROID_HOME`.

## Wrapper Gradle

O `gradle-wrapper.jar` (Apache 2.0) **não está** no repositório, para não incluir binários. Gera-o uma vez, com o Gradle da distro **ou** um Gradle 8.11 já instalado:

**Debian / Ubuntu**

```bash
sudo apt install --no-install-recommends gradle
gradle wrapper --gradle-version 8.11.1
```

O `gradle` do APT pode ser mais antigo que 8.11; o *wrapper* que ele gera ainda descarrega o Gradle **8.11.1** na primeira compilação.

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

Em alternativa, descarrega o JAR oficial e coloca-o em `gradle/wrapper/gradle-wrapper.jar`:

```text
https://github.com/gradle/gradle/raw/v8.11.1/gradle/wrapper/gradle-wrapper.jar
```

Torna o script executável:

```bash
chmod +x gradlew
```

Versões alinhadas com o projeto: **Gradle 8.11.1**, **Android Gradle Plugin 8.9.1**, **Kotlin 2.0.21**.

O *build* precisa de rede na primeira vez: o Gradle descarrega `spotify-app-remote-release-0.8.0.aar` de [github.com/spotify/android-sdk](https://github.com/spotify/android-sdk/releases) para `app/libs/` (ficheiro ignorado pelo Git).

## Compilar

Na raiz do repositório:

```bash
export JAVA_HOME  # se ainda não estiver na sessão
export ANDROID_HOME="$HOME/Android/Sdk"

./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

APKs:

- Debug: `app/build/outputs/apk/debug/`
- Release: `app/build/outputs/apk/release/` (só `arm64-v8a`)

Instalar num dispositivo por USB (`adb`, utilizador no grupo `plugdev` ou regras udev já instaladas):

```bash
./gradlew :app:installDebug
```

Release precisa de um *keystore*. Exemplo (não commits o keystore nem as passwords):

```bash
./gradlew :app:assembleRelease \
  -Pandroid.injected.signing.store.file="$HOME/keys/remarkable-release.jks" \
  -Pandroid.injected.signing.store.password=... \
  -Pandroid.injected.signing.key.alias=... \
  -Pandroid.injected.signing.key.password=...
```

Ou define `signingConfigs` só na tua máquina.

## Spotify

1. App Spotify instalada e sessão iniciada no telemóvel/tablet
2. Cria uma aplicação em [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard)
3. Redirect URI: `remarkable://callback`
4. Package name: `com.miguelthemann.remarkable`
5. Fingerprint SHA-1 do *keystore* de debug ou release (`keytool -list -v -keystore ...`)
6. Cola o **Client ID** em Definições dentro da Remarkable

Sem Client ID, o relógio e a meteorologia funcionam na mesma.

## Meteorologia

- Permissão `ACCESS_COARSE_LOCATION`, ou
- Cidade preenchida nas definições (geocoding Open-Meteo)

Não é necessária chave de API.

## Estrutura

```text
app/src/main/java/com/miguelthemann/remarkable/
  MainActivity.kt          UI Compose, ecrã sempre ligado
  ui/clock/                Relógio, ViewModel
  ui/settings/             Definições
  weather/                 Cliente Open-Meteo
  spotify/                 App Remote
  prefs/                   DataStore
  location/                LocationManager + Geocoder
```

## Idiomas

- Inglês: `app/src/main/res/values/` (predefinição)
- Português de Portugal: `app/src/main/res/values-pt-rPT/`

O Android escolhe `pt-PT` quando o idioma do sistema (ou o idioma por aplicação, Android 13+) é português de Portugal. Os restantes locales usam inglês.

## Temas, fundo e burn-in

Nas Definições podes escolher:

- Tema: sistema, claro, escuro ou **Monet** (Material You, API 31+)
- Cor de destaque (quando não usas Monet puro)
- Fundo: sólido, Monet, reativo à meteorologia, reativo à hora do dia, destaque, **cor sólida** (presets + HSV + HEX) ou **imagem**
- Estilo do relógio: analógico, digital ou ambos
- Posição dos módulos (hora, data, meteorologia, Spotify)
- Proteção contra burn-in (deslocamento) e **smart pixels**
- Launcher (HOME), protetor de ecrã (Daydream) e sobreposição sobre outras apps

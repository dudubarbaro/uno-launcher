# Uno Launcher — plano de construção

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Launcher Android próprio para o Uno do dono. Ele mostra a foto do carro, música, clima e dados do OBD2, lê e apaga códigos de falha, calcula o gasto da viagem e pode ser testado num tablet com modo demonstração antes de ir para a central.

**Architecture:** Um único módulo `:app` em Kotlin + Jetpack Compose. A lógica pura fica em arquivos sem dependência de Android, testados na JVM: parser do OBD2, consumo, viagem, sol, alertas e clima. Um serviço em primeiro plano roda o motor do OBD2 (real ou demo) e publica o estado num `CarRepo` singleton. As telas são composables sem estado, ligadas na `MainActivity`, e cada uma é fotografada pelo Roborazzi em três tamanhos de tela.

**Tech Stack:** AGP 8.7.3, Gradle 8.9 (em `~/tools`, chamado pelo script `./g`), JDK 17, Kotlin 2.0.21, Compose BOM 2024.12.01, activity-compose 1.9.3, lifecycle-runtime-compose 2.8.7, core-ktx 1.15.0. Testes com JUnit 4.13.2, kotlinx-coroutines-test 1.9.0, org.json 20240303 (só nos testes), Robolectric 4.14.1 e Roborazzi 1.36.0.

**Spec:** `docs/superpowers/specs/2026-11-14-uno-launcher-design.md`

**Caminhos:** `main/` = `app/src/main/java/io/github/dudubarbaro/unolauncher/` e `test/` = `app/src/test/java/io/github/dudubarbaro/unolauncher/`. Os comandos rodam na raiz `~/uno-launcher`.

## Global Constraints

- `applicationId`/`namespace` `io.github.dudubarbaro.unolauncher`, rótulo "Uno Launcher", `versionCode 1`, `versionName "0.1.0"`.
- `minSdk 26`, `targetSdk 35`, `compileSdk 35`, `buildToolsVersion "35.0.0"`.
- Robolectric roda com `sdk=34`, porque SDK 35 exige JDK 21 e aqui só tem o 17.
- Todo texto de tela em pt-BR, com vírgula decimal: "14,1 V", "R$ 12,40", "7,8 km/l".
- O fundo é sempre preto (black piano). O destaque padrão é `#E10600`. Temas: vermelho `#E10600`, branco `#F2F2F2`, laranja `#FF6A00` e azul `#1E88FF`.
- Fonte Chakra Petch (OFL), regular e bold. Nunca Orbitron nem Rajdhani. Nenhum código, imagem, nome ou texto do Rota Launcher.
- Todas as medidas saem de `LocalScale`: `scale = min(larguraDp/1024, alturaDp/600)`, limitado entre 0,7 e 1,6. Nenhum tamanho é fixo em pixels.
- Alvo de toque com no mínimo `56.dp * scale`. A tela fica travada deitada (`sensorLandscape`).
- OBD2 só por Bluetooth clássico RFCOMM, UUID `00001101-0000-1000-8000-00805F9B34FB`.
- A única rede usada é o met.no (`https://api.met.no/weatherapi/locationforecast/2.0/compact`), com User-Agent `UnoLauncher/<versionName> <unoContact>`.
- O e-mail do dono **nunca** entra no repositório. Ele fica na propriedade `unoContact` de `~/.gradle/gradle.properties`, junto com a assinatura.
- Nenhuma dependência além das listadas no Tech Stack.

## Review Focus

1. **Ignição desligada.** O adaptador responde `UNABLE TO CONNECT` ou `NO DATA`. O painel mostra "Ligue a ignição", os números mostram "—", o app tenta de novo e não trava. Teste: `ObdEngineTest.ignitionOffReportsIgnitionOff` (Task 6).
2. **Bluetooth cai no meio da viagem.** O app reconecta com espera crescente. O tempo sem dados não vira km nem litros. Testes: `TripMeterTest.gapIsIgnored` (Task 4) e `ObdEngineTest.ioErrorReconnects` (Task 6).
3. **A central perde energia enquanto salva a viagem.** A viagem atual não se corrompe e uma linha quebrada no histórico é ignorada. Testes: `TripStoreTest.corruptLineSkipped` e `TripStoreTest.currentSurvivesPartialTmp` (Task 5).
4. **O dono digita "5,89" ou lixo num campo numérico.** Vírgula é aceita. Lixo é recusado e o valor antigo fica. Teste: `ParseDecimalTest` (Task 5).
5. **Adaptador "OBDII" não pareado, Bluetooth desligado ou permissão negada (o caso do tablet).** O painel explica o que falta e o modo demo continua funcionando. Teste: `BtConnectorTest.pickAdapter` (Task 8).

---

### Task 1: Esqueleto do projeto, tema e git

**Files:**
- Create: `.gitignore`, `g`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`, `main/UnoApp.kt`, `main/MainActivity.kt`, `main/ui/Theme.kt`
- Create: `app/src/main/res/values/themes.xml`, `res/values/strings.xml`, `res/font/chakra_petch_regular.ttf`, `res/font/chakra_petch_bold.ttf`, `res/mipmap-anydpi-v26/ic_launcher.xml`, `res/drawable/ic_launcher_fg.xml`, `res/values/colors.xml`
- Create: `app/src/main/assets/licenses/ChakraPetch-OFL.txt`, `app/src/test/resources/robolectric.properties`
- Test: `test/ui/AccentTest.kt`

**Interfaces:**
- Produces: `enum class Accent(val key: String, val label: String, val color: Color)` com `VERMELHO("vermelho","Vermelho",0xFFE10600)`, `BRANCO("branco","Branco",0xFFF2F2F2)`, `LARANJA("laranja","Laranja",0xFFFF6A00)`, `AZUL("azul","Azul",0xFF1E88FF)` e `companion fun fromKey(k: String?): Accent` (desconhecido vira `VERMELHO`).
- Produces: `@Composable fun UnoTheme(accent: Accent, content: @Composable () -> Unit)`, `val LocalAccent: ProvidableCompositionLocal<Color>`, `val LocalScale: ProvidableCompositionLocal<Float>`, `val ChakraPetch: FontFamily`, `fun Modifier.pianoBackground(): Modifier`.

- [ ] **Step 1: Inicializar o git e os arquivos de build**

`git init -b main` em `~/uno-launcher`. O `.gitignore` tem `.superpowers/`, `.gradle/`, `build/`, `local.properties`, `*.jks`, `*.keystore`, `.idea/` e `*.iml`.

O script `g` fica executável (`chmod +x g`):
```sh
#!/bin/sh
export JAVA_HOME="$HOME/tools/jdk-17" ANDROID_HOME="$HOME/tools/android-sdk"
exec "$HOME/tools/gradle-8.9/bin/gradle" -p "$(dirname "$0")" "$@"
```

- `settings.gradle.kts`: repositórios `google()`, `mavenCentral()` e `gradlePluginPortal()`; `rootProject.name = "uno-launcher"`; `include(":app")`.
- `build.gradle.kts` da raiz: os plugins `com.android.application` 8.7.3, `org.jetbrains.kotlin.android` 2.0.21 e `org.jetbrains.kotlin.plugin.compose` 2.0.21, todos com `apply false`.
- `gradle.properties`: `android.useAndroidX=true`, `android.nonTransitiveRClass=true` e `org.gradle.jvmargs=-Xmx3g`.
- `app/build.gradle.kts`:
  - Valores das Global Constraints, `buildFeatures { compose = true; buildConfig = true }` e `jvmToolchain(17)`.
  - `testOptions.unitTests.isIncludeAndroidResources = true` e `testOptions.unitTests.all { it.systemProperty("roborazzi.test.record", "true") }`.
  - Dependências do Tech Stack: `material3`, `ui`, `ui-tooling-preview` via BOM.
  - Testes: `testImplementation` de junit, coroutines-test, org.json, robolectric, `io.github.takahirom.roborazzi:roborazzi:1.36.0`, `roborazzi-compose:1.36.0`, `androidx.compose.ui:ui-test-junit4` e `androidx.test:core:1.6.1`. Mais `debugImplementation("androidx.compose.ui:ui-test-manifest")`.
- `robolectric.properties`: `sdk=34`.

- [ ] **Step 2: Baixar a fonte**

Baixe `ChakraPetch-Regular.ttf`, `ChakraPetch-Bold.ttf` e `OFL.txt` de `https://github.com/google/fonts/raw/main/ofl/chakrapetch/`. Use primeiro uma pasta vazia nova no scratchpad e depois copie para os nomes em `res/font/` e `assets/licenses/`.

- [ ] **Step 3: Escrever o teste que falha**

```kotlin
class AccentTest {
    @Test fun fromKeyKnown() = assertEquals(Accent.AZUL, Accent.fromKey("azul"))
    @Test fun fromKeyUnknownIsRed() {
        assertEquals(Accent.VERMELHO, Accent.fromKey("roxo"))
        assertEquals(Accent.VERMELHO, Accent.fromKey(null))
    }
}
```

- [ ] **Step 4: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*AccentTest'`
Expected: falha de compilação com `Unresolved reference: Accent`.

- [ ] **Step 5: Implementar o tema, o manifesto e a activity mínima**

- `Theme.kt`: um `darkColorScheme` com `background`/`surface` `#000000`, `primary` igual à cor do destaque e tipografia em `ChakraPetch`. `pianoBackground()` desenha um radial `#1B1B1F` → `#09090B` (55%) → `#000000`, mais um brilho linear branco de 6% para 0% nos 35% de cima.
- Manifesto:
  - `<application android:name=".UnoApp" android:label="@string/app_name" android:icon="@mipmap/ic_launcher" android:theme="@style/Theme.Uno">`.
  - `MainActivity` com `exported=true`, `launchMode="singleTask"`, `screenOrientation="sensorLandscape"`, `configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|density|uiMode"` e `windowSoftInputMode="adjustResize"`.
  - Dois intent-filters: MAIN+LAUNCHER e MAIN+HOME+DEFAULT.
- `themes.xml`: `Theme.Uno` com pai `android:Theme.Material.NoActionBar` e `android:windowBackground` preto.
- Ícone adaptativo: fundo `#000000`. Na frente, um arco de conta-giros de 270° em `#E10600` com um ponteiro branco, desenho original em vetor.
- `MainActivity.setContent { UnoTheme(Accent.VERMELHO) { Box(Modifier.fillMaxSize().pianoBackground()) { Text("Uno Launcher") } } }`.

- [ ] **Step 6: Rodar os testes e o build**

Run: `./g :app:testDebugUnitTest :app:assembleDebug && ~/tools/android-sdk/build-tools/35.0.0/aapt2 dump xmltree --file AndroidManifest.xml app/build/outputs/apk/debug/app-debug.apk | grep -cE 'category.HOME|category.LAUNCHER'`
Expected: `BUILD SUCCESSFUL` e `2`.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "build: project skeleton, theme and docs"
```

---

### Task 2: Parser das respostas do ELM327

**Files:**
- Create: `main/obd/Obd.kt`, `main/obd/Model.kt`
- Test: `test/obd/ObdTest.kt`

**Interfaces:**
- Produces, em `Model.kt`:
  - `data class Sample(val rpm: Int? = null, val speedKmh: Int? = null, val coolantC: Int? = null, val batteryV: Double? = null, val mapKpa: Int? = null, val iatC: Int? = null, val mafGs: Double? = null)`
  - `data class Dtcs(val stored: List<String>, val pending: List<String>, val permanent: List<String>) { val count: Int }`, onde `count` é o total de códigos distintos.
  - `enum class Link { OFF, CONNECTING, IGNITION_OFF, CONNECTED, DEMO }`
- Produces, `object Obd`:
  - `fun pidData(raw: String, pid: Int): IntArray?`
  - `fun rpm(raw: String): Int?`, `speedKmh`, `coolantC`, `mapKpa` e `iatC` (os quatro `(String) -> Int?`)
  - `fun mafGs(raw: String): Double?` e `fun voltage(raw: String): Double?`
  - `fun supportedPids(raw: String): Set<Int>`
  - `fun isCan(dpnRaw: String): Boolean`
  - `fun unableToConnect(raw: String): Boolean`
  - `fun dtcs(raw: String, responseMode: Int, isCan: Boolean): List<String>`
  - `fun decodeDtc(hi: Int, lo: Int): String`

- [ ] **Step 1: Escrever o teste que falha**

```kotlin
class ObdTest {
    @Test fun liveValues() {
        assertEquals(1726, Obd.rpm("41 0C 1A F8\r\r>"))
        assertEquals(1000, Obd.rpm("SEARCHING...\r41 0C 0F A0\r"))
        assertEquals(1726, Obd.rpm("010C\r410C1AF8\r"))          // eco ligado, sem espaços
        assertEquals(62, Obd.speedKmh("41 0D 3E"))
        assertEquals(89, Obd.coolantC("41 05 81"))
        assertEquals(40, Obd.mapKpa("41 0B 28"))
        assertEquals(30, Obd.iatC("41 0F 46"))
        assertEquals(7.5, Obd.mafGs("41 10 02 EE")!!, 1e-9)
        assertEquals(14.1, Obd.voltage("14.1V")!!, 1e-9)
    }
    @Test fun noAnswer() {
        assertNull(Obd.rpm("NO DATA"))
        assertNull(Obd.speedKmh("UNABLE TO CONNECT"))
        assertNull(Obd.voltage("?"))
        assertTrue(Obd.unableToConnect("SEARCHING...\rUNABLE TO CONNECT"))
        assertFalse(Obd.unableToConnect("41 0C 1A F8"))
    }
    @Test fun supported() {
        val s = Obd.supportedPids("41 00 BE 1F A8 13")
        assertTrue(s.containsAll(listOf(0x01, 0x05, 0x0C, 0x0D, 0x0F, 0x10)))
        assertFalse(0x0B in s); assertFalse(0x02 in s)
    }
    @Test fun protocol() {
        assertTrue(Obd.isCan("A6")); assertTrue(Obd.isCan("6"))
        assertFalse(Obd.isCan("3")); assertFalse(Obd.isCan("A5"))
    }
    @Test fun dtcNonCan() {
        assertEquals(listOf("P0133"), Obd.dtcs("43 01 33 00 00 00 00", 0x43, false))
        assertEquals(listOf("P0171", "P0300", "P0133", "P0420"),
            Obd.dtcs("43 01 71 03 00 01 33\r43 04 20 00 00 00 00", 0x43, false))
    }
    @Test fun dtcCan() {
        assertEquals(listOf("P0171", "P0300"), Obd.dtcs("43 02 01 71 03 00", 0x43, true))
        assertEquals(listOf("P0171", "P0300", "P0133", "P0420", "U0123"),
            Obd.dtcs("00C\r0:430501710300\r1:01330420C12300\r\r>", 0x43, true))
        assertEquals(emptyList<String>(), Obd.dtcs("47 00", 0x47, true))
        assertEquals(emptyList<String>(), Obd.dtcs("NO DATA", 0x4A, true))
    }
    @Test fun decode() {
        assertEquals("P0133", Obd.decodeDtc(0x01, 0x33)); assertEquals("C0123", Obd.decodeDtc(0x41, 0x23))
        assertEquals("B0123", Obd.decodeDtc(0x81, 0x23)); assertEquals("U0123", Obd.decodeDtc(0xC1, 0x23))
        assertEquals("P1234", Obd.decodeDtc(0x12, 0x34))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*ObdTest'`
Expected: falha de compilação com `Unresolved reference: Obd`.

- [ ] **Step 3: Implementar `Obd` em `main/obd/Obd.kt`**

- Limpeza: maiúsculas e sem espaços. Quebra em `\r`/`\n` e descarta `SEARCHING...`, `>` e linhas vazias.
- `pidData` procura a primeira linha que começa com `"41%02X".format(pid)` e devolve os bytes seguintes. Isso ignora o eco do comando.
- Fórmulas:
  - RPM = `(A*256+B)/4`
  - velocidade = `A`
  - temperaturas = `A-40`
  - MAP = `A`
  - MAF = `(A*256+B)/100`
- `supportedPids`: o bit 7 do primeiro byte é o PID 0x01.
- `isCan`: tira o `A` inicial e confere se o dígito está entre `6` e `9`.
- `dtcs`:
  - Remove a linha de contagem (3 dígitos hex sozinhos) e os prefixos `N:`.
  - Sem CAN, cada linha começa com o modo e traz 3 pares de bytes.
  - Com CAN, junta tudo, pula o modo e o byte de contagem e lê essa quantidade de pares.
  - Pares `0000` são descartados.
- `decodeDtc`: os 2 bits altos dão a letra (P/C/B/U), os 2 seguintes dão o primeiro dígito e o resto são 3 dígitos hex.

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*ObdTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(obd): parse ELM327 live data and DTC responses"
```

---

### Task 3: Tabela de códigos de falha em português

**Files:**
- Create: `main/obd/DtcNames.kt`
- Test: `test/obd/DtcNamesTest.kt`

**Interfaces:**
- Produces: `object DtcNames { fun describe(code: String): String }`

- [ ] **Step 1: Escrever o teste que falha**

```kotlin
class DtcNamesTest {
    @Test fun known() = assertTrue(DtcNames.describe("P0300").contains("falha de ignição", ignoreCase = true))
    @Test fun manufacturer() {
        assertEquals("Código do fabricante, confirme com um mecânico", DtcNames.describe("P1234"))
        assertEquals("Código do fabricante, confirme com um mecânico", DtcNames.describe("U3000"))
    }
    @Test fun genericUnknown() = assertEquals("Código genérico sem descrição no app", DtcNames.describe("P0999"))
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*DtcNamesTest'`
Expected: falha de compilação com `Unresolved reference: DtcNames`.

- [ ] **Step 3: Implementar `describe` com um `mapOf` em pt-BR**

- Fabricante é o segundo caractere `1`, ou `3` em B/C/U.
- No mínimo estes códigos genéricos:
  - P0100–P0118, P0120–P0123 e P0130–P0141;
  - P0171, P0172, P0174 e P0175;
  - P0300–P0304, P0325, P0335 e P0340;
  - P0351–P0354, P0420, P0440–P0456;
  - P0500, P0505, P0560–P0563, P0600–P0606 e U0100.
- A descrição de P0300 contém "falha de ignição".

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*DtcNamesTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(obd): pt-BR DTC descriptions"
```

---

### Task 4: Consumo e acumulador da viagem

**Files:**
- Create: `main/trip/Fuel.kt`, `main/trip/TripMeter.kt`
- Test: `test/trip/FuelTest.kt`, `test/trip/TripMeterTest.kt`

**Interfaces:**
- Consumes: `Sample` (Task 2).
- Produces:
  - `enum class Fuel(val key: String, val label: String, val afr: Double, val densityGPerL: Double)`: `GASOLINA("gasolina","gasolina",13.2,750.0)` e `ETANOL("etanol","etanol",9.0,809.0)`, com `companion fun fromKey(k: String?): Fuel` (padrão `GASOLINA`).
  - `fun airGramsPerSecond(mapKpa: Double, iatC: Double, rpm: Double, displacementL: Double = 0.999, ve: Double = 0.80): Double`
  - `fun fuelLitersPerHour(s: Sample, fuel: Fuel, multiplier: Double): Double`
  - `data class TripState(val startMs: Long = 0, val km: Double = 0.0, val seconds: Double = 0.0, val liters: Double = 0.0) { fun kmPerL(): Double?; fun cost(pricePerL: Double): Double }`
  - `fun advance(t: TripState, s: Sample, dtSec: Double, nowMs: Long, fuel: Fuel, multiplier: Double): TripState`

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
class FuelTest {
    @Test fun speedDensity() = assertEquals(7.3473, airGramsPerSecond(40.0, 30.0, 2400.0), 1e-3)
    @Test fun litersPerHourMap() {
        val s = Sample(rpm = 2400, mapKpa = 40, iatC = 30)
        assertEquals(2.6718, fuelLitersPerHour(s, Fuel.GASOLINA, 1.0), 1e-3)
        assertEquals(3.6328, fuelLitersPerHour(s, Fuel.ETANOL, 1.0), 1e-3)
        assertEquals(2.6718 * 1.1, fuelLitersPerHour(s, Fuel.GASOLINA, 1.1), 1e-3)
    }
    @Test fun mafWins() = assertEquals(2.7273, fuelLitersPerHour(Sample(rpm = 2400, mapKpa = 40, iatC = 30, mafGs = 7.5), Fuel.GASOLINA, 1.0), 1e-3)
    @Test fun engineOffOrMissing() {
        assertEquals(0.0, fuelLitersPerHour(Sample(rpm = 0, mapKpa = 100, iatC = 30), Fuel.GASOLINA, 1.0), 0.0)
        assertEquals(0.0, fuelLitersPerHour(Sample(rpm = 900), Fuel.GASOLINA, 1.0), 0.0)
    }
}
class TripMeterTest {
    private val s = Sample(rpm = 2400, speedKmh = 60, mapKpa = 40, iatC = 30)
    @Test fun integrates() {
        var t = TripState()
        repeat(60) { t = advance(t, s, 1.0, 1000L * (it + 1), Fuel.GASOLINA, 1.0) }
        assertEquals(1.0, t.km, 1e-9); assertEquals(60.0, t.seconds, 1e-9)
        assertEquals(2.6718 / 60, t.liters, 1e-4); assertEquals(1000L, t.startMs)
        assertEquals(22.457, t.kmPerL()!!, 0.05); assertEquals(t.liters * 5.89, t.cost(5.89), 1e-9)
    }
    @Test fun gapIsIgnored() {
        val t = advance(TripState(km = 1.0), s, 10.0, 0, Fuel.GASOLINA, 1.0)
        assertEquals(1.0, t.km, 0.0); assertEquals(0.0, t.liters, 0.0)
    }
    @Test fun noLitersNoKmPerL() = assertNull(TripState(km = 1.0).kmPerL())
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*FuelTest' --tests '*TripMeterTest'`
Expected: falha de compilação com `Unresolved reference`.

- [ ] **Step 3: Implementar**

- `airGramsPerSecond = mapKpa*1000 * displacementL/1000 * ve * (rpm/120) / (287.05 * (iatC+273.15)) * 1000`.
- `fuelLitersPerHour`:
  - devolve 0 se `rpm` for nulo ou ≤ 0;
  - o ar vem do `mafGs` se existir, senão do MAP+IAT, e se faltar qualquer um deles devolve 0;
  - resultado = `ar/afr*3600/densidade*multiplier`.
- `advance`:
  - se `dtSec > 5.0`, devolve `t` sem mudar (só ajusta `startMs`);
  - `startMs` vira `nowMs` quando estiver 0 na primeira amostra.
  - Ponha o comentário `// ponytail: sem corte de combustível na desaceleração; o multiplicador absorve o erro`.
- `kmPerL` é nulo quando `liters < 0.01`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*FuelTest' --tests '*TripMeterTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(trip): speed-density fuel estimate and trip integration"
```

---

### Task 5: Configurações, histórico e registro de eventos em disco

**Files:**
- Create: `main/settings/Settings.kt`, `main/settings/SettingsRepo.kt`, `main/trip/TripStore.kt`, `main/sys/EventLog.kt`
- Test: `test/settings/ParseDecimalTest.kt`, `test/settings/SettingsRepoTest.kt` (Robolectric), `test/trip/TripStoreTest.kt`, `test/sys/EventLogTest.kt`

**Interfaces:**
- Consumes: `Fuel`, `TripState` (Task 4) e `Accent` (Task 1).
- Produces:
  - `data class Settings`, todos os campos com padrão:
    - `fuel = Fuel.GASOLINA`, `pricePerL = 0.0`, `fuelMultiplier = 1.0`, `odometerKm = 0.0`, `nextServiceKm = 0.0`, `adapterAddress: String? = null`;
    - `startOpenApp: String? = null`, `startResumeMusic = true`, `startObd = true`;
    - `accent = Accent.VERMELHO`, `autoBrightness = false`, `dayBrightness = 255`, `nightBrightness = 60`, `fullscreen = false`;
    - `alertCoolantC = 105`, `alertBatteryLowV = 12.0`, `alertBatteryHighV = 15.0`, `alertServiceBeforeKm = 500.0`;
    - `demo = false`, `lat: Double? = null`, `lon: Double? = null`.
  - `fun parseDecimal(s: String): Double?`
  - `class SettingsRepo(prefs: SharedPreferences) { val flow: StateFlow<Settings>; val value: Settings; fun update(f: (Settings) -> Settings) }`
  - `data class Trip(val startMs: Long, val endMs: Long, val km: Double, val seconds: Double, val liters: Double, val fuel: Fuel, val cost: Double)`
  - `class TripStore(dir: File) { fun loadCurrent(): TripState?; fun saveCurrent(t: TripState); fun finish(t: TripState, endMs: Long, fuel: Fuel, pricePerL: Double); fun trips(): List<Trip> }`
  - `class EventLog(file: File) { fun add(event: String, nowMs: Long); fun recent(): List<String> }`

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
class ParseDecimalTest {
    @Test fun parses() {
        assertEquals(5.89, parseDecimal("5,89")!!, 1e-9); assertEquals(5.89, parseDecimal(" 5.89 ")!!, 1e-9)
        assertEquals(12.0, parseDecimal("12")!!, 1e-9)
        assertNull(parseDecimal("")); assertNull(parseDecimal("abc")); assertNull(parseDecimal("-3")); assertNull(parseDecimal("1,2,3"))
    }
}
@RunWith(RobolectricTestRunner::class)
class SettingsRepoTest {
    @Test fun roundTrip() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("t", 0)
        val changed = Settings(fuel = Fuel.ETANOL, pricePerL = 4.19, adapterAddress = "00:11:22:33:44:55", startOpenApp = "com.waze",
            accent = Accent.AZUL, fullscreen = true, alertCoolantC = 110, demo = true, lat = -22.91, lon = -43.17, odometerKm = 98765.4)
        SettingsRepo(prefs).update { changed }
        assertEquals(changed, SettingsRepo(prefs).value)
    }
}
class TripStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun currentAndFinish() {
        val st = TripStore(tmp.root)
        assertNull(st.loadCurrent())
        st.saveCurrent(TripState(1000, 12.5, 900.0, 1.1))
        assertEquals(TripState(1000, 12.5, 900.0, 1.1), TripStore(tmp.root).loadCurrent())
        st.finish(TripState(1000, 12.5, 900.0, 1.1), 2000, Fuel.ETANOL, 4.0)
        assertNull(st.loadCurrent())
        assertEquals(listOf(Trip(1000, 2000, 12.5, 900.0, 1.1, Fuel.ETANOL, 4.4)), st.trips())
    }
    @Test fun tinyTripNotSaved() {
        val st = TripStore(tmp.root); st.finish(TripState(1, 0.05, 10.0, 0.0), 2, Fuel.GASOLINA, 5.0)
        assertTrue(st.trips().isEmpty())
    }
    @Test fun corruptLineSkipped() {
        val st = TripStore(tmp.root); st.finish(TripState(1, 3.0, 60.0, 0.2), 2, Fuel.GASOLINA, 5.0)
        File(tmp.root, "trips.csv").appendText("1;2;lixo\n")
        assertEquals(1, st.trips().size)
    }
    @Test fun currentSurvivesPartialTmp() {
        val st = TripStore(tmp.root); st.saveCurrent(TripState(1, 2.0, 3.0, 0.1))
        File(tmp.root, "current.tmp").writeText("1;2")   // corte de energia no meio da escrita
        assertEquals(TripState(1, 2.0, 3.0, 0.1), TripStore(tmp.root).loadCurrent())
    }
    @Test fun newestFirst() {
        val st = TripStore(tmp.root)
        st.finish(TripState(1, 1.0, 1.0, 0.1), 10, Fuel.GASOLINA, 1.0); st.finish(TripState(20, 1.0, 1.0, 0.1), 30, Fuel.GASOLINA, 1.0)
        assertEquals(listOf(20L, 1L), st.trips().map { it.startMs })
    }
}
class EventLogTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun keepsLast50NewestFirst() {
        val log = EventLog(File(tmp.root, "events.txt"))
        repeat(60) { log.add("E$it", it * 1000L) }
        val r = log.recent(); assertEquals(50, r.size); assertTrue(r.first().endsWith("E59")); assertTrue(r.last().endsWith("E10"))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*ParseDecimalTest' --tests '*SettingsRepoTest' --tests '*TripStoreTest' --tests '*EventLogTest'`
Expected: falha de compilação com `Unresolved reference`.

- [ ] **Step 3: Implementar**

- `parseDecimal`: troca `,` por `.`, depois `toDoubleOrNull`. Recusa negativo e mais de um separador.
- `SettingsRepo`: uma chave de prefs por campo. `Fuel` e `Accent` são guardados pelo `key`. `lat`/`lon` nulos removem a chave. `update` grava com `apply()` e emite no flow.
- `TripStore`:
  - `current.txt` tem uma linha `startMs;km;seconds;liters`;
  - `saveCurrent` grava em `current.tmp` e depois faz `renameTo` para `current.txt`;
  - `loadCurrent` só lê `current.txt` e devolve nulo se a linha estiver ruim;
  - `trips.csv` tem uma linha por viagem: `startMs;endMs;km;seconds;liters;fuelKey;cost`;
  - `finish` só grava se `km >= 0.1`, depois apaga a atual;
  - `trips()` ignora linhas ruins e ordena pela mais recente;
  - números gravados com `Double.toString()` e lidos com `toDouble()`, para a ida e volta ser exata.
- `EventLog`: linhas `yyyy-MM-dd HH:mm:ss  EVENTO` no fuso do aparelho, com no máximo 50 linhas no arquivo.

- [ ] **Step 4: Rodar e ver passar**

Run: o mesmo comando do Step 2.
Expected: PASS. Na primeira vez o Robolectric baixa o android-all 34, o que pode levar minutos.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: settings, trip history and event log storage"
```

---

### Task 6: Sessão ELM327 e motor OBD2 real

**Files:**
- Create: `main/obd/ElmSession.kt`, `main/obd/CarEngine.kt`, `main/obd/ObdEngine.kt`
- Test: `test/obd/FakeElm.kt`, `test/obd/ElmSessionTest.kt`, `test/obd/ObdEngineTest.kt`

**Interfaces:**
- Consumes: `Obd`, `Sample`, `Dtcs` e `Link` (Task 2).
- Produces:
  - `class ElmSession(input: InputStream, output: OutputStream, onClose: () -> Unit = {}) : Closeable { suspend fun cmd(c: String, timeoutMs: Long = 2000): String }`. Em timeout lança `IOException`.
  - `interface CarEngine { suspend fun run(onLink: (Link) -> Unit, onSample: (Sample) -> Unit); suspend fun readDtcs(): Dtcs?; suspend fun clearDtcs(): Boolean }`
  - `class ObdEngine(open: () -> ElmSession) : CarEngine`, com `internal fun pollPlan(tick: Int, supported: Set<Int>): List<String>` visível para teste.

- [ ] **Step 1: Escrever o fake e os testes que falham**

`FakeElm(script: Map<String, String>)` devolve `session(): ElmSession`. Cada comando escrito (até `\r`) entra em `sent: MutableList<String>` e enfileira `script[cmd] ?: "?"` + `"\r\r>"`. Se `failAfter` estiver definido, o comando seguinte lança `IOException`.

```kotlin
class ElmSessionTest {
    @Test fun readsUntilPrompt() = runTest {
        val f = FakeElm(mapOf("ATRV" to "14.1V")); assertEquals("14.1V", f.session().cmd("ATRV"))
    }
    @Test fun timeoutThrows() {
        val silent = ElmSession(object : InputStream() { override fun read() = -1; override fun available() = 0 }, ByteArrayOutputStream())
        assertThrows(IOException::class.java) { runBlocking { silent.cmd("010C", 50) } }
    }
}
class ObdEngineTest {
    private val car = mapOf("ATZ" to "ELM327 v1.5", "ATE0" to "OK", "ATL0" to "OK", "ATS0" to "OK", "ATH0" to "OK", "ATSP0" to "OK",
        "0100" to "41 00 BE 3E A8 13", "ATDPN" to "A3", "010C" to "41 0C 1A F8", "010D" to "41 0D 3E", "0105" to "41 05 81",
        "ATRV" to "14.1V", "010B" to "41 0B 28", "010F" to "41 0F 46", "03" to "43 01 71 00 00 00 00", "07" to "NO DATA",
        "0A" to "NO DATA", "04" to "44")
    @Test fun initThenPoll() = runTest {
        val f = FakeElm(car); val links = mutableListOf<Link>(); var last = Sample()
        val job = launch { ObdEngine { f.session() }.run({ links += it }, { last = it }) }
        advanceTimeBy(5_000); job.cancel()
        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "0100", "ATDPN"), f.sent.take(8))
        assertEquals(Link.CONNECTED, links.last()); assertEquals(1726, last.rpm); assertEquals(62, last.speedKmh)
        assertEquals(89, last.coolantC); assertEquals(14.1, last.batteryV!!, 1e-9); assertEquals(40, last.mapKpa)
    }
    @Test fun pollPlanUsesSupported() {
        val e = ObdEngine { error("unused") }
        val mapCar = (0 until 8).flatMap { e.pollPlan(it, setOf(0x0B, 0x0F)) }.toSet()
        assertEquals(setOf("010C", "010D", "0105", "ATRV", "010B", "010F"), mapCar)
        val mafCar = (0 until 8).flatMap { e.pollPlan(it, setOf(0x10)) }.toSet()
        assertEquals(setOf("010C", "010D", "0105", "ATRV", "0110"), mafCar)
        assertEquals(listOf("010C", "010D"), e.pollPlan(0, emptySet()).take(2))
    }
    @Test fun dtcsAndClear() = runTest {
        val f = FakeElm(car); val e = ObdEngine { f.session() }
        val job = launch { e.run({}, {}) }; advanceTimeBy(1_000)
        assertEquals(Dtcs(listOf("P0171"), emptyList(), emptyList()), e.readDtcs())
        assertTrue(e.clearDtcs()); assertTrue("04" in f.sent); job.cancel()
    }
    @Test fun ignitionOffReportsIgnitionOff() = runTest {
        val f = FakeElm(car + ("0100" to "SEARCHING...\rUNABLE TO CONNECT")); val links = mutableListOf<Link>()
        val job = launch { ObdEngine { f.session() }.run({ links += it }, {}) }; advanceTimeBy(12_000); job.cancel()
        assertTrue(Link.IGNITION_OFF in links); assertFalse(Link.CONNECTED in links)
        assertTrue(f.sent.count { it == "0100" } >= 2)   // continua tentando
    }
    @Test fun ioErrorReconnects() = runTest {
        var opens = 0; val links = mutableListOf<Link>()
        val job = launch { ObdEngine { opens++; FakeElm(car, failAfter = if (opens == 1) 12 else null).session() }.run({ links += it }, {}) }
        advanceTimeBy(10_000); job.cancel()
        assertTrue(opens >= 2); assertEquals(listOf(Link.CONNECTING, Link.CONNECTED, Link.CONNECTING, Link.CONNECTED), links.take(4))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*ElmSessionTest' --tests '*ObdEngineTest'`
Expected: falha de compilação com `Unresolved reference: ElmSession`.

- [ ] **Step 3: Implementar**

- `ElmSession.cmd`:
  1. Descarta os bytes pendentes, escreve `"$c\r"` e dá flush.
  2. Dentro de `withTimeout(timeoutMs)`, fica em loop: lê o que `available()` tiver, ou espera com `delay(10)`, até achar `>`.
  3. Devolve o texto antes do `>`, sem espaços nas pontas.
  4. Troca `TimeoutCancellationException` por `IOException("timeout $c")`.
- `ObdEngine.run`, em loop até ser cancelado:
  1. `onLink(CONNECTING)` e `open()`. O motor não troca de dispatcher: o serviço já o roda em `Dispatchers.IO`, e assim os testes ficam em tempo virtual.
  2. Inicialização: `ATZ` com 3000 ms, depois `ATE0 ATL0 ATS0 ATH0 ATSP0`, depois `0100` com 10000 ms.
  3. Se `Obd.unableToConnect`: `onLink(IGNITION_OFF)`, `delay(5000)` e volta ao passo 2 na mesma sessão.
  4. Senão lê `supportedPids` e `ATDPN` (→ `isCan`) e chama `onLink(CONNECTED)`.
  5. Poll: os comandos de `pollPlan(tick)`, mesclados num `Sample`, `onSample` uma vez por ciclo e `delay(100)`.
  6. Em `IOException`: fecha, espera 2 s dobrando até 30 s (volta para 2 s depois de um CONNECTED) e recomeça no passo 1. Só o passo 1 emite `CONNECTING`.
- `pollPlan(tick)`:
  - sempre começa com `010C` e `010D`;
  - depois um item da lista lenta, `["0105","ATRV"]` + (`0110` se o MAF for suportado; senão `010B`/`010F` se forem suportados), escolhido por `tick % tamanho`.
  - Com `supported` vazio (antes do `0100`), a lista lenta é só `["0105","ATRV"]`.
- `readDtcs`/`clearDtcs` usam o mesmo `Mutex` do poll:
  - `readDtcs` manda `03`, `07` e `0A` e mapeia com `Obd.dtcs(…, 0x43/0x47/0x4A, isCan)`;
  - `clearDtcs` manda `04` e devolve `true` se a resposta tiver `44`;
  - os dois devolvem `null`/`false` quando não há sessão conectada.

- [ ] **Step 4: Rodar e ver passar**

Run: o mesmo comando do Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(obd): ELM327 session, init, polling and reconnect"
```

---

### Task 7: Alertas

**Files:**
- Create: `main/car/Alerts.kt`
- Test: `test/car/AlertsTest.kt`

**Interfaces:**
- Consumes: `Sample` (Task 2) e `Settings` (Task 5).
- Produces:
  - `enum class Alert(val text: String, val urgent: Boolean)`: `MOTOR_QUENTE("Motor quente", true)`, `BATERIA_BAIXA("Bateria baixa", true)`, `BATERIA_ALTA("Tensão alta na bateria", true)`, `REVISAO("Revisão chegando", false)`.
  - `fun alerts(s: Sample, cfg: Settings): Set<Alert>`

- [ ] **Step 1: Escrever o teste que falha**

```kotlin
class AlertsTest {
    private val cfg = Settings()
    @Test fun coolant() {
        assertEquals(setOf(Alert.MOTOR_QUENTE), alerts(Sample(coolantC = 106), cfg))
        assertTrue(alerts(Sample(coolantC = 105), cfg).isEmpty())
    }
    @Test fun battery() {
        assertEquals(setOf(Alert.BATERIA_BAIXA), alerts(Sample(rpm = 800, batteryV = 11.9), cfg))
        assertTrue(alerts(Sample(rpm = 0, batteryV = 11.9), cfg).isEmpty())
        assertEquals(setOf(Alert.BATERIA_ALTA), alerts(Sample(batteryV = 15.1), cfg))
    }
    @Test fun service() {
        assertEquals(setOf(Alert.REVISAO), alerts(Sample(), cfg.copy(odometerKm = 9600.0, nextServiceKm = 10000.0)))
        assertTrue(alerts(Sample(), cfg.copy(odometerKm = 9400.0, nextServiceKm = 10000.0)).isEmpty())
        assertTrue(alerts(Sample(), cfg.copy(odometerKm = 9600.0, nextServiceKm = 0.0)).isEmpty())
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*AlertsTest'`
Expected: falha de compilação com `Unresolved reference: alerts`.

- [ ] **Step 3: Implementar `alerts`**

- Motor quente: `coolantC > alertCoolantC`.
- Bateria baixa: `batteryV < alertBatteryLowV` com `rpm > 400`.
- Bateria alta: `batteryV > alertBatteryHighV`.
- Revisão: `nextServiceKm > 0 && odometerKm >= nextServiceKm - alertServiceBeforeKm`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*AlertsTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(car): engine, battery and service alerts"
```

---

### Task 8: Modo demo, conexão Bluetooth, CarRepo e serviço em primeiro plano

**Files:**
- Create: `main/obd/DemoEngine.kt`, `main/obd/BtConnector.kt`, `main/car/CarRepo.kt`, `main/car/ObdService.kt`
- Modify: `main/UnoApp.kt` (objeto `Graph`), `app/src/main/AndroidManifest.xml`
- Test: `test/obd/DemoEngineTest.kt`, `test/obd/BtConnectorTest.kt`

**Interfaces:**
- Consumes: `CarEngine`, `ObdEngine`, `ElmSession` (Task 6); `advance`, `TripState` (Task 4); `TripStore`, `SettingsRepo`, `EventLog` (Task 5); `alerts` (Task 7).
- Produces:
  - `class DemoEngine(tickMs: Long = 250) : CarEngine`
  - `fun pickAdapter(bonded: List<Pair<String, String>>, saved: String?): String?`, onde cada par é (nome, endereço).
  - `fun btSession(ctx: Context, address: String): ElmSession`
  - `fun hasBluetooth(ctx: Context): Boolean`: `BLUETOOTH_CONNECT` concedida no API 31+; antes disso sempre `true`.
  - `data class CarState(val link: Link = Link.OFF, val sample: Sample = Sample(), val dtcs: Dtcs? = null, val trip: TripState = TripState(), val alerts: Set<Alert> = emptySet(), val problem: String? = null)`
  - `object CarRepo { val state: StateFlow<CarState>; @Volatile var engine: CarEngine?; @Volatile var onNewTrip: (() -> Unit)?; fun set(f: (CarState) -> CarState); suspend fun readDtcs(): Dtcs?; suspend fun clearDtcs(): Boolean; fun newTrip() }`. `readDtcs` também grava o resultado em `state.dtcs`. `clearDtcs`, quando dá certo, chama `readDtcs` de novo.
  - `class ObdService : Service`, com `companion fun start(ctx: Context)`
  - `object Graph { lateinit var settings: SettingsRepo; lateinit var trips: TripStore; lateinit var events: EventLog }`, iniciado em `UnoApp.onCreate`.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
class DemoEngineTest {
    @Test fun plausibleAndClearable() = runTest {
        val e = DemoEngine(); val samples = mutableListOf<Sample>(); var link: Link? = null
        val job = launch { e.run({ link = it }, { samples += it }) }; advanceTimeBy(60_000); job.cancel()
        assertEquals(Link.DEMO, link); assertTrue(samples.size > 100)
        assertTrue(samples.all { it.rpm!! in 700..5000 && it.speedKmh!! in 0..120 && it.batteryV!! in 13.5..14.6 })
        assertEquals(2, e.readDtcs()!!.count); assertTrue(e.clearDtcs()); assertEquals(0, e.readDtcs()!!.count)
    }
}
class BtConnectorTest {
    private val bonded = listOf("JBL" to "AA:AA", "OBDII" to "BB:BB")
    @Test fun pickAdapter() {
        assertEquals("CC:CC", pickAdapter(bonded + ("X" to "CC:CC"), "CC:CC"))
        assertEquals("BB:BB", pickAdapter(bonded, "ZZ:ZZ"))
        assertEquals("BB:BB", pickAdapter(bonded, null))
        assertNull(pickAdapter(listOf("JBL" to "AA:AA"), null))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*DemoEngineTest' --tests '*BtConnectorTest'`
Expected: falha de compilação com `Unresolved reference`.

- [ ] **Step 3: Implementar `DemoEngine` e `pickAdapter`/`btSession`**

- Demo, com `t` em segundos:
  - `speed = 55 + 45·sin(t/25)` limitado a 0..120;
  - `rpm = 850 + speed·38`;
  - `coolant` sobe de 60 a 90 em 120 s;
  - `battery = 14.1 + 0.1·sin(t/7)`;
  - `map = 35 + 25·|sin(t/9)|` e `iat = 30`;
  - DTCs iniciais: armazenado `P0171` e pendente `P0300`.
- `pickAdapter`: o endereço salvo, se estiver pareado; senão o primeiro chamado "OBDII"; senão nulo.
- `btSession`:
  1. `adapter.cancelDiscovery()` e `createRfcommSocketToServiceRecord(SPP)`.
  2. `connect()`; se der `IOException`, tenta de novo com `createInsecureRfcommSocketToServiceRecord`.
  3. Devolve `ElmSession(socket.inputStream, socket.outputStream) { socket.close() }`.

- [ ] **Step 4: Rodar e ver passar**

Run: o mesmo comando do Step 2.
Expected: PASS.

- [ ] **Step 5: Implementar `CarRepo` e `ObdService`**

- `ObdService`:
  - Canal de notificação "obd" (baixa importância): "Uno Launcher · OBD2". `ServiceCompat.startForeground(this, 1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)`.
  - Observa `Graph.settings.flow.map { it.demo to it.adapterAddress }.distinctUntilChanged()` com `collectLatest` e escolhe o motor:
    - `DemoEngine`;
    - ou `ObdEngine { btSession(this, endereço) }`;
    - ou nenhum, com `problem` = "Bluetooth desligado", "Pareie o adaptador OBDII nas configurações de Bluetooth" ou "Libere o Bluetooth em Configurações".
  - Liga `CarRepo.engine` e `CarRepo.onNewTrip`. Quando o link vira CONNECTED ou DEMO, chama `CarRepo.readDtcs()` uma vez, para a contagem do painel.
  - Roda tudo num `CoroutineScope(SupervisorJob() + Dispatchers.IO)`, cancelado no `onDestroy`.
  - A cada `onSample`, nesta ordem:
    1. `dt` pelo `SystemClock.elapsedRealtime()`;
    2. `trip = advance(…, settings.fuel, settings.fuelMultiplier)`;
    3. `alerts(sample, settings)`;
    4. publica em `CarRepo`;
    5. alerta `urgent` novo (que não estava no conjunto anterior) vira notificação no canal "alertas" (alta importância).
  - A cada 10 s, só fora do demo: `trips.saveCurrent(trip)` e `settings.update { it.copy(odometerKm = it.odometerKm + Δkm) }`.
  - No demo, a viagem começa em `TripState()` e nunca é salva. Ao sair do demo, a viagem real volta de `trips.loadCurrent()`.
  - Em `onDestroy`, salva a viagem (se não for demo) e zera `CarRepo.engine`.
- `CarRepo.newTrip()`: se o serviço estiver rodando, chama `onNewTrip`. Senão faz `Graph.trips.loadCurrent()?.let { finish(it, agora, fuel, price) }`.
- Manifesto:
  - `<service android:name=".car.ObdService" android:exported="false" android:foregroundServiceType="connectedDevice"/>`;
  - permissões `BLUETOOTH` e `BLUETOOTH_ADMIN` (`maxSdkVersion="30"`), `BLUETOOTH_CONNECT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` e `POST_NOTIFICATIONS`.
- `ObdService.start` não faz nada se `!hasBluetooth(ctx)`. Assim o serviço nunca cai por falta de permissão no API 34+.

- [ ] **Step 6: Build e lint**

Run: `./g :app:testDebugUnitTest :app:lintDebug`
Expected: `BUILD SUCCESSFUL` e nenhum erro de lint (`MissingPermission` resolvido com checagem explícita).

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(car): demo engine, bluetooth link and foreground OBD service"
```

---

### Task 9: Sol, brilho e localização

**Files:**
- Create: `main/env/Sun.kt`, `main/env/Brightness.kt`, `main/env/Location.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `test/env/SunTest.kt`

**Interfaces:**
- Consumes: `Settings`, `SettingsRepo` (Task 5).
- Produces:
  - `object Sun { fun times(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): Pair<ZonedDateTime, ZonedDateTime>?; fun isDay(now: ZonedDateTime, lat: Double?, lon: Double?): Boolean; fun nextChange(now: ZonedDateTime, lat: Double?, lon: Double?): ZonedDateTime }`
  - `object Brightness { fun canWrite(ctx: Context): Boolean; fun apply(ctx: Context) }`
  - `class BrightnessReceiver : BroadcastReceiver`
  - `fun refreshLocation(ctx: Context)`

- [ ] **Step 1: Escrever o teste que falha**

```kotlin
class SunTest {
    private val rio = ZoneId.of("America/Sao_Paulo"); private val lat = -22.9068; private val lon = -43.1729
    private fun near(h: Int, m: Int, t: ZonedDateTime) = assertTrue("$t", abs(t.hour * 60 + t.minute - (h * 60 + m)) <= 5)
    @Test fun rioNovember() { val (r, s) = Sun.times(LocalDate.of(2026, 11, 14), lat, lon, rio)!!; near(5, 1, r); near(18, 12, s) }
    @Test fun rioJune() { val (r, s) = Sun.times(LocalDate.of(2026, 6, 21), lat, lon, rio)!!; near(6, 32, r); near(17, 15, s) }
    @Test fun dayNight() {
        assertTrue(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, rio), lat, lon))
        assertFalse(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 23, 0, 0, 0, rio), lat, lon))
        assertFalse(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 5, 59, 0, 0, rio), null, null))
        assertTrue(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 6, 0, 0, 0, rio), null, null))
    }
    @Test fun next() {
        near(18, 12, Sun.nextChange(ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, rio), lat, lon))
        val n = Sun.nextChange(ZonedDateTime.of(2026, 11, 14, 20, 0, 0, 0, rio), lat, lon)
        assertEquals(15, n.dayOfMonth); near(5, 1, n)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*SunTest'`
Expected: falha de compilação com `Unresolved reference: Sun`.

- [ ] **Step 3: Implementar**

- `Sun.times`: equação do nascer do sol da NOAA (equação do tempo e declinação pela série de Fourier em função do dia do ano; zênite 90,833°). Devolve nulo quando o sol não nasce ou não se põe.
- Sem lat/lon, `isDay` e `nextChange` usam 06:00 e 18:00 locais.
- `Brightness.apply`:
  - não faz nada se `!autoBrightness` ou se não puder escrever;
  - grava `SCREEN_BRIGHTNESS_MODE` = manual e `SCREEN_BRIGHTNESS` = `dayBrightness`/`nightBrightness`;
  - agenda o `BrightnessReceiver` com `AlarmManager.set(RTC, nextChange)`, que é inexato e não precisa de permissão de alarme exato.
- `refreshLocation`:
  - com `ACCESS_COARSE_LOCATION`, pega o `getLastKnownLocation` mais recente entre os providers `gps`, `network` e `passive`;
  - grava `lat`/`lon` arredondados a 2 casas.
- Manifesto: o receiver sem exportar e as permissões `WRITE_SETTINGS` (`tools:ignore="ProtectedPermissions"`) e `ACCESS_COARSE_LOCATION`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*SunTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(env): local sunrise/sunset and day/night brightness"
```

---

### Task 10: Clima do met.no

**Files:**
- Create: `main/env/Weather.kt`
- Modify: `app/build.gradle.kts` (`buildConfigField`), `app/src/main/AndroidManifest.xml` (`INTERNET`), `main/UnoApp.kt` (`Graph.weather = WeatherRepo(prefs)`)
- Test: `test/env/MetnoTest.kt`

**Interfaces:**
- Produces:
  - `data class Weather(val tempC: Double, val symbol: String, val atMs: Long)`
  - `object Metno { fun url(lat: Double, lon: Double): String; fun parse(json: String, nowMs: Long): Weather?; fun emoji(symbol: String): String; fun userAgent(): String }`
  - `class WeatherRepo(prefs: SharedPreferences) { val flow: StateFlow<Weather?>; suspend fun refreshIfStale(lat: Double, lon: Double, nowMs: Long) }`
  - `lateinit var weather: WeatherRepo` dentro de `Graph`.

- [ ] **Step 1: Escrever o teste que falha**

```kotlin
class MetnoTest {
    private val json = """{"properties":{"timeseries":[{"time":"2026-11-14T15:00:00Z","data":{"instant":{"details":{"air_temperature":27.3}},
        "next_1_hours":{"summary":{"symbol_code":"clearsky_day"}}}}]}}"""
    @Test fun parses() = assertEquals(Weather(27.3, "clearsky_day", 5L), Metno.parse(json, 5L))
    @Test fun sixHourFallback() = assertEquals("rain", Metno.parse(json.replace("next_1_hours", "next_6_hours").replace("clearsky_day", "rain"), 0)!!.symbol)
    @Test fun broken() = assertNull(Metno.parse("{}", 0))
    @Test fun emoji() { assertEquals("☀", Metno.emoji("clearsky_day")); assertEquals("🌧", Metno.emoji("lightrainshowers_night")); assertEquals("⛈", Metno.emoji("heavyrainandthunder")) }
    @Test fun url() = assertEquals("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=-22.91&lon=-43.17", Metno.url(-22.9068, -43.1729))
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*MetnoTest'`
Expected: falha de compilação com `Unresolved reference: Metno`.

- [ ] **Step 3: Implementar**

- `parse` usa `org.json`, que já vem no Android. Pega o símbolo de `next_1_hours`, senão de `next_6_hours`. Qualquer exceção devolve nulo.
- `emoji`, verificado nesta ordem:
  - `thunder`→⛈, `snow`→❄, `rain|sleet`→🌧, `fog`→🌫, `cloudy`→☁, `fair|partlycloudy`→⛅;
  - `clearsky_night`→☾, `clearsky`→☀;
  - qualquer outro→"".
- `url` arredonda a 2 casas com `Locale.ROOT`.
- `userAgent()` = `"UnoLauncher/${BuildConfig.VERSION_NAME} ${BuildConfig.MET_CONTACT}".trim()`. Em `app/build.gradle.kts`: `buildConfigField("String", "MET_CONTACT", "\"${findProperty("unoContact") ?: ""}\"")`.
- `refreshIfStale`:
  - não faz nada se a última busca tiver menos de 30 min;
  - `HttpURLConnection` em `Dispatchers.IO` com o User-Agent e timeouts de 10 s;
  - guarda o resultado em prefs;
  - em erro, mantém o último valor.

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*MetnoTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(env): met.no weather with offline cache"
```

---

### Task 11: Música (sessões de mídia)

**Files:**
- Create: `main/media/MediaRepo.kt`, `main/media/MediaListener.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces:
  - `data class NowPlaying(val title: String, val artist: String, val art: Bitmap?, val durationMs: Long, val positionMs: Long, val updatedAtMs: Long, val speed: Float, val playing: Boolean) { fun positionAt(nowElapsedMs: Long): Long }`
  - `object MediaRepo { val flow: StateFlow<NowPlaying?>; fun hasAccess(ctx: Context): Boolean; fun start(ctx: Context); fun playPause(); fun next(); fun prev(); fun resume(ctx: Context) }`
  - `class MediaListener : NotificationListenerService`

- [ ] **Step 1: Implementar**

- `MediaListener.onListenerConnected` chama `MediaRepo.start(this)`.
- `start`:
  - `MediaSessionManager.getActiveSessions(ComponentName(ctx, MediaListener::class.java))` e `addOnActiveSessionsChangedListener`;
  - escolhe a sessão tocando, senão a primeira;
  - registra um `MediaController.Callback` para metadados e estado e publica `NowPlaying`.
- `hasAccess` = `NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)`.
- `resume`: se houver controller, chama `transportControls.play()`. Senão usa `AudioManager.dispatchMediaKeyEvent` com DOWN e UP de `KEYCODE_MEDIA_PLAY`, que chega ao último app de música, normalmente o Spotify.
- `positionAt` = `positionMs + (agora - updatedAtMs) * speed` quando estiver tocando, limitado à duração.
- Manifesto: `<service android:name=".media.MediaListener" android:exported="false" android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">` com o intent-filter `android.service.notification.NotificationListenerService`.

- [ ] **Step 2: Build e lint**

Run: `./g :app:assembleDebug :app:lintDebug`
Expected: `BUILD SUCCESSFUL` e nenhum erro de lint.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(media): now-playing via notification listener"
```

---

### Task 12: Eventos do sistema e ações ao ligar

**Files:**
- Create: `main/sys/BootReceiver.kt`, `main/sys/Startup.kt`
- Modify: `main/UnoApp.kt`, `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `EventLog`, `Graph` (Tasks 5 e 8); `MediaRepo.resume` (Task 11); `ObdService.start` (Task 8); `Brightness.apply` (Task 9).
- Produces:
  - `class BootReceiver : BroadcastReceiver`
  - `object Startup { fun runIfNewBoot(activity: Activity); fun launch(ctx: Context, pkg: String): Boolean }`

- [ ] **Step 1: Implementar**

- `BootReceiver`:
  - grava `intent.action` no `EventLog` para `BOOT_COMPLETED`, `android.intent.action.QUICKBOOT_POWERON` e `com.htc.intent.action.QUICKBOOT_POWERON`;
  - chama `Brightness.apply`.
- `UnoApp.onCreate`:
  - grava "APP INICIADO (boot #N)", com `N = Settings.Global.BOOT_COUNT`;
  - registra no app, com `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`: `SCREEN_ON`, `SCREEN_OFF`, `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED` e `ACTION_SHUTDOWN`, cada um gravado no `EventLog`.
- `Startup.runIfNewBoot`:
  - roda uma vez por `BOOT_COUNT`, guardado na prefs `lastBoot`, chamado pela activity porque o Android não deixa um receiver abrir telas;
  - nesta ordem:
    1. `ObdService.start` se `startObd` ou `demo` estiver ligado;
    2. `MediaRepo.resume` depois de 3 s se `startResumeMusic`;
    3. `launch(startOpenApp)` se estiver definido.
  - Ponha `// ponytail: dispara por boot; se a central só dormir/acordar, ligar ao SCREEN_ON depois do teste no carro`.
- `launch` usa `getLaunchIntentForPackage`. Se o app não existir, mostra o toast "<pkg> não está instalado" e devolve `false`.
- Manifesto:
  - receiver `exported=true` com as três actions de boot e a permissão `RECEIVE_BOOT_COMPLETED`;
  - `<queries><intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent></queries>`.

- [ ] **Step 2: Build e lint**

Run: `./g :app:testDebugUnitTest :app:lintDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(sys): boot event log and startup actions"
```

---

### Task 13: Componentes, tela inicial e painel OBD2, com fotos de tela

**Files:**
- Create: `main/ui/Fmt.kt`, `main/ui/Kit.kt`, `main/ui/HomeScreen.kt`, `main/ui/ObdScreen.kt`
- Test: `test/ui/FmtTest.kt`, `test/ui/Shots.kt`, `test/ui/ScreensShotTest.kt`

**Interfaces:**
- Consumes: `CarState`, `Link`, `Alert` (Tasks 7 e 8); `NowPlaying` (Task 11); `Weather`, `Metno.emoji` (Task 10); `Settings`; `UnoTheme`, `LocalScale`, `LocalAccent` (Task 1).
- Produces:
  - `object Fmt { fun temp(c: Int?): String; fun volts(v: Double?): String; fun money(v: Double): String; fun kmPerL(v: Double?): String; fun km(v: Double): String; fun duration(sec: Double): String; fun clock(t: ZonedDateTime): String; fun date(t: ZonedDateTime): String }`
  - Em `Kit.kt`:
    - `@Composable fun ScaledRoot(content: @Composable () -> Unit)` (`BoxWithConstraints` que fornece `LocalScale`)
    - `Tile(label: String, glyph: String, highlight: Boolean, onClick: () -> Unit, modifier: Modifier)`
    - `Bar(fraction: Float)`
    - `BackButton(onClick)`
    - `DemoBadge()`
    - `AlertBanner(alerts: Set<Alert>)`
  - `data class HomeUi(val clock: String, val date: String, val weather: String?, val coolant: String, val battery: String, val obdHint: String?, val music: NowPlaying?, val musicAccess: Boolean, val photo: ImageBitmap?)`
  - `@Composable fun HomeScreen(ui: HomeUi, onWaze: () -> Unit, onSpotify: () -> Unit, onObd: () -> Unit, onSettings: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit, onGrantMusic: () -> Unit)`
  - `@Composable fun ObdScreen(car: CarState, settings: Settings, photo: ImageBitmap?, onBack: () -> Unit, onDtcs: () -> Unit, onTrips: () -> Unit, onConnect: () -> Unit)`
  - Em `test/ui/Shots.kt`: `fun shoot(name: String, content: @Composable () -> Unit)`

- [ ] **Step 1: Escrever o teste de formatação que falha**

```kotlin
class FmtTest {
    @Test fun ptBr() {
        assertEquals("89°C", Fmt.temp(89)); assertEquals("—", Fmt.temp(null))
        assertEquals("14,1 V", Fmt.volts(14.08)); assertEquals("—", Fmt.volts(null))
        assertEquals("R$ 12,40", Fmt.money(12.4)); assertEquals("7,8 km/l", Fmt.kmPerL(7.81)); assertEquals("— km/l", Fmt.kmPerL(null))
        assertEquals("12,5 km", Fmt.km(12.46)); assertEquals("1h05", Fmt.duration(3900.0)); assertEquals("15 min", Fmt.duration(900.0))
        val t = ZonedDateTime.of(2026, 11, 14, 14, 32, 0, 0, ZoneId.of("America/Sao_Paulo"))
        assertEquals("14:32", Fmt.clock(t)); assertEquals("sex 14 nov", Fmt.date(t))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*FmtTest'`
Expected: falha de compilação com `Unresolved reference: Fmt`.

- [ ] **Step 3: Implementar `Fmt`**

Usa `String.format(Locale("pt","BR"), …)`. A data é `EEE d MMM` em pt-BR, minúscula e sem pontos.

- [ ] **Step 4: Rodar e ver passar**

Run: `./g :app:testDebugUnitTest --tests '*FmtTest'`
Expected: PASS.

- [ ] **Step 5: Escrever o teste de fotos de tela**

`Shots.kt`: `shoot(name, content)` roda nos três qualifiers abaixo. Para cada um chama `RuntimeEnvironment.setQualifiers(q)` e `captureRoboImage("build/shots/$name-$tag.png") { UnoTheme(Accent.VERMELHO) { ScaledRoot { content() } } }`.
- `"w1024dp-h600dp-land-mdpi"` → `1024x600`
- `"w853dp-h480dp-land-hdpi"` → `1280x720`
- `"w1280dp-h800dp-land-mdpi"` → `tablet`

`ScreensShotTest` usa `@RunWith(RobolectricTestRunner::class)`, `@GraphicsMode(GraphicsMode.Mode.NATIVE)` e `@Config(application = Application::class)`. Testes:
- `home()`: `HomeUi("14:32","sex 14 nov","27° ☀","89°C","14,1 V",null, NowPlaying("Nome da música","Artista",null,200_000,70_000,0,1f,true), true, null)`.
- `homeNoObdNoMusic()`: os valores "—", `obdHint = "OBD2 desconectado"`, `music = null` e `musicAccess = false`.
- `obd()`: `CarState(Link.CONNECTED, Sample(2400,62,89,14.1,40,30), Dtcs(emptyList(),emptyList(),emptyList()), TripState(0,12.0,1500.0,1.6), emptySet())` com `Settings(fuel = Fuel.ETANOL, pricePerL = 4.19)`.
- `obdIgnitionOff()`: `Link.IGNITION_OFF` com `Sample()`.
- `obdAlert()`: `alerts = setOf(Alert.MOTOR_QUENTE)` e `coolantC = 107`.

- [ ] **Step 6: Implementar `Kit`, `HomeScreen` e `ObdScreen`**

O layout segue o protótipo `layout-v2.html` aprovado. Todas as medidas multiplicam `LocalScale`.

- Home:
  - `Column`: em cima uma `Row` com `weight(1f)`, com a foto `weight(1.6f)` e a música `weight(1f)`; embaixo 4 `Tile` com altura de `22%` da tela.
  - Sem foto, um gradiente `#2A2A2E`→`#0C0C0E` com o texto "Escolha a foto do seu Uno em Configurações".
  - Na foto:
    - relógio de 56 sp com a data em cima à esquerda;
    - temperatura e bateria em destaque embaixo à direita;
    - faixa de destaque de 3 dp em 40% da base.
  - Música: capa (ou o glifo ♫), título, artista, `Bar` de progresso e ⏮ ⏯ ⏭ com o alvo mínimo das Global Constraints. Sem acesso, o botão "Liberar música".
  - Os atalhos são Waze ➤, Spotify ♫, OBD2 ◉ (borda de destaque) e Configurações ⚙.
- OBD:
  - fundo com a foto em 25% de opacidade;
  - `BackButton` com fundo de destaque;
  - conta-giros num `Canvas` com arco de 270°: fundo `#222222`, valor `rpm/7000` na cor de destaque, número "2.4" grande, "rpm x1000" e "62 km/h";
  - coluna da direita, nesta ordem:
    1. motor com `Bar` (40..120 °C);
    2. bateria com `Bar` (10..16 V);
    3. "viagem · etanol" com "R$ … · … km/l" e a linha pequena "estimativa", que abre `onTrips`;
    4. o cartão "Códigos de falha N", que abre `onDtcs`;
    5. o estado do link: "● OBD2 conectado", "Conectando…", "Ligue a ignição", "Modo demonstração" ou `problem`, mais o botão "Conectar" quando o link estiver OFF.
  - Sem preço, mostra "defina o preço" no lugar do R$.
- `AlertBanner`: uma faixa no topo com fundo de destaque e os `text` juntados por " · ".
- `DemoBadge`: um selo "DEMO" fixo no canto superior direito.

- [ ] **Step 7: Rodar as fotos e conferir**

Run: `./g :app:testDebugUnitTest --tests '*ScreensShotTest'`
Expected: PASS e 15 PNGs em `app/build/shots/`.

Abra cada um com a ferramenta de leitura de imagem. Confira que:
- nenhum texto foi cortado ou sobreposto;
- os 4 atalhos aparecem inteiros;
- o conta-giros é redondo;
- o tablet não tem elementos minúsculos.

Corrija e rode de novo até passar.

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(ui): home and OBD panel screens with screenshot checks"
```

---

### Task 14: Telas de códigos de falha e histórico de viagens

**Files:**
- Create: `main/ui/DtcScreen.kt`, `main/ui/TripsScreen.kt`
- Modify: `test/ui/ScreensShotTest.kt`

**Interfaces:**
- Consumes: `Dtcs`, `Link`, `DtcNames.describe`, `TripState`, `Trip`, `Settings`, `Fmt` e `Kit`.
- Produces:
  - `@Composable fun DtcScreen(dtcs: Dtcs?, link: Link, busy: Boolean, onBack: () -> Unit, onRead: () -> Unit, onClear: () -> Unit)`
  - `@Composable fun TripsScreen(current: TripState, trips: List<Trip>, settings: Settings, onBack: () -> Unit, onNewTrip: () -> Unit)`

- [ ] **Step 1: Adicionar as fotos que falham**

Em `ScreensShotTest`:
- `dtcs()`: `Dtcs(listOf("P0171"), listOf("P0300"), emptyList())` com `Link.CONNECTED`.
- `dtcsEmpty()`: `Dtcs(emptyList(), emptyList(), emptyList())`.
- `dtcsDisconnected()`: `null` com `Link.OFF`.
- `trips()`: a atual `TripState(0,12.0,1500.0,1.6)` e duas `Trip` com preço 4,19.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*ScreensShotTest'`
Expected: falha de compilação com `Unresolved reference: DtcScreen`.

- [ ] **Step 3: Implementar**

- `DtcScreen`:
  - seções "Armazenados", "Pendentes" e "Permanentes", cada código com `DtcNames.describe`;
  - sem códigos: "Nenhum código de falha ✓";
  - sem link: "Conecte o OBD2 para ler os códigos";
  - botões "Ler de novo" e "Apagar códigos", o segundo com fundo de destaque e ativo só com o link CONNECTED ou DEMO.
  - "Apagar códigos" abre um `AlertDialog`:
    - título "Apagar códigos de falha?";
    - texto "Apagar não conserta o defeito. Se o problema continuar, a luz de injeção acende de novo. Faça com a ignição ligada e o motor desligado.";
    - botões "Cancelar" e "Apagar".
- `TripsScreen`:
  - um cartão da viagem atual (km, tempo, litros, km/l, R$) com o botão "Nova viagem";
  - uma `LazyColumn` com o histórico: data `dd/MM HH:mm`, km, tempo, litros, km/l e R$;
  - a nota "litros e R$ são estimativas".

- [ ] **Step 4: Rodar as fotos e conferir**

Run: `./g :app:testDebugUnitTest --tests '*ScreensShotTest'`
Expected: PASS. Confira os novos PNGs como na Task 13.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): DTC and trip history screens"
```

---

### Task 15: Configurações, lista de apps e "Sobre este aparelho"

**Files:**
- Create: `main/ui/SettingsScreen.kt`, `main/ui/AppsScreen.kt`, `main/ui/AboutScreen.kt`, `main/sys/Perms.kt`
- Modify: `test/ui/ScreensShotTest.kt`

**Interfaces:**
- Consumes: `Settings`, `parseDecimal` (Task 5); `Accent`; `Fuel`; `Kit`.
- Produces:
  - `enum class Perm(val label: String) { BLUETOOTH("Bluetooth"), NOTIFICACOES("Notificações"), MUSICA("Acesso à música"), BRILHO("Alterar brilho"), LOCALIZACAO("Localização"), TELA_INICIAL("Tela inicial padrão") }`
  - `object Perms { fun granted(ctx: Context): Set<Perm>; fun runtime(p: Perm): Array<String>; fun settingsIntent(ctx: Context, p: Perm): Intent? }`
  - `@Composable fun SettingsScreen(s: Settings, granted: Set<Perm>, bonded: List<Pair<String, String>>, startAppLabel: String?, onChange: ((Settings) -> Settings) -> Unit, onPickPhoto: () -> Unit, onPerm: (Perm) -> Unit, onPickStartApp: () -> Unit, onApps: () -> Unit, onAbout: () -> Unit, onBack: () -> Unit)`
  - `data class AppEntry(val label: String, val pkg: String, val icon: ImageBitmap?)`
  - `@Composable fun AppsScreen(title: String, apps: List<AppEntry>, onPick: (String) -> Unit, onBack: () -> Unit)`
  - `@Composable fun AboutScreen(info: List<Pair<String, String>>, events: List<String>, onBack: () -> Unit)`

- [ ] **Step 1: Adicionar as fotos que falham**

- `settings()`: `Settings()` com `granted = setOf(Perm.BLUETOOTH)` e `bonded = listOf("OBDII" to "BB:BB")`.
- `apps()`: 12 `AppEntry` com `icon = null`.
- `about()`: as linhas "Resolução" 1024×600, "Densidade" 160 dpi e "Android" 13 (SDK 33), mais 3 eventos.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./g :app:testDebugUnitTest --tests '*ScreensShotTest'`
Expected: falha de compilação com `Unresolved reference: SettingsScreen`.

- [ ] **Step 3: Implementar**

`SettingsScreen` é uma `LazyColumn` com as seções da spec 5.1, nesta ordem:
1. **Carro:** foto, combustível (2 chips), preço do litro, ajuste fino do consumo, hodômetro atual, km da próxima revisão e adaptador (lista `bonded`).
2. **Ao ligar:** conectar OBD2, retomar música e abrir app (switch + "Escolher app").
3. **Tela:** tema (4 chips de cor), brilho automático, brilho de dia e de noite (sliders 0..255 mostrados em %) e tela cheia.
4. **Alertas:** temperatura, bateria baixa, bateria alta e "avisar revisão antes de (km)".
5. **Teste:** modo demonstração (switch).
6. **Apps:** abre `onApps`.
7. **Permissões:** cada `Perm` com ✓ ou o botão "Liberar".
8. **Sobre este aparelho:** abre `onAbout`.

Detalhes:
- Os campos numéricos usam `KeyboardType.Decimal` e mostram texto local. No `onDone` ou ao perder o foco, `parseDecimal`: se for nulo, volta para o valor salvo e mostra "valor inválido" em vermelho.
- `Perms.granted` checa:
  - Bluetooth por `hasBluetooth` (Task 8);
  - `POST_NOTIFICATIONS` só no API 33+;
  - `MediaRepo.hasAccess`, `Brightness.canWrite` e `ACCESS_COARSE_LOCATION`;
  - tela inicial padrão: `resolveActivity(Intent(MAIN).addCategory(HOME))` aponta para este pacote.
- `settingsIntent`:
  - `ACTION_NOTIFICATION_LISTENER_SETTINGS` para música;
  - `ACTION_MANAGE_WRITE_SETTINGS` com `package:` para brilho;
  - `ACTION_HOME_SETTINGS` para a tela inicial;
  - nulo para as runtime, que vão pela launcher de permissões.
- `AppsScreen`: uma grade de ícones de 5 colunas, ordenada pelo nome, com busca por texto no topo.

- [ ] **Step 4: Rodar as fotos e conferir**

Run: `./g :app:testDebugUnitTest --tests '*ScreensShotTest'`
Expected: PASS. Confira os PNGs.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): settings, app list and device info screens"
```

---

### Task 16: Ligar tudo na MainActivity

**Files:**
- Modify: `main/MainActivity.kt`

**Interfaces:**
- Consumes: todas as telas (Tasks 13–15), `CarRepo`, `ObdService.start`, `MediaRepo`, `WeatherRepo`, `Brightness`, `refreshLocation`, `Startup`, `Perms`, `Graph`.
- Produces: o app completo.

- [ ] **Step 1: Implementar**

Navegação:
- `enum class Screen { HOME, OBD, DTC, TRIPS, SETTINGS, APPS, PICK_START_APP, ABOUT }` com `rememberSaveable`.
- `BackHandler(screen != HOME)` volta para a tela mãe: DTC e TRIPS voltam para OBD; APPS, PICK_START_APP e ABOUT voltam para SETTINGS.
- `onNewIntent` com a categoria HOME volta para HOME.

Estado e telas:
- Coleta `Graph.settings.flow`, `CarRepo.state`, `MediaRepo.flow` e `WeatherRepo.flow`. O relógio é atualizado com `LaunchedEffect` a cada 1 s.
- `HomeUi` e o `obdHint` são montados a partir do `CarState`:
  - "OBD2 desconectado" quando o link não está CONNECTED nem DEMO;
  - "Ligue a ignição" quando está IGNITION_OFF.
- Os atalhos chamam `Startup.launch(ctx, "com.waze")` e `Startup.launch(ctx, "com.spotify.music")`.
- `UnoTheme(settings.accent)` envolve tudo. `AlertBanner` e `DemoBadge` (com `settings.demo`) ficam por cima de qualquer tela.

Foto do Uno:
- O `PickVisualMedia(ImageOnly)` copia o arquivo para `filesDir/car.jpg`.
- `BitmapFactory` com `inSampleSize` limita o lado maior a 1600 px. A foto é recarregada depois da troca.

Ciclo de vida:
- `onCreate`:
  1. Pede as permissões runtime que faltam (`Perms.runtime`).
  2. `Startup.runIfNewBoot(this)`.
- `onResume`:
  1. `refreshLocation`.
  2. `weather.refreshIfStale` se houver lat/lon.
  3. `Brightness.apply`.
  4. `ObdService.start` se `startObd` ou `demo` estiver ligado. Ele já confere `hasBluetooth`.
  5. Se `MediaRepo.hasAccess`, `MediaRepo.start`.
- Tela cheia: `WindowCompat.getInsetsController(window, decor)` com `hide/show(systemBars())` e `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, aplicada sempre que `settings.fullscreen` mudar.

Configurações e telas internas:
- `bonded` vem do `BluetoothAdapter.bondedDevices` só com permissão; senão lista vazia.
- A lista de apps usa `queryIntentActivities(MAIN/LAUNCHER)`, com o ícone via `loadIcon().toBitmap(96,96)` carregado em `Dispatchers.IO`.
- Na tela de códigos, `onRead`/`onClear` rodam com `CarRepo.readDtcs`/`clearDtcs` em `lifecycleScope`, com `busy` ligado durante a chamada.
- A tela de viagens lê `Graph.trips.trips()`. "Nova viagem" chama `CarRepo.newTrip()` e recarrega a lista.
- `About` mostra:
  - resolução (`currentWindowMetrics` no API 30+, senão `getRealMetrics`), densidade e Android (`RELEASE`/`SDK_INT`);
  - fabricante/modelo, boot #, versão do app e caminho do log;
  - `Graph.events.recent()`.

- [ ] **Step 2: Build completo e lint**

Run: `./g :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
Expected: `BUILD SUCCESSFUL` com todos os testes passando e nenhum erro de lint.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: wire screens, services and startup in MainActivity"
```

---

### Task 17: Assinatura, release e publicação do link

**Files:**
- Modify: `app/build.gradle.kts` (`signingConfigs`)
- Create: `scripts/release.sh`, `README.md`

**Interfaces:**
- Consumes: o app completo.
- Produces: o link `https://github.com/dudubarbaro/uno-launcher/releases/latest/download/uno-launcher.apk`.

- [ ] **Step 1: Criar a chave e as propriedades fora do repositório**

1. Se `~/.android-keys/uno-launcher.jks` já existir, **não** sobrescreva.
2. Gere uma senha com `python3 -c 'import secrets;print(secrets.token_urlsafe(24))'`.
3. Crie a chave: `keytool -genkeypair -keystore ~/.android-keys/uno-launcher.jks -alias uno -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Uno Launcher" -storepass <senha> -keypass <senha>`.
4. Leia `~/.gradle/gradle.properties` se ele existir e só acrescente: `unoKeystore`, `unoKeystorePassword`, `unoKeyAlias=uno`, `unoKeyPassword` e `unoContact=<e-mail que o dono informou na conversa>`.
5. Rode `chmod 600` no arquivo.

O e-mail não aparece em nenhum arquivo do repositório.

- [ ] **Step 2: Ligar a assinatura no build**

`signingConfigs.create("uno")` lê as quatro propriedades, e `release.signingConfig` aponta para ela. Isso só acontece `if (project.hasProperty("unoKeystore"))`; sem as propriedades o release sai sem assinatura, o que deixa outra pessoa compilar. `isMinifyEnabled = false`, com o comentário `// ponytail: sem R8; ligar se o tamanho do APK incomodar`.

- [ ] **Step 3: Build de release e verificação**

Run: `./g :app:testDebugUnitTest :app:lintRelease :app:assembleRelease && ~/tools/android-sdk/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk | head -1 && (git grep -qF "$(sed -n 's/^unoContact=//p' ~/.gradle/gradle.properties)" && echo EMAIL_VAZOU || echo SEM_EMAIL_NO_REPO)`
Expected: `BUILD SUCCESSFUL`, a linha `Signer #1 certificate DN: CN=Uno Launcher` e `SEM_EMAIL_NO_REPO`.

- [ ] **Step 4: `scripts/release.sh` e `README.md`**

`release.sh`, com `set -eu`:
1. Lê `versionName` de `app/build.gradle.kts`.
2. Para se a tag `v<versão>` já existir.
3. Roda `./g :app:assembleRelease`.
4. Copia o APK para `build/dist/uno-launcher.apk`.
5. `git tag v<versão> && git push origin main --tags`.
6. `gh release create v<versão> build/dist/uno-launcher.apk --repo dudubarbaro/uno-launcher --title "Uno Launcher <versão>" --notes "APK para a central e para o tablet."`.

`README.md` em pt-BR, curto:
- as três formas de instalar (link, pendrive, adb Wi-Fi: `adb connect <ip>:<porta>` + `adb install -r`);
- escolher como tela inicial;
- testar no tablet com o modo demonstração;
- aumentar `versionCode`/`versionName` antes de cada release;
- **fazer backup de `~/.android-keys/uno-launcher.jks` e das senhas em `~/.gradle/gradle.properties`**. Sem eles, uma versão nova não instala por cima e é preciso desinstalar, o que apaga o histórico.

Commit: `git add -A && git commit -m "build: release signing, release script and README"`.

- [ ] **Step 5: PARAR e pedir ao dono**

Isto publica para fora. Mostre ao dono:
- O repositório `dudubarbaro/uno-launcher` vai ser **público**: o código e o APK.
- O e-mail dele vai dentro do APK.
- Ele precisa rodar `! gh auth login`, escolhendo GitHub.com, HTTPS e o navegador, com a conta `dudubarbaro`.

Espere a confirmação explícita.

- [ ] **Step 6: Publicar e verificar o link**

1. `gh auth switch --user dudubarbaro` (se houver mais de uma conta) e confirme com `gh auth status`.
2. `gh repo create dudubarbaro/uno-launcher --public --source . --remote origin --push`.
3. `scripts/release.sh`.
4. Run: `curl -sIL -o /dev/null -w '%{http_code}\n' https://github.com/dudubarbaro/uno-launcher/releases/latest/download/uno-launcher.apk`

Expected: `200`.

Passe o link ao dono, com o passo a passo do tablet: abrir no navegador, permitir apps desconhecidos, instalar, abrir pelo ícone e ligar o modo demonstração em Configurações.

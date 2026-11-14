package io.github.dudubarbaro.unolauncher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.dudubarbaro.unolauncher.car.CarRepo
import io.github.dudubarbaro.unolauncher.car.ObdService
import io.github.dudubarbaro.unolauncher.car.engineRunning
import io.github.dudubarbaro.unolauncher.env.Brightness
import io.github.dudubarbaro.unolauncher.env.Metno
import io.github.dudubarbaro.unolauncher.env.refreshLocation
import io.github.dudubarbaro.unolauncher.media.MediaRepo
import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.obd.bondedDevices
import io.github.dudubarbaro.unolauncher.obd.hasBluetooth
import io.github.dudubarbaro.unolauncher.sys.Perm
import io.github.dudubarbaro.unolauncher.sys.Perms
import io.github.dudubarbaro.unolauncher.sys.Startup
import io.github.dudubarbaro.unolauncher.ui.AboutScreen
import io.github.dudubarbaro.unolauncher.ui.AlertBanner
import io.github.dudubarbaro.unolauncher.ui.AppEntry
import io.github.dudubarbaro.unolauncher.ui.AppsScreen
import io.github.dudubarbaro.unolauncher.ui.DemoBadge
import io.github.dudubarbaro.unolauncher.ui.DtcScreen
import io.github.dudubarbaro.unolauncher.ui.Fmt
import io.github.dudubarbaro.unolauncher.ui.HomeScreen
import io.github.dudubarbaro.unolauncher.ui.HomeUi
import io.github.dudubarbaro.unolauncher.ui.ObdScreen
import io.github.dudubarbaro.unolauncher.ui.ScaledRoot
import io.github.dudubarbaro.unolauncher.ui.SettingsScreen
import io.github.dudubarbaro.unolauncher.ui.TripsScreen
import io.github.dudubarbaro.unolauncher.ui.UnoTheme
import io.github.dudubarbaro.unolauncher.ui.pianoBackground
import io.github.dudubarbaro.unolauncher.ui.resolveShortcut
import io.github.dudubarbaro.unolauncher.ui.shortcutPackage
import io.github.dudubarbaro.unolauncher.ui.su
import java.io.File
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen(val parentName: String) {
    HOME("HOME"), OBD("HOME"), DTC("OBD"), TRIPS("OBD"), SETTINGS("HOME"), APPS("SETTINGS"), PICK_START_APP("SETTINGS"), PICK_SHORTCUT("SETTINGS"), ABOUT("SETTINGS");

    val parent: Screen get() = valueOf(parentName)
}

class MainActivity : ComponentActivity() {
    private val homePressed = MutableStateFlow(0)
    private val resumed = MutableStateFlow(0)
    private val photoFile by lazy { File(filesDir, "car.jpg") }

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        resumed.value++
        val denied = result.filterValues { !it }.keys
        // Negada de vez: só dá para liberar na tela do app nas configurações do sistema.
        if (denied.isNotEmpty() && denied.none { shouldShowRequestPermissionRationale(it) } && askedBefore) {
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        val s = Graph.settings.value
        if (hasBluetooth(this) && (s.startObd || s.demo)) ObdService.start(this)
    }
    private var askedBefore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("startup", MODE_PRIVATE)
        if (!prefs.getBoolean("askedPerms", false)) {
            prefs.edit().putBoolean("askedPerms", true).apply()
            val missing = Perm.entries.flatMap { Perms.runtime(it).toList() }.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
        }
        Startup.runIfNewBoot(this)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePressed.value++
    }

    override fun onResume() {
        super.onResume()
        resumed.value++
        refreshLocation(this)
        val s = Graph.settings.value
        if (s.lat != null && s.lon != null) lifecycleScope.launch { Graph.weather.refreshIfStale(s.lat, s.lon, System.currentTimeMillis()) }
        Brightness.apply(this)
        if (s.startObd || s.demo) ObdService.start(this)
        MediaRepo.start(this)
    }

    private fun onPerm(p: Perm) {
        val runtime = Perms.runtime(p)
        if (runtime.isNotEmpty()) {
            askedBefore = true
            permLauncher.launch(runtime)
        } else {
            val i = Perms.settingsIntent(this, p) ?: Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            runCatching { startActivity(i) }.onFailure { Toast.makeText(this, "Abra as configurações do sistema", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun applyFullscreen(on: Boolean) {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Foto do Uno reduzida (lado maior ≤ 1600 px) e girada conforme o EXIF. */
    private fun loadPhoto(): ImageBitmap? {
        if (!photoFile.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val bmp = BitmapFactory.decodeFile(photoFile.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = when (runCatching { ExifInterface(photoFile.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val out = if (degrees == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        return out.asImageBitmap()
    }

    private fun loadApps(icons: Boolean = true): List<AppEntry> {
        val pm = packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName, if (icons) runCatching { it.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull() else null) }
    }

    private fun appLabel(pkg: String): String? =
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()

    private fun deviceInfo(): List<Pair<String, String>> {
        val dm = resources.displayMetrics
        val res = if (Build.VERSION.SDK_INT >= 30) {
            windowManager.currentWindowMetrics.bounds.let { "${it.width()}×${it.height()}" }
        } else {
            @Suppress("DEPRECATION")
            DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }.let { "${it.widthPixels}×${it.heightPixels}" }
        }
        return listOf(
            "Resolução" to res,
            "Densidade" to "${dm.densityDpi} dpi (${dm.density}x)",
            "Android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Aparelho" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Boot" to "#${Startup.bootId(this)}",
            "Tela inicial padrão" to if (Perms.isDefaultHome(this)) "sim" else "não",
            "Versão do app" to BuildConfig.VERSION_NAME,
        )
    }

    @Composable
    private fun App() {
        val settings by Graph.settings.flow.collectAsStateWithLifecycle()
        val car by CarRepo.state.collectAsStateWithLifecycle()
        val music by MediaRepo.flow.collectAsStateWithLifecycle()
        val weather by Graph.weather.flow.collectAsStateWithLifecycle()
        val resumeTick by resumed.collectAsStateWithLifecycle()
        val home by homePressed.collectAsStateWithLifecycle()
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        var photoVersion by remember { mutableIntStateOf(0) }
        var pickSlot by rememberSaveable { mutableIntStateOf(0) }
        val settingsList = rememberLazyListState()
        var busy by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(home) { if (home > 0) screen = Screen.HOME }
        LaunchedEffect(settings.fullscreen) { applyFullscreen(settings.fullscreen) }
        LaunchedEffect(settings.autoBrightness, settings.dayBrightness, settings.nightBrightness, settings.lat) { Brightness.apply(this@MainActivity) }
        LaunchedEffect(settings.demo, settings.startObd) { if (settings.demo || settings.startObd) ObdService.start(this@MainActivity) }
        BackHandler(screen != Screen.HOME) { screen = screen.parent }

        val now by produceState(ZonedDateTime.now()) { while (true) { value = ZonedDateTime.now(); delay(1_000) } }
        var photo by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(photoVersion) { photo = withContext(Dispatchers.IO) { loadPhoto() } }
        val granted = remember(resumeTick) { Perms.granted(this) }
        var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
        LaunchedEffect(resumeTick) { apps = withContext(Dispatchers.IO) { loadApps() } }
        val shortcuts = remember(settings.shortcuts, apps) { settings.shortcuts.map { resolveShortcut(it, apps) } }
        val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { contentResolver.openInputStream(uri)?.use { inp -> photoFile.outputStream().use { inp.copyTo(it) } } }
                    }
                    photoVersion++
                }
            }
        }

        UnoTheme(settings.accent) {
            Box(Modifier.fillMaxSize().pianoBackground()) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    ScaledRoot {
                        Column(Modifier.fillMaxSize()) {
                            AlertBanner(car.alerts)
                            Box(Modifier.weight(1f)) {
                                when (screen) {
                                    Screen.HOME -> HomeScreen(
                                        HomeUi(
                                            clock = Fmt.clock(now),
                                            date = Fmt.date(now),
                                            weather = weather?.let { "${it.tempC.roundToInt()}° ${Metno.emoji(it.symbol)}".trim() },
                                            coolant = Fmt.temp(car.sample.coolantC),
                                            battery = Fmt.volts(car.sample.batteryV),
                                            obdHint = when (car.link) {
                                                Link.CONNECTED, Link.DEMO -> null
                                                Link.IGNITION_OFF -> "Ligue a ignição"
                                                else -> "OBD2 desconectado"
                                            },
                                            music = music,
                                            musicAccess = Perm.MUSICA in granted,
                                            photo = photo,
                                            lightsOn = car.engineRunning,
                                            shortcuts = shortcuts,
                                        ),
                                        onShortcut = { app ->
                                            val pkg = shortcutPackage(app) { loadApps(icons = false) }
                                            if (pkg.isBlank()) Toast.makeText(this@MainActivity, "${app.label} não está instalado", Toast.LENGTH_SHORT).show()
                                            else Startup.launch(this@MainActivity, pkg, app.label)
                                        },
                                        onObd = { screen = Screen.OBD },
                                        onSettings = { screen = Screen.SETTINGS },
                                        onPlayPause = MediaRepo::playPause,
                                        onNext = MediaRepo::next,
                                        onPrev = MediaRepo::prev,
                                        onGrantMusic = { onPerm(Perm.MUSICA) },
                                    )
                                    Screen.OBD -> ObdScreen(
                                        car, settings, photo,
                                        onBack = { screen = Screen.HOME },
                                        onDtcs = { screen = Screen.DTC },
                                        onTrips = { screen = Screen.TRIPS },
                                        onConnect = {
                                            if (!hasBluetooth(this@MainActivity)) onPerm(Perm.BLUETOOTH) else ObdService.start(this@MainActivity)
                                        },
                                    )
                                    Screen.DTC -> DtcScreen(
                                        car.dtcs, car.link, busy,
                                        onBack = { screen = Screen.OBD },
                                        onRead = {
                                            scope.launch {
                                                busy = true
                                                withContext(Dispatchers.IO) { CarRepo.readDtcs() }
                                                busy = false
                                            }
                                        },
                                        onClear = {
                                            scope.launch {
                                                busy = true
                                                val ok = withContext(Dispatchers.IO) { CarRepo.clearDtcs() }
                                                busy = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    if (ok) "Códigos apagados" else "Não foi possível apagar",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        },
                                    )
                                    Screen.TRIPS -> {
                                        var trips by remember { mutableStateOf(Graph.trips.trips()) }
                                        TripsScreen(
                                            car.trip, trips, settings,
                                            onBack = { screen = Screen.OBD },
                                            onNewTrip = { CarRepo.newTrip(); trips = Graph.trips.trips() },
                                        )
                                    }
                                    Screen.SETTINGS -> SettingsScreen(
                                        settings, granted,
                                        bonded = remember(resumeTick) { bondedDevices(this@MainActivity) },
                                        startAppLabel = settings.startOpenApp?.let(::appLabel),
                                        shortcutLabels = shortcuts.map { it.label },
                                        customPhoto = photo != null,
                                        listState = settingsList,
                                        onChange = Graph.settings::update,
                                        onPickPhoto = {
                                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        onDefaultPhoto = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) { photoFile.delete() }
                                                photoVersion++
                                            }
                                        },
                                        onPickShortcut = { pickSlot = it; screen = Screen.PICK_SHORTCUT },
                                        onPerm = ::onPerm,
                                        onPickStartApp = { screen = Screen.PICK_START_APP },
                                        onApps = { screen = Screen.APPS },
                                        onAbout = { screen = Screen.ABOUT },
                                        onBack = { screen = Screen.HOME },
                                    )
                                    Screen.APPS, Screen.PICK_START_APP, Screen.PICK_SHORTCUT -> {
                                        AppsScreen(
                                            when (screen) {
                                                Screen.PICK_START_APP -> "App que abre ao ligar"
                                                Screen.PICK_SHORTCUT -> "Atalho ${pickSlot + 1} da tela inicial"
                                                else -> "Apps"
                                            },
                                            apps,
                                            onPick = { pkg ->
                                                when (screen) {
                                                    Screen.PICK_START_APP -> {
                                                        Graph.settings.update { it.copy(startOpenApp = pkg) }
                                                        screen = Screen.SETTINGS
                                                    }
                                                    Screen.PICK_SHORTCUT -> {
                                                        val slot = pickSlot
                                                        Graph.settings.update { s -> s.copy(shortcuts = s.shortcuts.toMutableList().also { it[slot] = pkg }) }
                                                        screen = Screen.SETTINGS
                                                    }
                                                    else -> Startup.launch(this@MainActivity, pkg)
                                                }
                                            },
                                            onBack = { screen = Screen.SETTINGS },
                                        )
                                    }
                                    Screen.ABOUT -> AboutScreen(
                                        remember(resumeTick) { deviceInfo() },
                                        remember(resumeTick) { Graph.events.recent() },
                                        onBack = { screen = Screen.SETTINGS },
                                    )
                                }
                                if (settings.demo) DemoBadge(Modifier.align(Alignment.TopEnd).padding(8.su))
                            }
                        }
                    }
                }
            }
        }
    }
}

package io.github.dudubarbaro.unolauncher.car

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.dudubarbaro.unolauncher.Graph
import io.github.dudubarbaro.unolauncher.MainActivity
import io.github.dudubarbaro.unolauncher.R
import io.github.dudubarbaro.unolauncher.obd.CarEngine
import io.github.dudubarbaro.unolauncher.obd.DemoEngine
import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.obd.ObdEngine
import io.github.dudubarbaro.unolauncher.obd.Sample
import io.github.dudubarbaro.unolauncher.obd.bluetoothOn
import io.github.dudubarbaro.unolauncher.obd.bondedDevices
import io.github.dudubarbaro.unolauncher.obd.btSession
import io.github.dudubarbaro.unolauncher.obd.hasBluetooth
import io.github.dudubarbaro.unolauncher.obd.pickAdapter
import io.github.dudubarbaro.unolauncher.trip.TripState
import io.github.dudubarbaro.unolauncher.trip.advance
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Mantém o OBD2 (ou o demo) rodando e somando a viagem mesmo com outro app na tela. */
class ObdService : Service() {
    // Última barreira: um erro inesperado no OBD2 vira aviso na tela, nunca derruba a tela inicial.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            Graph.events.add("ERRO OBD2: ${e.javaClass.simpleName}", System.currentTimeMillis())
            CarRepo.engine = null
            CarRepo.set { it.copy(link = Link.OFF, problem = "Erro no OBD2, toque em Conectar") }
        },
    )
    private var collector: Job? = null
    private val retry = MutableStateFlow(0)
    private val lock = Any()
    private var trip = TripState()
    /** Nulo até a primeira viagem ser carregada: antes disso não há nada para salvar. */
    private var demo: Boolean? = null
    private var lastMs = 0L
    private var lastSaveMs = 0L
    private var savedKm = 0.0
    private var lastAlerts = emptySet<Alert>()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_OBD, "OBD2", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alertas do carro", NotificationManager.IMPORTANCE_HIGH))
        goForeground()
        CarRepo.onNewTrip = ::newTrip
        startCollector()
    }

    private fun startCollector() {
        collector = scope.launch {
            Graph.settings.flow.map { it.demo to it.adapterAddress }.distinctUntilChanged()
                .combine(retry) { cfg, _ -> cfg }
                .collectLatest { (d, addr) -> runEngine(d, addr) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        if (collector?.isActive != true) startCollector()
        else if (CarRepo.engine == null) retry.value++ // botão "Conectar" ou volta para o app
        return START_STICKY
    }

    override fun onDestroy() {
        synchronized(lock) { if (demo == false) persist() }
        scope.cancel()
        CarRepo.onNewTrip = null
        CarRepo.engine = null
        CarRepo.set { it.copy(link = Link.OFF) }
        super.onDestroy()
    }

    private fun goForeground() {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CH_OBD)
            .setSmallIcon(R.drawable.ic_stat_gauge).setContentTitle("Uno Launcher · OBD2")
            .setContentIntent(open).setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
    }

    private suspend fun runEngine(demoMode: Boolean, saved: String?) {
        synchronized(lock) {
            if (demo == false) persist()
            demo = demoMode
            trip = if (demoMode) TripState() else Graph.trips.loadCurrent() ?: TripState()
            savedKm = trip.km
            lastMs = 0
        }
        CarRepo.set { CarState(trip = trip) }
        val engine: CarEngine = if (demoMode) DemoEngine() else {
            val problem = when {
                !hasBluetooth(this) -> "Libere o Bluetooth em Configurações"
                !bluetoothOn(this) -> "Bluetooth desligado"
                else -> null
            }
            val address = if (problem == null) pickAdapter(bondedDevices(this), saved) else null
            if (address == null) {
                CarRepo.engine = null
                CarRepo.set { it.copy(link = Link.OFF, problem = problem ?: "Pareie o adaptador OBDII nas configurações de Bluetooth") }
                return
            }
            ObdEngine { btSession(this, address) }
        }
        CarRepo.engine = engine
        try {
            engine.run(::onLink, ::onSample)
        } finally {
            CarRepo.engine = null
        }
    }

    private fun onLink(l: Link) {
        val live = l == Link.CONNECTED || l == Link.DEMO
        // Sem conexão, os números somem ("—") em vez de ficarem velhos na tela.
        CarRepo.set { if (live) it.copy(link = l, problem = null) else it.copy(link = l, problem = null, sample = Sample(), alerts = emptySet()) }
        synchronized(lock) { lastMs = 0 }
        if (live) scope.launch { CarRepo.readDtcs() }
    }

    private fun onSample(s: Sample) {
        val now = SystemClock.elapsedRealtime()
        val cfg = Graph.settings.value
        val (t, al) = synchronized(lock) {
            val dt = if (lastMs == 0L) 0.0 else (now - lastMs) / 1000.0
            lastMs = now
            trip = advance(trip, s, dt, System.currentTimeMillis(), cfg.fuel, cfg.fuelMultiplier)
            if (demo == false && now - lastSaveMs >= SAVE_EVERY_MS) { persist(); lastSaveMs = now }
            trip to alerts(s, cfg)
        }
        CarRepo.set { it.copy(sample = s, trip = t, alerts = al) }
        notifyAlerts(al)
    }

    /** Salva a viagem atual e soma no hodômetro os km desde o último salvamento. Chamar com [lock]. */
    private fun persist() {
        Graph.trips.saveCurrent(trip)
        val dKm = trip.km - savedKm
        if (dKm > 0) Graph.settings.update { it.copy(odometerKm = it.odometerKm + dKm) }
        savedKm = trip.km
    }

    private fun newTrip() {
        val cfg = Graph.settings.value
        synchronized(lock) {
            if (demo == false) {
                persist()
                Graph.trips.finish(trip, System.currentTimeMillis(), cfg.fuel, cfg.pricePerL)
            }
            trip = TripState()
            savedKm = 0.0
        }
        CarRepo.set { it.copy(trip = TripState()) }
    }

    private fun notifyAlerts(al: Set<Alert>) {
        val urgent = al.filter { it.urgent }.toSet()
        val fresh = urgent - lastAlerts
        val gone = lastAlerts - urgent
        lastAlerts = urgent
        if (fresh.isEmpty() && gone.isEmpty()) return
        val nm = NotificationManagerCompat.from(this)
        gone.forEach { nm.cancel(100 + it.ordinal) }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        for (a in fresh) {
            nm.notify(
                100 + a.ordinal,
                NotificationCompat.Builder(this, CH_ALERT).setSmallIcon(R.drawable.ic_stat_gauge)
                    .setContentTitle(a.text).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build(),
            )
        }
    }

    companion object {
        private const val CH_OBD = "obd"
        private const val CH_ALERT = "alertas"
        private const val SAVE_EVERY_MS = 10_000L

        /**
         * Sobe mesmo sem permissão de Bluetooth (o modo demo funciona e o painel explica o que falta).
         * No Android 14+ o tipo connectedDevice fica liberado pela CHANGE_NETWORK_STATE declarada no manifesto.
         */
        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, ObdService::class.java))
        }
    }
}

package io.github.dudubarbaro.unolauncher.car

import io.github.dudubarbaro.unolauncher.obd.Sample
import io.github.dudubarbaro.unolauncher.settings.Settings

/** [urgent] = também vira notificação quando o app não está na tela. */
enum class Alert(val text: String, val urgent: Boolean) {
    MOTOR_QUENTE("Motor quente", true),
    BATERIA_BAIXA("Bateria baixa", true),
    BATERIA_ALTA("Tensão alta na bateria", true),
    REVISAO("Revisão chegando", false),
}

fun alerts(s: Sample, cfg: Settings): Set<Alert> = buildSet {
    if ((s.coolantC ?: Int.MIN_VALUE) > cfg.alertCoolantC) add(Alert.MOTOR_QUENTE)
    val v = s.batteryV
    if (v != null && v < cfg.alertBatteryLowV && (s.rpm ?: 0) > 400) add(Alert.BATERIA_BAIXA)
    if (v != null && v > cfg.alertBatteryHighV) add(Alert.BATERIA_ALTA)
    if (cfg.nextServiceKm > 0 && cfg.odometerKm >= cfg.nextServiceKm - cfg.alertServiceBeforeKm) add(Alert.REVISAO)
}

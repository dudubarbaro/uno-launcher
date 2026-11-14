package io.github.dudubarbaro.unolauncher.obd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.IOException
import java.util.UUID

private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/** Endereço salvo se ainda estiver pareado; senão o "OBDII" (ou outro com "OBD" no nome). */
fun pickAdapter(bonded: List<Pair<String, String>>, saved: String?): String? =
    saved?.takeIf { s -> bonded.any { it.second == s } }
        ?: bonded.firstOrNull { it.first == "OBDII" }?.second
        ?: bonded.firstOrNull { it.first.contains("OBD", ignoreCase = true) }?.second

fun hasBluetooth(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

fun bluetoothOn(ctx: Context): Boolean =
    ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true

/** Aparelhos pareados como (nome, endereço); vazio sem permissão ou com o Bluetooth desligado. */
@SuppressLint("MissingPermission") // checado por hasBluetooth
fun bondedDevices(ctx: Context): List<Pair<String, String>> {
    if (!hasBluetooth(ctx) || !bluetoothOn(ctx)) return emptyList()
    val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
    return adapter.bondedDevices.orEmpty().map { (it.name ?: it.address) to it.address }
}

/** Abre RFCOMM SPP; clones às vezes só aceitam a conexão "insegura". */
@SuppressLint("MissingPermission") // checado por hasBluetooth
fun btSession(ctx: Context, address: String): ElmSession {
    if (!hasBluetooth(ctx)) throw IOException("sem permissão de Bluetooth")
    val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: throw IOException("sem Bluetooth")
    // Sem cancelDiscovery(): no Android 12+ ele exige BLUETOOTH_SCAN, e o app nunca inicia uma busca.
    val socket = try {
        val device = adapter.getRemoteDevice(address)
        try {
            device.createRfcommSocketToServiceRecord(SPP).also { it.connect() }
        } catch (e: IOException) {
            device.createInsecureRfcommSocketToServiceRecord(SPP).also { it.connect() }
        }
    } catch (e: SecurityException) {
        throw IOException("sem permissão de Bluetooth", e)
    }
    return ElmSession(socket.inputStream, socket.outputStream) { socket.close() }
}

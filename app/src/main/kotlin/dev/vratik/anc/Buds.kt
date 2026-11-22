package dev.vratik.anc

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import dev.vratik.anc.proto.AncMode
import dev.vratik.anc.proto.BatteryState
import dev.vratik.anc.proto.Cmd
import dev.vratik.anc.proto.CommandBuilder
import dev.vratik.anc.proto.Frame
import dev.vratik.anc.proto.FrameDecoder
import dev.vratik.anc.proto.ListeningMode
import dev.vratik.anc.proto.WearState
import dev.vratik.anc.proto.modelFromSerial
import dev.vratik.anc.proto.parseAncMode
import dev.vratik.anc.proto.parseBattery
import dev.vratik.anc.proto.parseSerial
import dev.vratik.anc.proto.parseString
import dev.vratik.anc.proto.parseWear
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class BudsUi(
    val status: String = "Not connected",
    val connected: Boolean = false,
    val deviceName: String? = null,
    val model: String? = null,
    val firmware: String? = null,
    val battery: BatteryState = BatteryState(),
    val wear: WearState? = null,
    val anc: AncMode? = null,
    val listening: ListeningMode? = null,
)

/**
 * Single connection to the buds over classic-BT RFCOMM.
 * Only one app can hold the socket — Nothing X must be closed.
 */
@SuppressLint("MissingPermission") // checked in MainActivity before the service starts
object Buds {
    private val _ui = MutableStateFlow(BudsUi())
    val ui: StateFlow<BudsUi> = _ui

    private var socket: BluetoothSocket? = null
    private val cmd = CommandBuilder()
    private val decoder = FrameDecoder()
    @Volatile private var running = false

    // 16-bit Bluetooth SIG UUIDs (A2DP, HFP, AVRCP…) all end with this — skip them.
    private const val SIG_BASE = "-0000-1000-8000-00805f9b34fb"
    private val PROBE_CHANNELS = listOf(15, 17, 16, 18, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

    fun start(ctx: Context) {
        if (running) return
        running = true
        thread(name = "buds-connect") { runCatching { connect(ctx) }.onFailure { fail("connect: $it") } }
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
        socket = null
        _ui.update { it.copy(connected = false, status = "Disconnected") }
    }

    fun setAnc(mode: AncMode) = send(cmd.setAnc(mode))
    fun setListening(mode: ListeningMode) = send(cmd.setListeningMode(mode))
    fun refresh() { send(cmd.getBattery()); send(cmd.getAnc()) }

    private fun connect(ctx: Context) {
        val adapter = ctx.getSystemService(BluetoothManager::class.java).adapter
        val device = adapter?.bondedDevices?.firstOrNull { it.name?.contains("CMF", ignoreCase = true) == true }
            ?: return fail("No paired CMF buds found. Pair them in Android Bluetooth settings first.")

        _ui.update { it.copy(status = "Connecting to ${device.name}…", deviceName = device.name) }
        AppLog.d("device ${device.name}, uuids=${device.uuids?.joinToString()}")

        val (s, first) = open(device) ?: return fail("Buds didn't answer. Close Nothing X and try again.")
        socket = s
        _ui.update { it.copy(connected = true, status = "Connected") }
        handleBytes(first)
        readLoop(s)
    }

    /** Try vendor UUIDs from SDP first, then raw channel numbers. First one that answers 0x55 wins. */
    private fun open(device: BluetoothDevice): Pair<BluetoothSocket, ByteArray>? {
        val vendorUuids = device.uuids.orEmpty().map { it.uuid }.filterNot { it.toString().endsWith(SIG_BASE) }
        for (uuid in vendorUuids) {
            probe("uuid $uuid") { device.createRfcommSocketToServiceRecord(uuid) }?.let { return it }
        }
        val byChannel = runCatching {
            device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        }.getOrNull() ?: return null
        for (ch in PROBE_CHANNELS) {
            if (!running) return null
            probe("channel $ch") { byChannel.invoke(device, ch) as BluetoothSocket }?.let { return it }
        }
        return null
    }

    private fun probe(label: String, make: () -> BluetoothSocket): Pair<BluetoothSocket, ByteArray>? {
        val s = runCatching(make).getOrNull() ?: return null
        return try {
            s.connect()
            s.outputStream.write(cmd.handshake())
            var got = ByteArray(0)
            val reader = thread {
                runCatching {
                    val buf = ByteArray(256)
                    val n = s.inputStream.read(buf)
                    if (n > 0) got = buf.copyOf(n)
                }
            }
            reader.join(2000)
            if (got.isNotEmpty() && (got[0].toInt() and 0xFF) == 0x55) {
                AppLog.d("connected via $label")
                s to got
            } else {
                AppLog.d("$label: no 0x55 reply")
                s.close(); null
            }
        } catch (e: Exception) {
            AppLog.d("$label: ${e.message}")
            runCatching { s.close() }
            null
        }
    }

    private fun readLoop(s: BluetoothSocket) {
        val buf = ByteArray(1024)
        try {
            while (running) {
                val n = s.inputStream.read(buf)
                if (n < 0) break
                handleBytes(buf.copyOf(n))
            }
        } catch (e: Exception) {
            AppLog.d("read ended: ${e.message}")
        }
        running = false
        socket = null
        _ui.update { it.copy(connected = false, status = "Disconnected") }
    }

    private fun handleBytes(bytes: ByteArray) {
        decoder.feed(bytes).forEach { f ->
            if (!f.crcOk) AppLog.d("bad CRC on ${"%04x".format(f.cmd)}") else handle(f)
        }
    }

    private fun handle(f: Frame) {
        val p = f.payload
        when (f.cmd) {
            Cmd.GET_PROTO_VERSION -> {
                AppLog.d("protocol ${parseString(p)} → activating")
                send(cmd.activate())
                listOf(cmd.getSerial(), cmd.getFirmware(), cmd.getBattery(), cmd.getAnc(),
                    cmd.getListeningMode(), cmd.getEarphone()).forEach(::send)
            }
            Cmd.GET_BATTERY, Cmd.EVT_BATTERY -> _ui.update { it.copy(battery = parseBattery(p, it.battery)) }
            Cmd.GET_ANC, Cmd.EVT_ANC -> {
                val mode = parseAncMode(p)
                // Unknown values get logged — this is how we'll learn Adaptive's wire value.
                if (mode == null) AppLog.d("unknown ANC payload: ${p.hex()}")
                _ui.update { it.copy(anc = mode ?: it.anc) }
            }
            Cmd.GET_FIRMWARE -> _ui.update { it.copy(firmware = parseString(p)) }
            Cmd.GET_REMOTE_CONF -> {
                val serial = parseSerial(p)
                _ui.update { it.copy(model = modelFromSerial(serial) ?: "unknown") }
                AppLog.d("model ${modelFromSerial(serial)}")
            }
            Cmd.GET_LISTENING_MODE -> _ui.update { it.copy(listening = p.firstOrNull()?.let { b -> ListeningMode.fromWire(b.toInt()) }) }
            Cmd.EVT_STATUS -> send(cmd.getEarphone())
            Cmd.GET_EARPHONE -> _ui.update { it.copy(wear = parseWear(p)) }
            else -> AppLog.d("rx ${"%04x".format(f.cmd)}: ${p.hex()}")
        }
    }

    @Synchronized
    private fun send(bytes: ByteArray) {
        val s = socket ?: return AppLog.d("send skipped: not connected")
        runCatching { s.outputStream.write(bytes) }.onFailure { AppLog.d("send failed: ${it.message}") }
    }

    private fun fail(msg: String) {
        AppLog.d(msg)
        running = false
        _ui.update { it.copy(connected = false, status = msg) }
    }
}


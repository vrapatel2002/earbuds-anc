package dev.vratik.anc.proto

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Command IDs. 0xC0xx = GET, 0xF0xx = SET, 0xE0xx = device events. */
object Cmd {
    const val GET_PROTO_VERSION = 0xC001 // handshake; device replies, we then send SET_ACTIVATED
    const val GET_REMOTE_CONF = 0xC006   // serial number lives in here
    const val GET_BATTERY = 0xC007
    const val GET_EARPHONE = 0xC00A      // in-case / in-ear status
    const val GET_ANC = 0xC01E           // payload [0x03]
    const val GET_FIRMWARE = 0xC042
    const val GET_LISTENING_MODE = 0xC050 // CMF EQ preset
    const val GET_CUSTOM_EQ = 0xC044

    const val SET_ACTIVATED = 0xF001
    const val SET_ANC = 0xF00F
    const val SET_LISTENING_MODE = 0xF01D
    const val SET_CUSTOM_EQ = 0xF041

    const val EVT_BATTERY = 0xE001
    const val EVT_STATUS = 0xE002
    const val EVT_ANC = 0xE003
}

/**
 * ANC wire values (payload byte 1 of SET_ANC / type=1 entries of GET_ANC).
 * ADAPTIVE is NOT confirmed yet — needs an HCI snoop of Nothing X switching to
 * Adaptive. Do not send it until RESEARCH.md has a verified value.
 */
enum class AncMode(val wire: Int?) {
    HIGH(1), MID(2), LOW(3), OFF(5), TRANSPARENCY(7), ADAPTIVE(null);

    companion object {
        fun fromWire(v: Int): AncMode? = entries.firstOrNull { it.wire == v }
    }
}

/** CMF Buds Pro 2 (B172) "listening modes" = its EQ presets. */
enum class ListeningMode(val wire: Int) {
    BALANCED(0), ROCK(1), ELECTRONIC(2), POP(3), ENHANCE_VOCALS(4), CLASSICAL(5), CUSTOM(6);

    companion object {
        fun fromWire(v: Int): ListeningMode? = entries.firstOrNull { it.wire == v }
    }
}

/** Builds request frames. Owns the frame sequence number. */
class CommandBuilder {
    private var fsn = 0

    private fun next(cmd: Int, payload: ByteArray = ByteArray(0)): ByteArray {
        fsn = (fsn + 1) and 0xFF
        return encodeFrame(cmd, payload, fsn)
    }

    fun handshake() = next(Cmd.GET_PROTO_VERSION)
    fun activate() = next(Cmd.SET_ACTIVATED)
    fun getBattery() = next(Cmd.GET_BATTERY)
    fun getEarphone() = next(Cmd.GET_EARPHONE)
    fun getAnc() = next(Cmd.GET_ANC, byteArrayOf(0x03))
    fun getFirmware() = next(Cmd.GET_FIRMWARE)
    fun getSerial() = next(Cmd.GET_REMOTE_CONF)
    fun getListeningMode() = next(Cmd.GET_LISTENING_MODE)
    fun getCustomEq() = next(Cmd.GET_CUSTOM_EQ)

    fun setAnc(mode: AncMode): ByteArray {
        val wire = requireNotNull(mode.wire) { "$mode wire value not verified yet" }
        return next(Cmd.SET_ANC, byteArrayOf(0x01, wire.toByte(), 0x00))
    }

    fun setListeningMode(mode: ListeningMode) =
        next(Cmd.SET_LISTENING_MODE, byteArrayOf(mode.wire.toByte(), 0x00))

    fun setCustomEq(bassDb: Int, midDb: Int, trebleDb: Int) =
        next(Cmd.SET_CUSTOM_EQ, CustomEq.encode(bassDb, midDb, trebleDb))
}

/**
 * B172 "Custom" listening mode: 3-band parametric curve. Only gains are editable
 * (-6..+6 dB); freqs/Q are fixed and copied from the template.
 * Wire: [count][preamp f32][id][gain f32][freq f32][q f32] x3, little-endian floats.
 * Gains sit at offsets 6, 19, 32 in device band order: mid, treble, bass.
 */
object CustomEq {
    const val MIN_DB = -6
    const val MAX_DB = 6
    private val GAIN_OFFSETS = intArrayOf(6, 19, 32) // mid, treble, bass
    private val TEMPLATE = hex(
        "0300000000010000000000007544c3f5283f020000000000c05a450000803f" +
            "000000000000000c43cdcc4c3f000000000000000000"
    )

    fun encode(bassDb: Int, midDb: Int, trebleDb: Int): ByteArray {
        val gains = listOf(midDb, trebleDb, bassDb).map { it.coerceIn(MIN_DB, MAX_DB) }
        val out = TEMPLATE.copyOf()
        val preamp = -gains.max()
        // The official app sends -0.0 when the preamp would be >= 0.
        putF32(out, 1, if (preamp >= 0) -0.0f else preamp.toFloat())
        GAIN_OFFSETS.forEachIndexed { i, off -> putF32(out, off, gains[i].toFloat()) }
        return out
    }

    /** Returns (bass, mid, treble) in dB, or null if payload too short. */
    fun decode(payload: ByteArray): Triple<Int, Int, Int>? {
        if (payload.size < 36) return null
        val (mid, treble, bass) = GAIN_OFFSETS.map { Math.round(getF32(payload, it)) }
        return Triple(bass, mid, treble)
    }

    private fun putF32(b: ByteArray, at: Int, v: Float) =
        ByteBuffer.wrap(b, at, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v)

    private fun getF32(b: ByteArray, at: Int) =
        ByteBuffer.wrap(b, at, 4).order(ByteOrder.LITTLE_ENDIAN).float
}

internal fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

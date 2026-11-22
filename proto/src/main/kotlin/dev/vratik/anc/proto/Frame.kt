package dev.vratik.anc.proto

/**
 * Nothing / CMF "0x55" RFCOMM frame.
 *
 * Layout (both directions, multi-byte fields little-endian):
 *   [0x55][ctrl:2][cmd:2][len:2][fsn:1][payload:len][crc:2 if ctrl & 0x20]
 *
 * Host always sends ctrl=0x0160 with CRC. The device drops SETs in a session
 * where any non-CRC frame was sent, so never send without CRC.
 * Device responses come back with bit 15 of cmd cleared; we OR it back in so a
 * response has the same ID as its request.
 *
 * Reference: something-x protocol.py (MIT), github.com/SoaOaoS/something-x
 */
data class Frame(val cmd: Int, val payload: ByteArray, val crcOk: Boolean = true) {
    override fun equals(other: Any?) =
        other is Frame && cmd == other.cmd && payload.contentEquals(other.payload) && crcOk == other.crcOk

    override fun hashCode() = 31 * cmd + payload.contentHashCode()
}

const val SOF = 0x55
const val CTRL_HOST_CRC = 0x0160
private const val HEADER_SIZE = 8

/** CRC16-ARC: init 0xFFFF, reflected poly 0xA001. Covers header + payload. */
fun crc16(data: ByteArray, len: Int = data.size): Int {
    var crc = 0xFFFF
    for (i in 0 until len) {
        crc = crc xor (data[i].toInt() and 0xFF)
        repeat(8) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
        }
    }
    return crc and 0xFFFF
}

fun encodeFrame(cmd: Int, payload: ByteArray, fsn: Int): ByteArray {
    val out = ByteArray(HEADER_SIZE + payload.size + 2)
    out[0] = SOF.toByte()
    putU16(out, 1, CTRL_HOST_CRC)
    putU16(out, 3, cmd)
    putU16(out, 5, payload.size)
    out[7] = fsn.toByte()
    payload.copyInto(out, HEADER_SIZE)
    putU16(out, HEADER_SIZE + payload.size, crc16(out, HEADER_SIZE + payload.size))
    return out
}

/**
 * Streaming decoder: feed raw socket bytes, get complete frames back.
 * Junk bytes before a 0x55 are skipped (legacy 0x03 stream is ignored for now).
 */
class FrameDecoder {
    private var buf = ByteArray(0)

    fun feed(chunk: ByteArray): List<Frame> {
        buf += chunk
        val frames = mutableListOf<Frame>()
        while (true) {
            val start = buf.indexOfFirst { (it.toInt() and 0xFF) == SOF }
            if (start < 0) { buf = ByteArray(0); break }
            if (start > 0) buf = buf.copyOfRange(start, buf.size)
            if (buf.size < HEADER_SIZE) break

            val ctrl = getU16(buf, 1)
            val cmd = getU16(buf, 3)
            val len = getU16(buf, 5)
            val crcSize = if (ctrl and 0x20 != 0) 2 else 0
            val total = HEADER_SIZE + len + crcSize
            if (buf.size < total) break

            val crcOk = crcSize == 0 || getU16(buf, HEADER_SIZE + len) == crc16(buf, HEADER_SIZE + len)
            frames += Frame(cmd or 0x8000, buf.copyOfRange(HEADER_SIZE, HEADER_SIZE + len), crcOk)
            buf = buf.copyOfRange(total, buf.size)
        }
        return frames
    }
}

internal fun putU16(b: ByteArray, at: Int, v: Int) {
    b[at] = (v and 0xFF).toByte()
    b[at + 1] = ((v ushr 8) and 0xFF).toByte()
}

internal fun getU16(b: ByteArray, at: Int) =
    (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)

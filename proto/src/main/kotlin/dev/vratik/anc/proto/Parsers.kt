package dev.vratik.anc.proto

data class Battery(val percent: Int, val charging: Boolean)

data class BatteryState(val left: Battery? = null, val right: Battery? = null, val case: Battery? = null)

data class WearState(val leftInEar: Boolean, val rightInEar: Boolean)

/** GET_BATTERY / EVT_BATTERY: [count][type][val]...  type 2=L 3=R 4=case; val bit7=charging. */
fun parseBattery(p: ByteArray, prev: BatteryState = BatteryState()): BatteryState {
    var s = prev
    pairs(p).forEach { (type, v) ->
        val b = Battery(v and 0x7F, v and 0x80 != 0)
        s = when (type) {
            2 -> s.copy(left = b)
            3 -> s.copy(right = b)
            4 -> s.copy(case = b)
            else -> s
        }
    }
    return s
}

/** GET_EARPHONE: [count][type][val]...  type 2=L 3=R; val bit2=inEar, bit0=inCase. */
fun parseWear(p: ByteArray): WearState {
    var l = false
    var r = false
    pairs(p).forEach { (type, v) ->
        if (type == 2) l = v and 0x04 != 0
        if (type == 3) r = v and 0x04 != 0
    }
    return WearState(l, r)
}

/**
 * GET_ANC / EVT_ANC: [type][value][pad] triplets.
 * type=1 → current mode. Returns null if no mode entry or unknown value
 * (log unknown values — that's how we'll find Adaptive's wire value).
 */
fun parseAncMode(p: ByteArray): AncMode? {
    var i = 0
    while (i + 1 < p.size) {
        if (p[i].toInt() == 1) return AncMode.fromWire(p[i + 1].toInt() and 0xFF)
        i += 3
    }
    return null
}

/** GET_REMOTE_CONF: lines "deviceId,fieldId,value"; field 4 = serial. */
fun parseSerial(p: ByteArray): String? =
    String(p, Charsets.UTF_8).trim('\u0000').lines()
        .map { it.split(",", limit = 3) }
        .firstOrNull { it.size == 3 && it[1] == "4" && it[2].isNotBlank() }
        ?.get(2)?.trim()

/** Serial → model base. Serial "13xx76..." / "SHxx76..." → B172 (CMF Buds Pro 2). */
fun modelFromSerial(serial: String?): String? {
    if (serial == null || serial.length < 6) return null
    if (serial.take(2) !in setOf("13", "SH")) return null
    return when (serial.substring(4, 6)) {
        in setOf("76", "77", "78", "79", "80", "81", "82", "83") -> "B172"
        in setOf("54", "55", "56", "57", "58", "59") -> "B168"
        else -> null
    }
}

fun parseString(p: ByteArray) = String(p, Charsets.UTF_8).trim('\u0000').trim()

private fun pairs(p: ByteArray): List<Pair<Int, Int>> {
    if (p.isEmpty()) return emptyList()
    val count = p[0].toInt() and 0xFF
    return (0 until count).mapNotNull { k ->
        val i = 1 + k * 2
        if (i + 1 < p.size) (p[i].toInt() and 0xFF) to (p[i + 1].toInt() and 0xFF) else null
    }
}

package dev.vratik.anc.proto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Expected bytes generated from something-x's Python implementation (MIT),
// which is tested against a real CMF Buds Pro 2 (B172).
class ProtoTest {
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    @Test fun crcEmptyIsInit() = assertEquals(0xFFFF, crc16(ByteArray(0)))

    @Test fun frameSequenceMatchesReference() {
        val c = CommandBuilder()
        assertEquals("55600101c000000124df", c.handshake().hex())
        assertEquals("5560010ff0030002010100f993", c.setAnc(AncMode.HIGH).hex())
        assertEquals("5560010ff0030003010700fbcf", c.setAnc(AncMode.TRANSPARENCY).hex())
        assertEquals("5560011df00200040100eaf8", c.setListeningMode(ListeningMode.ROCK).hex())
        assertEquals(
            "55600141f035000503000040c001000000c000007544c3f5283f020000803f00c05a450000803f" +
                "000000404000000c43cdcc4c3f0000000000000000005337",
            c.setCustomEq(bassDb = 3, midDb = -2, trebleDb = 1).hex(),
        )
    }

    @Test fun adaptiveIsBlockedUntilVerified() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder().setAnc(AncMode.ADAPTIVE) }
    }

    @Test fun customEqAllNegative() = assertEquals(
        "030000008001000000c000007544c3f5283f02000000c000c05a450000803f00000000c000000c43cdcc4c3f000000000000000000",
        CustomEq.encode(-2, -2, -2).hex(),
    )

    @Test fun customEqRoundTrip() {
        for (v in -6..6) assertEquals(Triple(v, -v, 0), CustomEq.decode(CustomEq.encode(v, -v, 0)))
    }

    @Test fun customEqClamps() = assertEquals(Triple(6, -6, 0), CustomEq.decode(CustomEq.encode(9, -9, 0)))

    @Test fun decoderRoundTripsAndNormalizesResponseId() {
        // Device replies with bit 15 cleared; decoder ORs it back.
        val reply = encodeFrame(Cmd.GET_BATTERY and 0x7FFF, byteArrayOf(3, 2, 80, 3, 0xC8.toByte(), 4, 90), 9)
        val frames = FrameDecoder().feed(byteArrayOf(0x00, 0x03) + reply) // junk in front
        assertEquals(1, frames.size)
        assertEquals(Cmd.GET_BATTERY, frames[0].cmd)
        assertTrue(frames[0].crcOk)
        val b = parseBattery(frames[0].payload)
        assertEquals(Battery(80, false), b.left)
        assertEquals(Battery(72, true), b.right)
        assertEquals(Battery(90, false), b.case)
    }

    @Test fun decoderHandlesSplitChunks() {
        val f = encodeFrame(Cmd.EVT_ANC, byteArrayOf(1, 7, 0), 1)
        val d = FrameDecoder()
        assertEquals(0, d.feed(f.copyOfRange(0, 5)).size)
        val out = d.feed(f.copyOfRange(5, f.size))
        assertEquals(AncMode.TRANSPARENCY, parseAncMode(out.single().payload))
    }

    @Test fun badCrcFlagged() {
        val f = encodeFrame(Cmd.GET_BATTERY, byteArrayOf(1, 2, 50), 1)
        f[f.size - 1] = (f[f.size - 1] + 1).toByte()
        assertEquals(false, FrameDecoder().feed(f).single().crcOk)
    }

    @Test fun ancParse() {
        assertEquals(AncMode.HIGH, parseAncMode(byteArrayOf(1, 1, 0, 2, 1, 0)))
        assertEquals(AncMode.OFF, parseAncMode(byteArrayOf(1, 5, 0)))
        assertNull(parseAncMode(byteArrayOf(1, 0x42, 0))) // unknown → null, gets logged
    }

    @Test fun wearParse() = assertEquals(WearState(true, false), parseWear(byteArrayOf(2, 2, 4, 3, 1)))

    @Test fun serialAndModel() {
        val s = parseSerial("1,1,x\n1,4,SH10762543006451\n".toByteArray())
        assertEquals("SH10762543006451", s)
        assertEquals("B172", modelFromSerial(s))
        assertEquals("B172", modelFromSerial("1300760000000000"))
        assertNull(modelFromSerial("1300990000000000"))
        assertNull(modelFromSerial("QQ00760000000000"))
    }
}

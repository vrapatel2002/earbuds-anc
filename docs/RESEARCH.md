# RESEARCH.md — Device, Chip & Protocol Notes
Last Updated: 2026-11-21

## Device: CMF Buds Pro 2
- Model codes seen in the wild: **B172** (community-tested), **B187** (newer, untested). Confirm Vratik's.
- Hybrid ANC up to 50 dB, adaptive ANC that detects/compensates ear-canal leakage, 6 mics, 11 mm + 6 mm dual drivers, LDAC/AAC/SBC, BT 5.3, Google Fast Pair, dual-device pairing (no true multipoint).
- Nothing X ANC options: High / Mid / Low / Adaptive, plus Transparency.
- Reviews: ANC good for price; transparency hissy/lackluster.
- SoC not published — try to identify from firmware update files, FCC filing, or teardown.

## Protocol
- Classic Bluetooth **RFCOMM**, Nothing binary protocol (frames start `0x55`, commands + CRC). Same family across Nothing Ear (1/2/a/open) and CMF Buds.
- Only one app can hold the RFCOMM socket at a time → Nothing X must be closed/killed while our app is connected.
- No root needed on Android (unlike AirPods/LibrePods which needs an L2CAP workaround).

### Frame format
`[0x55][ctrl u16][cmd u16][len u16][fsn u8][payload][crc16 u16]` — little-endian. Host ctrl = `0x0160` (CRC on, always). CRC16-ARC (init 0xFFFF, poly 0xA001) over header+payload. Responses come back with cmd bit 15 cleared → OR `0x8000` to match the request. Session must start with `GET_PROTO_VERSION` → device replies → send `SET_ACTIVATED`, or SETs are silently dropped. RFCOMM channel: probe (something-x tries 15, 17, 16, …); accept only a channel that answers with `0x55`.

### Command table (source: something-x protocol.py, MIT — tested on B172)
| Function | Cmd | Payload | Response / notes | Verified on Vratik's unit |
|---|---|---|---|---|
| Handshake | `0xC001` GET | — | proto version string → then send `0xF001` | ❌ |
| Activate | `0xF001` SET | — | ACK; then query everything | ❌ |
| Battery | `0xC007` GET / `0xE001` EVT | — | `[count][type][val]…` type 2=L 3=R 4=case; val bit7=charging, bits0-6=% | ❌ |
| Wear status | `0xC00A` GET / `0xE002` EVT | — | `[count][type][val]…` bit2=in ear, bit0=in case. EVT is only a trigger → re-GET | ❌ |
| ANC get | `0xC01E` GET / `0xE003` EVT | `[03]` | `[type][val][pad]` triplets; type1=mode, type2=last level | ❌ |
| ANC set | `0xF00F` SET | `[01][mode][00]` | mode: 1=High 2=Mid 3=Low 5=Off 7=Transparency. **Adaptive = unknown (probably 4) — capture needed** | ❌ |
| Firmware | `0xC042` GET | — | UTF-8 string | ❌ |
| Serial | `0xC006` GET | — | lines `dev,field,value`; field 4=serial; SKU at serial[4:6] 76–83 → B172 | ❌ |
| Listening mode (EQ preset) | `0xC050` GET / `0xF01D` SET | `[mode][00]` | 0 Balanced 1 Rock 2 Electronic 3 Pop 4 Enhance Vocals 5 Classical 6 Custom | ❌ |
| Custom EQ | `0xC044` GET / `0xF041` SET | 53-byte template | gains f32 LE at offsets 6/19/32 (mid/treble/bass), −6..+6 dB; preamp at 1 = −max(gain), −0.0 if ≥0 | ❌ |
| Enhanced bass, spatial, in-ear toggle, low latency, gestures, find-my, fit test | ? | ? | Supported by BudsLink → get from its config or HCI snoop | ❌ |

### Unknowns to capture (Phase 1.3)
- Adaptive ANC wire value
- Fit test start/result
- Low latency, enhanced bass, spatial audio, in-ear detection toggle, gestures, find-my
- Any hidden params: transparency level, ANC fine strength, wind noise

## Reference projects
| Project | What it gives us | Notes |
|---|---|---|
| something-x (MIT) | **Source of our protocol module** — B172-specific listening modes + custom EQ, byte-level tests | |
| Gadgetbridge (Codeberg) — Nothing support | **Android/Java reference implementation**, CMF Buds Pro 2 "mostly supported" in nightly | GPL-3 → if we copy code, our app is GPL-3 |
| BudsLink (github.com/maniacx/BudsLink) | Feature map for B172/B187: battery, ANC level, adaptive, EQ preset, bass, spatial, in-ear, low latency, find my, gestures. Custom EQ not implemented | GNOME extension (JS) |
| EarA (flathub io.github.oneydef.eara) | Linux app incl. fit test | best-effort on CMF |
| ear (web) / earctl | Community web/CLI controllers | |
| Bharadwaj Raju blog — Nothing Ear (2) on Linux | How the protocol was captured (Android-x86 VM + HCI snoop) | method reference |
| LibrePods (github.com/kavishdevar/librepods) | **Feature/UX reference**: conversational awareness, head gestures, hearing aid, transparency customization, QS tile, popups | AirPods-only protocol; needs root/Xposed on most Android |

## Capture method
- Pixel 9: Developer options → Enable Bluetooth HCI snoop log → toggle BT → use Nothing X → `adb bugreport` → extract `btsnoop_hci.log` → Wireshark, filter `btrfcomm`.
- Linux: `btmon -w captures/x.btsnoop`.

## Conversation Mode notes
- Apple Conversation Awareness triggers on *your own* voice using bud mics/accelerometer. CMF buds don't expose that to apps.
- Our trigger: Pixel mic + VAD (Silero / WebRTC). Phone-in-pocket muffles audio — test early.
- Android: foreground service with `microphone` type + persistent notification required to listen in background; mic privacy indicator will show.

## Voice Focus notes
- Phone mic → denoise → playback into buds adds BT latency (~150–250 ms). Low latency mode might cut this. Must measure.

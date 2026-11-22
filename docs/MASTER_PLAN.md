# MASTER_PLAN.md — Earbuds ANC (Pixel 9 app for CMF Buds Pro 2)
Last Updated: 2026-11-21

Goal: an Android app on the Pixel 9 that fully replaces Nothing X for the CMF Buds Pro 2 and adds the "smart" layer Apple/Samsung have: conversation awareness, scene-based auto ANC, voice focus.
Phases are gated. Don't start a phase until the previous exit check passes.

---

## Phase 0 — Setup & Device ID
- [x] 0.1 Scaffold docs
- [x] 0.2 MemPalace wing `earbuds_anc`
- [x] 0.3 Device identified: CMF Buds Pro 2, Nothing X, RFCOMM protocol
- [ ] 0.4 Confirm model code (B172/B187) + firmware version
- [x] 0.5 Folder on disk, `git init`, register with code-review-graph
**Exit:** DEVICE table complete.

## Phase 1 — Protocol (reuse first, capture second)
- [x] 1.1 Study something-x (MIT, B172 support) / Gadgetbridge's Nothing device code (GPL-3, Java/Kotlin, Android — closest reference) + BudsLink config for B172
- [x] 1.2 Write command table (known commands; extras still TBD) in RESEARCH.md: battery, ANC mode/level, adaptive, transparency, EQ (preset + custom), in-ear detection, fit test, low latency, gestures, fw version
- [ ] 1.3 HCI snoop on Pixel 9 while using Nothing X → confirm every command on Vratik's unit; look for undocumented params (transparency level, ANC strength beyond Low/Mid/High, wind noise)
- [ ] 1.4 Optional: decompile Nothing X APK (jadx) to find hidden commands — for interop notes only, don't copy their code
**Exit:** All needed commands documented + replayed from a tiny test script.

## Phase 2 — Core app (Nothing X replacement)
- [~] 2.0 Update + monitoring pipeline (built; secrets + public release source pending) (before any feature work):
  - Fixed applicationId `dev.vratik.anc` + one release keystore, kept forever (backed up off-machine). Same id + same key + higher versionCode = in-place update, settings kept.
  - Private GitHub repo; GitHub Actions builds + signs the APK on every `v*` tag → GitHub Release.
  - In-app updater: checks latest Release, shows changelog, downloads APK, installs via PackageInstaller (one tap; can become silent once the app is its own installer of record).
  - Monitoring: in-app log screen + "export logs"; crash reports (ACRA → GitHub issue or Sentry free tier); version + firmware shown in About.
  - Wireless ADB from the G14 for live logcat during dev.
- [x] 2.1 Kotlin `proto` module: RFCOMM connect, frame encode/decode, CRC, request/response
- [x] 2.2 Foreground service keeps the connection; reconnects on BT events
- [~] 2.3 Compose UI (status, battery, ANC, EQ presets done; custom EQ, fit test, gestures todo): battery (L/R/case), ANC modes, EQ, fit test, gestures
- [x] 2.4 Quick Settings tile + notification controls for ANC mode
**Exit:** Nothing X can be uninstalled.

## Phase 3 — Stronger ANC (what's actually possible)
- [ ] 3.1 Baseline measurement: fixed noise source, phone-mic-in-cup method, each mode → `measurements/`
- [ ] 3.2 Fit-test-driven tip selection (biggest real-world win)
- [ ] 3.3 Smart Auto-ANC: our own scene logic on top of firmware Adaptive — phone mic samples ambient level/spectrum + activity recognition (walking/vehicle/still) → picks Low/Mid/High/Adaptive and EQ. Reacts faster/more aggressively than stock if measurements show stock Adaptive under-performs.
- [ ] 3.4 Anti-residual EQ profile: boost music slightly in the band where ANC leaks most (masking)
**Exit:** Measured improvement vs baseline, or documented that stock High is the ceiling.

## Phase 4 — Conversation Mode (Apple Conversation Awareness / Samsung Voice Detect)
- [ ] 4.1 Trigger research: earbud mics are NOT exposed to apps except via HFP call mode (kills LDAC, low quality, battery). So detect speech with **Pixel 9 mic + on-device VAD** (Silero VAD / WebRTC VAD), optional own-voice enrollment.
- [ ] 4.2 Action on speech detected: switch buds → Transparency + duck media (AudioFocus) ; after N s of silence → restore previous ANC + volume
- [ ] 4.3 Tuning: false-trigger rate in café / bus / home; sensitivity slider
- [ ] 4.4 Optional triggers: someone says your name (on-device keyword spotting), or double-tap gesture
**Exit:** Works reliably with phone in pocket/on table; false triggers acceptable.

## Phase 5 — Voice Focus (experimental — "only voices come through")
The buds' firmware transparency passes everything; no voice-only passthrough exists on-device.
- [ ] 5.1 Prototype: buds on ANC High; Pixel mic → DeepFilterNet/RNNoise (keep speech, drop noise) → play into buds
- [ ] 5.2 Measure end-to-end latency (BT alone ~150–250 ms; low latency mode may help). Decide if usable for face-to-face talk
- [ ] 5.3 Check whether firmware has a hidden transparency/voice-enhance param (from Phase 1.3/1.4) — that would beat this
**Exit:** Keep if latency is tolerable, otherwise drop and document.

## Phase 6 — Firmware-level ANC tuning (OPTIONAL, high risk)
Only if the SoC is identified, firmware is dumpable, and Vratik approves. No writes otherwise.

## Side track — Call mic noise suppression
Probably skip: buds already do Clear Voice; Android doesn't let apps process the call uplink without root.

---

## DECISIONS LOG
| Date | Decision | Why |
|---|---|---|
| 2026-11-21 | No phone-generated anti-noise | Bluetooth latency (~100–250 ms) makes it physically impossible |
| 2026-11-21 | Target = Android app on Pixel 9, Kotlin + Compose | Vratik's phone; RFCOMM works without root |
| 2026-11-21 | Reuse Gadgetbridge/BudsLink protocol work before capturing | CMF Buds Pro 2 already mostly reverse-engineered |
| 2026-11-21 | Conversation detection via phone mic + VAD, not bud mics | Bud mics only reachable via HFP, which drops LDAC and quality |
| 2026-11-21 | Build our own app, don't mod Nothing X | Nothing X is closed, obfuscated, ~140 MB, breaks on update, can't be shared |
| 2026-11-21 | Port protocol from something-x (MIT) | Has B172-specific code + tests; MIT keeps our licence free |
| 2026-11-21 | LibrePods = UX/feature reference only | It's AirPods-specific (Apple L2CAP protocol); nothing protocol-level transfers |

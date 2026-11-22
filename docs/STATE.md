# STATE.md — Earbuds ANC
Last Updated: 2026-11-21

## ONE-LINE STATUS
Phase 2 — app v0.1 skeleton builds green on GitHub Actions; signing key made on G14. Waiting on: repo secrets, public-vs-private decision for updater, HCI capture.

## DEVICE
| Field | Value |
|---|---|
| Brand / model | CMF by Nothing — CMF Buds Pro 2 |
| Model code | TBD — B172 (tested by community) or B187 (newer, untested). Check Nothing X → device info |
| SoC / ANC chip | Unknown — not published. Hybrid ANC, up to 50 dB, 6 mics (3 per bud), adaptive ANC + leak compensation |
| Codecs | LDAC, AAC, SBC. No LE Audio |
| Firmware version | TBD |
| Official app | Nothing X (Android, v3.5.x) |
| Transport | Classic BT RFCOMM, Nothing "0x55" binary protocol — no root needed on Android |
| ANC modes (firmware) | Off / Transparency / ANC Low / Mid / High / Adaptive |
| Other settings | EQ presets + custom EQ, enhanced bass, spatial audio, in-ear detection, low latency, find my buds, gestures, smart dial, fit test |
| Existing open-source support | Gadgetbridge (nightly, "mostly supported"), BudsLink (B172 tested), something-x, EarA, ear (web) |

## PLATFORMS
- **Primary: Android app for Pixel 9** (Kotlin + Jetpack Compose)
- Dev host: Arch Linux (ASUS ROG G14)

## WHAT EXISTS RIGHT NOW
- [x] AGENTS.md, HANDOFF.md, docs scaffold
- [x] MemPalace wing `earbuds_anc`
- [x] Device identified
- [ ] Model code + firmware version confirmed
- [x] GitHub repo vrapatel2002/earbuds-anc (private), cloned at ~/Projects/earbuds-anc, registered in code-review-graph
- [x] Android app skeleton: UI, RFCOMM connect, foreground service, QS tile, crash→issue, log export, self-updater
- [x] CI (tests + debug APK) green; release workflow on v* tags
- [x] Release keystore at ~/.config/earbuds-anc/ (SHA-256 9D:E1:18:D4…:B0:77)
- [ ] Actions secrets set (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- [ ] First release v0.1.0 installed on Pixel 9
- [ ] Baseline ANC measurement
- [x] Kotlin `proto` module (frame/CRC/decoder, ANC, listening mode, custom EQ, battery, wear, serial→model) + tests
- [ ] Protocol confirmed on Vratik's unit

## WHAT'S NEXT
1. Confirm model code (B172/B187) + firmware version.
2. HCI snoop on Pixel 9: switch Nothing X through Adaptive, fit test, low latency, bass, spatial, in-ear, gestures → fill unknowns in RESEARCH.md.
3. Quick real-device check from Arch: `python something-x` or a tiny script sending handshake → setAnc(HIGH) (Nothing X closed).
4. Phase 2: Android `:app` module (needs Android SDK on G14).

## LAST SESSION SUMMARY
- 2026-11-22: Repo created + pushed. Android app skeleton, CI, release workflow, self-updater, crash reporting written; CI green first run. Keystore generated on G14. Claude's GitHub connection can't set Actions secrets or create repos.
- 2026-11-21: Project created. Docs modeled on luminos-os.
- 2026-11-21: Decided: own app, not a modded Nothing X. Wrote `proto/` (pure Kotlin) ported from something-x (MIT); 12 tests pass via kotlinc. Adaptive ANC wire value unknown → blocked in code until captured.
- 2026-11-21: Target set to Pixel 9 Android app. Device = CMF Buds Pro 2 using Nothing X. Goal features: smarter/stronger ANC + Apple/Samsung-style conversation awareness (auto voice passthrough). LibrePods (AirPods) used as feature/UX reference.

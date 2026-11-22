# AGENTS.md — Earbuds ANC Project (Agent Protocol)

> Read this file fully before touching anything. Then query MemPalace (wing `earbuds_anc`).
> Applies to every agent: Claude Code, Gemini CLI, Antigravity, Codex.

---

## WHAT THIS PROJECT IS

An Android app for Vratik's **Pixel 9** that replaces Nothing X for his **CMF Buds Pro 2** and adds smart features: stronger/auto ANC, conversation mode (auto transparency + ducking when someone talks), voice focus.
Device details: docs/STATE.md → DEVICE. Protocol: Nothing RFCOMM 0x55 (see docs/RESEARCH.md).

### The physics agents must respect
- Real ANC (anti-noise) runs **on the earbud's own DSP chip**, using its feedforward/feedback mics, with sub-millisecond latency.
- Bluetooth adds ~100–250 ms latency. **A phone/PC app cannot generate anti-noise over Bluetooth.** Do not propose it, do not build it.
- So "better ANC through software" can only mean one of these levers:
  1. **Control** — switch to the strongest ANC mode/preset the firmware has, via the vendor's BLE/RFCOMM protocol (official app replacement).
  2. **Seal** — ear-tip fit test. Bad seal is the #1 cause of weak ANC, especially in the low end.
  3. **Shaping** — EQ / adaptive ANC settings the firmware exposes.
  4. **Firmware-level tuning** — ANC filter coefficients / gain, ONLY if the chip is documented or already reverse-engineered by others. High brick risk. Never flash without explicit approval from Vratik.
  5. **Call/mic noise suppression** (RNNoise, DeepFilterNet) on the host — this is a different feature (what the *other person* hears), not listening ANC.

If a task doesn't map to one of these, stop and ask.

---

## SOURCE OF TRUTH HIERARCHY

```
1. AGENTS.md              ← agent behavior rules (this file)
2. docs/MASTER_PLAN.md    ← phases, what's done, what's next
3. docs/STATE.md          ← current state + device details
4. docs/CODE_REFERENCE.md ← file map and architecture
5. docs/RESEARCH.md       ← protocol notes, captures, references
6. MemPalace wing earbuds_anc
```
Higher number = less authoritative. A decision logged in MASTER_PLAN.md overrides older notes.

---

## SESSION START (mandatory)

1. Read AGENTS.md → docs/STATE.md → the relevant section of docs/MASTER_PLAN.md.
2. `mempalace_search` in wing `earbuds_anc` for the task topic.
3. `code-review-graph`: run `build_or_update_graph_tool` (incremental), then `get_minimal_context_tool` / `get_impact_radius_tool` for files you'll touch.
4. Confirm scope back in one line before writing code.

## SESSION END (mandatory — the task is not done without this)

1. Update docs/STATE.md (status line, checkboxes, last session summary).
2. Mark completed steps ✅ in docs/MASTER_PLAN.md.
3. Update docs/CODE_REFERENCE.md if files were added/moved.
4. File key decisions in MemPalace: wing `earbuds_anc`, room `decisions` (or `protocol`, `firmware`, `app`).
5. Conventional commit (`feat:`, `fix:`, `docs:`, `re:` for reverse-engineering notes).

---

## CODE RULES

- **Simple code.** No abstractions until there are two real users of them. No frameworks "for later".
- Stack: **Kotlin + Jetpack Compose**, minSdk 31, target Pixel 9 (Android 15/16). Modules: `proto` (pure Kotlin, unit-testable, no Android deps), `service` (BT connection + foreground service), `ui`. Go/Python only for throwaway capture-analysis scripts on the Arch host.
- Keep Nothing X closed while testing — RFCOMM socket is single-owner.
- If code is copied from Gadgetbridge (GPL-3), mark the file header and the whole app becomes GPL-3. Prefer reimplementing from the documented command table.
- One feature per PR/commit series. Don't touch files outside the task's scope.
- Every protocol command gets a captured, byte-level example in docs/RESEARCH.md before code depends on it.
- No secrets, no device serials/MACs in commits — use `XX:XX:...` placeholders.

## SAFETY RULES (hard)

- **Never change `applicationId` (`dev.vratik.anc`) or the release signing key.** Either change forces an uninstall and wipes settings. Bump `versionCode` on every release.
- Never commit the keystore or its passwords — they live in GitHub Actions secrets + Vratik's backup.

- **Never flash firmware, write to flash/EEPROM, or send unknown/opcode-fuzzing commands to the real earbuds** without Vratik's explicit "yes" in the current session.
- Read-only sniffing (btmon, HCI snoop logs, Wireshark) is always OK.
- Keep a backup of any firmware dump before any write experiment.
- Volume/gain changes: cap test output levels — protect ears.

---

## COMMON MISTAKES — DO NOT MAKE THESE

| Mistake | Why it happens | Prevention |
|---|---|---|
| Building "phone-side ANC" over Bluetooth | Sounds plausible | Latency makes it impossible — see physics section |
| Guessing protocol bytes | Impatience | Capture first, document in RESEARCH.md, then code |
| Assuming the earbud model | Placeholder left in | Check STATE.md → DEVICE |
| Confusing call noise suppression with ANC | Same words | Lever 5 is separate; label it clearly |
| Skipping doc updates | "Code works, done" | Doc update is part of the task |
| Over-engineering | Agent habit | Simple code rule |

---

END OF AGENTS.md

# CODE_REFERENCE.md — File Map
Last Updated: 2026-11-21

```
earbuds-anc/
├── AGENTS.md            ← agent protocol (read first)
├── HANDOFF.md           ← paste-at-start template
├── CLAUDE.md / GEMINI.md← pointers to AGENTS.md
├── .mcp.json            ← mempalace + code-review-graph
├── docs/
│   ├── STATE.md         ← current state + device
│   ├── MASTER_PLAN.md   ← phases + decisions log
│   ├── RESEARCH.md      ← chip/protocol notes, captures
│   ├── WORKFLOW.md      ← tool pipeline
│   └── CODE_REFERENCE.md← this file
├── settings.gradle.kts
├── proto/               ← pure Kotlin protocol lib (no Android deps)
│   ├── src/main/kotlin/dev/vratik/anc/proto/
│   │   ├── Frame.kt     ← 0x55 frame encode, CRC16, streaming FrameDecoder
│   │   ├── Commands.kt  ← Cmd IDs, AncMode, ListeningMode, CommandBuilder, CustomEq
│   │   └── Parsers.kt   ← battery, wear, ANC mode, serial, model
│   └── src/test/…/ProtoTest.kt ← byte-exact vectors from something-x
├── app/src/main/kotlin/dev/vratik/anc/
│   ├── App.kt           ← Application, crash capture, AppLog ring buffer
│   ├── Buds.kt          ← RFCOMM connect (vendor UUID → channel probe), frame handling, state
│   ├── BudsService.kt   ← foreground service keeping the connection
│   ├── AncTileService.kt← Quick Settings tile
│   ├── Updater.kt       ← GitHub Releases check + PackageInstaller update
│   └── MainActivity.kt  ← Compose UI
├── .github/workflows/   ← ci.yml (push), release.yml (v* tags)
├── tools/               ← capture-analysis scripts (host)
├── measurements/        ← baseline + after recordings
└── captures/            ← HCI snoop / btmon logs (gitignored if they contain MACs)
```

Run tests: `./gradlew :proto:test`. Build: `./gradlew :app:assembleDebug` (needs Android SDK) — or let CI do it. Releasing: docs/RELEASING.md.

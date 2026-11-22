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
├── app/                 ← Android app (Kotlin/Compose): proto/, service/, ui/ (Phase 2)
├── tools/               ← capture-analysis scripts (host)
├── measurements/        ← baseline + after recordings
└── captures/            ← HCI snoop / btmon logs (gitignored if they contain MACs)
```

Run tests: `gradle :proto:test` (JDK 17).

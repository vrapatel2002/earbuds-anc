# TESTING.md — Real-device checklist (Pixel 9 + CMF Buds Pro 2)

Before testing: Nothing X closed or uninstalled. Turn on **Live debug** in the app and send Claude the Debug ID so the log can be watched from chat.

| # | Test | Pass if |
|---|---|---|
| 1 | Connect (buds out of case, in ears) | Status "Connected", log shows `connected via …` |
| 2 | Info | Model B172/B187 and firmware shown |
| 3 | Battery | L / R / Case % shown; put one bud in case → updates |
| 4 | In-ear | Take one bud out → "In ear" flips |
| 5 | ANC High → Transparency → Off → Mid → Low | You hear each change; the selected chip follows |
| 6 | Change mode with the bud gesture (press-and-hold) | App chip updates on its own (device event) |
| 7 | EQ presets | Sound changes per preset (Rock vs Classical is obvious) |
| 8 | Quick Settings tile | Add tile → tap cycles High/Transparency/Off |
| 9 | Background | Lock phone 5 min, come back → still connected, tile works |
| 10 | Reconnect | Buds in case + close lid, take out again → note if it reconnects (expected: tap Connect for now) |
| 11 | Updates | "Check for updates" → finds the next version / says up to date |

Report anything odd with the time it happened — Claude can match it in the live log.

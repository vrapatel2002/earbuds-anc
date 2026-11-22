# HANDOFF.md
# Earbuds ANC — Chat / Agent Handoff Template
Last Updated: 2026-11-21

PURPOSE: Copy the block below and paste it at the START of every new chat or agent session.
Update docs/STATE.md after each session.

---

## PASTE THIS AT CHAT START:

---

Continuing Earbuds ANC project — [REPLACE WITH ONE-LINE STATUS FROM STATE.md]

**What we're building:**
A companion app/toolkit to get stronger noise cancellation from my earbuds. Real ANC runs on the earbud DSP, so we work through the levers software actually has: vendor-protocol control (strongest mode/preset), ear-tip fit test, firmware-exposed EQ/adaptive settings, and — only if the chip is open/documented — firmware-level ANC tuning. Host-side mic noise suppression for calls is a separate optional track.

**Device:** [MODEL / CHIP / FIRMWARE from STATE.md]
**Platforms:** [Linux (Arch) / Android / Windows — from STATE.md]

**Current State:**
[PASTE CONTENTS OF docs/STATE.md HERE]

**NEXT TASK:**
[REPLACE WITH WHAT NEEDS TO HAPPEN THIS SESSION]

Read AGENTS.md before starting any task.
Read docs/MASTER_PLAN.md for the full phase breakdown.
Query MemPalace wing `earbuds_anc` and update the code-review-graph before editing code.

---

## HOW TO UPDATE THIS AFTER A SESSION

1. Open docs/STATE.md
2. Update "ONE-LINE STATUS" to current phase/step
3. Update "WHAT EXISTS RIGHT NOW" checkboxes
4. Update "WHAT'S NEXT"
5. Add entry to "LAST SESSION SUMMARY"
6. Update docs/MASTER_PLAN.md — mark completed steps with ✅
7. File decisions to MemPalace (wing `earbuds_anc`)

---

END OF HANDOFF.md

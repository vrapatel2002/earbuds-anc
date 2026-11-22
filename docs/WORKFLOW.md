# WORKFLOW.md — Earbuds ANC Tool Pipeline

```
Vratik (Director)
  ├── Claude Chat   → research, plan, write agent prompts, review
  ├── Claude Code   → protocol/Go code, precise fixes, final review + commit
  ├── Gemini CLI    → bulk implementation, scripts, captures processing
  └── Antigravity   → UI build (Qt/QML / Android) → hands to Claude Code for review
```

**Rule:** No agent starts until it has read AGENTS.md, queried MemPalace (`earbuds_anc`), and updated the code-review-graph.

## Standard session flow
1. Tell Claude Chat what you want.
2. Claude Chat → MemPalace → plan → writes the agent prompt (master prompt template).
3. Paste prompt into the right tool.
4. Agent works → updates docs → commits.
5. Claude Code reviews using code-review-graph (`detect_changes_tool`, `get_review_context_tool`).
6. You test on the real earbuds.
7. STATE.md + MemPalace updated.

## MCPs
| MCP | Used for |
|---|---|
| mempalace | Project memory — wing `earbuds_anc`, rooms: `decisions`, `protocol`, `firmware`, `app`, `measurements` |
| code-review-graph | Impact radius, review context, change detection before every commit |

## Anti-patterns
- Two agents editing the same file in parallel.
- Agent writes protocol code without a documented capture.
- Skipping the review step because "it compiled".

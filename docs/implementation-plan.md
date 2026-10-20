# Delivery milestones

The original requirements/design drafts were committed before application code. The user then authorized choosing either reporting/history approach, confirmed basic HR access, and selected free local Compose plus a new public repository.

| Milestone | Result / evidence |
| --- | --- |
| Requirements and tradeoffs | Draft `0c4d4ea`, settled pre-code baseline `e6350c3`; original-document 403 remains explicit |
| Pinned foundation / employee API | `eb77009`; real compile and Angular scaffold build, not claimed as runtime validation |
| Reporting / monetary rules / seed | `1977bc5`; numeric median, shared filters, deterministic 10,000-row seed; actual PostgreSQL tests |
| Responsive HR workspace | `b65b223`; create/edit/deactivate, visible conflicts, five frontend tests, six desktop/phone journeys and axe overview checks |
| Docker / test entrypoints / CI | `53953c9`; one-command deployment, actual Docker-only check script, readiness, isolated fresh-source startup, restart persistence |
| Reviewer package and demo | README, architecture, API/money/runbook, decisions, actual evidence, AI log, review map, recorded 25-second MP4 and screenshots |
| Source delivery | New public repository chosen by user; main-branch push and remote CI are verified during final delivery |

See [validation.md](validation.md) for requirement-to-test mapping, measured timings, exact executed checks, and platform/security/source limitations. A planned test or workflow definition is never counted as a passing execution. No hosting account, paid service, recruiter message, or assessment submission is part of this local delivery.

# Implementation and validation plan

All application milestones are pending. The initial documentation commit captures draft requirements and design before application code; it does not freeze unresolved scope.

| Milestone | Concrete outcome | Validation / evidence before completion |
| --- | --- | --- |
| 0. Requirements | Read original assessment, reconcile clarifications, answer initial questions, agree visibility/stack; commit approved one-page baseline | Source gaps and administrative placeholders explicit; decision register records actual answers |
| 1. Foundation | Exact runtime/dependency/image versions, Maven Wrapper, Angular lockfile, formatting/static checks, PostgreSQL migrations, env examples, minimal CI | Fresh-checkout setup; runtime versions; empty-database migration; compilation/build |
| 2. Vertical slice | HR login → paginated list → detail → salary update → summary, using a small fixture | Unit/security tests, PostgreSQL integration, browser execution probe |
| 3. Correctness | Shared filters, native/selected reporting, decimal median/mean, validation, soft deactivation, conflict recovery | Risk-focused monetary, query, concurrency, and API integration cases |
| 4. Dataset | Deterministic opt-in seed, 10,000 employees, justified indexes | Actual count, repeat-run preservation, stated environment and list/report timings |
| 5. UX | Responsive accessible flows and all required failure/empty/loading states | Desktop/mobile browser journeys, keyboard checks, rendered screenshots |
| 6. Containers | Multi-stage image, Compose readiness, database volume, configuration/runbook | Real image build/start; readiness; migrations; data after app restart; clean-checkout run |
| 7. Review / GitHub | README, API examples, decisions, AI log, test mapping, video script, review map, agreed remote push | Clean working tree, actual SHA and remote branch verified; CI outcomes checked |
| 8. Hosted demo | Agreed provider, cost/access plan, persistent DB, TLS and reviewer login | User authorization for spend/public deployment; actual URL and authenticated flow verified |

Make small commits when each coherent milestone works; do not manufacture incremental history after all implementation is complete. Report actual commands and pass/fail/blocked status after milestones.

## Requirement-to-test checklist

| Requirement | Planned test or check | Current status |
| --- | --- | --- |
| HR sign-in/out and protected APIs | Security integration + Playwright | Not implemented/run |
| CSRF on login and writes | API integration with missing/invalid/valid tokens | Not implemented/run |
| Server search/filter/page/sort | PostgreSQL repository/API fixtures, boundary and escaping cases | Not implemented/run |
| Salary edit and dashboard refresh | Transaction integration + browser journey | Not implemented/run |
| No lost updates | Two concurrent/stale expected versions; verify stored winner | Not implemented/run |
| Decimal money / mixed currency | Unit fixtures and PostgreSQL numeric aggregates | Awaiting reporting decision |
| Odd/even/empty median and weighted mean | Known PostgreSQL populations | Not implemented/run |
| 10,000 synthetic employees | Fresh DB count; seeded-key count; rerun preserving edited row | Not implemented/run |
| Responsive / accessible UI | Browser at phone/desktop width; keyboard/labels/errors | Not implemented/run |
| Docker Compose | Build, health, migration, persistence, shutdown/start | Blocked: socket denied in sandbox |
| Fresh checkout / CI | Reproduce documented install/build/test commands | Pending implementation and shell network access |
| GitHub push | Agreed target/visibility; verify remote SHA | App read access works; target unset, CLI auth fails |
| Deployed app / video | Actual deployment smoke test; video checklist | Not started; provider/budget unresolved |

No application test suite exists yet. A planned test is not a passing result.

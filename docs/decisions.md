# Decision register

No user answer to the initial three tradeoff questions has been received. “Continue” requests resumed the work after interrupted tool calls; they did not specify currency, history, budget, or publication visibility.

| ID | Topic | Recommendation / alternative and concrete cost | Status |
| --- | --- | --- | --- |
| D01 | Currency reports | Native records + fixed USD estimates enable comparable totals; require rate version/direction and rounding rules. Native-only reports are simpler; lack a comparable global cost total. | Awaiting user choice |
| D02 | Salary workflow | Current records without import reduce implementation/test scope; cannot reconstruct past salaries. Effective-dated history enables as-of questions; adds period validation, queries, and UI. | Awaiting user choice |
| D03 | Delivery | Need target, available time, and strictly free vs a user-specified capped expense. Free hosting must be assessed for persistence/sleeping limits; a paid option needs an explicit cap and approval. | Awaiting user constraints |
| D04 | Repository | Public simplifies reviewer access but exposes source. Private limits exposure but needs an actual reviewer identity and invitation. No target selected. | Discuss after initial choices |
| D05 | Stack / architecture | Angular + Java 21/Spring Boot + PostgreSQL, one modular app. Matches reported recruiter freedom and minimizes operations; couples releases and requires Java/Node builds. Separate services add deployment and security configuration. | Proposed; confirm after initial choices |
| D06 | Hosting | Local Compose demo first, then a concrete provider/cost/access plan. No hosting account or public-deployment permission is inferred. | Not selected |
| D07 | Money storage | Decimal native salary with explicit currency; two-decimal supported currencies initially. Avoids binary rounding; other minor-unit currencies require a later policy. | Engineering proposal |
| D08 | Concurrent editing | Optimistic locking and visible 409 recovery. Prevents silent overwrites; users sometimes reload/reapply an edit. | Engineering proposal |
| D09 | Employee lifecycle | Soft deactivation, active default, retain identity and uniqueness. Enables correction and preserves records; no legal retention/deletion policy is implied. | Engineering proposal |
| D10 | Session security | Same-origin server sessions, CSRF, secure deployment cookies. Simple HR browser flow; restarts expire sessions and horizontal scaling needs a session strategy. | Engineering proposal |

Record accepted answers here verbatim or as faithful concise summaries, with date, rationale, and affected requirements. Do not backfill a fictional approval history.

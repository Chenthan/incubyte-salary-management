# Decision register

On 20 October 2026 the user confirmed that either reporting/history approach is acceptable, documented fixed rates are sufficient, a basic HR sign-in is sufficient, and delivery should be free local Docker Compose with zero or minimal manual steps. The user then explicitly requested continuing implementation, including on their current system if needed. Engineering choices under that authorization are recorded below; no public hosting or spending is planned.

| ID | Topic | Recommendation / alternative and concrete cost | Status |
| --- | --- | --- | --- |
| D01 | Currency reports | Choose native records + fixed USD estimates: comparable totals, explicit version/direction and rounding rules. Native-only is simpler but lacks comparable global cost. | Chosen under user's authorization, 20 October |
| D02 | Salary workflow | Choose current records without import: smaller reliable workflow; cannot reconstruct past salaries. History would add period validation, queries, and UI. | Chosen under user's authorization, 20 October |
| D03 | Delivery | Free local Compose, one-command startup with generated local credentials. No paid/public host. Actual assessment submission date remains unspecified and does not block implementation. | Confirmed by user, 20 October |
| D04 | Repository | Public simplifies reviewer access but exposes source. Private limits exposure but needs an actual reviewer identity and invitation. No target selected. | Discuss after initial choices |
| D05 | Stack / architecture | Angular + Java 21/Spring Boot + PostgreSQL, one modular app. Matches reported recruiter freedom and minimizes operations; couples releases and requires Java/Node builds. | Proceeding following user's continue instruction |
| D06 | Hosting | Local Compose only; no public deployment, hosting account, or paid service required. | Confirmed local target |
| D07 | Money storage | Decimal native salary with explicit currency; two-decimal supported currencies initially. Avoids binary rounding; other minor-unit currencies require a later policy. | Engineering proposal |
| D08 | Concurrent editing | Optimistic locking and visible 409 recovery. Prevents silent overwrites; users sometimes reload/reapply an edit. | Engineering proposal |
| D09 | Employee lifecycle | Soft deactivation, active default, retain identity and uniqueness. Enables correction and preserves records; no legal retention/deletion policy is implied. | Engineering proposal |
| D10 | Session security | Same-origin server sessions, CSRF, secure deployment cookies. Simple HR browser flow; restarts expire sessions and horizontal scaling needs a session strategy. | Engineering proposal |

Record accepted answers here verbatim or as faithful concise summaries, with date, rationale, and affected requirements. Do not backfill a fictional approval history.

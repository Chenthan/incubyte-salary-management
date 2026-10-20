# Code-review and interview map

Start with the [requirements](requirements.md), [decisions](decisions.md), and [actual verification](validation.md). The original source document is unverified; do not treat recovered assessment wording as a new verbatim reading. The user explicitly authorized either reporting/history approach and chose free local Compose plus public source.

| Review first | Why / questions to ask |
| --- | --- |
| [Flyway migration](../backend/src/main/resources/db/migration/V1__employees_and_hr_access.sql) | Are salary precision, identity uniqueness, supported currencies/rates, and annual basis correct? Is the USD identity rate enforced? |
| [Shared population predicate](../backend/src/main/java/com/incubyte/salary/employee/PopulationFilter.java) and [employee queries](../backend/src/main/java/com/incubyte/salary/employee/EmployeeQueries.java) | Can input become SQL identifiers? Are wildcard characters literal, size bounded, ties stable, and list/report filters identical? |
| [Report queries](../backend/src/main/java/com/incubyte/salary/reporting/SalaryReports.java) and [money output](../backend/src/main/java/com/incubyte/salary/reporting/Money.java) | Where is rounding applied? Does median remain decimal? Why is averaging department means wrong? Can native currencies be combined accidentally? |
| [Employee transactions](../backend/src/main/java/com/incubyte/salary/employee/EmployeeService.java) and [versioned entity](../backend/src/main/java/com/incubyte/salary/employee/Employee.java) | What happens when two users save version 0? Why both expected-version comparison and database optimistic locking? How does deactivation affect reports? |
| [Security chain](../backend/src/main/java/com/incubyte/salary/auth/SecurityConfiguration.java) and [HR bootstrap](../backend/src/main/java/com/incubyte/salary/auth/HrBootstrap.java) | Are APIs authorized server-side? How do session fixation, CSRF rotation, cookie settings, and password hashing work? What is intentionally missing for production account administration? |
| [PostgreSQL integration tests](../backend/src/test/java/com/incubyte/salary/SalaryApplicationIT.java) | Do the fixtures distinguish weighted mean, odd/even median, aggregate rounding, missing references, and concurrent writes? Why refuse to truncate a non-test DB? |
| [Deterministic seed](../backend/src/main/java/com/incubyte/salary/demo/DemoSeeder.java) | Why are UUIDs/seed keys stable? What prevents reruns from overwriting edits? Why target only the seed-key conflict? |
| [Browser API/session code](../frontend/src/app/core/api.ts), [money pipe](../frontend/src/app/core/money.pipe.ts), [editor](../frontend/src/app/pages/employee-form.ts) | Does auth state stay out of localStorage? Are large decimals preserved? Does a conflict keep the draft and require explicit reload/reapply? |
| [Workspace](../frontend/src/app/pages/workspace.ts) and [browser tests](../frontend/e2e/workspace.spec.ts) | Are filters shareable/cancelled consistently? What did real CSS/CSP and contrast checks catch that a build did not? |
| [Dockerfile](../Dockerfile), [Compose](../compose.yml), [startup](../scripts/run.sh), [CI](../.github/workflows/ci.yml) | Are images reproducible, credentials generated privately, app readiness meaningful, and data persistent? Which platform checks actually ran? |

## Explain the choices

One modular service reduces deployment/security wiring and supports one local command. Frontend and backend releases are coupled; sessions are single-instance. A separate reverse proxy/frontend deployment would help independent releases but adds operation for this scope.

Native salaries remain authoritative. Fixed USD assumptions enable international comparisons with an explicit estimate label; they add rate direction/version and rounding rules. Native-only reporting would avoid those assumptions but would not give a comparable global cost total.

Current records keep the workflow focused. They cannot answer historical salary or headcount questions; effective-dated periods would require overlap validation and as-of reporting. Optimistic locking prevents lost edits but is not an audit trail.

SQL aggregates serve bounded results from the 10,000-row dataset. There is no cache or distributed service because no measured requirement justifies one. Offset pagination serves this scale and page-number UI; much larger/deep-page workloads could justify keyset pagination and measured search indexing.

## AI explanation

Use the [AI log](ai-usage.md) and actual commits. AI generated code/design suggestions; the user selected the scope freedoms and local/public delivery. Real PostgreSQL caught a SQL grouping error, and browser/visual checks caught production CSS loading and contrast issues. Those were corrected and rerun. No independent human review has yet been represented as complete.

Good improvements to discuss after review: salary audit/effective dates if the product needs historical questions, account administration/SSO for real deployment, separate migration/runtime database roles, tested backups, and stronger observability. Do not claim any of them exists in this release.

## Demo and rehearsal

The [25-second real recording](demo.mp4) shows sign-in, India/Engineering filters, records, creation, an INR salary change reflected in USD estimates, retained deactivation, and sign-out. It has no narrated audio; use this checklist for a longer narrated assessment video:

1. State annual-base scope, synthetic data, current records, and fixed illustrative FX.
2. Show Compose readiness and the seed-key count; keep `.env`/passwords off screen.
3. Filter the dashboard and drill into the same employees.
4. Demonstrate invalid input, a valid salary change, and updated reporting.
5. Open two edits to explain 409 recovery; show deactivation versus deletion.
6. Show phone/keyboard access, sign-out, and an anonymous API denial.
7. Explain one substantive alternative, actual test evidence, and current limits.

The recording and checklist accompany the published source. Review the complete codebase and practice the explanation in your own words.

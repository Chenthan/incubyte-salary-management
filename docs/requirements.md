# Requirements — one-page draft

**Status:** proposed, not an approved implementation baseline. Written before application coding on 19 October 2026.

**Evidence:** the supplied handoff reports recruiter-approved clarifications and deliverables recovered from an earlier assessment review. The original assessment document is not in the workspace. The supplied redirect link could not be read; original wording remains unverified. Reconcile it before finalizing this baseline, retaining later explicit clarifications.

## Goal and users

Give signed-in HR staff a clear way to find employees, maintain their gross annual base salaries, and understand headcount and salary cost across countries and departments. Payroll and tax calculation are excluded. One HR access level is sufficient.

## Questions the product should answer

1. How many active employees match a country, department, or name/employee-ID search?
2. What are their total, mean, median, minimum, and maximum annual salaries, with explicit currencies?
3. Which departments account for the largest salary cost within a comparable reporting currency?
4. Which employees exceed a salary threshold in a specified currency?

## Proposed first release

- HR sign-in/sign-out, with authenticated API access and protected mutations.
- Server-paginated employee search, country/department/status filters, stable allowlisted sorting, and bounded page size.
- Employee detail, create/edit, salary update, deactivation, and visible stale-update conflict handling.
- Dashboard and employee drill-down sharing the same population filters.
- Exactly 10,000 deterministic synthetic employees on a fresh, explicitly seeded demo database, across multiple countries; repeat runs preserve edits and do not duplicate employees.
- Responsive, keyboard-accessible UI with loading, empty, validation, unauthorized, conflict, and error states.
- Docker images and Compose, reproducible tests/builds, CI, source repository, verified running demonstration, and supporting design/AI documentation.

## Correctness and acceptance

1. Anonymous API reads fail with 401; authenticated mutations without a valid CSRF token fail with 403.
2. Native salary uses decimal storage and arithmetic, an annual basis, and an explicit currency. Mixed currencies are never summed into a currencyless total.
3. Filters, pagination, and aggregation execute on the server. Default reports include active employees only; empty populations have count 0 and unavailable averages/medians.
4. Valid edits persist, refresh matching reports, and survive an application restart. Stale versions return a visible conflict without losing another edit.
5. PostgreSQL integration tests verify migrations, numeric queries, constraints, filtering, and concurrency. Fast unit tests require neither network nor Docker.
6. Browser tests exercise sign-in, filtered browsing, salary edit → report refresh, validation, and unauthorized access. Actual execution evidence is required before claiming success.
7. Seed count and representative timings are measured on a stated environment. Documentation maps requirements to tests and identifies unverified deliverables.

## Exclusions and open decisions

No payroll, taxes, approval workflow, runtime AI, live FX, external identity provider, email delivery, import/export, projections, or distributed services are proposed. Salary history is undecided; current-only is recommended. Native-only reporting versus native records plus fixed-rate USD comparison is undecided. Submission target/time budget, hosting budget/provider, repository visibility, and reviewer access are unresolved. A deadline and reviewer identity in the correspondence were unfilled placeholders; no real values are inferred.

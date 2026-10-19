# Review and explanation guide

This is a proposed review map; application paths are reserved conventions, not existing implementation.

## Review in this order

1. `docs/requirements.md` and `docs/decisions.md`: distinguish recruiter-confirmed scope from proposals. Are currency, history, delivery, and publication choices actually accepted?
2. `docs/design.md`: trace a browser request through session/CSRF checks, validation, a transaction/query, and PostgreSQL. Are money units, population filters, rounding, and empty results explicit?
3. Future Flyway migrations and report queries: do the database constraints and numeric median/mean match the written rules? Can filters drift between list and reports?
4. Future auth/configuration and employee update code: are APIs protected server-side, mutations CSRF-protected, errors safe, and stale edits visible?
5. Future Angular API/filter/form code: are server errors and expired sessions recoverable, salary currencies visible, and keyboard/mobile flows usable?
6. Future tests and seed code: do fixtures expose odd/even median, currency mixing, stale writes, search escaping, and rerun preservation? Are tests using actual PostgreSQL?
7. Future Dockerfile, Compose, CI, and runbook: are versions reproducible, images non-root, credentials external, readiness meaningful, and data persistent?

## Questions to rehearse

- Why a modular single service, and what evidence would justify splitting it?
- Why decimal money and explicit currencies? Where exactly does rounding happen?
- Which HR questions are intentionally unanswered by the selected salary-history policy?
- How does one stale browser avoid overwriting another HR user's salary change?
- Why do cookies require CSRF protection, and what does a frontend route guard actually protect?
- What did AI propose, what did the user select, and which checks actually validated the result?
- Why does 10,000 rows not demonstrate 10,000 concurrent users?
- What is still missing before this could handle real employee information?

## Video/demo checklist — execute after implementation

1. State the agreed scope, annual salary basis, synthetic-data policy, and reporting currency/estimate rules.
2. Show the running environment and fresh seed verification; keep runtime credentials off screen.
3. Sign in, filter a dashboard population, and drill into the same filtered employees.
4. Open an employee, demonstrate validation, make a valid salary edit, and show the resulting summary change.
5. Demonstrate stale-edit recovery and employee deactivation behavior.
6. Show mobile/keyboard usability, logout, and protected API denial.
7. Explain architecture, one substantive alternative, actual test evidence, and known limitations.

A script is not a recorded video. A Compose demo is not a hosted deployment. Verify each deliverable separately.

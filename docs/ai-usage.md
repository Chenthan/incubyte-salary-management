# AI usage and verification log

This log records actual activity. It does not represent independent human review or completed implementation.

## 19 October 2026 — requirements and design preparation

**User instruction, faithful summary:** read the supplied handoff and linked assessment, create a comprehensive design and discuss tradeoffs, build in the workspace, dockerize services with Compose, validate, demonstrate, and push to GitHub using sound coding practices.

**Handoff constraints applied:** ask currency/history/delivery questions before application code; do not silently freeze recommendations; requirements document before coding; preserve genuine incremental history; report actual tests and access limits; keep private correspondence out of source control; do not email or submit the assessment.

**Actions:** read the full handoff; inspected workspace/instructions/runtime; read cloud-environment runtime, networking, and Docker skill references; checked managed environment status; tried requirements-link retrieval; verified GitHub connector read access; asked the three initial tradeoff questions; drafted these documents. Two additional-permission tool batches were interrupted; later “Continue” messages resumed work without answering the questions.

**Suggestions proposed:** one modular Spring Boot application, Angular, PostgreSQL, same-origin production packaging; native money as decimals; shared server-side filters; optimistic locking; opt-in deterministic seed; risk-focused unit/PostgreSQL/browser checks. Currency conversion, salary history, delivery budget, publication visibility, and stack acceptance are not recorded as approved.

**Review/validation actually performed:** official Angular/Boot compatibility pages read; runtime commands and access results recorded in `runtime-status.md`; `git diff --cached --check` passed; all relative links across nine Markdown files resolve and a private-source/contact-marker check passed. These are documentation checks. No application compilation, tests, browser render, benchmark, image build, deployment, push, or human code review has occurred.

**Rejected/deferred:** no runtime LLM, paid API, live FX, broker, Redis, Kubernetes, CSV import, export, or invented deployment. Do not turn an unexecuted test configuration into a claimed result.

Future entries should record the real prompt/summary, accepted/rejected design or generated changes, affected files, actual test commands/results, and remaining limitations at each implementation milestone.

## 20 October 2026 — scope authorization

The user confirmed either currency/history approach is acceptable, documented fixed rates are sufficient, a single HR sign-in is sufficient, and requested free local Compose with minimal manual setup. A later message explicitly instructed continuing, using the user's current system if required. Under that authorization, chose native records plus fixed-rate USD estimates, current-only salaries without import, and one-command local deployment. Updated the requirements baseline before writing application code. GitHub target/visibility was requested asynchronously; no publication choice is inferred. Network/Docker access still requires the workspace sandbox permission process.

The user subsequently selected a new public `incubyte-salary-management` repository. Narrow sandbox grants failed during mount setup; full command escalation established working network and Docker 28.4.0 access. The requirements redirect then returned HTTP 403. Exact versions were resolved from npm/Maven metadata; official Maven Wrapper scripts downloaded; PostgreSQL/build images pulled with immutable digests recorded. `npm install --no-fund` generated the lockfile. The generated Angular production build passed. Maven/JDK-container `spotless:apply test` compiled the initial authentication/employee implementation successfully, with **no tests present yet**. That compilation is not runtime, security, or integration validation. GitHub CLI authentication also succeeded with network access; earlier sandbox failures did not establish an invalid credential.

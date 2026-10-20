# Validation evidence

Recorded 20 October 2026. Results below describe checks actually executed in the managed Linux cloud workspace and GitHub Actions. Independent human review and public application hosting are outside this verification record.

## Executed checks

| Check | Actual result |
| --- | --- |
| Backend unit tests | 7 passed: conversion/rounding/rate failures and literal filters/allowlisted stable sorting |
| PostgreSQL integration | 14 passed against PostgreSQL 17.11; actual migrations, constraints, HTTP security, reports, and concurrent transactions |
| Angular unit tests | 5 passed: exact decimal-string display and safe/recoverable API errors |
| Strict Angular production build | Passed; initial JS/CSS bundle approximately 282 kB before transfer compression |
| Strict E2E TypeScript and formatting | Passed |
| Maven Wrapper bootstrap | Executed checksum-pinned `./backend/mvnw --version`: Maven 3.9.16 on Java 21.0.12.1 |
| Browser journeys | 6 passed: three scenarios at 1440×1080 and again at 390×844, Chromium |
| Overview accessibility | Zero axe findings for tested WCAG 2 A/AA and 2.1 AA rules at both sizes; no page-wide horizontal overflow |
| Docker-only `./scripts/test.sh` | Passed: actual container builds, 7 unit + 14 PostgreSQL integration + 5 frontend tests, formatting, and test type checking |
| One-command `./scripts/run.sh` | Passed: generated private config, build, migration, seed, database readiness, application readiness |
| Real browser recording | Passed; 25.32-second MP4, plus actual desktop/phone/detail/sign-in screenshots |
| Remote GitHub Actions | [Passed on `main` at `08213b6`](https://github.com/Chenthan/incubyte-salary-management/actions/runs/39394576034): Docker-only checks, assembled application startup, 10,000 seeded records, and desktop/phone browser journeys |

GitHub Actions repeats builds/tests on main pushes and PRs. The linked completed run validates commit `08213b67065d1c5c7199ddbee0721473dbb29121` on an Ubuntu 24.04 runner. Later commits have their own results in the repository's [Actions tab](https://github.com/Chenthan/incubyte-salary-management/actions); the recorded result does not imply that an unobserved run passed.

The browser mutation journey includes creation, invalid salary input, valid update, an external edit while the original form stays open, 409 recovery with draft preservation, reload/reapply, report refresh, deactivation, and logout. The other journey checks shared country/department filters, pagination, and zero results. These are real HTTP/browser checks, not mocks of the algorithms.

## Requirement mapping

| Requirement | Evidence |
| --- | --- |
| Requirements before coding | Initial docs commit `0c4d4ea`; settled baseline `e6350c3`; application code starts later |
| Basic HR authentication | Integration `sessionLoginCsrfAndLogoutAreEnforcedOnTheServer`; browser mutation journey |
| Pagination/filter/sort on server | `PopulationFilterTest`; integration literal search/page/tie-breaker/filter agreement; browser browsing journey |
| Native/converted money correctness | `MoneyTest`; PostgreSQL mixed currency, direction, precision, odd/even/empty median, weighted mean, native department groups |
| Salary writes and no lost updates | Integration stale and truly concurrent updates; browser external edit/reload/reapply |
| Employee lifecycle | Integration/browser deactivation retains identity and removes employee from active reports |
| Input/DB validation | Integration invalid precision/zero/negative/duplicate input plus real database constraint failures |
| Synthetic 10,000-record seed | Integration fresh count, six countries, deterministic IDs, rerun preservation; fresh demo SQL below |
| Responsive usable UI | Actual desktop/phone journeys, screenshots, agent visual inspection, axe overview check |
| Reproducible local deployment | Digest-pinned images, npm lockfile, checksum-pinned Maven Wrapper, executed startup and Docker-only test scripts |
| Intentional AI workflow | Genuine incremental commits and [AI usage log](ai-usage.md), including errors caught by real checks |
| Demo/source delivery | [Recorded MP4](demo.mp4), [review guide](review-guide.md), [published public repository](https://github.com/Chenthan/incubyte-salary-management), and passing remote CI linked above |

## Seed verification

Before any browser-created records, this SQL on the fresh running demo returned **10000 / 9500 / 6 / 10000**:

```sql
SELECT count(*), count(*) FILTER (WHERE active), count(DISTINCT country_code),
       count(*) FILTER (WHERE demo_seed_key IS NOT NULL)
FROM employees;
```

The seed test edits a generated employee, reruns seeding, verifies the edit is preserved, then recreates a fresh population and confirms the same stable ID. The benchmark after browser/demo work had 10,007 total records, 9,500 active; the seven added synthetic demonstration/test employees had been deactivated. Extra user-created rows do not change the 10,000 seed-key requirement.

## Observed performance

Measured **20 October 2026, 12:56 IST** with `python3 scripts/benchmark.py`: one authenticated standard-library HTTP client over loopback, sequential requests, one excluded warm-up then 20 samples per endpoint. p95 uses nearest rank. JSON parsing and local HTTP overhead are included; login time is excluded.

Docker reported 3 CPUs and 10,452,471,808 bytes of daemon memory. The app had a 768 MiB limit and PostgreSQL 512 MiB; other host workloads were not controlled. Java Temurin 21.0.12.1, Boot 4.1.1, PostgreSQL 17.11. Population: 9,500 active seeded employees; 10,007 retained total rows after browser/demo work.

| Request | Median | p95 | Range |
| --- | ---: | ---: | ---: |
| Active employee page, 25 rows, name sort | 13.97 ms | 20.72 ms | 9.17–22.95 ms |
| Global active salary summary | 46.55 ms | 52.62 ms | 42.53–56.22 ms |
| India engineering salary summary | 9.30 ms | 11.48 ms | 5.83–11.54 ms |

These are small warm-run observations in this environment, not an SLA, production benchmark, or evidence for 10,000 concurrent users.

## Limits and unverified items

- Original assessment redirect returned HTTP 403; original wording is unverified. Baseline uses the supplied handoff and later user scope authorization.
- Native Windows Docker Desktop and ARM/macOS startup were not executed here. The PowerShell launcher is provided; Linux AMD64 is the demonstrated environment.
- Axe covered the populated overview at two sizes, not every state or assistive technology. Agent visual review is not independent human accessibility review.
- No public application hosting/TLS deployment, account recovery/administration, login throttling, salary history/audit, real employee data, or production backup/recovery exercise is claimed.

## Restart and clean-checkout verification

Compared an MD5 fingerprint over every full employee row, ordered by ID, before and after an actual application restart/repeated seed. The fingerprint was identical. This is change detection for the test, not a security hash or a backup.

Exported committed application/Compose source at `53953c9` with `git archive`, without `.env`, `node_modules`, targets, or local build output. Ran the normal startup script in that clean directory with an isolated Compose project and port 18081. It generated new credentials, built from source, created a separate database volume, and returned **10000 total / 9500 active / 6 countries**, frontend HTTP **200**, readiness **UP**. Its disposable volume was removed; the original application volume was retained. Subsequent review-package edits are documentation only.

The first clean-build attempt exhausted the managed cloud's 32 GiB root filesystem after many builds using Docker's VFS storage driver. Removed unused build cache and this project's disposable test images; retained application containers/images and the database volume. Recovered storage, restarted health checks, and verified the original employee fingerprint was still identical. The clean-start retry above then passed. Normal startup scripts do not prune caches or delete volumes.

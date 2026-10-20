# HTTP API

Same-origin JSON API under `/api`; all employee/reference/report endpoints require the server-side HR session. No JWT/localStorage token is used. Amounts in responses are decimal strings. All writes, including login/logout, require the session CSRF token.

| Method/path | Contract |
| --- | --- |
| GET `/api/auth/csrf` | Public; returns `token` and `headerName` for this session |
| POST `/api/auth/login` | Form-urlencoded `username`/`password` plus CSRF header; 204 or 401 |
| GET `/api/auth/me` | `{ "username": "hr" }` or 401 |
| POST `/api/auth/logout` | CSRF header; invalidates session; 204 |
| GET `/api/reference-data` | Supported countries, departments, and versioned fixed rates |
| GET `/api/employees` | Filtered, stably sorted bounded page |
| POST `/api/employees` | Validated creation; 201 with Location and full employee view |
| GET `/api/employees/{id}` | Full employee view with current `version`; 404 if absent |
| PUT `/api/employees/{id}` | Full editable input plus expected `version`; immutable employee code; 409 on conflict |
| PATCH `/api/employees/{id}/status` | `{ "active": false, "version": 0 }`; retained record, optimistic locking |
| GET `/api/reports/salary-summary` | Headcount, native groups, labelled USD estimate, department costs, rates, and timestamp |

## Filters and pages

Shared population parameters: `q` (name/employee ID, up to 100 characters; `%`/`_` are literal), `country`, `department`, `status=ACTIVE|INACTIVE|ALL`, `currency`, `minSalary`, `usd=true|false`. Default: active employees, USD comparison, no other restriction. A native threshold requires `currency`. Reference codes must be supported.

List-only parameters: zero-based `page` (default 0, cap 100000), `size` (default 25, range 1–100), `sort=name|code|salary|country|department`, `direction=asc|desc`. Every sort appends unique ID. Native salary sorting groups by currency before salary; USD sorting uses the unrounded estimate.

Examples of authenticated reads:

```text
GET /api/employees?country=IN&department=ENGINEERING&size=25&sort=salary&direction=desc
GET /api/reports/salary-summary?country=IN&department=ENGINEERING
GET /api/employees?usd=false&currency=INR&minSalary=1000000.00
```

The page shape is `{ content: [...], totalElements, page, size, totalPages }`. Each employee includes native `annualSalary`, `currencyCode`, labelled-in-UI `usdEstimate`, `active`, `version`, and `updatedAt` in addition to identity/reference fields.

## Write example

Creation body (synthetic example):

```json
{
  "employeeCode": "REVIEW-001",
  "firstName": "Jordan",
  "lastName": "Demo",
  "email": "review.001@example.invalid",
  "countryCode": "IN",
  "departmentCode": "ENGINEERING",
  "annualSalary": "1000000.00",
  "currencyCode": "INR"
}
```

For an update, send these fields and the `version` from the last read; keep `employeeCode` unchanged. The successful response has the new version. A stale response has status 409, `code: STALE_EMPLOYEE`, and a safe detail; the UI preserves the draft until the user reloads/reapplies. Duplicate identity/email is a separate conflict.

Obtain a fresh CSRF token after login/logout because Spring Security rotates authentication/session state. A frontend route guard does not replace server authorization. `scripts/benchmark.py` contains an executable standard-library client that reads local credentials privately, signs in with cookies/CSRF, and performs report/page reads without logging credentials.

## Errors and reporting

400: malformed/invalid input or filters, with `fieldErrors` for validated DTO fields. 401: sign-in required/failed. 403: denied or missing/invalid CSRF. 404: absent employee. 409: stale edit or identity/constraint conflict. Unexpected 500 responses do not expose stack traces or SQL.

Reports always separate native currencies and include illustrative rate metadata with the USD estimate. In native UI mode, department costs are separated by currency and no cross-currency ranking is displayed. [Monetary semantics](money.md) define empty values, rounding, median, and threshold behavior.

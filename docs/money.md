# Salary and reporting rules

All salaries are gross **annual base salary**. Taxes, bonuses, payroll schedules, and employer contributions are excluded. The default population is active employees matching the selected search, country, department, currency, and threshold. Inactive records remain available explicitly.

Native salary is required, strictly positive, at most 9,999,999,999.99, and accepts at most two fractional digits. All six supported currencies use two fractional digits. The API accepts amounts as decimal strings, stores Java `BigDecimal` / PostgreSQL `NUMERIC(12,2)`, and rejects excessive precision instead of rounding entered salaries. Country and salary currency are separate fields.

## Fixed USD assumptions

Version **illustrative-v1**, effective **2026-01-01**. These are deliberately fixed assessment assumptions, **not historical or live market quotes**. Direction is **USD per one native currency unit**:

| Native currency | USD per unit |
| --- | ---: |
| USD | 1.00000000 |
| INR | 0.01200000 |
| EUR | 1.10000000 |
| GBP | 1.27000000 |
| CAD | 0.74000000 |
| SGD | 0.75000000 |

`illustrative USD annual salary = native annual salary × USD per unit`.

For example, INR 1,000,000.00 becomes an illustrative USD 12,000.00. Native records remain INR 1,000,000.00. USD has identity rate 1. The database rejects unsupported/missing currency references and nonpositive rates; the Java conversion boundary also fails explicitly for a missing/invalid rate.

The migration defines this immutable application rate version. There is no API to edit rates and no FX network request. Changing reporting assumptions requires an explicit new migration/version and review of historical interpretation; it must not silently relabel the existing version.

## Precision, populations, and empty results

- Retain intermediate conversion precision. Aggregate first, then round reported monetary outputs to two decimals, **HALF_UP**. PostgreSQL positive-numeric rounding and Java output formatting use this policy.
- Individually rounded row estimates may sum to a different cent than a rounded aggregate. Two INR 0.42 rows each display USD 0.01, while their unrounded combined estimate 0.01008 displays USD 0.01.
- Mean is total divided by employee count, not the average of department means. Native aggregates group by currency. A global native salary total is never produced.
- Median orders decimal amounts in PostgreSQL, picks the middle row for odd populations or averages the middle pair for even populations, and preserves numeric arithmetic.
- Empty population: count 0, USD total 0.00, and null mean/median/minimum/maximum. An explicitly requested empty native currency group also has total 0.00; otherwise there are no native groups.
- Threshold means **strictly greater than** the entered amount. USD-estimate mode compares unrounded converted values. Native threshold mode requires an explicit native currency; a threshold of zero is a valid population filter even though a saved salary must be positive.
- List and report use one shared predicate builder. Each response has a consistent database snapshot, but separate browser requests do not promise a single cross-request snapshot if another HR user edits concurrently.

Salary distributions alone do not establish pay fairness. Current records cannot answer what salary an employee had before an edit. These limits are product choices, not missing arithmetic.

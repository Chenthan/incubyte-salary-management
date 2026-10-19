# Salary Management

An HR workspace for managing current annual base salaries and answering compensation questions across a synthetic international workforce.

**Status: design draft, 19 October 2026.** No application code, images, deployment, benchmarks, or passing tests exist yet. The supplied handoff explicitly requires a tradeoff discussion before coding. Currency reporting, salary history, and delivery constraints await the user's answers; this repository does not treat recommendations as accepted decisions.

- [One-page requirements draft](docs/requirements.md)
- [Architecture, data model, APIs, money rules, and tradeoffs](docs/design.md)
- [Implementation and validation plan](docs/implementation-plan.md)
- [Decision register](docs/decisions.md)
- [Observed runtime and access status](docs/runtime-status.md)
- [AI usage and verification log](docs/ai-usage.md)
- [Reviewer and interview guide](docs/review-guide.md)
- [Administrative clarification draft — unsent](docs/administrative-clarification.md)

The proposed implementation is Angular + Spring Boot + PostgreSQL, in one repository, with Docker Compose and a same-origin browser/API deployment. Exact package and image versions will be pinned and validated during the foundation milestone. Setup, demo credentials, API examples, and test commands will be added when they have actually been exercised.

The 10,000-employee requirement describes dataset size; it does not establish a concurrent-user capacity target. All employee data will be synthetic. No correspondence, private document links, personal contact details, credentials, or production employee data belong in the repository.

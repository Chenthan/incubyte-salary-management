# Runtime and reproducible setup

Verified in the managed Linux AMD64 cloud workspace on 20 October 2026. Project: `/workspace/incubyte-salary-management`, branch `main`. No project `AGENTS.md` or existing application code was present at the start.

| Component/check | Observed result |
| --- | --- |
| Docker / Compose | 28.4.0 / 2.40.3; actual image builds and health checks run |
| Java in app/build images | Temurin 21.0.12.1; compiler and runtime exercised |
| Host Java | Debian OpenJDK 21.0.12.1; host compiler absent, so builds/tests use the pinned JDK container |
| Maven Wrapper | 3.9.16; official only-script 3.3.4 wrapper; ZIP SHA-256 verified by actual wrapper bootstrap |
| Node / npm | 24.19.0 / 11.9.0; actual locked install, tests, build, type checks |
| Angular / TypeScript / RxJS | 21.2.25 (CLI/build 21.2.27) / 5.9.3 / 7.8.2 |
| PostgreSQL | 17.11; identical immutable image for test/demo DBs |
| Browser | System Chromium 154.0.8037.57, Playwright 1.64.1; actual desktop/phone journeys and recording |
| Public GitHub repository | [Chenthan/incubyte-salary-management](https://github.com/Chenthan/incubyte-salary-management); `main` published and remote CI passed; see [validation evidence](validation.md) |
| Assessment redirect | HTTP 403; original document not read |

## Ordinary local setup

Only a running Docker engine with Compose v2.20+ is required for deployment. Run `./scripts/run.sh` or PowerShell `scripts/run.ps1`; it generates local configuration and waits for readiness. `./scripts/test.sh` runs containerized backend/PostgreSQL/frontend checks. Browser tests additionally use Node/Playwright; see the root README. Native Java development requires a JDK 21, not just a JRE.

Direct dependency versions, npm lockfile, Maven Wrapper checksum, and image digests are committed. Official compatibility references: [Angular](https://angular.dev/reference/versions), [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html). Actual installations/builds verify the selected combination rather than relying on version ranges alone.

## Managed-cloud support

The runtime HTTP policy is enforced and permits outbound HTTP through its configured proxy; no VPN or outbound identities were configured. Preserve inherited proxy and CA settings. Shell sandboxing initially blocked network/Docker access; narrow socket mount grants failed during sandbox setup. Authorized command escalation was required for actual network/build/browser operations. An earlier sandboxed `gh auth status` failure did not establish an invalid token; authenticated CLI access worked once network access was available.

`compose.cloud.yml` supplies `CODEX_PROXY_CERT` as a BuildKit secret. `scripts/maven-network.sh` adapts Maven proxy settings and a temporary Java trust store, deleting temporary trust/settings in the same build step. The final image contains neither the session certificate nor credentials. TLS verification remains enabled. The main `run.sh` detects this optional environment variable; regular local machines do not need it.

Actual developer commands used include:

```sh
bash scripts/cloud-check.sh backend spotless:apply verify -Pintegration
./scripts/test.sh
./scripts/run.sh
cd frontend
npm test
npm run build
npm run format:check
npm run typecheck:e2e
CAPTURE_DEMO=1 PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/chromium npm run e2e
```

The cloud's VFS Docker driver uses substantial physical disk across repeated builds. A clean-build attempt hit its 32 GiB filesystem limit; unused cache/project test images were cleared and the isolated startup rerun passed. Database row fingerprints were preserved. No volume-pruning operation belongs in normal startup. The [validation report](validation.md) records exact evidence, measurements, platform limits, and unverified items.

#!/usr/bin/env python3
"""Read-only, sequential local API measurements; not a concurrent-load benchmark."""
from datetime import datetime, timezone
from http.cookiejar import CookieJar
import json
import os
from pathlib import Path
import statistics
import time
from urllib.parse import urlencode
from urllib.request import build_opener, HTTPCookieProcessor, ProxyHandler, Request

root = Path(__file__).resolve().parent.parent
configuration = dict(line.split("=", 1) for line in (root / ".env").read_text().splitlines() if "=" in line and not line.startswith("#"))
port = os.environ.get("APP_PORT", configuration.get("APP_PORT", "8080"))
base = "http://127.0.0.1:" + str(int(port))
client = build_opener(ProxyHandler({}), HTTPCookieProcessor(CookieJar()))


def get(path):
    with client.open(base + path, timeout=15) as response:
        return json.load(response)


csrf = get("/api/auth/csrf")
request = Request(base + "/api/auth/login", data=urlencode({"username": configuration["HR_USERNAME"], "password": configuration["HR_PASSWORD"]}).encode(), headers={csrf["headerName"]: csrf["token"], "Content-Type": "application/x-www-form-urlencoded"})
with client.open(request, timeout=15) as response:
    if response.status != 204:
        raise RuntimeError("Sign-in did not succeed")

measurements = []
for name, path in [
    ("employee page, active, 25 rows", "/api/employees?size=25&sort=name"),
    ("global active salary summary", "/api/reports/salary-summary"),
    ("India engineering salary summary", "/api/reports/salary-summary?country=IN&department=ENGINEERING"),
]:
    get(path)  # one warm-up, excluded
    durations = []
    for _ in range(20):
        started = time.perf_counter()
        get(path)
        durations.append((time.perf_counter() - started) * 1000)
    durations.sort()
    measurements.append({"name": name, "samples": 20, "median_ms": round(statistics.median(durations), 2), "p95_ms": round(durations[18], 2), "minimum_ms": round(durations[0], 2), "maximum_ms": round(durations[-1], 2)})

result = {"measured_at_utc": datetime.now(timezone.utc).isoformat(), "method": "One authenticated client; sequential HTTP requests over loopback; one excluded warm-up per endpoint; no concurrent load; p95 is nearest rank.", "active_employees": get("/api/reports/salary-summary")["headcount"], "all_employees": get("/api/employees?status=ALL&size=1")["totalElements"], "measurements": measurements}
(root / "artifacts").mkdir(exist_ok=True)
(root / "artifacts" / "benchmark.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))

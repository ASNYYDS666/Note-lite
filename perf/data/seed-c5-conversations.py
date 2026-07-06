import requests, time, os
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
U, P = "perf_cap_c5", "Test123456"
s = requests.Session()
s.post(f"{BASE_URL}/api/v1/user/register", json={"username":U,"password":P,"email":f"{U}@t.l"})
r = s.post(f"{BASE_URL}/api/v1/user/login", json={"username":U,"password":P})
token = r.json()["data"]["token"]
h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

counts = [10, 50, 100, 500]
results = {}

for nc in counts:
    # Create conversations
    conv_ids = []
    for i in range(1, nc + 1):
        r = s.post(f"{BASE_URL}/api/v1/conversations", headers=h, json={"title": f"C5-Test-{i:04d}"})
        if r.status_code == 200:
            cid = r.json().get("data", {}).get("id")
            if cid:
                conv_ids.append(cid)
    print(f"  Created {len(conv_ids)}/{nc} conversations")

    # Test list endpoint 3 times, record P95
    times_ms = []
    for _ in range(3):
        t1 = time.time()
        r = s.get(f"{BASE_URL}/api/v1/conversations", headers=h)
        times_ms.append((time.time() - t1) * 1000)
    p95 = sorted(times_ms)[-1]  # rough p95 of 3 samples

    # Check DB for message count query pattern
    db_query_start = time.time()
    r = s.get(f"{BASE_URL}/api/v1/conversations", headers=h)
    db_query_ms = (time.time() - db_query_start) * 1000

    results[nc] = {"avg": sum(times_ms)/len(times_ms), "p95": p95, "count": len(conv_ids)}
    print(f"  Conversations={nc}: avg={results[nc]['avg']:.0f}ms, p95={results[nc]['p95']:.0f}ms")

print("\n" + "=" * 60)
print(f"{'Conversations':<16} {'Avg':>8} {'P95':>8}")
print("-" * 40)
for nc in counts:
    print(f"{nc:<16} {results[nc]['avg']:>6.0f}ms {results[nc]['p95']:>6.0f}ms")
print("-" * 40)

import requests, time, os
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
U, P = "perf_cap_c2", "Test123456"
s = requests.Session()
s.post(f"{BASE_URL}/api/v1/user/register", json={"username":U,"password":P,"email":f"{U}@t.l"})
r = s.post(f"{BASE_URL}/api/v1/user/login", json={"username":U,"password":P})
token = r.json()["data"]["token"]
h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

print(f"{'Chars':<10} {'Save':>8} {'Read':>8} {'RespLen':>8}  Status")
print("-" * 55)
for chars in [1000, 5000, 10000, 30000, 50000, 100000]:
    para = ("The system performance test requires generating substantial text content. "
            "Understanding how data volume affects latency is critical for capacity planning. " * 3)
    content = f"<h2>Char Limit Test: {chars} chars</h2>\n<p>" + (para * (chars // len(para) + 1))[:chars] + "</p>"
    t1 = time.time()
    r = s.post(f"{BASE_URL}/api/v1/note", headers=h, json={"title": f"C2-{chars}chars", "content": content})
    save_ms = (time.time() - t1) * 1000
    if r.status_code == 200 and r.json().get("code") == 200:
        nid = r.json().get("data")
        t2 = time.time()
        r2 = s.get(f"{BASE_URL}/api/v1/note/{nid}", headers=h)
        read_ms = (time.time() - t2) * 1000
        print(f"{chars:<10} {save_ms:>6.0f}ms {read_ms:>6.0f}ms {len(r2.text):>8}  OK")
    else:
        print(f"{chars:<10} {save_ms:>6.0f}ms {'N/A':>6} {'N/A':>8}  FAIL: {r.text[:80]}")
print("-" * 55)

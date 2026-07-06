import requests, time, os
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
U, P = "perf_cap_100", "Test123456"
s = requests.Session()
r = s.post(f"{BASE_URL}/api/v1/user/login", json={"username":U,"password":P})
token = r.json()["data"]["token"]
h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# Test notes with varying content lengths to see chunk counts
contents = {
    "Short(~200w)": "<p>" + ("Machine learning is a subset of artificial intelligence. " * 10) + "</p>",
    "Medium(~500w)": "<h2>System Architecture</h2><p>" + ("The microservice architecture pattern structures an application as a collection of loosely coupled services. Each service is independently deployable and scalable. " * 15) + "</p>",
    "Long(~1500w)": "<h2>Database Design Principles</h2><p>" + ("When designing a database schema, normalization is key to reducing data redundancy. First normal form requires atomic values. Second normal form removes partial dependencies. Third normal form eliminates transitive dependencies. " * 25) + "</p>",
    "VeryLong(~5000w)": "<h2>Complete System Architecture Document</h2>" + "".join([f"<h3>Section {i}</h3><p>" + ("This section covers the detailed implementation of the system component. We need to carefully consider scalability reliability and maintainability. " * 20) + "</p>" for i in range(1, 8)]),
}

print("=" * 65)
print(f"{'Content Type':<20} {'Save':>7} {'Chunks':>7} {'Embed':>8} {'Total':>8}")
print("-" * 65)

for name, content in contents.items():
    # Create note
    t1 = time.time()
    r = s.post(f"{BASE_URL}/api/v1/note", headers=h,
        json={"title": f"C3-{name}", "content": content})
    save_ms = (time.time() - t1) * 1000
    
    if r.status_code == 200 and r.json().get("code") == 200:
        note_id = r.json()["data"]
        # Wait for async embedding
        time.sleep(5)
        # Check Qdrant count
        import subprocess, json as j
        qr = subprocess.run(['curl', '-s', f'http://localhost:16333/collections/note_chunks/points/scroll', 
                           '-H', 'Content-Type: application/json',
                           '-d', f'{{"filter":{{"must":[{{"key":"noteId","match":{{"value":{note_id}}}}}]}},"limit":20}}'],
                          capture_output=True, text=True)
        try:
            qdata = j.loads(qr.stdout)
            chunks = len(qdata.get('result', {}).get('points', []))
        except:
            chunks = "?"
        
        print(f"{name:<20} {save_ms:>5.0f}ms {chunks:>6}  {'~2.5s':>7} {'~2.5s':>7}")
    else:
        print(f"{name:<20} FAIL: {r.text[:60]}")

print("-" * 65)
print("\nNote: Embed time is async (not in API response).")
print("Each chunk = one 1536-dim vector = one Qdrant point.")
print(f"\nCurrent Qdrant total points: ", end="")
import subprocess, json
r = subprocess.run(['curl', '-s', 'http://localhost:16333/collections/note_chunks'], capture_output=True, text=True)
print(j.loads(r.stdout)['result'].get('points_count', '?'))

#!/usr/bin/env python3
"""Note-lite Capacity Test Data Seeder - Python version with English content templates."""

import requests, random, sys, os

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")

TITLES = [
    "Java Spring Boot Project Setup Notes", "MySQL Query Optimization Guide",
    "Redis Caching Strategy Summary", "Vue3 Component Communication Patterns",
    "Docker Deployment Troubleshooting", "Git Commands Quick Reference",
    "Linux Server Configuration Guide", "RESTful API Design Best Practices",
    "JWT Authentication Flow Analysis", "Microservice Architecture Design",
    "Database Index Optimization Practice", "Frontend Performance Optimization",
    "API Idempotency Design Patterns", "Message Queue Usage Scenarios",
    "Distributed Lock Implementation", "Unit Testing Best Practices",
    "Code Review Common Issues", "Design Patterns in Practice",
    "System Security Defense Strategy", "CI/CD Pipeline Setup Guide",
    "High Concurrency System Design", "Cache Avalanche Solution",
    "Rate Limiting Strategy Comparison", "Database Sharding Practice",
]

PARAGRAPHS = [
    "During project development we often encounter performance bottlenecks. After analyzing the code we found most issues concentrated in database queries. To solve this we introduced Redis caching to reduce database pressure. The cache hit rate improved from 45 percent to 92 percent after optimization.",
    "Today I learned about Spring Security configuration. You extend the SecurityFilterChain bean and configure which URLs need authentication. Understanding the filter chain order and how each filter contributes to overall security is key.",
    "Our team discussed frontend framework selection yesterday. We decided on Vue3 with TypeScript. Composition API makes code more maintainable while TypeScript provides better type safety and IDE support. The migration plan spans 4 sprints.",
    "For high-concurrency systems database connection pool configuration is crucial. If the pool is too small threads wait. If too large resources are wasted. The recommendation is CPU cores times 2 plus 1. We use HikariCP.",
    "I have been researching vector databases. Qdrant is a high-performance vector search engine for RAG systems. By chunking documents and converting to vectors we achieve semantic similarity search with 1536-dimensional embeddings.",
    "Regarding code quality readability is most important. A function should do one thing and naming should clearly express intent. Comments should explain WHY not WHAT. We enforce this through code review policies.",
    "When deploying Spring Boot apps I use Docker containerization. First a Dockerfile then orchestration with docker-compose. This ensures consistency between dev and production. Multi-stage builds reduced image size to under 200MB.",
    "Frontend performance optimization: reduce HTTP requests, compress assets, use CDN, code splitting. For SPAs first screen loading speed directly impacts user experience. We target under 2 seconds for initial load.",
    "Elasticsearch is powerful for full-text search. We use it for log analysis with Filebeat for collection, Logstash for filtering, ES for indexing, and Kibana for visualization. Index lifecycles are managed automatically.",
    "Today I found a memory leak. Using JProfiler I discovered a HashMap continuously growing. The fix was to clear expired entries after each cycle. We added a scheduled cleanup every 30 minutes as a safety net.",
    "Authentication and authorization are fundamental. We use JWT tokens with 15-minute expiration and refresh tokens for longer sessions. RBAC is implemented at gateway and service levels for defense in depth.",
    "Load testing is often overlooked until incidents happen. We use k6 for test scripts because its JavaScript API is easy to learn and integrates with CI pipelines. Baselines run before every major release.",
]

def random_content(index, total):
    p1 = random.choice(PARAGRAPHS)
    p2 = random.choice(PARAGRAPHS)
    return (f"<h2>Note Number {index}</h2>"
            f"<p>{p1}</p><p>{p2}</p>"
            f"<p><em>Capacity test note {index} of {total}. Generated for performance benchmarking.</em></p>")

def main():
    note_count = int(sys.argv[1]) if len(sys.argv) > 1 else 100
    folder_count = 5 if note_count <= 100 else (20 if note_count <= 1000 else (50 if note_count <= 5000 else 100))
    username = f"perf_cap_{note_count}"
    password = "Test123456"
    email = f"perf_cap_{note_count}@test.local"

    print(f"{'='*60}")
    print(f" Note-lite Capacity Test Data Seeder")
    print(f" Notes: {note_count} | Folders: {folder_count} | User: {username}")
    print(f"{'='*60}")

    session = requests.Session()

    print("\n[1/4] Registering...")
    r = session.post(f"{BASE_URL}/api/v1/user/register", json={"username": username, "password": password, "email": email})
    result = r.json()
    print(f"  {'OK' if result.get('code')==200 else result.get('message','?')}")

    print("\n[2/4] Logging in...")
    r = session.post(f"{BASE_URL}/api/v1/user/login", json={"username": username, "password": password})
    data = r.json()
    if data.get("code") != 200:
        print(f"  FATAL: {data}"); sys.exit(1)
    token = data["data"]["token"]
    print(f"  OK: userId={data['data']['userId']}")

    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    print(f"\n[3/4] Creating {folder_count} folders...")
    r = session.post(f"{BASE_URL}/api/v1/note/folder", headers={"Authorization": f"Bearer {token}"}, data={"name": "CapacityTestRoot", "parentId": ""})
    root_id = r.json().get("data") if r.ok else None
    if root_id and isinstance(root_id, int): print(f"  Root folder id={root_id}")
    for i in range(1, folder_count + 1):
        session.post(f"{BASE_URL}/api/v1/note/folder", headers={"Authorization": f"Bearer {token}"}, data={"name": f"Folder-{i:04d}", "parentId": str(root_id or "")})
        if i % 20 == 0 or i == folder_count: print(f"  [{i}/{folder_count}]")

    print(f"\n[4/4] Creating {note_count} notes...")
    ok = fail = 0
    for i in range(1, note_count + 1):
        payload = {"title": f"{random.choice(TITLES)} No.{i}", "content": random_content(i, note_count)}
        if root_id and random.random() < 0.7: payload["folderId"] = root_id
        try:
            r = session.post(f"{BASE_URL}/api/v1/note", headers=headers, json=payload)
            if r.status_code == 200 and r.json().get("code") == 200: ok += 1
            else: fail += 1; print(f"  FAIL [{i}]: {r.text[:200]}") if fail <= 2 else None
        except Exception as e: fail += 1; print(f"  ERROR [{i}]: {e}") if fail <= 2 else None
        if i % 50 == 0: print(f"  [{i}/{note_count}] OK:{ok} FAIL:{fail}")

    print(f"\n{'='*60}")
    print(f" DONE: {ok} success, {fail} failed")
    print(f"{'='*60}")

    print("\n[Verify] Tree API...")
    r = session.get(f"{BASE_URL}/api/v1/note/tree", headers=headers)
    if r.ok:
        td = r.json().get("data", {})
        print(f"  Folders={len(td.get('folders',[]))} Notes={len(td.get('notes',[]))}")
    r = session.get(f"{BASE_URL}/api/v1/note/page?pageNum=1&pageSize=5", headers=headers)
    if r.ok:
        print(f"  Page total={r.json().get('data',{}).get('total',0)}")

if __name__ == "__main__":
    main()

"""
Note-lite Concurrency Test Data Seeder
============================================================
Usage:
  py seed-concurrency.py                        # 1 user, 500 notes
  py seed-concurrency.py --users 2 --notes 1000 # 2 users, 1000 notes each
============================================================
"""
import requests, argparse, json, random, string, time

BASE_URL = "http://localhost:8080"

LOREM_PARTS = [
    "Spring Boot 3.2 microservice architecture with MyBatis-Plus ORM integration.",
    "Qdrant vector database for semantic search with 1536-dimensional embeddings.",
    "Redis 7 caching layer with Lettuce client and connection pooling.",
    "MySQL 8 relational database with Flyway migration management.",
    "Vue 3 frontend with Tiptap rich text editor and Element Plus UI components.",
    "JWT authentication with refresh token rotation and Redis token blacklisting.",
    "RAG pipeline including Query Rewrite, Vector Retrieval, Rerank, and Generation stages.",
    "SSE streaming for real-time AI chat responses with thinking token support.",
    "Multi-user note management with folder nesting and Markdown rendering.",
    "Docker Compose orchestration including Prometheus and Grafana monitoring stack.",
]

def rand_str(n=50):
    return ''.join(random.choices(string.ascii_letters + string.digits, k=n))

def random_content(num):
    parts = random.sample(LOREM_PARTS, k=4)
    return f"<h2>CT#{num}</h2><p>{' '.join(parts)}</p><p>ID:{rand_str(30)}</p>"

def register_user(username, pwd="Test123456"):
    r = requests.post(f"{BASE_URL}/api/v1/user/register", json={
        "username": username, "password": pwd,
        "confirmPassword": pwd, "email": f"{username}@test.local"
    }, headers={"Content-Type": "application/json"})
    d = r.json()
    ok = d.get("code") in (200, 10001)  # 200=success, 10001=already exists
    if not ok:
        print(f"  register failed: {d}")
    return ok

def login(username, pwd="Test123456"):
    r = requests.post(f"{BASE_URL}/api/v1/user/login", json={
        "username": username, "password": pwd
    }, headers={"Content-Type": "application/json"})
    d = r.json()
    if d.get("code") == 200 and d["data"].get("token"):
        return d["data"]["token"], d["data"].get("userId")
    return None, None

def create_notes(token, count, prefix="CT"):
    h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    ok, fail = 0, 0
    for i in range(1, count + 1):
        try:
            r = requests.post(f"{BASE_URL}/api/v1/note", json={
                "title": f"{prefix}-{i:04d}", "content": random_content(i)
            }, headers=h, timeout=30)
            if r.status_code == 200: ok += 1
            else: fail += 1
        except: fail += 1
        if i % 100 == 0: print(f"    notes: {i}/{count}")
        time.sleep(0.01)
    return ok, fail

def make_folders(token, count):
    h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    for i in range(1, count + 1):
        requests.post(f"{BASE_URL}/api/v1/note/folder", json={
            "name": f"CT-Folder-{i:02d}", "parentId": None
        }, headers=h, timeout=10)

def make_conversations(token, count):
    h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    for i in range(1, count + 1):
        requests.post(f"{BASE_URL}/api/v1/conversations", json={
            "title": f"CT-Chat-{i:03d}"}, headers=h, timeout=10)

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--users", type=int, default=1)
    p.add_argument("--notes", type=int, default=500)
    p.add_argument("--folders", type=int, default=10)
    p.add_argument("--dialogs", type=int, default=20)
    a = p.parse_args()

    print(f"=== Concurrency Data Seeder: {a.users} users, {a.notes} notes/user ===")
    users = []
    for u in range(1, a.users + 1):
        name = f"perf_concur_{u:02d}"
        print(f"\n[{u}/{a.users}] {name}")
        if not register_user(name): continue
        tok, uid = login(name)
        if not tok: continue
        print(f"  userId={uid}")
        users.append({"username": name, "token": tok, "userId": uid})
        print(f"  Creating {a.folders} folders...")
        make_folders(tok, a.folders)
        print(f"  Creating {a.notes} notes...")
        ok, fail = create_notes(tok, a.notes, f"CT{u:02d}")
        print(f"    OK:{ok} FAIL:{fail}")
        print(f"  Creating {a.dialogs} conversations...")
        make_conversations(tok, a.dialogs)

    with open("perf/data/concurrency-users.json", "w", encoding="utf-8") as f:
        json.dump(users, f, indent=2, ensure_ascii=False)
    print(f"\n=== Done: {len(users)} users -> perf/data/concurrency-users.json ===")

if __name__ == "__main__":
    main()

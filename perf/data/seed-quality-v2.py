#!/usr/bin/env python3
"""检索质量重测 - 种子数据脚本 v2
   50+ 笔记 / 25 题 / 7 类: 短中长超长+代码混合
   Usage: python perf/data/seed-quality-v2.py [--base-url URL] [--skip-interference]

   测试用户: testone / 123456"""
import requests, sys, os, time, argparse, json

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
DATA_DIR = os.path.dirname(os.path.abspath(__file__))

def login(session, username, password):
    r = session.post(f"{BASE_URL}/api/v1/user/login", json={"username": username, "password": password})
    data = r.json()
    if data.get("code") != 200:
        raise Exception(f"Login failed: {data}")
    token = data["data"]["token"]
    print(f"  [Login] userId={data['data']['userId']}, token={token[:20]}...")
    return token

def create_note(session, token, title, content):
    r = session.post(f"{BASE_URL}/api/v1/note", json={"title": title, "content": content}, headers={
        "Authorization": f"Bearer {token}", "Content-Type": "application/json"})
    if r.status_code == 200 and r.json().get("code") == 200:
        return r.json()["data"]
    else:
        print(f"  [Error] {r.text[:200]}")
        return None

def load_notes():
    """Load all seed notes from the companion JSON data file"""
    data_file = os.path.join(os.path.dirname(DATA_DIR), "quality", "seed-notes-v2.json")
    with open(data_file, "r", encoding="utf-8") as f:
        data = json.load(f)
    return data.get("golden", []), data.get("interference", [])

def main():
    parser = argparse.ArgumentParser(description="Seed data v2 for retrieval quality retest")
    parser.add_argument("--base-url", default=BASE_URL)
    parser.add_argument("--skip-interference", action="store_true")
    args = parser.parse_args()

    golden, interference = load_notes()

    print("=" * 60)
    print("  Note-lite Retrieval Quality Retest - Seed Data v2")
    print("=" * 60)
    print(f"  Golden notes: {len(golden)}, Interference: {len(interference)}")

    session = requests.Session()
    token = login(session, "testone", "123456")

    total_chars = 0
    print(f"\n[1/2] Creating golden notes ({len(golden)})...")
    for i, note in enumerate(golden):
        nid = create_note(session, token, note["title"], note["content"])
        chars = len(note["content"])
        total_chars += chars
        if nid:
            print(f"  [{i+1}/{len(golden)}] OK \"{note['title']}\" ({chars} chars)")
        time.sleep(0.3)
    print(f"  Total chars: {total_chars:,}")

    if not args.skip_interference:
        ichars = 0
        print(f"\n[2/2] Creating interference notes ({len(interference)})...")
        for i, note in enumerate(interference):
            nid = create_note(session, token, note["title"], note["content"])
            ichars += len(note["content"])
            if nid:
                print(f"  [{i+1}/{len(interference)}] OK \"{note['title']}\"")
            time.sleep(0.2)
        print(f"  Total chars: {ichars:,}")

    total = len(golden) + (0 if args.skip_interference else len(interference))
    print(f"\n{'=' * 60}")
    print(f"  Done! Created {total} notes.")
    print(f"  User: testone / 123456")
    print(f"  Wait 2-5 min for embedding to complete.")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()

import requests, time, os, random
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
U, P = "perf_cap_c4", "Test123456"
s = requests.Session()
s.post(f"{BASE_URL}/api/v1/user/register", json={"username":U,"password":P,"email":f"{U}@t.l"})
r = s.post(f"{BASE_URL}/api/v1/user/login", json={"username":U,"password":P})
token = r.json()["data"]["token"]
h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# ── Create nested folder chain: root → L1 → L2 → L3 → ... ──
max_depth = 10
parent_id = None

print("=" * 60)
print(" Creating nested folder chain (depth 1-10)")
print("=" * 60)

for depth in range(1, max_depth + 1):
    resp = s.post(f"{BASE_URL}/api/v1/note/folder",
        headers={"Authorization": f"Bearer {token}"},
        data={"name": f"Depth-{depth:02d}", "parentId": str(parent_id) if parent_id else ""})
    folder_data = resp.json().get("data")
    if folder_data and isinstance(folder_data, int):
        parent_id = folder_data
        print(f"  Depth {depth:2d}: folder_id={parent_id}")
    else:
        print(f"  Depth {depth:2d}: FAIL - {resp.text[:100]}")
        break

print()

# ── Also create some breadth: 10 child folders under each level ──
# This tests tree loading when there are many folders at the same level
print("Creating breadth folders (5 per level, levels 1-3)...")
# Get folder for level 1 (we need to re-login to get fresh tree)
r = s.get(f"{BASE_URL}/api/v1/note/tree", headers=h)
tree = r.json().get("data", {})

def find_first_folder_id(folders):
    for f in folders:
        return f.get("id")
    return None

# Find root's first child
root_folders = tree.get("folders", [])
if root_folders:
    l1_id = find_first_folder_id(root_folders)
    for depth in range(2, 4):
        # add siblings at this level
        for i in range(5):
            s.post(f"{BASE_URL}/api/v1/note/folder",
                headers={"Authorization": f"Bearer {token}"},
                data={"name": f"Breadth-D{depth}-F{i}", "parentId": str(l1_id) if l1_id else ""})

print()

# ── Now test tree loading time at current state ──
print("Testing tree load time...")
times = []
for i in range(5):
    t1 = time.time()
    r = s.get(f"{BASE_URL}/api/v1/note/tree", headers=h)
    ms = (time.time() - t1) * 1000
    times.append(ms)

times.sort()
print(f"  Tree with {max_depth}-deep folders + breadth: P50={times[2]:.0f}ms, P95={times[4]:.0f}ms")

# ── Also create some notes inside deep folder ──
para = "Test content for deep folder capacity testing. " * 5
print(f"\nCreating 20 notes inside deepest folder (depth={max_depth})...")
for i in range(20):
    s.post(f"{BASE_URL}/api/v1/note", headers=h,
        json={"title": f"DeepNote-{i}", "content": f"<p>{para}</p>", "folderId": parent_id})

print("Done.")

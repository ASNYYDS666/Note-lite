// Note-lite Concurrency Test - Round 2: CRUD Mixed Scenario Ramp-Up
// Simulates real user behavior: login -> tree -> page -> detail -> create
// Ramp-up stages: 5 -> 10 -> 20 -> 35 -> 50 -> 70 -> 100 VUs
// Usage:
//   k6 run perf/scripts/concurrency-crud.js -e USERNAME=perf_concur_01 -e PASSWORD=Test123456
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter, Rate } from "k6/metrics";

const trendTree = new Trend("crud_tree_duration", true);
const trendCreate = new Trend("crud_create_duration", true);
const trendPageList = new Trend("crud_page_list_duration", true);
const trendDetail = new Trend("crud_detail_duration", true);
const rateSuccess = new Rate("crud_success_rate");
const counterErrors = new Counter("crud_errors");

export const options = {
  stages: [
    { duration: "2m", target: 5 },
    { duration: "2m", target: 10 },
    { duration: "2m", target: 20 },
    { duration: "2m", target: 35 },
    { duration: "2m", target: 50 },
    { duration: "2m", target: 70 },
    { duration: "2m", target: 100 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    "crud_success_rate": ["rate>0.95"],
    "crud_tree_duration": ["p(95)<3000"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const USERNAME = __ENV.USERNAME || "perf_concur_01";
const PASSWORD = __ENV.PASSWORD || "Test123456";

let cachedToken = null;

function getToken() {
  if (cachedToken) return cachedToken;
  const res = http.post(BASE_URL + "/api/v1/user/login", JSON.stringify({
    username: USERNAME, password: PASSWORD,
  }), { headers: { "Content-Type": "application/json" } });
  const body = JSON.parse(res.body);
  if (body.code === 200 && body.data && body.data.token) {
    cachedToken = body.data.token;
    return cachedToken;
  }
  console.error("login failed: " + res.body.substring(0, 100));
  return null;
}

export default function () {
  const token = getToken();
  if (!token) { counterErrors.add(1); rateSuccess.add(false); return; }

  // --- Tree (every user every iteration) ---
  group("tree", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/note/tree", {
      headers: { Authorization: "Bearer " + token },
    });
    trendTree.add(Date.now() - start);
    const ok = res.status === 200;
    rateSuccess.add(ok);
    if (!ok) counterErrors.add(1);
  });
  sleep(0.5);

  // --- PageList (70% of users) ---
  if (Math.random() < 0.7) {
    group("pageList", () => {
      const page = Math.floor(Math.random() * 3) + 1;
      const start = Date.now();
      const res = http.get(BASE_URL + "/api/v1/note/page?pageNum=" + page + "&pageSize=20", {
        headers: { Authorization: "Bearer " + token },
      });
      trendPageList.add(Date.now() - start);
      rateSuccess.add(res.status === 200);
    });
    sleep(0.3);
  }

  // --- Note Detail (30% of users) ---
  if (Math.random() < 0.3) {
    group("detail", () => {
      const noteId = Math.floor(Math.random() * 500) + 1;
      const start = Date.now();
      const res = http.get(BASE_URL + "/api/v1/note/" + noteId, {
        headers: { Authorization: "Bearer " + token },
      });
      trendDetail.add(Date.now() - start);
      rateSuccess.add(res.status === 200 || res.status === 400);
    });
    sleep(0.3);
  }

  // --- Create Note (15% of users) ---
  if (Math.random() < 0.15) {
    group("create", () => {
      const title = "CT-" + Date.now() + "-" + __VU + "-" + __ITER;
      const start = Date.now();
      const res = http.post(BASE_URL + "/api/v1/note", JSON.stringify({
        title: title,
        content: "<p>Concurrency test note from VU=" + __VU + "</p>",
      }), {
        headers: {
          Authorization: "Bearer " + token,
          "Content-Type": "application/json",
        },
      });
      trendCreate.add(Date.now() - start);
      rateSuccess.add(res.status === 200);
    });
    sleep(0.5);
  }

  sleep(1);
}

export function handleSummary(data) {
  const g = (name, stat) => data.metrics[name] ? data.metrics[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(0) + "ms" : "N/A";
  return {
    stdout: [
      "=== CRUD Stress Results ===",
      "Tree:      P50=" + fmt(g("crud_tree_duration", "med")) + " P95=" + fmt(g("crud_tree_duration", "p(95)")),
      "Create:    P50=" + fmt(g("crud_create_duration", "med")) + " P95=" + fmt(g("crud_create_duration", "p(95)")),
      "PageList:  P50=" + fmt(g("crud_page_list_duration", "med")) + " P95=" + fmt(g("crud_page_list_duration", "p(95)")),
      "Detail:    P50=" + fmt(g("crud_detail_duration", "med")) + " P95=" + fmt(g("crud_detail_duration", "p(95)")),
      "Success:   " + (g("crud_success_rate", "rate") != null ? (g("crud_success_rate", "rate") * 100).toFixed(1) : "N/A") + "%",
    ].join("\n"),
  };
}

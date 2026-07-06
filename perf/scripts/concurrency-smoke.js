// Note-lite Concurrency Test - Smoke Test (Fix-Verify Loop)
// Quick check: 5 VUs x 5 iterations on core endpoints.
// Use as "before/after fix" baseline in the fix-verify cycle.
// Usage:
//   k6 run perf/scripts/concurrency-smoke.js -e USERNAME=perf_concur_01 -e PASSWORD=Test123456
//   k6 run perf/scripts/concurrency-smoke.js --out json=perf/reports/concurrency/before.json
//   k6 run perf/scripts/concurrency-smoke.js --out json=perf/reports/concurrency/after.json
//   k6 compare perf/reports/concurrency/before.json perf/reports/concurrency/after.json
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter, Rate } from "k6/metrics";

const treeTrend = new Trend("smoke_tree_duration", true);
const pageListTrend = new Trend("smoke_page_list_duration", true);
const conversationsTrend = new Trend("smoke_conversations_duration", true);
const successRate = new Rate("smoke_success");
const errorCount = new Counter("smoke_errors");

export const options = {
  iterations: 5,
  vus: 5,
  duration: "3m",
  noConnectionReuse: false,
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const USERNAME = __ENV.USERNAME || "perf_concur_01";
const PASSWORD = __ENV.PASSWORD || "Test123456";

function getToken() {
  const res = http.post(BASE_URL + "/api/v1/user/login", JSON.stringify({
    username: USERNAME, password: PASSWORD,
  }), { headers: { "Content-Type": "application/json" } });
  const body = JSON.parse(res.body);
  if (body.code === 200 && body.data && body.data.token) {
    return body.data.token;
  }
  return null;
}

export default function () {
  const token = getToken();
  if (!token) { errorCount.add(1); successRate.add(false); return; }

  group("tree", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/note/tree", {
      headers: { Authorization: "Bearer " + token },
    });
    treeTrend.add(Date.now() - start);
    const ok = res.status === 200;
    successRate.add(ok);
    if (!ok) errorCount.add(1);
  });
  sleep(0.5);

  group("pageList", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/note/page?pageNum=1&pageSize=20", {
      headers: { Authorization: "Bearer " + token },
    });
    pageListTrend.add(Date.now() - start);
    const ok = res.status === 200;
    successRate.add(ok);
    if (!ok) errorCount.add(1);
  });
  sleep(0.3);

  group("conversations", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/conversations", {
      headers: { Authorization: "Bearer " + token },
    });
    conversationsTrend.add(Date.now() - start);
    const ok = res.status === 200;
    successRate.add(ok);
    if (!ok) errorCount.add(1);
  });
  sleep(1);
}

export function handleSummary(data) {
  const g = (name, stat) => data.metrics[name] ? data.metrics[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(0) + "ms" : "N/A";
  return {
    stdout: [
      "=== Smoke Test (5 VUs x 5 iters) ===",
      "Tree:      P50=" + fmt(g("smoke_tree_duration", "med")) + " P95=" + fmt(g("smoke_tree_duration", "p(95)")),
      "PageList:  P50=" + fmt(g("smoke_page_list_duration", "med")) + " P95=" + fmt(g("smoke_page_list_duration", "p(95)")),
      "ConvList:  P50=" + fmt(g("smoke_conversations_duration", "med")) + " P95=" + fmt(g("smoke_conversations_duration", "p(95)")),
      "Success:   " + (g("smoke_success", "rate") != null ? (g("smoke_success", "rate") * 100).toFixed(1) : "N/A") + "%",
    ].join("\n"),
  };
}

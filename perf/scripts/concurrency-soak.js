// Note-lite Concurrency Test - 30-Minute Soak Test
// Holds ~35 VUs (70% of estimated breaking point) for 30 minutes.
// Observe JVM heap trend, GC frequency, connection leaks.
// Usage:
//   k6 run perf/scripts/concurrency-soak.js -e USERNAME=perf_concur_01 -e PASSWORD=Test123456
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter, Rate } from "k6/metrics";

const treeTrend = new Trend("soak_tree_duration", true);
const successRate = new Rate("soak_success");
const errorCount = new Counter("soak_errors");

export const options = {
  stages: [
    { duration: "1m", target: 35 },
    { duration: "28m", target: 35 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    "soak_success": ["rate>0.95"],
  },
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

  const start = Date.now();
  const res = http.get(BASE_URL + "/api/v1/note/tree", {
    headers: { Authorization: "Bearer " + token },
  });
  treeTrend.add(Date.now() - start);
  const ok = res.status === 200;
  successRate.add(ok);
  if (!ok) errorCount.add(1);

  sleep(Math.random() * 3 + 2);
}

export function handleSummary(data) {
  const g = (name, stat) => data.metrics[name] ? data.metrics[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(0) + "ms" : "N/A";
  return {
    stdout: [
      "=== Soak Test Results (30min) ===",
      "Tree P50: " + fmt(g("soak_tree_duration", "med")),
      "Tree P95: " + fmt(g("soak_tree_duration", "p(95)")),
      "Tree P99: " + fmt(g("soak_tree_duration", "p(99)")),
      "Success:  " + (g("soak_success", "rate") != null ? (g("soak_success", "rate") * 100).toFixed(1) : "N/A") + "%",
      "Errors:   " + (g("soak_errors", "count") || 0),
      "",
      ">>> CRITICAL: Check Grafana for:",
      "  - jvm_memory_used_bytes{area=heap} (trend over 30min)",
      "  - jvm_gc_pause_seconds_sum (GC frequency)",
      "  - hikaricp_connections_active (trend over time)",
      "  - sse_connections_active (should be 0 during this test)",
    ].join("\n"),
  };
}

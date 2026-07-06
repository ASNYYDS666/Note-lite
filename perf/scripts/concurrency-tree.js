// Note-lite Concurrency Test - Tree Loading Stress
// Isolate pressure on GET /api/v1/note/tree.
// While running, check Grafana: HikariCP pending, threadpool, JVM memory.
// Usage:
//   k6 run perf/scripts/concurrency-tree.js -e USERNAME=perf_concur_01 -e PASSWORD=Test123456
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter, Rate } from "k6/metrics";

const treeTrend = new Trend("tree_stress_duration", true);
const successRate = new Rate("tree_stress_success");
const errorCount = new Counter("tree_stress_errors");

export const options = {
  stages: [
    { duration: "1m", target: 5 },
    { duration: "2m", target: 10 },
    { duration: "2m", target: 20 },
    { duration: "2m", target: 40 },
    { duration: "2m", target: 60 },
    { duration: "2m", target: 80 },
    { duration: "2m", target: 100 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    "tree_stress_duration": ["p(95)<5000"],
    "tree_stress_success": ["rate>0.95"],
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

  sleep(1);
}

export function handleSummary(data) {
  const g = (name, stat) => data.metrics[name] ? data.metrics[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(0) + "ms" : "N/A";
  return {
    stdout: [
      "=== Tree Stress Results ===",
      "P50: " + fmt(g("tree_stress_duration", "med")),
      "P95: " + fmt(g("tree_stress_duration", "p(95)")),
      "P99: " + fmt(g("tree_stress_duration", "p(99)")),
      "Success: " + (g("tree_stress_success", "rate") != null ? (g("tree_stress_success", "rate") * 100).toFixed(1) : "N/A") + "%",
      "",
      ">>> CHECK GRAFANA FOR:",
      "  - hikaricp_connections_active / pending",
      "  - threadpool_pool_active{pool=webAsyncExecutor}",
      "  - jvm_memory_used_bytes{area=heap}",
    ].join("\n"),
  };
}

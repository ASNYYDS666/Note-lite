// Note-lite Concurrency Test - Round 1: Single-VU Baseline
// 1 VU, 20 iterations. Establish optimal latency before adding concurrency.
// Usage:
//   k6 run perf/scripts/concurrency-baseline.js -e USERNAME=perf_concur_01 -e PASSWORD=Test123456
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter } from "k6/metrics";

const trendLogin = new Trend("baseline_login_duration", true);
const trendTree = new Trend("baseline_tree_duration", true);
const trendPageList = new Trend("baseline_page_list_duration", true);
const trendNoteDetail = new Trend("baseline_note_detail_duration", true);
const trendConversations = new Trend("baseline_conversations_duration", true);
const counterErrors = new Counter("baseline_errors");

export const options = {
  iterations: 20,
  vus: 1,
  duration: "5m",
  noConnectionReuse: false,
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const USERNAME = __ENV.USERNAME || "perf_concur_01";
const PASSWORD = __ENV.PASSWORD || "Test123456";
const NOTE_ID  = __ENV.NOTE_ID  || "1";

let cachedToken = null;

function getToken() {
  if (cachedToken) return cachedToken;
  const start = Date.now();
  const res = http.post(BASE_URL + "/api/v1/user/login", JSON.stringify({
    username: USERNAME, password: PASSWORD,
  }), { headers: { "Content-Type": "application/json" } });
  trendLogin.add(Date.now() - start);
  const body = JSON.parse(res.body);
  if (body.code === 200 && body.data && body.data.token) {
    cachedToken = body.data.token;
    console.log("login ok, userId=" + body.data.userId);
    return cachedToken;
  }
  console.error("login failed: " + res.body.substring(0, 200));
  return null;
}

export default function () {
  const token = getToken();
  if (!token) { counterErrors.add(1); return; }

  group("T1-tree", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/note/tree", {
      headers: { Authorization: "Bearer " + token },
    });
    trendTree.add(Date.now() - start);
    if (!check(res, { "tree-200": (r) => r.status === 200 })) counterErrors.add(1);
  });
  sleep(0.3);

  group("T2-pageList", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/note/page?pageNum=1&pageSize=20", {
      headers: { Authorization: "Bearer " + token },
    });
    trendPageList.add(Date.now() - start);
    if (res.status !== 200) counterErrors.add(1);
  });
  sleep(0.3);

  group("T3-noteDetail", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/note/" + NOTE_ID, {
      headers: { Authorization: "Bearer " + token },
    });
    trendNoteDetail.add(Date.now() - start);
    if (res.status !== 200 && res.status !== 400) counterErrors.add(1);
  });
  sleep(0.3);

  group("T4-conversations", () => {
    const start = Date.now();
    const res = http.get(BASE_URL + "/api/v1/conversations", {
      headers: { Authorization: "Bearer " + token },
    });
    trendConversations.add(Date.now() - start);
    if (res.status !== 200) counterErrors.add(1);
  });
}

export function handleSummary(data) {
  const g = (name, stat) => data.metrics[name] ? data.metrics[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(0) + "ms" : "N/A";
  return {
    stdout: [
      "=== Baseline (" + USERNAME + ") ===",
      "Tree:      P50=" + fmt(g("baseline_tree_duration", "med")) + " P95=" + fmt(g("baseline_tree_duration", "p(95)")),
      "PageList:  P50=" + fmt(g("baseline_page_list_duration", "med")) + " P95=" + fmt(g("baseline_page_list_duration", "p(95)")),
      "Detail:    P50=" + fmt(g("baseline_note_detail_duration", "med")) + " P95=" + fmt(g("baseline_note_detail_duration", "p(95)")),
      "ConvList:  P50=" + fmt(g("baseline_conversations_duration", "med")) + " P95=" + fmt(g("baseline_conversations_duration", "p(95)")),
    ].join("\n"),
  };
}

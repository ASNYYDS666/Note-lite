// Note-lite 并发极限测试 - 无 sleep 纯净并发
// 移除所有 sleep，让 VUs 持续猛打，找到真正崩溃点。
// 加压: 10->30->60->100->150->200->300 VUs
// 用法:
//   k6 run perf/scripts/concurrency-crud-nosleep.js -e USERNAME=perf_concur_01 -e PASSWORD=Test123456
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter, Rate } from "k6/metrics";

const trendTree = new Trend("nosleep_tree_duration", true);
const trendPageList = new Trend("nosleep_page_list_duration", true);
const trendDetail = new Trend("nosleep_detail_duration", true);
const rateSuccess = new Rate("nosleep_success");
const counterErrors = new Counter("nosleep_errors");

export const options = {
  stages: [
    { duration: "1m", target: 10 },
    { duration: "1m", target: 30 },
    { duration: "1m", target: 60 },
    { duration: "1m", target: 100 },
    { duration: "1m", target: 150 },
    { duration: "1m", target: 200 },
    { duration: "1m", target: 300 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    "nosleep_success": ["rate>0.8"],
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
  return null;
}

export default function () {
  const token = getToken();
  if (!token) { counterErrors.add(1); rateSuccess.add(false); return; }

  // 每轮必做的 tree 请求
  const t1 = Date.now();
  const r1 = http.get(BASE_URL + "/api/v1/note/tree", {
    headers: { Authorization: "Bearer " + token },
  });
  trendTree.add(Date.now() - t1);
  rateSuccess.add(r1.status === 200);
  if (r1.status !== 200) counterErrors.add(1);

  // 一定概率做 pageList
  if (Math.random() < 0.5) {
    const page = Math.floor(Math.random() * 3) + 1;
    const t2 = Date.now();
    const r2 = http.get(BASE_URL + "/api/v1/note/page?pageNum=" + page + "&pageSize=20", {
      headers: { Authorization: "Bearer " + token },
    });
    trendPageList.add(Date.now() - t2);
    rateSuccess.add(r2.status === 200);
    if (r2.status !== 200) counterErrors.add(1);
  }

  // 一定概率查详情
  if (Math.random() < 0.2) {
    const noteId = Math.floor(Math.random() * 500) + 1;
    const t3 = Date.now();
    const r3 = http.get(BASE_URL + "/api/v1/note/" + noteId, {
      headers: { Authorization: "Bearer " + token },
    });
    trendDetail.add(Date.now() - t3);
    rateSuccess.add(r3.status === 200 || r3.status === 400);
    if (r3.status !== 200 && r3.status !== 400) counterErrors.add(1);
  }
  // 注意：无 sleep！
}

export function handleSummary(data) {
  const m = data.metrics;
  const g = (name, stat) => m[name] ? m[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(1) + "ms" : "N/A";
  const reqs = g("nosleep_tree_duration", "count") || 0;
  return {
    stdout: [
      "=== No-Sleep Stress Results ===",
      "Tree:       P50=" + fmt(g("nosleep_tree_duration", "med")) + " P95=" + fmt(g("nosleep_tree_duration", "p(95)")) + " P99=" + fmt(g("nosleep_tree_duration", "p(99)")),
      "PageList:   P50=" + fmt(g("nosleep_page_list_duration", "med")) + " P95=" + fmt(g("nosleep_page_list_duration", "p(95)")),
      "Detail:     P50=" + fmt(g("nosleep_detail_duration", "med")) + " P95=" + fmt(g("nosleep_detail_duration", "p(95)")),
      "SuccessRate:" + (g("nosleep_success", "rate") != null ? (g("nosleep_success", "rate") * 100).toFixed(1) : "N/A") + "%",
      "TotalReqs:  " + reqs,
      "VUs max:    " + (m["vus_max"]?.values?.max || "N/A"),
    ].join("\n"),
  };
}

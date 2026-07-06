// Note-lite AI Chat SSE — 并发测试脚本
// 用例1: 基准 | 用例2/4/5: QUESTION_MODE=short 省 Token
// Usage:
//   用例1: k6 run ... -e QUESTION_MODE=normal
//   用例2: k6 run ... -e QUESTION_MODE=short
import http from "k6/http";
import { Trend, Counter, Rate } from "k6/metrics";

const sseFirstToken = new Trend("sse_first_token", true);
const sseComplete    = new Trend("sse_complete", true);
const sseTokensPerSec = new Trend("sse_tokens_per_sec", true);
const sseSuccess     = new Rate("sse_success");
const sseErrors      = new Counter("sse_errors");

export const options = {
  stages: [
    // 用例2: 阶梯寻顶 10→30→50→70→90→110→130 VUs
    { duration: "1m",  target: 10 },
    { duration: "2m",  target: 10 },
    { duration: "30s", target: 30 },
    { duration: "2m",  target: 30 },
    { duration: "30s", target: 50 },
    { duration: "2m",  target: 50 },
    { duration: "30s", target: 70 },
    { duration: "2m",  target: 70 },
    { duration: "30s", target: 90 },
    { duration: "2m",  target: 90 },
    { duration: "30s", target: 110 },
    { duration: "2m",  target: 110 },
    { duration: "30s", target: 130 },
    { duration: "2m",  target: 130 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    // 用例2 放宽阈值：允许高负载下部分失败和延迟升高
    "sse_success": ["rate>0.90"],
    "sse_first_token": ["p(95)<6000"],   // 基线 1509ms × 4 ≈ 6000ms
    "sse_complete": ["p(95)<60000"],
  },
};

const BASE_URL   = __ENV.BASE_URL   || "http://localhost:8080";
const USERNAME   = __ENV.USERNAME   || "perf_concur_01";
const PASSWORD   = __ENV.PASSWORD   || "Test123456";
const PROFILE_ID = __ENV.PROFILE_ID || "7";
const QUESTION_MODE = __ENV.QUESTION_MODE || "normal";
const DEBUG = __ENV.DEBUG === "true";

// 方案要求：短/中/长三种长度的问题池
const QUESTION_POOL = [
  { text: "介绍一下这个项目的架构",             type: "short",  expectTokens: 150 },
  { text: "RAG 流程是怎么工作的",               type: "medium", expectTokens: 400 },
  { text: "详细说明数据库设计和缓存策略",          type: "long",  expectTokens: 800 },
];

function getToken() {
  const res = http.post(BASE_URL + "/api/v1/user/login", JSON.stringify({
    username: USERNAME, password: PASSWORD,
  }), { headers: { "Content-Type": "application/json" } });
  try {
    const body = JSON.parse(res.body);
    return (body.code === 200 && body.data && body.data.token) ? body.data.token : null;
  } catch (e) {
    return null;
  }
}

function parseSSEBody(body) {
  var tokenCount = 0;
  if (!body) { if (DEBUG && __ITER < 3) console.log("PARSE: body is null/empty"); return 0; }

  var s = String(body);
  if (s.length < 10) { if (DEBUG && __ITER < 3) console.log("PARSE: body too short, len=" + s.length + " content=[" + s + "]"); return 0; }

  // SSE 行用 \n\n 分隔事件，每行 data: {...}
  var lines = s.split("\n");
  if (DEBUG && __ITER < 3) console.log("PARSE: bodyLen=" + s.length + " lines=" + lines.length + " first80=" + s.substring(0, 80));

  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    if (line.indexOf("data: ") !== 0 && line.indexOf("data:") !== 0) continue;
    var payload = (line.charAt(5) === " ") ? line.substring(6).trim() : line.substring(5).trim();
    if (payload === "" || payload === "[DONE]") continue;
    try {
      var json = JSON.parse(payload);
      if (json.done === true || json.done === "true") continue;
      var content = json.token || json.content || "";
      if (DEBUG && __ITER < 3 && tokenCount === 0 && content.length > 0) {
        console.log("PARSE_OK: token=[" + content + "]");
      }
      tokenCount += content.length;
    } catch (e) {
      if (DEBUG && __ITER < 3) console.log("PARSE_JSON_ERR: " + String(e).substring(0, 60) + " payload=" + payload.substring(0, 60));
    }
  }
  if (DEBUG && __ITER < 3) console.log("PARSE_RESULT: tokenCount=" + tokenCount);
  return tokenCount;
}

export default function () {
  const token = getToken();
  if (!token) { sseErrors.add(1); sseSuccess.add(false); return; }

  const questions = QUESTION_MODE === "short"
    ? [{ text: "Hi", type: "short", expectTokens: 5 }]
    : QUESTION_POOL;
  const q = questions[__ITER % questions.length];
  const question = q.text;

  const totalStart = Date.now();

  const res = http.post(BASE_URL + "/api/v1/chat", JSON.stringify({
    question: question,
    scopeType: "ALL",
    style: "concise",
    profileId: parseInt(PROFILE_ID),
    modelName: "qwen-plus",
  }), {
    headers: {
      Authorization: "Bearer " + token,
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    timeout: 120000,
    responseType: "text",
  });

  const totalElapsed = Date.now() - totalStart;

  if (res.status === 200) {
    sseSuccess.add(true);
    sseComplete.add(totalElapsed);
    sseFirstToken.add(res.timings.waiting);

    const rawBody = res.body || "";

    // 调试模式：首次迭代打印原始 SSE 响应（前 2000 字符）
    if (DEBUG && __ITER === 0) {
      console.log("=== DEBUG: RAW SSE (first 2000 chars) ===");
      console.log(rawBody.substring(0, 2000));
      console.log("=== DEBUG END ===");
    }

    const tokenCount = parseSSEBody(rawBody);
    if (tokenCount > 0 && totalElapsed > 0) {
      sseTokensPerSec.add(tokenCount / (totalElapsed / 1000));
    }

    console.log(`[${__ITER}] ${q.type} | TTFB: ${res.timings.waiting}ms | Tokens: ~${tokenCount} | Total: ${totalElapsed}ms`);
  } else {
    sseSuccess.add(false);
    sseErrors.add(1);
    console.error(`Chat failed: HTTP ${res.status}`);
  }
}

export function handleSummary(data) {
  const g = (name, stat) => data.metrics[name] ? data.metrics[name].values[stat] : null;
  const fmt = (v) => v != null ? Number(v).toFixed(0) + "ms" : "N/A";
  const successRate = g("sse_success", "rate");

  return {
    stdout: [
      "╔══════════════════════════════════════════╗",
      "║  AI Chat SSE - Baseline (用例1)          ║",
      "╚══════════════════════════════════════════╝",
      "",
      "  TTFB P50:         " + fmt(g("sse_first_token", "med")),
      "  TTFB P95:         " + fmt(g("sse_first_token", "p(95)")),
      "  TTFB P99:         " + fmt(g("sse_first_token", "p(99)")),
      "  流完成 P50:        " + fmt(g("sse_complete", "med")),
      "  流完成 P95:        " + fmt(g("sse_complete", "p(95)")),
      "  流完成 P99:        " + fmt(g("sse_complete", "p(99)")),
      "  Token速率 avg:     " + (g("sse_tokens_per_sec", "avg") != null ? Number(g("sse_tokens_per_sec", "avg")).toFixed(1) + " chars/s" : "N/A"),
      "  成功率:            " + (successRate != null ? (successRate * 100).toFixed(1) : "N/A") + "%",
      "  错误数:            " + (g("sse_errors", "count") || 0),
      "",
      "  Grafana (http://localhost:3001):",
      "    SSE Active → 必须归零",
      "    JVM Heap   → 锯齿形平稳",
      "    HikariCP   → 不超过 2",
    ].join("\n"),
  };
}

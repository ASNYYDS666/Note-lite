// Note-lite 用例3: 长文本线程饥饿 (Slow Consumer)
// 5 VU 长请求 (2万字prompt) + 3 VU 短请求 ("Hi") 对照组
// 验证：长请求是否钉住 chatExecutor 线程，拖慢短请求
// Usage:
//   k6 run perf/scripts/concurrency-chat-longtext.js \
//     -e USERNAME=perf_concur_01 -e PASSWORD=Test123456 -e PROFILE_ID=7
import http from "k6/http";
import { Trend, Counter, Rate } from "k6/metrics";

const sseFirstToken = new Trend("sse_first_token", true);
const sseComplete    = new Trend("sse_complete", true);
const sseTokensPerSec = new Trend("sse_tokens_per_sec", true);
const sseSuccess     = new Rate("sse_success");
const sseErrors      = new Counter("sse_errors");

// 场景标签区分长短请求
const sseFirstToken_short = new Trend("sse_first_token_short", true);
const sseFirstToken_long  = new Trend("sse_first_token_long", true);
const sseComplete_short   = new Trend("sse_complete_short", true);
const sseComplete_long    = new Trend("sse_complete_long", true);

export const options = {
  scenarios: {
    // 短请求组（对照组）：持续循环 10 分钟
    short_requests: {
      executor: "constant-vus",
      vus: 3,
      duration: "10m",
      exec: "shortRequest",
      startTime: "0s",
    },
    // 长请求组：2 分钟后一次性发出 5 个长请求
    long_requests: {
      executor: "per-vu-iterations",
      vus: 5,
      iterations: 1,
      exec: "longRequest",
      startTime: "2m",        // 预热 2 分钟后才发出
      maxDuration: "5m",      // 留给长请求 5 分钟完成
    },
  },
  thresholds: {
    "sse_success": ["rate>0.95"],
    "sse_first_token_short": ["p(95)<4000"],  // 短请求 TTFB 应合理
  },
};

const BASE_URL   = __ENV.BASE_URL   || "http://localhost:8080";
const USERNAME   = __ENV.USERNAME   || "perf_concur_01";
const PASSWORD   = __ENV.PASSWORD   || "Test123456";
const PROFILE_ID = __ENV.PROFILE_ID || "7";

// 生成约 2 万字的中文长文本（模拟合同审阅场景）
function generateLongText() {
  const template = `
请仔细审阅以下合同条款，从法律风险、商业合理性和合规性三个维度进行分析总结：

========================================
合同编号：HT-2026-0615-XS-${Math.floor(Math.random() * 10000)}
签订日期：2026年6月15日
甲方：星辰科技有限公司
乙方：青鸟云计算服务有限公司

第一条 服务内容与范围
1.1 乙方向甲方提供云计算基础设施服务，包括但不限于：弹性计算服务（ECS）、对象存储服务（OSS）、内容分发网络（CDN）、负载均衡（SLB）、关系型数据库（RDS）、缓存服务（Redis）、消息队列（MQ）、日志服务（SLS）、监控服务（CMS）、容器服务（ACK）。
1.2 服务等级协议（SLA）约定月度可用性不低于99.95%。若月度可用性低于该标准，乙方应按不可用时长向甲方赔偿，赔偿金额为当月服务费的10%乘以不可用时长占比。月度可用性低于99.0%的，甲方有权单方解除合同并要求乙方退还已预付的全部费用。
1.3 乙方应提供7×24小时技术支持服务，响应时间不超过15分钟，故障恢复时间不超过2小时。如因乙方原因导致故障恢复时间超过4小时，每次应向甲方支付违约金人民币5万元。
1.4 服务范围不包括：甲方自行开发的应用程序故障、甲方员工操作失误导致的问题、第三方提供的软件或服务引发的故障、不可抗力事件。

第二条 服务费用与支付
2.1 本合同服务费采用月付制，月服务费为人民币28万元整（含税）。甲方应于每月5日前支付当月服务费。
2.2 如甲方逾期支付，每逾期一日应按未付金额的千分之三向乙方支付违约金。逾期超过15日的，乙方有权暂停服务。逾期超过30日的，乙方有权解除合同。
2.3 服务费包含基础技术支持费用。如需定制化开发或驻场服务，双方另行签订补充协议并另行计费。
2.4 本合同有效期内，如因市场原因乙方调整标准定价，乙方应提前30日书面通知甲方。价格下调的，甲方自动享受调整后的价格；价格上调的，需经甲方书面同意。

第三条 数据安全与隐私保护
3.1 乙方应采取不低于行业标准的安全措施保护甲方数据，包括但不限于：数据传输加密（TLS 1.3）、存储加密（AES-256）、访问控制（RBAC）、操作审计（完整日志记录）、定期漏洞扫描与渗透测试、数据备份（每日全量+实时增量）。
3.2 乙方不得以任何形式访问、使用、复制、修改、披露甲方的业务数据，除非获得甲方事先书面授权或法律法规强制要求。如乙方违反本条款，应向甲方支付违约金人民币200万元，并赔偿甲方全部损失。
3.3 发生数据安全事件时，乙方应在发现后2小时内通知甲方，并在24小时内提交书面事故报告。乙方应在72小时内完成应急处置并消除安全隐患。
3.4 甲方有权委托独立第三方机构对乙方的安全措施进行年度审计，审计费用由甲方承担。乙方应予以配合并提供必要的访问权限。
3.5 合同终止或到期后，乙方应在30日内彻底删除甲方所有数据，并提供数据删除证明。如需提前删除，乙方应在收到甲方书面通知后5个工作日内完成。

第四条 知识产权
4.1 甲方在使用乙方服务过程中产生的所有数据、配置信息、应用程序代码、业务逻辑等知识产权均归甲方所有。
4.2 乙方提供的软件、平台、工具、API接口等知识产权归乙方所有。甲方不得进行反向工程、反编译或反汇编。
4.3 双方在履行本合同过程中各自独立开发的技术成果，知识产权归开发方所有。双方合作开发的技术成果，知识产权由双方共有，任何一方不得单独许可第三方使用。

第五条 保密条款
5.1 双方应对在合作过程中获知的对方商业秘密、技术秘密和其他保密信息承担保密义务。保密期限自获知保密信息之日起至该信息进入公有领域之日止，但不得少于合同终止后5年。
5.2 未经对方书面同意，任何一方不得向第三方披露保密信息。但法律法规、司法机关或监管机构要求披露的除外，披露方应在披露前及时通知对方。

第六条 违约责任
6.1 任何一方违反本合同约定，应向对方赔偿因此造成的直接经济损失。间接损失、预期利益损失、商誉损失等不在赔偿范围内，但因故意或重大过失造成的除外。
6.2 如因乙方原因导致服务中断且未在约定时间内恢复，乙方应按实际中断时长的10倍向甲方补偿服务时长。
6.3 甲方不得利用乙方服务从事任何违法违规活动，包括但不限于：传播违法信息、进行网络攻击、侵犯他人知识产权。如甲方违反本条款，乙方有权立即终止服务且不退还任何费用，甲方应赔偿乙方因此遭受的全部损失。

第七条 合同期限与终止
7.1 本合同有效期为3年，自双方盖章之日起生效。合同到期前60日，如双方均未提出异议，合同自动续期1年。
7.2 任何一方发生破产、清算、解散、被吊销营业执照等情形时，另一方可立即解除本合同。
7.3 合同解除后，双方应在15个工作日内完成费用结算和数据迁移。乙方应提供必要的协助确保甲方数据完整迁移。

第八条 争议解决
8.1 双方因本合同产生的争议，应首先通过友好协商解决。协商不成的，任何一方可向甲方所在地有管辖权的人民法院提起诉讼。
8.2 争议解决期间，除争议事项外，双方应继续履行本合同其他条款。

第九条 其他约定
9.1 本合同一式两份，双方各执一份，具有同等法律效力。
9.2 本合同的任何修改、补充均应以书面形式经双方盖章确认。
9.3 本合同未尽事宜，双方可另行签订补充协议，补充协议与本合同具有同等法律效力。

甲方（盖章）：星辰科技有限公司
授权代表（签字）：_________
日期：2026年6月15日

乙方（盖章）：青鸟云计算服务有限公司
授权代表（签字）：_________
日期：2026年6月15日
`.repeat(4);  // 重复4次 ≈ 2万字

  return template.substring(0, 20000);
}

const LONG_TEXT = generateLongText();

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

function parseSSEBody(body, label) {
  var tokenCount = 0;
  if (!body) return 0;
  var s = String(body);
  if (s.length < 10) { console.log("PARSE_" + label + ": body too short, len=" + s.length); return 0; }

  // 检查是否为错误响应
  if (s.indexOf('"error"') >= 0) {
    console.log("PARSE_" + label + ": ERROR response body=[" + s.substring(0, 200) + "]");
  }

  var lines = s.split("\n");
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    if (line.indexOf("data:") !== 0) continue;
    var payload = (line.charAt(5) === " ") ? line.substring(6).trim() : line.substring(5).trim();
    if (payload === "" || payload === "[DONE]") continue;
    try {
      var json = JSON.parse(payload);
      if (json.done) continue;
      var content = json.token || json.content || "";
      tokenCount += content.length;
    } catch (e) {}
  }
  if (tokenCount === 0 && s.length > 10) {
    console.log("PARSE_" + label + ": ZERO tokens, bodyLen=" + s.length + " first200=[" + s.substring(0, 200) + "]");
  }
  return tokenCount;
}

function doChat(question, label) {
  var totalStart = Date.now();
  var res = http.post(BASE_URL + "/api/v1/chat", JSON.stringify({
    question: question,
    scopeType: "ALL",
    style: "concise",
    profileId: parseInt(PROFILE_ID),
    modelName: "qwen-plus",
  }), {
    headers: {
      Authorization: "Bearer " + getToken(),
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    timeout: 300000,   // 5 分钟超时（长文本可能需要 2-3 分钟）
    responseType: "text",
  });

  var totalElapsed = Date.now() - totalStart;
  if (res.status === 200) {
    sseSuccess.add(true);
    sseComplete.add(totalElapsed);
    sseFirstToken.add(res.timings.waiting);

    if (label === "short") {
      sseComplete_short.add(totalElapsed);
      sseFirstToken_short.add(res.timings.waiting);
    } else {
      sseComplete_long.add(totalElapsed);
      sseFirstToken_long.add(res.timings.waiting);
    }

    var tokenCount = parseSSEBody(res.body || "", label);
    if (tokenCount > 0 && totalElapsed > 0) {
      sseTokensPerSec.add(tokenCount / (totalElapsed / 1000));
    }
    console.log("[" + label + "] TTFB: " + res.timings.waiting.toFixed(0) + "ms | Tokens: ~" + tokenCount + " | Total: " + totalElapsed + "ms");
  } else {
    sseSuccess.add(false);
    sseErrors.add(1);
    console.error("[" + label + "] FAIL: HTTP " + res.status);
  }
}

// 短请求（对照组 VU 执行）
export function shortRequest() {
  doChat("Hi", "short");
}

// 长请求（一次性 VU 执行）
export function longRequest() {
  var question = "请作为资深法务顾问，审阅以下合同并给出专业意见：\n\n" + LONG_TEXT;
  doChat(question, "long");
}

export function handleSummary(data) {
  var g = function(name, stat) {
    return data.metrics[name] ? data.metrics[name].values[stat] : null;
  };
  var fmt = function(v) { return v != null ? Number(v).toFixed(0) + "ms" : "N/A"; };
  var successRate = g("sse_success", "rate");

  var shortTTFB_p50 = fmt(g("sse_first_token_short", "med"));
  var shortTTFB_p95 = fmt(g("sse_first_token_short", "p(95)"));
  var shortComp_p50 = fmt(g("sse_complete_short", "med"));
  var shortComp_p95 = fmt(g("sse_complete_short", "p(95)"));
  var longTTFB_p50  = fmt(g("sse_first_token_long", "med"));
  var longTTFB_p95  = fmt(g("sse_first_token_long", "p(95)"));
  var longComp_p50  = fmt(g("sse_complete_long", "med"));
  var longComp_p95  = fmt(g("sse_complete_long", "p(95)"));

  return {
    stdout: [
      "╔══════════════════════════════════════════╗",
      "║  用例3: 长文本线程饥饿 (Slow Consumer)   ║",
      "╚══════════════════════════════════════════╝",
      "",
      "  短请求 TTFB P50/P95:  " + shortTTFB_p50 + " / " + shortTTFB_p95,
      "  短请求 完成 P50/P95:  " + shortComp_p50 + " / " + shortComp_p95,
      "  长请求 TTFB P50/P95:  " + longTTFB_p50 + " / " + longTTFB_p95,
      "  长请求 完成 P50/P95:  " + longComp_p50 + " / " + longComp_p95,
      "  成功率:              " + (successRate != null ? (successRate * 100).toFixed(1) : "N/A") + "%",
      "  错误数:              " + (g("sse_errors", "count") || 0),
      "",
      "  Grafana 检查:",
      "    chatExecutor 活跃 ≤ 8 (5长+3短)",
      "    chatExecutor 队列 ≤ 5",
      "    HikariCP pending = 0 (硬底线)",
      "    GC 暂停 P99 ≤ 500ms",
    ].join("\n"),
  };
}

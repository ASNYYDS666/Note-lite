// Note-lite RAG Retrieval Quality Evaluator
const http = require("http");
const https = require("https");
const fs = require("fs");
const path = require("path");

function parseArgs() {
  var args = process.argv.slice(2);
  var opts = {
    baseUrl: "http://localhost:8080",
    username: null, password: null, token: null, profileId: null,
    modelName: null,
    delay: 3000,
    goldenFile: path.join(__dirname, "golden-questions.json"),
    outputFile: path.join(__dirname, "quality-results.json"),
  };
  for (var i = 0; i < args.length; i++) {
    var v = args[i + 1];
    if (args[i] === "--base-url") { opts.baseUrl = v; i++; }
    else if (args[i] === "--username") { opts.username = v; i++; }
    else if (args[i] === "--password") { opts.password = v; i++; }
    else if (args[i] === "--token") { opts.token = v; i++; }
    else if (args[i] === "--profileId") { opts.profileId = parseInt(v); i++; }
    else if (args[i] === "--modelName") { opts.modelName = v; i++; }
    else if (args[i] === "--delay") { opts.delay = parseInt(v); i++; }
    else if (args[i] === "--golden-file") { opts.goldenFile = v; i++; }
    else if (args[i] === "--output") { opts.outputFile = v; i++; }
  }
  return opts;
}

function httpPost(baseUrl, urlPath, body, headers) {
  headers = headers || {};
  return new Promise(function(resolve, reject) {
    var url = new URL(urlPath, baseUrl);
    var lib = url.protocol === "https:" ? https : http;
    var bodyStr = JSON.stringify(body);
    var req = lib.request({
      hostname: url.hostname, port: url.port, path: url.pathname,
      method: "POST",
      headers: Object.assign({ "Content-Type": "application/json" }, headers),
    }, function(res) {
      var data = "";
      res.on("data", function(c) { data += c; });
      res.on("end", function() {
        try { resolve({ status: res.statusCode, body: JSON.parse(data) }); }
        catch (e) { resolve({ status: res.statusCode, body: data }); }
      });
    });
    req.on("error", reject);
    req.write(bodyStr);
    req.end();
  });
}

async function login(baseUrl, username, password) {
  console.log("[Login] " + username + " ...");
  var res = await httpPost(baseUrl, "/api/v1/user/login", { username: username, password: password });
  if (res.body && res.body.code === 200) {
    console.log("[Login] OK, userId=" + res.body.data.userId);
    return res.body.data.token;
  }
  throw new Error("Login failed: " + JSON.stringify(res.body));
}

function checkHit(answer, expectedKeywords, minMatchRatio) {
  var lower = answer.toLowerCase();
  var matched = [];
  var missed = [];
  for (var i = 0; i < expectedKeywords.length; i++) {
    var kw = expectedKeywords[i];
    if (lower.indexOf(kw.toLowerCase()) !== -1) { matched.push(kw); }
    else { missed.push(kw); }
  }
  var ratio = matched.length / expectedKeywords.length;
  return { hit: ratio >= minMatchRatio, matched: matched, missed: missed, ratio: ratio, minMatchRatio: minMatchRatio };
}

function chatSSE(baseUrl, token, question, profileId, modelName) {
  return new Promise(function(resolve, reject) {
    var url = new URL("/api/v1/chat", baseUrl);
    var lib = url.protocol === "https:" ? https : http;
    var payload = {
      question: question, scopeType: "ALL", style: "concise", profileId: profileId
    };
    if (modelName) { payload.modelName = modelName; }
    payload = JSON.stringify(payload);
    var req = lib.request({
      hostname: url.hostname, port: url.port, path: url.pathname, method: "POST",
      headers: {
        "Authorization": "Bearer " + token,
        "Content-Type": "application/json",
        "Accept": "text/event-stream",
      },
      timeout: 120000,
    }, function(res) {
      if (res.statusCode !== 200) {
        var errBody = "";
        res.on("data", function(c) { errBody += c; });
        res.on("end", function() { reject(new Error("HTTP " + res.statusCode + ": " + errBody)); });
        return;
      }
      var fullResponse = "";
      var buffer = "";
      res.on("data", function(chunk) {
        buffer += chunk.toString();
        var lines = buffer.split("\n");
        buffer = lines.pop() || "";
        for (var i = 0; i < lines.length; i++) {
          var line = lines[i];
          if (line.indexOf("data:") === 0) {
            try {
              var data = JSON.parse(line.slice(5));
              if (data.error) { reject(new Error("AI error: " + data.error)); return; }
              if (data.token && !data.thinking) { fullResponse += data.token; }
            } catch (e) { /* skip unparseable line */ }
          }
        }
      });
      res.on("end", function() { resolve(fullResponse); });
      res.on("error", reject);
    });
    req.on("error", reject);
    req.on("timeout", function() { req.destroy(); reject(new Error("Request timeout")); });
    req.write(payload);
    req.end();
  });
}

async function main() {
  var opts = parseArgs();
  if (!opts.profileId) {
    console.error("ERROR: --profileId is required");
    console.error("Usage: node perf/quality/evaluate.js --token <JWT> --profileId <ID>");
    process.exit(1);
  }
  var token = opts.token;
  if (!token) {
    if (!opts.username || !opts.password) {
      console.error("ERROR: need --token or (--username + --password)");
      process.exit(1);
    }
    token = await login(opts.baseUrl, opts.username, opts.password);
  }
  console.log("");
  console.log("Reading: " + opts.goldenFile);
  var testCases = JSON.parse(fs.readFileSync(opts.goldenFile, "utf8"));
  // Normalize field names: support both long (v1) and short (v2/v3) formats
  for (var ti = 0; ti < testCases.length; ti++) {
    var tc = testCases[ti];
    if (!tc.question && tc.q) { tc.question = tc.q; }
    if (!tc.category && tc.cat) { tc.category = tc.cat; }
    if (!tc.expectedKeywords && tc.kw) { tc.expectedKeywords = tc.kw; }
    if (!tc.minMatchRatio && tc.minR !== undefined) { tc.minMatchRatio = tc.minR; }
  }
  console.log("  " + testCases.length + " test cases");
  console.log("");
  var results = [];
  var totalStart = Date.now();
  for (var i = 0; i < testCases.length; i++) {
    var tc = testCases[i];
    console.log("=".repeat(60));
    console.log("[" + tc.id + "] " + tc.category);
    console.log("  Q: " + tc.question);
    console.log("  Expected keywords: " + tc.expectedKeywords.join(", "));
    var caseStart = Date.now();
    var answer = "";
    var error = null;
    try {
      answer = await chatSSE(opts.baseUrl, token, tc.question, opts.profileId, opts.modelName);
    } catch (e) {
      error = e.message;
      console.log("  [ERROR] " + e.message);
    }
    var latency = Date.now() - caseStart;
    var check;
    if (error) {
      check = { hit: false, matched: [], missed: tc.expectedKeywords, ratio: 0, minMatchRatio: tc.minMatchRatio };
    } else {
      check = checkHit(answer, tc.expectedKeywords, tc.minMatchRatio);
    }
    var icon = check.hit ? "PASS" : "FAIL";
    console.log("  AI: " + answer.substring(0, 150) + (answer.length > 150 ? "..." : ""));
    console.log("  Match: " + check.matched.length + "/" + tc.expectedKeywords.length + " => " + icon);
    console.log("  Found: [" + check.matched.join(", ") + "]  Missing: [" + check.missed.join(", ") + "]");
    console.log("  Latency: " + latency + "ms");
    results.push({
      id: tc.id, question: tc.question, category: tc.category,
      hit: check.hit, matchedCount: check.matched.length, expectedCount: tc.expectedKeywords.length,
      matchRatio: check.ratio.toFixed(2), matched: check.matched, missed: check.missed,
      answerPreview: answer.substring(0, 200), fullAnswer: answer, latencyMs: latency, error: error
    });
    if (i < testCases.length - 1) {
      console.log("  (waiting " + opts.delay + "ms ...)");
      await new Promise(function(r) { setTimeout(r, opts.delay); });
    }
  }
  var totalDuration = Date.now() - totalStart;
  var total = results.length;
  var hitCount = results.filter(function(r) { return r.hit; }).length;
  var byCategory = {};
  results.forEach(function(r) {
    if (!byCategory[r.category]) { byCategory[r.category] = { total: 0, hit: 0 }; }
    byCategory[r.category].total++;
    if (r.hit) { byCategory[r.category].hit++; }
  });
  console.log("");
  console.log("=".repeat(60));
  console.log("  RAG Retrieval Quality Report");
  console.log("=".repeat(60));
  console.log("  Total Questions: " + total);
  console.log("  Hits:            " + hitCount);
  console.log("  Hit Rate:        " + (hitCount / total * 100).toFixed(1) + "%");
  console.log("  Total Duration:  " + (totalDuration / 1000).toFixed(1) + "s");
  console.log("");
  console.log("  By Category:");
  var cats = Object.keys(byCategory);
  for (var ci = 0; ci < cats.length; ci++) {
    var cat = cats[ci];
    var stats = byCategory[cat];
    var pct = (stats.hit / stats.total * 100).toFixed(1);
    var bar = "=".repeat(Math.round(stats.hit / stats.total * 20));
    console.log("  " + cat + "  " + bar + "  " + stats.hit + "/" + stats.total + " (" + pct + "%)");
  }
  console.log("");
  console.log("  Details:");
  console.log("  ID     Category         Result  Rate    Latency");
  console.log("  " + "-".repeat(50));
  for (var ri = 0; ri < results.length; ri++) {
    var r = results[ri];
    var s = r.hit ? "PASS" : "FAIL";
    var p = (r.matchRatio * 100).toFixed(0) + "%";
    console.log("  " + r.id + "  " + r.category + "  " + s + "  " + p + "  " + r.latencyMs + "ms");
  }
  var report = {
    summary: {
      total: total, hitCount: hitCount,
      hitRate: (hitCount / total * 100).toFixed(1) + "%",
      totalDurationMs: totalDuration,
      byCategory: byCategory,
      timestamp: new Date().toISOString(),
    },
    results: results,
  };
  fs.writeFileSync(opts.outputFile, JSON.stringify(report, null, 2), "utf8");
  console.log("");
  console.log("  Report saved: " + opts.outputFile);
  process.exit(hitCount === total ? 0 : 1);
}

main().catch(function(e) {
  console.error("Fatal:", e);
  process.exit(2);
});

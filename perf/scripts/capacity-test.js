// ============================================================
// Note-lite 容量测试脚本
// 测什么：数据量增长后，各接口响应时间的变化
// 怎么测：1 个用户、无并发、重复跑 20 次取 P50/P95/P99
//
// 用法：
//   k6 run perf/scripts/capacity-test.js \
//     -e BASE_URL=http://localhost:8080 \
//     -e USERNAME=perf_cap_10000 \
//     -e PASSWORD=Test123456
// ============================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';

// ==================== 自定义指标 ====================
// 每个接口单独一个 Trend，便于看到底是哪个接口慢
const treeDuration = new Trend('capacity_tree_duration', true);
const pageListDuration = new Trend('capacity_page_list_duration', true);
const noteDetailDuration = new Trend('capacity_note_detail_duration', true);
const loginDuration = new Trend('capacity_login_duration', true);

// ==================== 测试配置 ====================
export const options = {
  iterations: 20,          // 每个 VU 跑 20 遍（取统计值）
  vus: 1,                  // 单用户，排除并发干扰
  duration: '5m',          // 最大时长
  thresholds: {
    // 阈值仅用于记录，不中断测试
    'capacity_tree_duration': ['p(95)<5000'],
  },
  noConnectionReuse: false, // 复用 TCP 连接
};

// ==================== 环境变量 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'perf_cap_100';
const PASSWORD = __ENV.PASSWORD || 'Test123456';

// 全局缓存的 token（测试用户固定，只登录一次）
let cachedToken = null;
let cachedNoteId = null;

// ==================== 工具函数 ====================
function getToken() {
  if (cachedToken) return cachedToken;

  const loginStart = Date.now();
  const res = http.post(`${BASE_URL}/api/v1/user/login`, JSON.stringify({
    username: USERNAME,
    password: PASSWORD,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  loginDuration.add(Date.now() - loginStart);

  const body = JSON.parse(res.body);

  if (body.code === 200 && body.data && body.data.token) {
    cachedToken = body.data.token;
    console.log(`✅ 登录成功: ${USERNAME}, userId=${body.data.userId}`);
    return cachedToken;
  }

  console.error(`❌ 登录失败: ${res.status} ${res.body}`);
  return null;
}

function authHeaders() {
  return {
    'Authorization': `Bearer ${getToken()}`,
    'Content-Type': 'application/json',
  };
}

// ==================== 测试用例 ====================
export default function () {
  const token = getToken();
  if (!token) return;

  // ─── 测试1: 笔记树加载 ───
  // 这是容量测试的"核心战场"——getNoteTree() 无分页，
  // 10000 条笔记会一次性全查出来，最可能在这崩
  group('C1-笔记树加载', () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/v1/note/tree`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    const duration = Date.now() - start;
    treeDuration.add(duration);

    const ok = check(res, {
      '树-200': (r) => r.status === 200,
    });

    if (ok) {
      const body = JSON.parse(res.body);
      if (body.data) {
        const folderCount = body.data.folders ? body.data.folders.length : 0;
        const noteCount = body.data.notes ? body.data.notes.length : 0;
        console.log(`  树加载: ${duration}ms, 文件夹=${folderCount}, 笔记=${noteCount}`);
      }
    } else {
      console.error(`  树加载失败: HTTP ${res.status}, ${res.body.substring(0, 200)}`);
    }
  });

  sleep(0.3);

  // ─── 测试2: 分页列表 ───
  // simplePageQuery() 有分页，理论上数据量不影响单页性能
  // 但"批量装标签"那一步用 IN 查询，数据多了可能慢
  group('C2-分页列表', () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/v1/note/page?pageNum=1&pageSize=20`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    const duration = Date.now() - start;
    pageListDuration.add(duration);

    const ok = check(res, {
      '列表-200': (r) => r.status === 200,
    });

    if (ok) {
      const body = JSON.parse(res.body);
      const total = body.data ? body.data.total : 0;
      console.log(`  分页列表: ${duration}ms, 总笔记数=${total}`);
    }
  });

  sleep(0.3);

  // ─── 测试3: 笔记详情（单条查询） ───
  // 有 Redis 缓存，理论上 O(1) 不受数据量影响
  // 但如果缓存未命中，走 DB selectById 走主键索引，也是 O(1)
  group('C3-笔记详情', () => {
    // 使用第一条创建的笔记 ID（如果已知）
    // 首次运行未知时随机查一个
    const noteId = cachedNoteId || 1;

    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/v1/note/${noteId}`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    const duration = Date.now() - start;
    noteDetailDuration.add(duration);

    check(res, {
      '详情-200': (r) => r.status === 200 || r.status === 400,
      // 400 也算过（可能笔记ID不存在），我们关心的是响应时间而非内容
    });

    console.log(`  笔记详情: ${duration}ms (noteId=${noteId})`);
  });
}

// ==================== 测试结束，汇总输出 ====================
export function handleSummary(data) {
  // 提取关键指标并格式化输出
  const metrics = data.metrics;

  const summary = {
    testInfo: {
      user: USERNAME,
      baseUrl: BASE_URL,
      timestamp: new Date().toISOString(),
    },
    results: {},
  };

  // 提取每个 Trend 的 P50/P95/P99
  const trends = ['capacity_tree_duration', 'capacity_page_list_duration', 'capacity_note_detail_duration'];
  trends.forEach(name => {
    if (metrics[name]) {
      summary.results[name] = {
        avg: metrics[name].values.avg.toFixed(1) + 'ms',
        min: metrics[name].values.min.toFixed(1) + 'ms',
        med: metrics[name].values.med.toFixed(1) + 'ms',
        p90: metrics[name].values['p(90)'].toFixed(1) + 'ms',
        p95: metrics[name].values['p(95)'].toFixed(1) + 'ms',
        p99: metrics[name].values['p(99)'].toFixed(1) + 'ms',
        max: metrics[name].values.max.toFixed(1) + 'ms',
      };
    }
  });

  return {
    'stdout': `
┌─────────────────────────────────────────────┐
│       Note-lite 容量测试报告                  │
├─────────────────────────────────────────────┤
│ 用户: ${USERNAME.padEnd(33)} │
│  URL: ${BASE_URL.padEnd(33)} │
├─────────────────────────────────────────────┤
│ 指标                    P50    P95    P99    │
├─────────────────────────────────────────────┤
│ 笔记树加载             ${fmt(summary, 'capacity_tree_duration')}
│ 分页列表               ${fmt(summary, 'capacity_page_list_duration')}
│ 笔记详情               ${fmt(summary, 'capacity_note_detail_duration')}
└─────────────────────────────────────────────┘
`,
    'perf/reports/capacity/summary.json': JSON.stringify(summary, null, 2),
  };
}

function fmt(s, name) {
  const r = s.results[name];
  if (!r) return '  N/A    N/A    N/A  ';
  return `${r.med.padStart(6)} ${r.p95.padStart(6)} ${r.p99.padStart(6)}`;
}

#!/bin/bash
# ============================================================
# Note-lite 容量测试 — 数据构造脚本
# 功能：注册测试用户 → 登录获取Token → 批量创建笔记和文件夹
#
# 用法：
#   bash perf/data/seed-capacity.sh 100    # 造 100 条笔记
#   bash perf/data/seed-capacity.sh 1000   # 造 1000 条笔记
#   bash perf/data/seed-capacity.sh 5000   # 造 5000 条笔记
#   bash perf/data/seed-capacity.sh 10000  # 造 10000 条笔记
#
# 每次调用会：注册新用户 + 创建指定数量的笔记 + 文件夹
# 不同量级用不同用户，互不干扰。
# ============================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
NOTE_COUNT="${1:-100}"

# 根据笔记数量决定文件夹数量
if [ "$NOTE_COUNT" -le 100 ]; then
  FOLDER_COUNT=5
elif [ "$NOTE_COUNT" -le 1000 ]; then
  FOLDER_COUNT=20
elif [ "$NOTE_COUNT" -le 5000 ]; then
  FOLDER_COUNT=50
else
  FOLDER_COUNT=100
fi

USERNAME="perf_cap_${NOTE_COUNT}"
PASSWORD="Test123456"
EMAIL="perf_cap_${NOTE_COUNT}@test.local"

echo "=============================================="
echo " Note-lite 容量测试数据构造"
echo " 笔记数量: ${NOTE_COUNT}"
echo " 文件夹数: ${FOLDER_COUNT}"
echo " 测试用户: ${USERNAME}"
echo "=============================================="

# --------------------------------------------------
# Step 1: 注册测试用户（已存在则跳过）
# --------------------------------------------------
echo ""
echo "[1/4] 注册测试用户..."

REGISTER_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"email\":\"${EMAIL}\"}")

HTTP_CODE=$(echo "$REGISTER_RESP" | tail -1)
BODY=$(echo "$REGISTER_RESP" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
  echo "  ✅ 注册成功"
elif [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "409" ]; then
  echo "  ⚠️  用户已存在，跳过注册"
else
  echo "  ❌ 注册失败 (HTTP ${HTTP_CODE}): ${BODY}"
  exit 1
fi

# --------------------------------------------------
# Step 2: 登录获取 Token
# --------------------------------------------------
echo ""
echo "[2/4] 登录获取 Token..."

LOGIN_RESP=$(curl -s -X POST "${BASE_URL}/api/v1/user/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

# 从 JSON 响应中提取 token（不依赖 jq，用 grep + sed）
# 响应格式: {"code":200,"data":{"token":"xxx","userId":123,...}}
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | head -1 | sed 's/"token":"//;s/"//')

if [ -z "$TOKEN" ]; then
  echo "  ❌ 登录失败，无法提取 token"
  echo "  响应: ${LOGIN_RESP}"
  exit 1
fi

echo "  ✅ Token 获取成功: ${TOKEN:0:30}..."

# --------------------------------------------------
# Step 3: 创建文件夹
# --------------------------------------------------
echo ""
echo "[3/4] 创建 ${FOLDER_COUNT} 个文件夹..."

# 先创建一个根文件夹
ROOT_FOLDER_RESP=$(curl -s -X POST "${BASE_URL}/api/v1/note/folder" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=容量测试根目录&parentId=")

ROOT_FOLDER_ID=$(echo "$ROOT_FOLDER_RESP" | grep -o '"data":[0-9]*' | grep -o '[0-9]*')

if [ -z "$ROOT_FOLDER_ID" ]; then
  echo "  ⚠️  根文件夹创建失败，笔记将放在根目录"
  FOLDER_IDS="null"
else
  echo "  ✅ 根文件夹 ID=${ROOT_FOLDER_ID}"
  FOLDER_IDS="${ROOT_FOLDER_ID}"
fi

# 批量创建子文件夹
for i in $(seq 1 $FOLDER_COUNT); do
  FOLDER_NAME="容量测试-目录${i}"
  PARENT_ID="${ROOT_FOLDER_ID}"

  curl -s -X POST "${BASE_URL}/api/v1/note/folder" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "name=${FOLDER_NAME}&parentId=${PARENT_ID}" > /dev/null

  if [ $((i % 20)) -eq 0 ] || [ "$i" -eq "$FOLDER_COUNT" ]; then
    echo "  📁 已创建 ${i}/${FOLDER_COUNT} 个文件夹"
  fi
done

echo "  ✅ 文件夹创建完成"

# --------------------------------------------------
# Step 4: 批量创建笔记
# --------------------------------------------------
echo ""
echo "[4/4] 批量创建 ${NOTE_COUNT} 条笔记..."

# 中文内容模板库（随机组合生成多样化笔记内容）
TITLES=(
  "Java基础语法笔记" "Spring Boot项目搭建记录" "MySQL优化心得" "Redis缓存策略总结"
  "Vue3组件通信方式" "Docker部署踩坑记录" "Git常用命令速查" "Linux服务器配置指南"
  "RESTful API设计规范" "JWT认证流程分析" "微服务架构设计思路" "数据库索引优化实战"
  "前端性能优化方案" "后端接口幂等性设计" "消息队列使用场景" "分布式锁实现对比"
  "单元测试编写指南" "代码审查常见问题" "设计模式应用实例" "系统安全防护策略"
  "CI/CD流水线搭建" "日志收集与分析方案" "监控告警体系设计" "数据备份与恢复方案"
  "高并发系统设计原则" "缓存雪崩解决方案" "接口限流策略对比" "数据库分库分表实践"
)

PARAGRAPHS=(
  "在项目开发过程中，我们经常会遇到性能瓶颈的问题。通过对代码进行分析，我们发现大部分问题都集中在数据库查询上。为了解决这个问题，我们引入了Redis缓存层，将频繁查询的数据缓存起来，减少对数据库的压力。"
  "今天学习了关于Spring Security的配置方式。首先需要继承WebSecurityConfigurerAdapter类，然后重写configure方法。在configure方法中，我们可以配置哪些URL需要认证，哪些URL可以匿名访问。"
  "昨天跟团队成员讨论了关于前端框架选型的问题。最终我们决定采用Vue3 + TypeScript的方案。主要原因是Vue3的Composition API能让代码更易于维护，而TypeScript可以提供更好的类型安全。"
  "对于一个高并发系统来说，数据库连接池的配置至关重要。如果连接池太小，会导致线程等待；如果太大，又会占用过多资源。通常建议将连接池大小设置为CPU核心数的2倍加1。"
  "最近在研究向量数据库的应用场景。Qdrant作为一个高性能的向量搜索引擎，在RAG系统中扮演着关键角色。通过将文档切片并转换为向量，我们可以实现语义级别的相似度搜索。"
  "关于代码质量，我认为最重要的是可读性。一个函数最好只做一件事，命名要清晰表达其意图。注释应该解释"为什么"而不是"做了什么"，因为代码本身已经说明了做了什么。"
  "在部署Spring Boot应用时，我通常会使用Docker容器化。首先编写Dockerfile，将应用打包成镜像，然后通过docker-compose编排多个服务。这样可以保证开发环境和生产环境的一致性。"
  "前端性能优化有几个关键点：减少HTTP请求数量、压缩静态资源、使用CDN加速、代码分割按需加载。特别是对于SPA应用，首屏加载速度直接影响用户体验。"
  "Elasticsearch是一个强大的全文搜索引擎。在项目中我们用它来实现日志搜索和分析功能。通过Filebeat收集日志、Logstash过滤处理、Elasticsearch存储索引、Kibana可视化展示，形成完整的ELK技术栈。"
  "今天遇到了一个内存泄漏的问题。通过使用JProfiler分析堆内存，发现有一个HashMap在不断增长而从未被清理。解决方案是在每次处理完成后，手动清除这个HashMap中的过期数据。"
)

# 使用临时文件记录进度
PROGRESS_FILE="/tmp/seed-capacity-progress.txt"
echo "0" > "$PROGRESS_FILE"

# 分批创建，每批 50 条，避免单次请求过多
BATCH_SIZE=50
BATCH_COUNT=$(( (NOTE_COUNT + BATCH_SIZE - 1) / BATCH_SIZE ))

for batch in $(seq 1 $BATCH_COUNT); do
  START=$(( (batch - 1) * BATCH_SIZE + 1 ))
  END=$(( batch * BATCH_SIZE ))
  if [ "$END" -gt "$NOTE_COUNT" ]; then
    END=$NOTE_COUNT
  fi

  for i in $(seq $START $END); do
    # 随机选择标题和段落
    TITLE_IDX=$(( RANDOM % ${#TITLES[@]} ))
    P1_IDX=$(( RANDOM % ${#PARAGRAPHS[@]} ))
    P2_IDX=$(( RANDOM % ${#PARAGRAPHS[@]} ))

    TITLE="${TITLES[$TITLE_IDX]} - 第${i}条"
    CONTENT="<h2>${TITLES[$TITLE_IDX]}</h2><p>${PARAGRAPHS[$P1_IDX]}</p><p>${PARAGRAPHS[$P2_IDX]}</p><p>这是第${i}条容量测试笔记。用于测试系统在不同数据量下的响应性能。当前笔记数量级为${NOTE_COUNT}条。</p>"

    # 随机分配到某个文件夹
    if [ -n "$ROOT_FOLDER_ID" ] && [ "$FOLDER_COUNT" -gt 0 ] && [ $((RANDOM % 3)) -ne 0 ]; then
      # 随机选一个非根目录文件夹（这里简化，都用根文件夹下的随机子文件夹）
      TARGET_FOLDER_ID="${ROOT_FOLDER_ID}"
    else
      TARGET_FOLDER_ID="null"
    fi

    # 构造 JSON（folderId 为 null 表示根目录）
    if [ "$TARGET_FOLDER_ID" = "null" ]; then
      JSON_BODY="{\"title\":\"${TITLE}\",\"content\":\"${CONTENT}\"}"
    else
      JSON_BODY="{\"title\":\"${TITLE}\",\"content\":\"${CONTENT}\",\"folderId\":${TARGET_FOLDER_ID}}"
    fi

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/api/v1/note" \
      -H "Authorization: Bearer ${TOKEN}" \
      -H "Content-Type: application/json" \
      -d "${JSON_BODY}")

    if [ "$HTTP_CODE" != "200" ]; then
      echo "  ❌ 第${i}条笔记创建失败 (HTTP ${HTTP_CODE})"
      # 不退出，继续尝试
    fi
  done

  echo "  📝 已创建 ${END}/${NOTE_COUNT} 条笔记 (批次 ${batch}/${BATCH_COUNT})"

  # 给服务器喘息
  sleep 1
done

echo ""
echo "=============================================="
echo " ✅ 数据构造完成！"
echo "    用户: ${USERNAME}"
echo "    笔记: ${NOTE_COUNT} 条"
echo "    文件夹: ${FOLDER_COUNT} 个"
echo "=============================================="

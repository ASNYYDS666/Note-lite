# Note-lite

轻量级全栈笔记应用，支持富文本编辑、文件夹管理、笔记分享，内置 AI 对话面板，基于 RAG 向量检索实现笔记智能问答。

## 功能

- **富文本编辑器** — 基于 Tiptap，支持 Markdown 快捷输入、代码高亮、查找替换
- **文件夹 + 标签管理** — 树形目录结构，拖拽排序，软删除 + 回收站
- **笔记分享** — 生成分享码，外部用户只读查看
- **AI 对话** — 右侧面板即问即答，流式输出，上下文多轮对话
- **RAG 向量检索** — 笔记自动向量化存入 Qdrant，对话时检索相关片段注入 LLM
- **多 AI 厂商** — 支持 OpenAI / 阿里云百炼 / DeepSeek / 硅基流动等，可独立配置 Chat 和 Embedding 服务商
- **本地 Embedding** — 可选部署本地 bge-small-zh-v1.5 模型，Embedding 不产生 API 费用

## 快速开始

### 方式一：轻量启动（使用外部 Embedding API）

适合不想下载大体积模型镜像的用户。启动后，在 AI 设置页面填入任意支持 Embedding 的 API Key 即可。

```bash
git clone https://github.com/ASNYYDS666/Note-lite.git
cd Note-lite

# 仅启动核心服务（不含本地 Embedding、不含监控）
docker compose up -d
```

访问 `http://localhost:3100`，注册账号后进入 **设置 → AI 模型**，配置 API Profile。

### 方式二：全量启动（含本地 Embedding）

适合想完全离线使用 Embedding、不产生 API 费用的用户。需额外拉取约 2GB 的 Embedding 服务镜像。

```bash
git clone https://github.com/your-org/Note-lite.git
cd Note-lite

# 启动全部服务 + 本地 Embedding
docker compose --profile embedding up -d

# 等待模型下载（首次约 2-3 分钟）
docker logs -f note-embedding
# 看到 "Model loaded. Dimension: 512" 即就绪
```

### 可选：启动监控面板

```bash
docker compose --profile monitoring up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`（默认账号 admin / admin）

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `1234` |
| `JWT_SECRET` | JWT 签名密钥（**生产必须改，≥ 32 字符**） | `change-me-in-production-...` |
| `AI_AES_KEY` | AI API Key AES 加密密钥（**生产必须改，16 字节**） | `change-me-in-production` |
| `HF_ENDPOINT` | HuggingFace 模型下载镜像（仅 `--profile embedding`） | `https://hf-mirror.com` |
| `GRAFANA_ADMIN_PASSWORD` | Grafana 管理员密码（仅 `--profile monitoring`） | `admin` |

> **生产环境**：建议创建 `.env` 文件，用 `openssl rand -hex 32` 生成随机值填入。切勿使用默认值。

### 部署到云服务器

```bash
git clone https://github.com/ASNYYDS666/Note-lite.git && cd Note-lite

# 创建 .env 文件，填入随机密钥
cat > .env << 'EOF'
JWT_SECRET=$(openssl rand -hex 32)
AI_AES_KEY=$(openssl rand -hex 16)
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 8)
EOF

# 启动（含本地 Embedding）
docker compose --profile embedding up -d --build

# 等待启动完成（约 60 秒）
docker logs -f note-backend
# 看到 "Started NoteServiceApplication" 即就绪

# 创建 Qdrant 集合
curl -X PUT http://localhost:16333/collections/note_chunks \
  -H 'Content-Type: application/json' \
  -d '{"vectors":{"size":512,"distance":"Cosine"}}'
```

浏览器访问 `http://<服务器IP>:3100`，注册账号后即可使用。

> **安全提醒**：在云服务商控制台修改安全组，仅放行 3100 端口。MySQL（3306）、Redis（6379）、Qdrant（16333）等端口不应对外暴露。

### 用户使用流程

1. 打开 `http://<服务器IP>:3100`，注册账号（邮箱可选）
2. 进入 **设置 → AI 模型**，配置 API Profile：
   - 选择 DeepSeek 等厂商，填入 API Key，点击刷新模型列表
   - 选择 Chat 模型和 Embedding 模型
3. 进入 **设置 → Embedding 配置**，启用本地 Embedding 或选择远程厂商
4. 创建笔记 → 笔记自动向量化存入 Qdrant
5. 打开右侧 AI 面板 → 提问时自动检索相关笔记内容注入对话

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite + Element Plus + Tiptap + Pinia |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7 + Caffeine |
| 向量库 | Qdrant |
| Embedding | BAAI/bge-small-zh-v1.5 (512d) 或任意 OpenAI 兼容 API |
| 监控 | Prometheus + Grafana + Micrometer |
| 部署 | Docker Compose |

## 项目结构

```
Note-lite/
├── note-service/             # Spring Boot 后端
│   └── src/main/
│       ├── java/com/note/service/
│       │   ├── ai/            # AI 对话 + RAG 管道
│       │   ├── controller/    # REST API
│       │   └── ...
│       └── resources/
│           ├── db/migration/  # Flyway 数据库迁移
│           ├── application.yml
│           ├── application-dev.yml
│           └── application-prod.yml
├── note-ui/                  # Vue 3 前端
│   └── src/
│       ├── components/       # UI 组件
│       ├── views/            # 页面
│       └── api/              # API 请求层
├── embedding/                # 本地 Embedding 服务（Python）
├── docker/                   # Prometheus / Grafana 配置
└── docker-compose.yml
```

## 开发

```bash
# 启动基础设施
docker compose up -d mysql redis qdrant

# 启动 Embedding（可选，也可用外部 API）
python embedding/embedding_server.py

# 启动后端（dev profile）
cd note-service && mvn spring-boot:run

# 启动前端
cd note-ui && npm run dev
```

## License

MIT

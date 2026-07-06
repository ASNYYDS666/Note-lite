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
git clone https://github.com/your-org/Note-lite.git
cd Note-lite

# 启动全部服务（不含本地 Embedding，约 1GB 拉取）
docker compose up -d
```

浏览器打开 `http://localhost:3100`，进入 **设置 → AI 模型 → Embedding 配置**，选择一个厂商并填入 API Key。

> 内置 Embedding 厂商：OpenAI / 阿里云百炼 / 硅基流动 / 腾讯混元 / 火山引擎 / Infini / 百川。

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
| `JWT_SECRET` | JWT 签名密钥 | `change-me-in-production` |
| `AI_AES_KEY` | AI API Key AES 加密密钥 | `change-me-in-production` |
| `HF_ENDPOINT` | HuggingFace 模型下载镜像（仅 `--profile embedding`） | `https://hf-mirror.com` |
| `GRAFANA_ADMIN_PASSWORD` | Grafana 管理员密码（仅 `--profile monitoring`） | `admin` |

> 生产环境务必修改 `JWT_SECRET` 和 `AI_AES_KEY`，建议用 `openssl rand -hex 32` 生成随机值，写入 `.env` 文件。

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

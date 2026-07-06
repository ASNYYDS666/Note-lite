#!/usr/bin/env python3
"""检索质量测试 - 种子数据脚本"""
import requests, sys, os, time, argparse

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")

GOLDEN_NOTES = [
    {
        "title": "青鸟OA系统-认证模块设计",
        "content": "<h2>青鸟OA系统认证模块</h2><p>青鸟OA系统采用双因子认证机制，用户登录时需要同时提供短信验证码和指纹识别两种验证方式。系统使用阿里云短信服务发送6位数字验证码，指纹模块对接华为指纹识别SDK v3.2.1。</p><p>Token管理方面，JWT有效期设为30分钟，支持refresh token自动续期。</p>"
    },
    {
        "title": "青鸟科技2024年度技术规划会议纪要",
        "content": "<h1>青鸟科技2024年度技术规划会议纪要</h1><h2>一、会议基本信息</h2><p>时间：2024年3月15日 14:00-17:30<br>地点：3号楼601会议室<br>参会人员：技术总监张伟、架构师李明、DBA王强、各研发组长</p><h2>二、Q1工作回顾</h2><p>Q1完成了用户中心微服务拆分，API网关从Spring Cloud Gateway 3.x升级到4.0，性能提升约35%。完成了ELK日志平台的搭建，日志查询响应时间从8秒降到1.2秒。</p><h2>三、Q2重点工作</h2><p>Q2计划完成订单系统的重构，从单体架构拆分为订单服务、支付服务、库存服务三个独立微服务。消息队列从RabbitMQ迁移到Apache RocketMQ 5.0，预期消息吞吐量提升3倍。</p><h2>四、中间件选型评审</h2><p>讨论了缓存方案：对比了Redis Cluster和Codis，最终决定继续使用Redis 7.2 Cluster模式，增加3个节点应对流量增长。</p><h2>五、安全合规</h2><p>等保三级认证工作启动，预计Q3完成测评。数据脱敏方案确定使用ShardingSphere 5.4。</p><h2>六、数据库迁移决议</h2><p>经过充分技术评估和POC验证，会议最终决议：核心数据库从MySQL 8.0迁移至PostgreSQL 15，迁移工作预计在2024年Q3完成。迁移原因：更好的JSON支持、更强大的全文检索、更优秀的复杂查询性能。DBA团队将负责制定详细的迁移方案。</p><h2>七、团队建设</h2><p>Q2计划招聘高级Java工程师2名、大数据工程师1名。组织技术分享每两周一次。</p>"
    },
    {
        "title": "青鸟科技产品研发规划2024",
        "content": "<h2>青鸟科技产品研发规划</h2><p>2024年公司制定了新一代产品研发战略，以下是各项目概览。核心基础设施升级计划已启动，包括Kubernetes集群从1.27升级到1.29，以及CI/CD流水线从Jenkins迁移到GitHub Actions。云服务成本优化方面，通过引入Spot实例和预留实例组合策略，预计全年节省云成本约30%。目前AWS EKS集群运行着120个微服务实例。</p><p>安全方面，引入了零信任架构，所有服务间通信启用mTLS，使用HashiCorp Vault管理敏感配置。监控体系从Zabbix迁移到Prometheus+Grafana技术栈，告警规则已配置超过200条。日志存储从本地ES集群迁移到托管服务。</p><p>关于新产品项目——内部代号<b>凤凰计划</b>，这是一款面向企业客户的智能数据分析平台。经过董事会审批，项目总预算为<b>850万</b>元人民币。项目周期18个月，团队规模预计30人。</p>"
    },
    {
        "title": "麒麟微服务平台-技术选型",
        "content": "<h2>麒麟微服务平台技术选型</h2><p>麒麟平台是一个面向金融行业的微服务基础平台。经过技术选型评审，平台采用以下技术栈：注册中心使用Nacos 2.3，配置中心同样使用Nacos，服务间通信的消息队列选用Apache RocketMQ 5.1.0，分布式缓存使用Redis 7.2 Cluster，数据库采用MySQL 8.0.35组复制架构，ORM框架使用MyBatis-Plus 3.5.8。</p>"
    },
    {
        "title": "青鸟支付系统-架构设计",
        "content": "<h2>青鸟支付系统架构设计</h2><p>青鸟支付系统是为青鸟电商平台设计的新一代支付引擎。核心数据存储方面，经过对PostgreSQL、MySQL和TiDB的对比测试，最终选定MySQL 8.0.35作为主数据库。选择依据：团队对MySQL运维经验丰富、InnoDB引擎的事务支持成熟、周边工具链完善。</p><p>支付流水表使用分库分表策略，按商户ID哈希分为16个库，每个库64张表，预期支持日均千万级交易流水。</p>"
    },
]

INTERFERENCE_NOTES = [
    {"title": "星辰电商平台-微服务架构设计", "content": "<h2>星辰电商平台微服务架构</h2><p>星辰电商采用Spring Cloud Alibaba微服务技术栈。注册中心使用Eureka，消息队列使用RabbitMQ 3.12，缓存使用Redis 6.2 Sentinel模式，数据库使用MySQL 5.7.42。</p>"},
    {"title": "蓝鲸物流系统-技术架构文档", "content": "<h2>蓝鲸物流系统技术架构</h2><p>蓝鲸系统基于Dubbo 3.2微服务框架构建。注册中心使用Zookeeper 3.8，消息队列使用Kafka 3.6，缓存使用Caffeine+Redis二级缓存，数据库使用PostgreSQL 14。</p>"},
    {"title": "飞鹰监控平台-架构方案", "content": "<h2>飞鹰监控平台架构方案</h2><p>飞鹰平台采用Go-Micro微服务框架。服务发现使用Consul，消息队列使用NATS JetStream，时序数据库使用InfluxDB 2.7，缓存使用Redis 7.0。</p>"},
    {"title": "玄武数据中台-技术栈说明", "content": "<h2>玄武数据中台技术栈</h2><p>玄武中台基于Spring Boot 3.2 + gRPC构建。注册中心使用Nacos 2.3，消息队列使用Apache Pulsar 3.0，缓存使用Hazelcast 5.3，数据库使用MySQL 8.0.33。</p>"},
    {"title": "朱雀AI平台-服务架构概述", "content": "<h2>朱雀AI平台服务架构</h2><p>朱雀平台采用Kubernetes原生微服务架构。服务网格使用Istio 1.20，消息队列使用Redpanda 23.3，向量数据库使用Qdrant 1.7，模型服务使用Triton Inference Server。</p>"},
    {"title": "白泽数据分析平台-基础设施", "content": "<h2>白泽数据分析平台基础设施</h2><p>白泽平台基于阿里云原生技术栈。注册中心使用Nacos，消息队列使用阿里云RocketMQ商业版，数据仓库使用MaxCompute，缓存使用Tair。</p>"},
    {"title": "饕餮爬虫平台-系统设计", "content": "<h2>饕餮爬虫平台系统设计</h2><p>饕餮平台采用Python Scrapy+Celery分布式架构。任务队列使用Redis 7.2 + RabbitMQ 3.12双通道，数据存储使用MongoDB 7.0，搜索引擎使用Elasticsearch 8.10。</p>"},
    {"title": "穷奇安全平台-架构概览", "content": "<h2>穷奇安全平台架构概览</h2><p>穷奇平台采用Flink流计算+微服务架构。注册中心使用Consul 1.16，消息队列使用Kafka 3.5，图数据库使用Neo4j 5.14，缓存使用Redis 7.2。</p>"},
    {"title": "混沌混沌工程平台-技术文档", "content": "<h2>混沌混沌工程平台技术文档</h2><p>混沌平台基于LitmusChaos+自研Agent架构。注册中心使用Etcd 3.5，消息队列使用NATS 2.10，存储使用CockroachDB 23.1，监控使用VictoriaMetrics。</p>"},
    {"title": "天枢配置中心-技术设计", "content": "<h2>天枢配置中心技术设计</h2><p>天枢是一个企业级配置管理平台。后端使用Go语言开发，注册中心使用Etcd 3.5，消息队列使用RabbitMQ 3.12，数据库使用TiDB 7.1。</p>"},
]

def register_user(session, username, password, email):
    r = session.post(f"{BASE_URL}/api/v1/user/register", json={
        "username": username, "password": password, "email": email})
    msg = r.json().get("message", r.json().get("code"))
    print(f"  [注册] {username} -> {msg}")

def login(session, username, password):
    r = session.post(f"{BASE_URL}/api/v1/user/login", json={
        "username": username, "password": password})
    data = r.json()
    if data.get("code") != 200:
        raise Exception(f"登录失败: {data}")
    token = data["data"]["token"]
    user_id = data["data"]["userId"]
    print(f"  [登录] userId={user_id}, token={token[:20]}...")
    return token, user_id

def create_note(session, token, title, content, folder_id=None):
    payload = {"title": title, "content": content}
    if folder_id:
        payload["folderId"] = folder_id
    r = session.post(f"{BASE_URL}/api/v1/note", json=payload, headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"})
    if r.status_code == 200 and r.json().get("code") == 200:
        return r.json()["data"]
    else:
        print(f"  [错误] 创建笔记失败: {r.text[:200]}")
        return None

def main():
    global BASE_URL
    parser = argparse.ArgumentParser(description="检索质量测试数据准备")
    parser.add_argument("--base-url", default=BASE_URL, help="应用地址")
    parser.add_argument("--skip-interference", action="store_true", help="跳过干扰笔记创建")
    args = parser.parse_args()
    BASE_URL = args.base_url

    print("=" * 60)
    print("  Note-lite 检索质量测试 - 种子数据脚本")
    print("=" * 60)

    session = requests.Session()
    username = "perf_quality"
    password = "Test123456"
    email = "perf_quality@test.local"

    print("\n[1/3] 准备测试用户...")
    register_user(session, username, password, email)
    token, user_id = login(session, username, password)

    print(f"\n[2/3] 创建黄金标准笔记 ({len(GOLDEN_NOTES)} 条)...")
    for i, note in enumerate(GOLDEN_NOTES):
        note_id = create_note(session, token, note["title"], note["content"])
        if note_id:
            print(f"  [{i+1}/{len(GOLDEN_NOTES)}] OK id={note_id} \"{note['title']}\" ({len(note['content'])} 字符)")
        time.sleep(0.2)

    if not args.skip_interference:
        print(f"\n[3/3] 创建干扰笔记 ({len(INTERFERENCE_NOTES)} 条)...")
        for i, note in enumerate(INTERFERENCE_NOTES):
            note_id = create_note(session, token, note["title"], note["content"])
            if note_id:
                print(f"  [{i+1}/{len(INTERFERENCE_NOTES)}] OK id={note_id} \"{note['title']}\"")
            time.sleep(0.15)

    total = len(GOLDEN_NOTES) + (0 if args.skip_interference else len(INTERFERENCE_NOTES))
    print(f"\n{'=' * 60}")
    print(f"  完成! 共创建 {total} 条笔记")
    print(f"  测试用户: {username} / {password}")
    print(f"  用户ID: {user_id}")
    print(f"{'=' * 60}")
    print("\n  重要提示:")
    print("  嵌入是异步的, 请等待 1-2 分钟让 Embedding 完成。")
    print("  检查 Qdrant 向量数: curl http://localhost:16333/collections/note_chunks")
    print("\n  下一步: node perf/quality/evaluate.js --profileId <你的profileId>")

if __name__ == "__main__":
    main()

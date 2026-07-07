-- V14: 修正本地 Embedding 的 base_url 为 Docker 容器名
-- V13 写死了 localhost，Docker 容器内 localhost 指向容器自身，无法访问 embedding 服务
UPDATE `ai_provider` SET `base_url` = 'http://embedding:8081' WHERE `provider_key` = 'local-embedding' AND `base_url` = 'http://localhost:8081';

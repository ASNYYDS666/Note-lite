package com.note.service.ai.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.note.service.ai.facade.VectorDoc;
import com.note.service.ai.facade.VectorStore;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QdrantVectorStore implements VectorStore {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final String qdrantHost;
    private final Cache<String, List<VectorDoc>> searchCache;

    public QdrantVectorStore(WebClient.Builder webClientBuilder,
                             ObjectMapper objectMapper,
                             @Value("${qdrant.host}") String qdrantHost) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.qdrantHost = qdrantHost;
        this.searchCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    private String baseUrl() {
        return "http://" + qdrantHost + ":6333";
    }

    private WebClient client() {
        return webClientBuilder.baseUrl(baseUrl()).build();
    }

    // ==================== Collection 管理 ====================

    public void createCollection(String collectionName, int vectorSize) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode vectors = body.putObject("vectors");
        vectors.put("size", vectorSize);
        vectors.put("distance", "Cosine");

        try {
            client().put()
                    .uri("/collections/" + collectionName)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Collection 创建成功: {}, vectorSize={}", collectionName, vectorSize);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 400) {
                log.info("Collection 已存在，跳过创建: {}", collectionName);
            } else {
                log.error("创建 Collection 失败: {}, status={}", collectionName, e.getStatusCode());
                throw new BusinessException(ErrorCode.AI_VECTOR_STORE_ERROR,
                        "创建 Collection 失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Qdrant 连接异常: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_VECTOR_STORE_ERROR);
        }
    }

    // ==================== 向量写入 ====================

    public void upsert(String collectionName, List<VectorDoc> docs) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode points = body.putArray("points");

        for (VectorDoc doc : docs) {
            ObjectNode point = points.addObject();
            point.put("id", doc.getId());
            ArrayNode vecArray = point.putArray("vector");
            for (Float v : doc.getVector()) {
                vecArray.add(v.doubleValue());
            }
            point.set("payload", objectMapper.valueToTree(doc.getPayload()));
        }

        try {
            client().put()
                    .uri("/collections/" + collectionName + "/points?wait=true")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            searchCache.invalidateAll();
            log.debug("Qdrant upsert 完成: collection={}, count={}", collectionName, docs.size());
        } catch (Exception e) {
            log.error("Qdrant upsert 异常: collection={}", collectionName, e);
            throw new BusinessException(ErrorCode.AI_VECTOR_STORE_ERROR);
        }
    }

    // ==================== 向量检索 ====================

    public List<VectorDoc> search(String collectionName, List<Float> vector,
                                   Map<String, Object> filter, int topK) {
        String cacheKey = buildCacheKey(collectionName, vector, filter, topK);
        List<VectorDoc> cached = searchCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Qdrant 检索缓存命中: collection={}, topK={}, results={}",
                    collectionName, topK, cached.size());
            return cached;
        }

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode vecArray = body.putArray("vector");
        for (Float v : vector) {
            vecArray.add(v.doubleValue());
        }
        body.put("limit", topK);
        body.put("with_payload", true);

        if (filter != null && !filter.isEmpty()) {
            body.set("filter", buildFilter(filter));
        }

        try {
            JsonNode response = client().post()
                    .uri("/collections/" + collectionName + "/points/search")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<VectorDoc> results = new ArrayList<>();
            JsonNode resultArray = response.get("result");
            if (resultArray != null) {
                for (JsonNode point : resultArray) {
                    VectorDoc doc = new VectorDoc();
                    doc.setId(point.get("id").asText());
                    doc.setScore(point.get("score").asDouble());
                    doc.setPayload(jsonNodeToMap(point.get("payload")));
                    results.add(doc);
                }
            }
            searchCache.put(cacheKey, results);
            log.debug("Qdrant 检索完成: collection={}, topK={}, found={}, cacheSize={}",
                    collectionName, topK, results.size(), searchCache.estimatedSize());
            return results;

        } catch (Exception e) {
            log.error("Qdrant 检索异常: collection={}", collectionName, e);
            throw new BusinessException(ErrorCode.AI_VECTOR_STORE_ERROR);
        }
    }

    // ==================== 向量删除 ====================

    public void deleteByNoteId(String collectionName, Long noteId) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode filter = body.putObject("filter");
        ArrayNode must = filter.putArray("must");
        ObjectNode condition = must.addObject();
        condition.put("key", "noteId");
        condition.putObject("match").put("value", noteId);

        try {
            client().post()
                    .uri("/collections/" + collectionName + "/points/delete")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            searchCache.invalidateAll();
            log.debug("Qdrant 删除完成: collection={}, noteId={}", collectionName, noteId);
        } catch (Exception e) {
            log.error("Qdrant 删除异常: collection={}, noteId={}", collectionName, noteId, e);
            throw new BusinessException(ErrorCode.AI_VECTOR_STORE_ERROR);
        }
    }

    // ==================== 通用删除（接口方法） ====================

    @Override
    public void deleteByFilter(String collectionName, Map<String, Object> filter) {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("filter", buildFilter(filter));

        try {
            client().post()
                    .uri("/collections/" + collectionName + "/points/delete")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            searchCache.invalidateAll();
            log.debug("Qdrant 删除完成: collection={}, filter={}", collectionName, filter);
        } catch (Exception e) {
            log.error("Qdrant 删除异常: collection={}", collectionName, e);
            throw new BusinessException(ErrorCode.AI_VECTOR_STORE_ERROR);
        }
    }

    // ==================== 健康检查 ====================

    public boolean isHealthy() {
        try {
            String result = client().get()
                    .uri("/healthz")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return result != null && result.contains("healthz check passed");
        } catch (Exception e) {
            log.warn("Qdrant 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 缓存工具 ====================

    private String buildCacheKey(String collection, List<Float> vector,
                                 Map<String, Object> filter, int topK) {
        String vecSig = vector.stream()
                .limit(16)
                .map(v -> String.format("%.1f", v))
                .collect(Collectors.joining(","));
        String filterSig = filter != null ? filter.toString() : "null";
        return String.format("%s|%s|%s|%d", collection, vecSig, filterSig, topK);
    }

    public Map<String, Object> getCacheStats() {
        return Map.of(
                "size", searchCache.estimatedSize(),
                "hitRate", String.format("%.2f", searchCache.stats().hitRate()),
                "hitCount", searchCache.stats().hitCount(),
                "missCount", searchCache.stats().missCount()
        );
    }

    // ==================== 私有工具方法 ====================

    private ObjectNode buildFilter(Map<String, Object> filter) {
        ObjectNode filterNode = objectMapper.createObjectNode();
        ArrayNode must = filterNode.putArray("must");
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            ObjectNode condition = must.addObject();
            condition.put("key", entry.getKey());
            ObjectNode match = condition.putObject("match");
            setJsonValue(match, "value", entry.getValue());
        }
        return filterNode;
    }

    private void setJsonValue(ObjectNode node, String field, Object value) {
        if (value instanceof Long) {
            node.put(field, (Long) value);
        } else if (value instanceof Integer) {
            node.put(field, (Integer) value);
        } else if (value instanceof Boolean) {
            node.put(field, (Boolean) value);
        } else if (value instanceof Double || value instanceof Float) {
            node.put(field, ((Number) value).doubleValue());
        } else {
            node.put(field, value.toString());
        }
    }

    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                map.put(entry.getKey(), value.asText());
            } else if (value.isInt()) {
                map.put(entry.getKey(), value.asInt());
            } else if (value.isLong()) {
                map.put(entry.getKey(), value.asLong());
            } else if (value.isDouble() || value.isFloat()) {
                map.put(entry.getKey(), value.asDouble());
            } else if (value.isBoolean()) {
                map.put(entry.getKey(), value.asBoolean());
            } else {
                map.put(entry.getKey(), value.asText());
            }
        });
        return map;
    }
}

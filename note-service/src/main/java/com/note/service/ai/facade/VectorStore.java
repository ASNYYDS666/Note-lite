package com.note.service.ai.facade;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    /** 向量检索 */
    List<VectorDoc> search(String collection, List<Float> vector,
                           Map<String, Object> filter, int topK);

    /** 批量写入向量 */
    void upsert(String collection, List<VectorDoc> docs);

    /** 按过滤条件删除 */
    void deleteByFilter(String collection, Map<String, Object> filter);

    /** 创建 Collection */
    void createCollection(String collection, int vectorSize);

    /** 健康检查 */
    boolean isHealthy();
}

package com.note.service.ai.pipeline;

public interface RAGStage {

    /** 执行当前阶段，直接修改 ctx */
    void process(RAGContext ctx);

    /** 是否启用 */
    default boolean isEnabled(RAGContext ctx) {
        return true;
    }

    /** 阶段名称（日志用） */
    default String stageName() {
        return this.getClass().getSimpleName();
    }
}

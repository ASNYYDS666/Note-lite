package com.note.service.ai.pipeline;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Component
public class RAGPipeline {

    private final List<RAGStage> stages;
    private final MeterRegistry registry;

    public RAGPipeline(List<RAGStage> stages, MeterRegistry registry) {
        this.stages = stages;
        this.registry = registry;
        log.info("RAG 管道已组装: stageCount={}, stages={}",
                stages.size(),
                stages.stream().map(RAGStage::stageName).toList());
    }

    /**
     * 执行管道：顺序同步执行 Stage 1-5，返回 Stage 6 的流式响应。
     * 每个活跃 Stage 通过 AOP {@code @MonitorStage} 自动记录耗时指标。
     */
    public Flux<String> execute(RAGContext ctx) {
        Timer.Sample totalSample = Timer.start(registry);
        try {
            for (RAGStage stage : stages) {
                if (stage.isEnabled(ctx)) {
                    try {
                        stage.process(ctx);
                    } catch (Exception e) {
                        log.error("[{}] Stage failed", stage.stageName(), e);
                        return Flux.error(e);
                    }
                } else {
                    log.debug("[{}] 已跳过", stage.stageName());
                }
            }
            return ctx.getResponseStream();
        } finally {
            totalSample.stop(Timer.builder("rag.pipeline.duration")
                    .register(registry));
            registry.counter("rag.pipeline.calls").increment();
        }
    }
}

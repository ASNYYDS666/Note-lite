package com.note.service.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 将 Spring 管理的线程池指标暴露到 Micrometer/Prometheus。
 * 暴露指标：
 * - pool_size       (当前线程数)
 * - pool_active     (活跃线程数)
 * - pool_idle       (空闲线程数 = pool_size - active)
 * - pool_queue      (队列中等待的任务数)
 * - pool_max        (最大线程数)
 * - pool_core       (核心线程数)
 */
@Component
public class ThreadPoolMetrics implements MeterBinder {

    private final ThreadPoolTaskExecutor chatExecutor;
    private final ThreadPoolTaskExecutor embeddingExecutor;
    private final ThreadPoolTaskExecutor webAsyncExecutor;

    public ThreadPoolMetrics(
            @Qualifier("chatExecutor") ThreadPoolTaskExecutor chatExecutor,
            @Qualifier("embeddingExecutor") ThreadPoolTaskExecutor embeddingExecutor,
            @Qualifier("webAsyncExecutor") ThreadPoolTaskExecutor webAsyncExecutor) {
        this.chatExecutor = chatExecutor;
        this.embeddingExecutor = embeddingExecutor;
        this.webAsyncExecutor = webAsyncExecutor;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerPool(registry, "chatExecutor", chatExecutor);
        registerPool(registry, "embeddingExecutor", embeddingExecutor);
        registerPool(registry, "webAsyncExecutor", webAsyncExecutor);
    }

    private void registerPool(MeterRegistry registry, String poolName, ThreadPoolTaskExecutor executor) {
        List<Tag> tags = List.of(Tag.of("pool", poolName));
        ThreadPoolExecutor tpe = executor.getThreadPoolExecutor();

        registry.gauge("threadpool.pool.size", tags, tpe, tp -> (double) tp.getPoolSize());
        registry.gauge("threadpool.pool.active", tags, tpe, tp -> (double) tp.getActiveCount());
        registry.gauge("threadpool.pool.idle", tags, tpe,
                tp -> (double) (tp.getPoolSize() - tp.getActiveCount()));
        registry.gauge("threadpool.pool.max", tags, tpe, tp -> (double) tp.getMaximumPoolSize());
        registry.gauge("threadpool.pool.core", tags, tpe, tp -> (double) tp.getCorePoolSize());
        registry.gauge("threadpool.queue.size", tags, tpe, tp -> (double) tp.getQueue().size());
        registry.gauge("threadpool.task.completed", tags, tpe, tp -> (double) tp.getCompletedTaskCount());
    }
}

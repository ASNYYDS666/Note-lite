package com.note.service.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 追踪 SSE 连接数（当前活跃、历史累计、错误次数）。
 * 在 ChatController 中：SSE 建立时 inc()，完成时 dec()，错误时 recordError()。
 */
@Component
public class SSEConnectionMetrics {

    private final AtomicLong activeConnections = new AtomicLong(0);
    private final Counter totalConnections;
    private final Counter errorConnections;

    public SSEConnectionMetrics(MeterRegistry registry) {
        this.totalConnections = Counter.builder("sse.connections.total")
                .description("SSE 连接历史总数")
                .register(registry);
        this.errorConnections = Counter.builder("sse.connections.errors")
                .description("SSE 连接错误次数")
                .register(registry);

        registry.gauge("sse.connections.active", activeConnections, AtomicLong::doubleValue);
    }

    public void onConnectionOpen() {
        activeConnections.incrementAndGet();
        totalConnections.increment();
    }

    public void onConnectionClose() {
        activeConnections.decrementAndGet();
    }

    public void onConnectionError() {
        activeConnections.decrementAndGet();
        errorConnections.increment();
    }

    public long getActiveCount() {
        return activeConnections.get();
    }
}

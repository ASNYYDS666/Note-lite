package com.note.service.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.time.Duration;

// 自定义监控指标工具类
// 使用Micrometer记录缓存命中/未命中次数以及查询耗时

@Component
public class MicrometerMetrics {
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer queryTimer;

    // 构造器通过MeterRegistry注册指标
    // MeterRegistry由Spring Boot自动配置，直接注入即可
    public MicrometerMetrics(MeterRegistry registry) {
        // 缓存命中次数计数器
        this.cacheHitCounter = Counter.builder("note.cache.hit")
                .description("Number of cache hits for note detail")
                .register(registry);

        // 缓存未命中次数计数器
        this.cacheMissCounter = Counter.builder("note.cache.miss")
                .description("Number of cache misses for note detail")
                .register(registry);

        // 查询耗时计时器（记录所有 getDetail 的耗时，单位毫秒）
        this.queryTimer = Timer.builder("note.query.duration")
                .description("Time taken to retrieve note detail")
                .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
                .sla(
                        Duration.ofMillis(50),   // 50ms
                        Duration.ofMillis(100),  // 100ms
                        Duration.ofMillis(200),  // 200ms
                        Duration.ofMillis(500)
                )               // 延迟阈值（毫秒）用于直方图
                .register(registry);
    }

    // 记录一次命中
    public void recordCacheHit() {
        cacheHitCounter.increment();
    }

    // 记录一次缓存未命中
    public void recordCacheMiss() {
        cacheMissCounter.increment();
    }

    // 记录耗时（自动计时）
    // 执行一个查询操作并自动记录耗时
    // @param supplier 待执行的业务逻辑（可能抛出异常）
    // @return 业务逻辑的返回值
    public <T> T recordQuery(CheckedSupplier<T> supplier) {
        // Timer.record() 接受一个 lambda，自动计时并记录到指标中
        return queryTimer.record(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                // 将受检异常包装为非受检，因为 record() 方法只允许 Runnable 或 Supplier（不抛异常）
                throw new RuntimeException(e);
            }
        });
    }

    // 函数式接口允许抛出 Exception
    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}

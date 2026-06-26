package com.note.service.ai.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RAGMetricsAspect {

    private final MeterRegistry registry;

    @Around("@annotation(monitorStage)")
    public Object record(ProceedingJoinPoint pjp, MonitorStage monitorStage) throws Throwable {
        String stage = monitorStage.value();
        Timer.Sample sample = Timer.start(registry);
        try {
            Object result = pjp.proceed();
            sample.stop(Timer.builder("rag.stage.duration")
                    .tag("stage", stage)
                    .tag("status", "success")
                    .register(registry));
            registry.counter("rag.stage.calls",
                    "stage", stage,
                    "status", "success").increment();
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("rag.stage.duration")
                    .tag("stage", stage)
                    .tag("status", "error")
                    .register(registry));
            registry.counter("rag.stage.calls",
                    "stage", stage,
                    "status", "error",
                    "errorType", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }
}

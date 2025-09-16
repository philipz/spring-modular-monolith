package com.sivalabs.bookstore.orders.cache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CacheErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CacheErrorHandler.class);

    private final int failureThreshold;
    private final Duration circuitOpenDuration;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile LocalDateTime circuitOpenedAt = null;
    private volatile boolean circuitOpen = false;

    private final ConcurrentHashMap<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastErrorTimes = new ConcurrentHashMap<>();

    public CacheErrorHandler(
            @Value("${bookstore.cache.circuit-breaker-failure-threshold:5}") int failureThreshold,
            @Value("${bookstore.cache.circuit-breaker-recovery-timeout-ms:30000}") long circuitOpenMs) {
        this.failureThreshold = failureThreshold;
        this.circuitOpenDuration = Duration.ofMillis(circuitOpenMs);
    }

    public CacheErrorHandler() {
        this.failureThreshold = 5;
        this.circuitOpenDuration = Duration.ofMillis(30_000L);
    }

    public <T> T executeWithFallback(Supplier<T> operation, String operationName, String key) {
        return executeWithFallback(operation, operationName, key, () -> null);
    }

    public <T> T executeWithFallback(Supplier<T> operation, String operationName, String key, Supplier<T> fallback) {
        if (isCircuitOpen()) {
            logger.debug("Circuit breaker is open, skipping cache operation: {} for key: {}", operationName, key);
            recordError(operationName, "Circuit breaker open");
            return fallback.get();
        }

        try {
            T result = operation.get();
            recordSuccess(operationName);
            return result;
        } catch (Exception e) {
            handleCacheError(e, operationName, key);
            return fallback.get();
        }
    }

    public boolean executeVoidOperation(Runnable operation, String operationName, String key) {
        if (isCircuitOpen()) {
            logger.debug("Circuit breaker is open, skipping cache operation: {} for key: {}", operationName, key);
            recordError(operationName, "Circuit breaker open");
            return false;
        }

        try {
            operation.run();
            recordSuccess(operationName);
            return true;
        } catch (Exception e) {
            handleCacheError(e, operationName, key);
            return false;
        }
    }

    public void handleCacheError(Exception exception, String operationName, String key) {
        recordError(operationName, exception.getMessage());
        logger.warn(
                "Cache operation failed - Operation: {}, Key: {}, Error: {}",
                operationName,
                key,
                exception.getMessage());
        logger.debug("Cache operation failure details for {} with key {}", operationName, key, exception);

        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold && !circuitOpen) {
            openCircuit();
        }
    }

    public boolean isCircuitOpen() {
        if (!circuitOpen) {
            return false;
        }

        LocalDateTime openedAt = circuitOpenedAt;
        if (openedAt != null && Duration.between(openedAt, LocalDateTime.now()).compareTo(circuitOpenDuration) > 0) {
            logger.info("Circuit breaker entering half-open state - attempting cache recovery");
            return false;
        }

        return true;
    }

    public boolean checkCacheHealth(Supplier<Boolean> healthCheck) {
        try {
            if (healthCheck.get()) {
                closeCircuit();
                logger.info("Cache health check passed - circuit breaker closed");
                return true;
            } else {
                logger.debug("Cache health check failed - circuit breaker remains open");
                return false;
            }
        } catch (Exception e) {
            logger.warn("Cache health check threw exception: {}", e.getMessage());
            recordError("health-check", e.getMessage());
            return false;
        }
    }

    public boolean shouldFallbackToDatabase(String operationName) {
        AtomicInteger count = errorCounts.get(operationName);
        if (count != null && count.get() > failureThreshold / 2) {
            logger.debug("Frequent cache errors detected for {} - recommending database fallback", operationName);
            return true;
        }
        return false;
    }

    public void recordSuccess(String operationName) {
        consecutiveFailures.set(0);
        errorCounts.remove(operationName);
        lastErrorTimes.remove(operationName);
        if (circuitOpen) {
            closeCircuit();
            logger.info("Cache circuit breaker closed after successful operation: {}", operationName);
        }
    }

    public String getCacheErrorStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Error Counts by Operation:\n");
        errorCounts.forEach((op, count) ->
                stats.append("  - ").append(op).append(": ").append(count.get()).append("\n"));
        stats.append("Last Error Times:\n");
        lastErrorTimes.forEach((op, time) ->
                stats.append("  - ").append(op).append(": ").append(time).append("\n"));
        return stats.toString();
    }

    public void resetErrorState() {
        consecutiveFailures.set(0);
        circuitOpen = false;
        circuitOpenedAt = null;
        errorCounts.clear();
        lastErrorTimes.clear();
    }

    private void openCircuit() {
        circuitOpen = true;
        circuitOpenedAt = LocalDateTime.now();
        logger.warn("Cache circuit breaker OPENED after {} consecutive failures", failureThreshold);
    }

    private void closeCircuit() {
        circuitOpen = false;
        consecutiveFailures.set(0);
        circuitOpenedAt = null;
    }

    private void recordError(String operationName, String errorMessage) {
        errorCounts.computeIfAbsent(operationName, key -> new AtomicInteger(0)).incrementAndGet();
        lastErrorTimes.put(operationName, LocalDateTime.now());
        logger.debug("Recorded cache error for operation {}: {}", operationName, errorMessage);
    }
}

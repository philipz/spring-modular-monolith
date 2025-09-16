package com.sivalabs.bookstore.orders.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.orders.domain.OrderCachePort;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Lazy
public class OrderCacheService extends AbstractCacheService<String, OrderEntity> implements OrderCachePort {

    private static final int CACHE_READ_TIMEOUT_MS = 500;
    private static final int CACHE_WRITE_TIMEOUT_MS = 1000;

    public OrderCacheService(
            @Qualifier("ordersCache") IMap<String, Object> ordersCache,
            @Autowired(required = false) CacheErrorHandler errorHandler) {
        super(ordersCache, errorHandler != null ? errorHandler : new CacheErrorHandler(), OrderEntity.class);
    }

    @Override
    protected String getCacheDisplayName() {
        return "Orders";
    }

    @Override
    protected String createHealthCheckKey() {
        return "health-check-" + System.currentTimeMillis();
    }

    @Override
    public Optional<OrderEntity> findByOrderNumber(String orderNumber) {
        return errorHandler.executeWithFallback(
                () -> {
                    Object cachedValue = cache.get(orderNumber);
                    OrderEntity order = safeCast(cachedValue, orderNumber);
                    return Optional.ofNullable(order);
                },
                "findByOrderNumber",
                orderNumber,
                Optional::empty);
    }

    public Optional<OrderEntity> findByOrderNumberWithTimeout(String orderNumber) {
        return errorHandler.executeWithFallback(
                () -> {
                    try {
                        Object cachedValue = cache.getAsync(orderNumber)
                                .toCompletableFuture()
                                .get(CACHE_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        OrderEntity order = safeCast(cachedValue, orderNumber);
                        return Optional.ofNullable(order);
                    } catch (Exception e) {
                        throw new RuntimeException("Cache operation timeout or error", e);
                    }
                },
                "findByOrderNumberWithTimeout",
                orderNumber,
                Optional::empty);
    }

    @Override
    public boolean cacheOrder(String orderNumber, OrderEntity order) {
        return cacheEntity(orderNumber, order);
    }

    public boolean cacheOrderWithTimeout(String orderNumber, OrderEntity order) {
        if (order == null) {
            logger.warn("Attempted to cache null order for key: {}", orderNumber);
            return false;
        }

        return errorHandler.executeVoidOperation(
                () -> {
                    try {
                        cache.putAsync(orderNumber, order)
                                .toCompletableFuture()
                                .get(CACHE_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException("Cache write timeout or error", e);
                    }
                },
                "cacheOrderWithTimeout",
                orderNumber);
    }

    public boolean cacheOrderWithTtl(String orderNumber, OrderEntity order, int ttlSeconds) {
        if (order == null) {
            logger.warn("Attempted to cache null order for key: {}", orderNumber);
            return false;
        }

        return errorHandler.executeVoidOperation(
                () -> cache.put(orderNumber, order, ttlSeconds, TimeUnit.SECONDS), "cacheOrderWithTtl", orderNumber);
    }

    public boolean updateCachedOrder(String orderNumber, OrderEntity order) {
        return updateCachedEntity(orderNumber, order);
    }

    public boolean evictFromCache(String orderNumber) {
        logger.debug("Evicting order from cache: {}", orderNumber);
        return errorHandler.executeVoidOperation(
                () -> {
                    cache.evict(orderNumber);
                    logger.debug("Order evicted from cache successfully: {}", orderNumber);
                },
                "evictFromCache",
                orderNumber);
    }

    public boolean testCacheConnectivity() {
        logger.debug("Testing cache connectivity");
        return errorHandler.checkCacheHealth(() -> {
            try {
                String healthCheckKey = "health-check-" + System.currentTimeMillis();
                String healthCheckValue = "connectivity-test";
                cache.put(healthCheckKey, healthCheckValue);
                Object retrieved = cache.get(healthCheckKey);
                boolean exists = cache.containsKey(healthCheckKey);
                cache.remove(healthCheckKey);
                return healthCheckValue.equals(retrieved) && exists;
            } catch (Exception e) {
                logger.warn("Cache connectivity test failed with exception: {}", e.getMessage());
                return false;
            }
        });
    }

    public String getHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Cache Health Report ===\n");
        report.append(getCircuitBreakerStatus());
        report.append("\nCache Statistics:\n");
        report.append(getCacheStats());
        report.append("\nConnectivity Test: ");
        boolean healthy = isHealthy();
        report.append(healthy ? "PASS" : "FAIL");

        if (!healthy && isCircuitBreakerOpen()) {
            report.append("\nRecommendation: Cache is unavailable. Operations will fallback to database.");
        } else if (!healthy) {
            report.append("\nRecommendation: Cache issues detected. Monitor for potential circuit breaker activation.");
        }

        return report.toString();
    }

    public Optional<OrderEntity> findWithAutomaticFallback(
            String orderNumber, java.util.function.Supplier<Optional<OrderEntity>> fallbackFunction) {
        if (shouldFallbackToDatabase("findWithAutomaticFallback")) {
            logger.debug("Circuit breaker recommends database fallback for order lookup: {}", orderNumber);
            return fallbackFunction.get();
        }

        Optional<OrderEntity> cached = findByOrderNumber(orderNumber);

        if (cached.isEmpty() && !errorHandler.isCircuitOpen()) {
            logger.debug("Cache miss for order {}, trying fallback", orderNumber);
            return fallbackFunction.get();
        }

        return cached;
    }
}

package com.sivalabs.bookstore.orders.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.AbstractCacheService;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Service providing cache operations abstraction for Order entities.
 *
 * This service extends AbstractCacheService to provide order-specific
 * cache operations while inheriting common cache functionality.
 *
 * Key features:
 * - Cache operations with timeout handling
 * - Eviction and expiry management with TTL support
 * - Order-specific caching patterns
 */
@Service
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Lazy
public class OrderCacheService extends AbstractCacheService<String, OrderEntity> {

    // Cache operation timeouts
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

    /**
     * Find an order by its order number from the cache.
     *
     * @param orderNumber the order number to search for
     * @return Optional containing the order if found in cache, empty if not found or cache error
     */
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

    /**
     * Find an order by its order number with timeout.
     *
     * @param orderNumber the order number to search for
     * @return Optional containing the order if found in cache, empty if not found or timeout
     */
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

    /**
     * Cache an order entity.
     * This operation will trigger write-through to the database if configured.
     *
     * @param orderNumber the order number (cache key)
     * @param order the order entity to cache
     * @return true if caching was successful, false if it failed
     */
    public boolean cacheOrder(String orderNumber, OrderEntity order) {
        return cacheEntity(orderNumber, order);
    }

    /**
     * Cache an order entity with timeout.
     *
     * @param orderNumber the order number (cache key)
     * @param order the order entity to cache
     * @return true if caching was successful, false if it failed or timeout
     */
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

    /**
     * Cache an order entity with TTL (Time To Live).
     *
     * @param orderNumber the order number (cache key)
     * @param order the order entity to cache
     * @param ttlSeconds time to live in seconds
     * @return true if caching was successful, false if it failed
     */
    public boolean cacheOrderWithTtl(String orderNumber, OrderEntity order, int ttlSeconds) {
        if (order == null) {
            logger.warn("Attempted to cache null order for key: {}", orderNumber);
            return false;
        }

        return errorHandler.executeVoidOperation(
                () -> {
                    cache.put(orderNumber, order, ttlSeconds, TimeUnit.SECONDS);
                },
                "cacheOrderWithTtl",
                orderNumber);
    }

    /**
     * Update an existing cached order.
     *
     * @param orderNumber the order number (cache key)
     * @param order the updated order entity
     * @return true if update was successful, false if it failed
     */
    public boolean updateCachedOrder(String orderNumber, OrderEntity order) {
        return updateCachedEntity(orderNumber, order);
    }

    /**
     * Evict an order from the cache only (without triggering database operations).
     *
     * @param orderNumber the order number to evict
     * @return true if eviction was successful, false if it failed
     */
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

    /**
     * Test cache connectivity and attempt to close circuit breaker if open.
     * This method performs a health check and can be used to verify cache recovery.
     *
     * @return true if cache is healthy and available, false otherwise
     */
    public boolean testCacheConnectivity() {
        logger.debug("Testing cache connectivity");

        return errorHandler.checkCacheHealth(() -> {
            try {
                // Comprehensive health check
                String healthCheckKey = "health-check-" + System.currentTimeMillis();
                String healthCheckValue = "connectivity-test";

                // Test basic operations
                cache.put(healthCheckKey, healthCheckValue);
                Object retrieved = cache.get(healthCheckKey);
                boolean exists = cache.containsKey(healthCheckKey);
                cache.remove(healthCheckKey);

                boolean healthy = healthCheckValue.equals(retrieved) && exists;

                if (healthy) {
                    logger.debug("Cache connectivity test passed");
                } else {
                    logger.warn("Cache connectivity test failed - operations didn't work as expected");
                }

                return healthy;

            } catch (Exception e) {
                logger.warn("Cache connectivity test failed with exception: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get comprehensive cache and circuit breaker health information.
     * This combines cache statistics with circuit breaker status for monitoring.
     *
     * @return formatted health report string
     */
    public String getHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Cache Health Report ===\n");

        // Circuit breaker status
        report.append(getCircuitBreakerStatus());

        // Cache statistics
        report.append("\nCache Statistics:\n");
        report.append(getCacheStats());

        // Connectivity test
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

    /**
     * Find an order with automatic fallback decision based on circuit breaker state.
     * This method demonstrates the circuit breaker pattern in action.
     *
     * @param orderNumber the order number to find
     * @param fallbackFunction function to call if cache is unavailable
     * @return Optional containing the order if found, result of fallback otherwise
     */
    public Optional<OrderEntity> findWithAutomaticFallback(
            String orderNumber, java.util.function.Supplier<Optional<OrderEntity>> fallbackFunction) {
        // Check circuit breaker state first
        if (shouldFallbackToDatabase("findWithAutomaticFallback")) {
            logger.debug("Circuit breaker recommends database fallback for order lookup: {}", orderNumber);
            return fallbackFunction.get();
        }

        // Try cache first
        Optional<OrderEntity> cached = findByOrderNumber(orderNumber);

        // If cache returns empty due to miss (not error), try fallback
        if (cached.isEmpty() && !errorHandler.isCircuitOpen()) {
            logger.debug("Cache miss for order {}, trying fallback", orderNumber);
            return fallbackFunction.get();
        }

        return cached;
    }
}

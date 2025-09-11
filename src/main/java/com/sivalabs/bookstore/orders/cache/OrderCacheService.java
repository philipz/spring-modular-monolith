package com.sivalabs.bookstore.orders.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Service providing cache operations abstraction for Order entities.
 *
 * This service acts as the primary interface for all cache operations related to orders.
 * It handles error scenarios gracefully using CacheErrorHandler and provides a clean
 * abstraction over the underlying Hazelcast IMap.
 *
 * Key features:
 * - Graceful error handling with fallback support
 * - Cache operations with timeout handling
 * - Eviction and expiry management
 * - Cache warming and preloading support
 * - Monitoring and metrics integration
 */
@Service
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Lazy
public class OrderCacheService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCacheService.class);

    // Cache operation timeouts
    private static final int CACHE_READ_TIMEOUT_MS = 500;
    private static final int CACHE_WRITE_TIMEOUT_MS = 1000;

    private final IMap<String, Object> ordersCache;
    private final CacheErrorHandler errorHandler;

    public OrderCacheService(
            @Qualifier("ordersCache") IMap<String, Object> ordersCache, CacheErrorHandler errorHandler) {
        this.ordersCache = ordersCache;
        this.errorHandler = errorHandler;
        logger.info("OrderCacheService initialized with cache: {} and error handler", ordersCache.getName());
    }

    /**
     * Find an order by its order number from the cache.
     *
     * @param orderNumber the order number to search for
     * @return Optional containing the order if found in cache, empty if not found or cache error
     */
    public Optional<OrderEntity> findByOrderNumber(String orderNumber) {
        logger.debug("Looking up order in cache: {}", orderNumber);

        return errorHandler.executeWithFallback(
                () -> {
                    Object cachedValue = ordersCache.get(orderNumber);
                    if (cachedValue instanceof OrderEntity orderEntity) {
                        logger.debug("Order found in cache: {}", orderNumber);
                        return Optional.of(orderEntity);
                    } else if (cachedValue != null) {
                        logger.warn(
                                "Unexpected object type in cache for key {}: {}", orderNumber, cachedValue.getClass());
                        return Optional.empty();
                    } else {
                        logger.debug("Order not found in cache: {}", orderNumber);
                        return Optional.empty();
                    }
                },
                "findByOrderNumber",
                orderNumber,
                () -> {
                    logger.debug("Cache lookup failed for {}, returning empty", orderNumber);
                    return Optional.empty();
                });
    }

    /**
     * Find an order by its order number with timeout.
     *
     * @param orderNumber the order number to search for
     * @return Optional containing the order if found in cache, empty if not found or timeout
     */
    public Optional<OrderEntity> findByOrderNumberWithTimeout(String orderNumber) {
        logger.debug("Looking up order in cache with timeout: {}", orderNumber);

        return errorHandler.executeWithFallback(
                () -> {
                    try {
                        Object cachedValue = ordersCache
                                .getAsync(orderNumber)
                                .toCompletableFuture()
                                .get(CACHE_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                        if (cachedValue instanceof OrderEntity orderEntity) {
                            logger.debug("Order found in cache (with timeout): {}", orderNumber);
                            return Optional.of(orderEntity);
                        } else if (cachedValue != null) {
                            logger.warn(
                                    "Unexpected object type in cache for key {}: {}",
                                    orderNumber,
                                    cachedValue.getClass());
                            return Optional.empty();
                        } else {
                            logger.debug("Order not found in cache (with timeout): {}", orderNumber);
                            return Optional.empty();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Cache operation timeout or error", e);
                    }
                },
                "findByOrderNumberWithTimeout",
                orderNumber,
                () -> {
                    logger.debug("Cache lookup with timeout failed for {}, returning empty", orderNumber);
                    return Optional.empty();
                });
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
        if (order == null) {
            logger.warn("Attempted to cache null order for key: {}", orderNumber);
            return false;
        }

        logger.debug("Caching order: {} with ID: {}", orderNumber, order.getId());

        return errorHandler.executeVoidOperation(
                () -> {
                    ordersCache.put(orderNumber, order);
                    logger.debug("Order cached successfully: {}", orderNumber);
                },
                "cacheOrder",
                orderNumber);
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

        logger.debug("Caching order with timeout: {} with ID: {}", orderNumber, order.getId());

        return errorHandler.executeVoidOperation(
                () -> {
                    try {
                        ordersCache
                                .putAsync(orderNumber, order)
                                .toCompletableFuture()
                                .get(CACHE_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        logger.debug("Order cached successfully with timeout: {}", orderNumber);
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

        logger.debug("Caching order with TTL: {} seconds for key: {}", ttlSeconds, orderNumber);

        return errorHandler.executeVoidOperation(
                () -> {
                    ordersCache.put(orderNumber, order, ttlSeconds, TimeUnit.SECONDS);
                    logger.debug("Order cached with TTL successfully: {}", orderNumber);
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
        if (order == null) {
            logger.warn("Attempted to update cache with null order for key: {}", orderNumber);
            return false;
        }

        logger.debug("Updating cached order: {}", orderNumber);

        return errorHandler.executeVoidOperation(
                () -> {
                    // Use replace to only update if the key already exists
                    Object previous = ordersCache.replace(orderNumber, order);
                    if (previous != null) {
                        logger.debug("Cached order updated successfully: {}", orderNumber);
                    } else {
                        logger.debug("Order not in cache, performing regular put: {}", orderNumber);
                        ordersCache.put(orderNumber, order);
                    }
                },
                "updateCachedOrder",
                orderNumber);
    }

    /**
     * Remove an order from the cache.
     *
     * @param orderNumber the order number to remove
     * @return true if removal was successful, false if it failed
     */
    public boolean removeFromCache(String orderNumber) {
        logger.debug("Removing order from cache: {}", orderNumber);

        return errorHandler.executeVoidOperation(
                () -> {
                    Object removed = ordersCache.remove(orderNumber);
                    if (removed != null) {
                        logger.debug("Order removed from cache successfully: {}", orderNumber);
                    } else {
                        logger.debug("Order not in cache for removal: {}", orderNumber);
                    }
                },
                "removeFromCache",
                orderNumber);
    }

    /**
     * Check if an order exists in the cache.
     *
     * @param orderNumber the order number to check
     * @return true if the order exists in cache, false otherwise
     */
    public boolean existsInCache(String orderNumber) {
        return errorHandler.executeWithFallback(
                () -> {
                    boolean exists = ordersCache.containsKey(orderNumber);
                    logger.debug("Cache existence check for {}: {}", orderNumber, exists);
                    return exists;
                },
                "existsInCache",
                orderNumber,
                () -> {
                    logger.debug("Cache existence check failed for {}, assuming false", orderNumber);
                    return false;
                });
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
                    ordersCache.evict(orderNumber);
                    logger.debug("Order evicted from cache successfully: {}", orderNumber);
                },
                "evictFromCache",
                orderNumber);
    }

    /**
     * Get cache statistics and health information.
     *
     * @return String containing cache statistics
     */
    public String getCacheStats() {
        return errorHandler.executeWithFallback(
                () -> {
                    StringBuilder stats = new StringBuilder();
                    stats.append("Orders Cache Statistics:\n");
                    stats.append(String.format("  Cache Name: %s\n", ordersCache.getName()));
                    stats.append(String.format("  Cache Size: %d\n", ordersCache.size()));

                    if (ordersCache.getLocalMapStats() != null) {
                        var localStats = ordersCache.getLocalMapStats();
                        stats.append(String.format("  Local Map Stats:\n"));
                        stats.append(String.format("    Owned Entry Count: %d\n", localStats.getOwnedEntryCount()));
                        stats.append(String.format("    Backup Entry Count: %d\n", localStats.getBackupEntryCount()));
                        stats.append(String.format("    Hits: %d\n", localStats.getHits()));
                        stats.append(String.format("    Get Operations: %d\n", localStats.getGetOperationCount()));
                        stats.append(String.format("    Put Operations: %d\n", localStats.getPutOperationCount()));
                    }

                    return stats.toString();
                },
                "getCacheStats",
                "global",
                () -> "Cache stats unavailable due to error\n");
    }

    /**
     * Check cache health by performing a simple operation.
     *
     * @return true if cache is healthy, false otherwise
     */
    public boolean isHealthy() {
        return errorHandler.checkCacheHealth(() -> {
            try {
                // Simple health check - check if we can perform basic operations
                String healthCheckKey = "health-check-" + System.currentTimeMillis();
                ordersCache.put(healthCheckKey, "health-check-value");
                Object value = ordersCache.get(healthCheckKey);
                ordersCache.remove(healthCheckKey);

                return "health-check-value".equals(value);
            } catch (Exception e) {
                logger.debug("Cache health check failed", e);
                return false;
            }
        });
    }

    /**
     * Check if the cache circuit breaker is open (cache unavailable).
     * When the circuit breaker is open, all cache operations will be bypassed
     * and the service will fallback to database operations.
     *
     * @return true if circuit breaker is open (cache unavailable), false otherwise
     */
    public boolean isCircuitBreakerOpen() {
        return errorHandler.isCircuitOpen();
    }

    /**
     * Get circuit breaker status information for monitoring.
     *
     * @return formatted string with circuit breaker status and error statistics
     */
    public String getCircuitBreakerStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Cache Circuit Breaker Status:\n");
        status.append(String.format(
                "  Circuit State: %s\n",
                errorHandler.isCircuitOpen() ? "OPEN (Bypassing Cache)" : "CLOSED (Cache Active)"));
        status.append("  Error Statistics:\n");
        status.append(errorHandler.getCacheErrorStats());

        return status.toString();
    }

    /**
     * Determine if cache operations should fallback to database.
     * This provides a unified decision point for cache vs database operations.
     *
     * @param operationName the name of the operation being considered
     * @return true if should use database, false if cache is available
     */
    public boolean shouldFallbackToDatabase(String operationName) {
        if (errorHandler.isCircuitOpen()) {
            logger.debug("Circuit breaker is open - recommending database fallback for {}", operationName);
            return true;
        }

        return errorHandler.shouldFallbackToDatabase(operationName);
    }

    /**
     * Manually reset the circuit breaker and error state.
     * This can be used for recovery scenarios or testing.
     * Use with caution as it bypasses the automatic recovery mechanism.
     *
     * @return true if reset was successful
     */
    public boolean resetCircuitBreaker() {
        logger.warn("Manually resetting cache circuit breaker - this should only be done for recovery or testing");

        return errorHandler.executeVoidOperation(
                () -> {
                    errorHandler.resetErrorState();
                    logger.info("Cache circuit breaker has been manually reset");
                },
                "resetCircuitBreaker",
                "manual-reset");
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
                ordersCache.put(healthCheckKey, healthCheckValue);
                Object retrieved = ordersCache.get(healthCheckKey);
                boolean exists = ordersCache.containsKey(healthCheckKey);
                ordersCache.remove(healthCheckKey);

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

    /**
     * Warm up the cache by preloading frequently accessed orders.
     * This method is intended to be called on application startup.
     *
     * @param orderNumbers collection of order numbers to preload
     * @return number of orders successfully preloaded
     */
    public int warmUpCache(Iterable<String> orderNumbers) {
        int successCount = 0;

        logger.info("Starting cache warm-up");

        for (String orderNumber : orderNumbers) {
            boolean warmed = errorHandler.executeWithFallback(
                    () -> {
                        // This will trigger the MapStore to load from database
                        Object value = ordersCache.get(orderNumber);
                        return value != null;
                    },
                    "warmUpCache",
                    orderNumber,
                    () -> false);

            if (warmed) {
                successCount++;
            }
        }

        logger.info("Cache warm-up completed: {} orders preloaded", successCount);
        return successCount;
    }

    /**
     * Get all order keys currently in the cache.
     *
     * @return Set of order numbers (keys) in the cache
     */
    public Set<String> getAllCacheKeys() {
        return errorHandler.executeWithFallback(
                () -> {
                    Set<String> keys = ordersCache.keySet();
                    logger.debug("Retrieved {} cache keys", keys.size());
                    return keys;
                },
                "getAllCacheKeys",
                "global",
                () -> {
                    logger.debug("Failed to retrieve cache keys, returning empty set");
                    return Set.of();
                });
    }

    /**
     * Get all cached orders.
     *
     * @return List of all OrderEntity objects in the cache
     */
    public List<OrderEntity> getAllCachedOrders() {
        return errorHandler.executeWithFallback(
                () -> {
                    List<OrderEntity> orders = new ArrayList<>();
                    for (Object value : ordersCache.values()) {
                        if (value instanceof OrderEntity orderEntity) {
                            orders.add(orderEntity);
                        } else if (value != null) {
                            logger.warn("Unexpected object type in cache: {}", value.getClass());
                        }
                    }
                    logger.debug("Retrieved {} cached orders", orders.size());
                    return orders;
                },
                "getAllCachedOrders",
                "global",
                () -> {
                    logger.debug("Failed to retrieve cached orders, returning empty list");
                    return List.of();
                });
    }

    /**
     * Get all cached orders as a map of orderNumber -> OrderEntity.
     *
     * @return Map of order numbers to OrderEntity objects
     */
    public Map<String, OrderEntity> getAllCachedOrdersAsMap() {
        return errorHandler.executeWithFallback(
                () -> {
                    Map<String, OrderEntity> orderMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : ordersCache.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof OrderEntity orderEntity) {
                            orderMap.put(entry.getKey(), orderEntity);
                        } else if (value != null) {
                            logger.warn(
                                    "Unexpected object type in cache for key {}: {}", entry.getKey(), value.getClass());
                        }
                    }
                    logger.debug("Retrieved {} cached orders as map", orderMap.size());
                    return orderMap;
                },
                "getAllCachedOrdersAsMap",
                "global",
                () -> {
                    logger.debug("Failed to retrieve cached orders as map, returning empty map");
                    return Map.of();
                });
    }

    /**
     * Get a subset of cached orders with pagination support.
     *
     * @param limit maximum number of orders to return
     * @param offset starting position (for pagination)
     * @return List of OrderEntity objects (paginated)
     */
    public List<OrderEntity> getCachedOrdersPaginated(int limit, int offset) {
        return errorHandler.executeWithFallback(
                () -> {
                    List<OrderEntity> allOrders = getAllCachedOrders();

                    if (offset >= allOrders.size()) {
                        return List.of();
                    }

                    int endIndex = Math.min(offset + limit, allOrders.size());
                    List<OrderEntity> paginatedOrders = allOrders.subList(offset, endIndex);

                    logger.debug(
                            "Retrieved {} paginated cached orders (limit: {}, offset: {})",
                            paginatedOrders.size(),
                            limit,
                            offset);
                    return paginatedOrders;
                },
                "getCachedOrdersPaginated",
                String.format("limit=%d,offset=%d", limit, offset),
                () -> {
                    logger.debug("Failed to retrieve paginated cached orders, returning empty list");
                    return List.of();
                });
    }

    /**
     * Get basic cache content summary with counts and sample data.
     *
     * @return Map containing cache content summary
     */
    public Map<String, Object> getCacheContentSummary() {
        return errorHandler.executeWithFallback(
                () -> {
                    Map<String, Object> summary = new HashMap<>();

                    Set<String> keys = ordersCache.keySet();
                    List<OrderEntity> orders = getAllCachedOrders();

                    summary.put("totalEntries", keys.size());
                    summary.put("orderCount", orders.size());
                    summary.put("keys", keys);

                    // Add sample orders (first 3)
                    List<OrderEntity> sampleOrders = orders.stream().limit(3).toList();
                    summary.put("sampleOrders", sampleOrders);

                    logger.debug("Generated cache content summary with {} entries", keys.size());
                    return summary;
                },
                "getCacheContentSummary",
                "global",
                () -> {
                    logger.debug("Failed to generate cache content summary, returning empty summary");
                    Map<String, Object> emptySummary = new HashMap<>();
                    emptySummary.put("totalEntries", 0);
                    emptySummary.put("orderCount", 0);
                    emptySummary.put("keys", Set.of());
                    emptySummary.put("sampleOrders", List.of());
                    return emptySummary;
                });
    }
}

package com.sivalabs.bookstore.orders.cache;

import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCacheService<K, V> {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractCacheService.class);

    protected final IMap<K, Object> cache;
    protected final CacheErrorHandler errorHandler;
    protected final Class<V> valueType;

    protected AbstractCacheService(IMap<K, Object> cache, CacheErrorHandler errorHandler, Class<V> valueType) {
        this.cache = cache;
        this.errorHandler = errorHandler;
        this.valueType = valueType;
        logger.info(
                "{} initialized with cache: {} and error handler", getClass().getSimpleName(), cache.getName());
    }

    public String getCacheStats() {
        return errorHandler.executeWithFallback(
                () -> {
                    StringBuilder stats = new StringBuilder();
                    stats.append(getCacheDisplayName()).append(" Cache Statistics:\n");
                    stats.append(String.format("  Cache Name: %s\n", cache.getName()));
                    stats.append(String.format("  Cache Size: %d\n", cache.size()));

                    if (cache.getLocalMapStats() != null) {
                        var localStats = cache.getLocalMapStats();
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

    public boolean isHealthy() {
        return errorHandler.checkCacheHealth(() -> {
            try {
                K healthCheckKey = createHealthCheckKey();
                cache.put(healthCheckKey, "health-check-value");
                Object value = cache.get(healthCheckKey);
                cache.remove(healthCheckKey);
                return "health-check-value".equals(value);
            } catch (Exception e) {
                logger.debug("Cache health check failed", e);
                return false;
            }
        });
    }

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

    public boolean isCircuitBreakerOpen() {
        return errorHandler.isCircuitOpen();
    }

    public boolean shouldFallbackToDatabase(String operationName) {
        if (errorHandler.isCircuitOpen()) {
            logger.debug("Circuit breaker is open - recommending database fallback for {}", operationName);
            return true;
        }
        return errorHandler.shouldFallbackToDatabase(operationName);
    }

    public boolean resetCircuitBreaker() {
        logger.warn("Manually resetting cache circuit breaker - used for recovery or testing");
        return errorHandler.executeVoidOperation(
                () -> {
                    errorHandler.resetErrorState();
                    logger.info("Cache circuit breaker has been manually reset");
                },
                "resetCircuitBreaker",
                "manual-reset");
    }

    public boolean existsInCache(K key) {
        return errorHandler.executeWithFallback(
                () -> {
                    boolean exists = cache.containsKey(key);
                    logger.debug("Cache existence check for {}: {}", key, exists);
                    return exists;
                },
                "existsInCache",
                String.valueOf(key),
                () -> {
                    logger.debug("Cache existence check failed for {}, assuming false", key);
                    return false;
                });
    }

    public boolean removeFromCache(K key) {
        logger.debug("Removing {} from cache: {}", getCacheDisplayName().toLowerCase(), key);
        return errorHandler.executeVoidOperation(
                () -> {
                    Object removed = cache.remove(key);
                    if (removed != null) {
                        logger.debug("{} removed from cache successfully: {}", getCacheDisplayName(), key);
                    } else {
                        logger.debug("{} not in cache for removal: {}", getCacheDisplayName(), key);
                    }
                },
                "removeFromCache",
                String.valueOf(key));
    }

    public int warmUpCache(Iterable<K> keys) {
        int successCount = 0;
        logger.info("Starting {} cache warm-up", getCacheDisplayName().toLowerCase());
        for (K key : keys) {
            boolean warmed = errorHandler.executeWithFallback(
                    () -> {
                        Object value = cache.get(key);
                        return value != null;
                    },
                    "warmUpCache",
                    String.valueOf(key),
                    () -> false);
            if (warmed) {
                successCount++;
            }
        }
        logger.info("{} cache warm-up completed. {} entries preloaded.", getCacheDisplayName(), successCount);
        return successCount;
    }

    protected boolean cacheEntity(K key, V entity) {
        if (entity == null) {
            logger.warn("Attempted to cache null entity for key: {}", key);
            return false;
        }

        return errorHandler.executeVoidOperation(() -> cache.put(key, entity), "cacheEntity", String.valueOf(key));
    }

    protected boolean updateCachedEntity(K key, V entity) {
        if (entity == null) {
            logger.warn("Attempted to update cache with null entity for key: {}", key);
            return false;
        }

        return errorHandler.executeVoidOperation(
                () -> {
                    if (cache.containsKey(key)) {
                        cache.put(key, entity);
                        logger.debug("Updated cached entity for key: {}", key);
                    } else {
                        logger.debug("No cached entity found for key: {} to update", key);
                    }
                },
                "updateCachedEntity",
                String.valueOf(key));
    }

    protected V safeCast(Object cachedValue, K key) {
        if (cachedValue == null) {
            return null;
        }
        if (!valueType.isInstance(cachedValue)) {
            logger.warn(
                    "Cached value type mismatch for key {}. Expected: {}, Actual: {}",
                    key,
                    valueType.getName(),
                    cachedValue.getClass().getName());
            return null;
        }
        return valueType.cast(cachedValue);
    }

    protected abstract String getCacheDisplayName();

    protected abstract K createHealthCheckKey();
}

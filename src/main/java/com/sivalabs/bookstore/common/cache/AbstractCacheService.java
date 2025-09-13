package com.sivalabs.bookstore.common.cache;

import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for cache services providing common functionality.
 *
 * This class encapsulates common cache operations and patterns used across
 * different domain-specific cache services, reducing code duplication and
 * ensuring consistent behavior.
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 */
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

    /**
     * Get cache statistics and health information.
     *
     * @return String containing cache statistics
     */
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

    /**
     * Check cache health by performing a simple operation.
     *
     * @return true if cache is healthy, false otherwise
     */
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
     * Check if the circuit breaker is currently open (cache unavailable).
     *
     * @return true if circuit breaker is open, false otherwise
     */
    public boolean isCircuitBreakerOpen() {
        return errorHandler.isCircuitOpen();
    }

    /**
     * Determine if cache operations should fallback to database.
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
     *
     * @return true if reset was successful
     */
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

    /**
     * Check if a key exists in the cache.
     *
     * @param key the key to check
     * @return true if the key exists in cache, false otherwise
     */
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

    /**
     * Remove an entry from the cache.
     *
     * @param key the key to remove
     * @return true if removal was successful, false otherwise
     */
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

    /**
     * Warm up the cache by preloading frequently accessed entries.
     *
     * @param keys collection of keys to preload
     * @return number of entries successfully preloaded
     */
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

        logger.info("{} cache warm-up completed: {} entries preloaded", getCacheDisplayName(), successCount);
        return successCount;
    }

    /**
     * Get the display name for this cache type (e.g., "Products", "Orders", "Inventory").
     * Subclasses should override this method to provide appropriate display names.
     *
     * @return display name for logging and error messages
     */
    protected abstract String getCacheDisplayName();

    /**
     * Create a unique health check key for this cache type.
     * Subclasses should override this method to provide type-appropriate keys.
     *
     * @return health check key
     */
    protected abstract K createHealthCheckKey();

    /**
     * Safely cast cached object to the expected value type.
     *
     * @param cachedValue the object retrieved from cache
     * @param key the cache key for logging purposes
     * @return the cast object or null if casting fails
     */
    @SuppressWarnings("unchecked")
    protected V safeCast(Object cachedValue, K key) {
        if (cachedValue == null) {
            return null;
        }

        if (valueType.isInstance(cachedValue)) {
            return (V) cachedValue;
        } else {
            logger.warn("Unexpected object type in cache for key {}: {}", key, cachedValue.getClass());
            return null;
        }
    }

    /**
     * Cache an entity with null validation.
     *
     * @param key the cache key
     * @param value the value to cache
     * @return true if caching was successful, false otherwise
     */
    protected boolean cacheEntity(K key, V value) {
        if (value == null) {
            logger.warn(
                    "Attempted to cache null {} for key: {}",
                    getCacheDisplayName().toLowerCase(),
                    key);
            return false;
        }

        logger.debug("Caching {}: {}", getCacheDisplayName().toLowerCase(), key);

        return errorHandler.executeVoidOperation(
                () -> {
                    cache.put(key, value);
                    logger.debug("{} cached successfully: {}", getCacheDisplayName(), key);
                },
                "cache" + getCacheDisplayName(),
                String.valueOf(key));
    }

    /**
     * Update an existing cached entity.
     *
     * @param key the cache key
     * @param value the updated value
     * @return true if update was successful, false otherwise
     */
    protected boolean updateCachedEntity(K key, V value) {
        if (value == null) {
            logger.warn(
                    "Attempted to update cache with null {} for key: {}",
                    getCacheDisplayName().toLowerCase(),
                    key);
            return false;
        }

        logger.debug("Updating cached {}: {}", getCacheDisplayName().toLowerCase(), key);

        return errorHandler.executeVoidOperation(
                () -> {
                    Object previous = cache.replace(key, value);
                    if (previous != null) {
                        logger.debug(
                                "Cached {} updated successfully: {}",
                                getCacheDisplayName().toLowerCase(),
                                key);
                    } else {
                        logger.debug("{} not in cache, performing regular put: {}", getCacheDisplayName(), key);
                        cache.put(key, value);
                    }
                },
                "updateCached" + getCacheDisplayName(),
                String.valueOf(key));
    }
}

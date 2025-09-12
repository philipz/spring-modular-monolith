package com.sivalabs.bookstore.catalog.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Service providing cache operations abstraction for Product entities.
 *
 * This is the product-focused counterpart to OrderCacheService. It wires the
 * products cache map and the shared CacheErrorHandler. Detailed cache methods
 * will be implemented in subsequent tasks.
 */
@Service
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Lazy
public class ProductCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ProductCacheService.class);

    private final IMap<String, Object> productsCache;
    private final CacheErrorHandler errorHandler;

    public ProductCacheService(
            @Qualifier("productsCache") IMap<String, Object> productsCache, CacheErrorHandler errorHandler) {
        this.productsCache = productsCache;
        this.errorHandler = errorHandler;
        logger.info("ProductCacheService initialized with cache: {} and error handler", productsCache.getName());
    }

    /**
     * Find a product by its unique product code from the cache.
     *
     * @param productCode the product code to search for
     * @return Optional containing the product if found in cache, empty if not found or cache error
     */
    public Optional<ProductEntity> findByProductCode(String productCode) {
        logger.debug("Looking up product in cache: {}", productCode);

        return errorHandler.executeWithFallback(
                () -> {
                    Object cachedValue = productsCache.get(productCode);
                    if (cachedValue instanceof ProductEntity productEntity) {
                        logger.debug("Product found in cache: {}", productCode);
                        return Optional.of(productEntity);
                    } else if (cachedValue != null) {
                        logger.warn(
                                "Unexpected object type in cache for key {}: {}", productCode, cachedValue.getClass());
                        return Optional.empty();
                    } else {
                        logger.debug("Product not found in cache: {}", productCode);
                        return Optional.empty();
                    }
                },
                "findByProductCode",
                productCode,
                () -> {
                    logger.debug("Cache lookup failed for {}, returning empty", productCode);
                    return Optional.empty();
                });
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
                    stats.append("Products Cache Statistics:\n");
                    stats.append(String.format("  Cache Name: %s\n", productsCache.getName()));
                    stats.append(String.format("  Cache Size: %d\n", productsCache.size()));

                    if (productsCache.getLocalMapStats() != null) {
                        var localStats = productsCache.getLocalMapStats();
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
                String healthCheckKey = "health-check-" + System.currentTimeMillis();
                productsCache.put(healthCheckKey, "health-check-value");
                Object value = productsCache.get(healthCheckKey);
                productsCache.remove(healthCheckKey);
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
     * Find a product with automatic fallback based on circuit breaker state.
     *
     * @param productCode product code to find
     * @param fallbackFunction supplier to call if cache is unavailable or miss
     * @return Optional containing the product if found, result of fallback otherwise
     */
    public Optional<ProductEntity> findWithAutomaticFallback(
            String productCode, java.util.function.Supplier<Optional<ProductEntity>> fallbackFunction) {
        if (shouldFallbackToDatabase("findWithAutomaticFallback")) {
            logger.debug("Circuit breaker recommends database fallback for product lookup: {}", productCode);
            return fallbackFunction.get();
        }

        Optional<ProductEntity> cached = findByProductCode(productCode);

        if (cached.isEmpty() && !errorHandler.isCircuitOpen()) {
            logger.debug("Cache miss for product {}, trying fallback", productCode);
            return fallbackFunction.get();
        }

        return cached;
    }

    /**
     * Warm up the cache by preloading frequently accessed products.
     *
     * @param productCodes collection of product codes to preload
     * @return number of products successfully preloaded
     */
    public int warmUpCache(Iterable<String> productCodes) {
        int successCount = 0;
        logger.info("Starting products cache warm-up");

        for (String code : productCodes) {
            boolean warmed = errorHandler.executeWithFallback(
                    () -> {
                        Object value = productsCache.get(code);
                        return value != null;
                    },
                    "warmUpCache",
                    code,
                    () -> false);

            if (warmed) {
                successCount++;
            }
        }

        logger.info("Products cache warm-up completed: {} products preloaded", successCount);
        return successCount;
    }

    /**
     * Cache a product entity.
     *
     * @param productCode the product code (cache key)
     * @param product the product entity to cache
     * @return true if caching was successful, false otherwise
     */
    public boolean cacheProduct(String productCode, ProductEntity product) {
        if (product == null) {
            logger.warn("Attempted to cache null product for key: {}", productCode);
            return false;
        }

        logger.debug("Caching product: {} with ID: {}", productCode, product.getId());

        return errorHandler.executeVoidOperation(
                () -> {
                    productsCache.put(productCode, product);
                    logger.debug("Product cached successfully: {}", productCode);
                },
                "cacheProduct",
                productCode);
    }

    /**
     * Update an existing cached product.
     *
     * @param productCode the product code (cache key)
     * @param product the updated product entity
     * @return true if update was successful, false otherwise
     */
    public boolean updateCachedProduct(String productCode, ProductEntity product) {
        if (product == null) {
            logger.warn("Attempted to update cache with null product for key: {}", productCode);
            return false;
        }

        logger.debug("Updating cached product: {}", productCode);

        return errorHandler.executeVoidOperation(
                () -> {
                    Object previous = productsCache.replace(productCode, product);
                    if (previous != null) {
                        logger.debug("Cached product updated successfully: {}", productCode);
                    } else {
                        logger.debug("Product not in cache, performing regular put: {}", productCode);
                        productsCache.put(productCode, product);
                    }
                },
                "updateCachedProduct",
                productCode);
    }

    /**
     * Remove a product from the cache.
     *
     * @param productCode the product code to remove
     * @return true if removal was successful, false otherwise
     */
    public boolean removeFromCache(String productCode) {
        logger.debug("Removing product from cache: {}", productCode);

        return errorHandler.executeVoidOperation(
                () -> {
                    Object removed = productsCache.remove(productCode);
                    if (removed != null) {
                        logger.debug("Product removed from cache successfully: {}", productCode);
                    } else {
                        logger.debug("Product not in cache for removal: {}", productCode);
                    }
                },
                "removeFromCache",
                productCode);
    }

    /**
     * Check if a product exists in the cache.
     *
     * @param productCode the product code to check
     * @return true if the product exists in cache, false otherwise
     */
    public boolean existsInCache(String productCode) {
        return errorHandler.executeWithFallback(
                () -> {
                    boolean exists = productsCache.containsKey(productCode);
                    logger.debug("Cache existence check for {}: {}", productCode, exists);
                    return exists;
                },
                "existsInCache",
                productCode,
                () -> {
                    logger.debug("Cache existence check failed for {}, assuming false", productCode);
                    return false;
                });
    }
}

package com.sivalabs.bookstore.inventory.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import com.sivalabs.bookstore.inventory.domain.InventoryEntity;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Service providing cache operations abstraction for Inventory entities.
 *
 * This is the inventory-focused counterpart to OrderCacheService and ProductCacheService.
 * It manages caching of inventory data using Long IDs as keys and provides consistent
 * error handling through the shared CacheErrorHandler.
 *
 * Key features:
 * - Long ID-based cache operations for inventory lookups
 * - Consistent error handling with circuit breaker pattern
 * - Optimized TTL configuration for inventory volatility
 * - Integration with inventory domain operations
 */
@Service
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Lazy
public class InventoryCacheService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryCacheService.class);

    private final IMap<Long, Object> inventoryCache;
    private final CacheErrorHandler errorHandler;

    public InventoryCacheService(
            @Qualifier("inventoryCache") IMap<Long, Object> inventoryCache,
            @Autowired(required = false) CacheErrorHandler errorHandler) {
        this.inventoryCache = inventoryCache;
        this.errorHandler = errorHandler != null ? errorHandler : new CacheErrorHandler();
        logger.info(
                "InventoryCacheService initialized with cache: {} and error handler: {}",
                inventoryCache.getName(),
                errorHandler != null ? "provided" : "default");
    }

    /**
     * Find an inventory record by its unique ID from the cache.
     *
     * @param inventoryId the inventory ID to search for
     * @return Optional containing the inventory if found in cache, empty if not found or cache error
     */
    public Optional<InventoryEntity> findById(Long inventoryId) {
        logger.debug("Looking up inventory in cache: {}", inventoryId);

        return errorHandler.executeWithFallback(
                () -> {
                    Object cachedValue = inventoryCache.get(inventoryId);
                    if (cachedValue instanceof InventoryEntity inventoryEntity) {
                        logger.debug("Inventory found in cache: {}", inventoryId);
                        return Optional.of(inventoryEntity);
                    } else if (cachedValue != null) {
                        logger.warn(
                                "Unexpected object type in cache for key {}: {}", inventoryId, cachedValue.getClass());
                        return Optional.empty();
                    } else {
                        logger.debug("Inventory not found in cache: {}", inventoryId);
                        return Optional.empty();
                    }
                },
                "findById",
                String.valueOf(inventoryId),
                () -> {
                    logger.debug("Cache lookup failed for {}, returning empty", inventoryId);
                    return Optional.empty();
                });
    }

    /**
     * Cache an inventory entity.
     *
     * @param inventoryId the inventory ID (cache key)
     * @param inventory the inventory entity to cache
     * @return true if caching was successful, false otherwise
     */
    public boolean cacheInventory(Long inventoryId, InventoryEntity inventory) {
        if (inventory == null) {
            logger.warn("Attempted to cache null inventory for key: {}", inventoryId);
            return false;
        }

        logger.debug("Caching inventory: {} with product code: {}", inventoryId, inventory.getProductCode());

        return errorHandler.executeVoidOperation(
                () -> {
                    inventoryCache.put(inventoryId, inventory);
                    logger.debug("Inventory cached successfully: {}", inventoryId);
                },
                "cacheInventory",
                String.valueOf(inventoryId));
    }

    /**
     * Update an existing cached inventory record.
     *
     * @param inventoryId the inventory ID (cache key)
     * @param inventory the updated inventory entity
     * @return true if update was successful, false otherwise
     */
    public boolean updateCachedInventory(Long inventoryId, InventoryEntity inventory) {
        if (inventory == null) {
            logger.warn("Attempted to update cache with null inventory for key: {}", inventoryId);
            return false;
        }

        logger.debug("Updating cached inventory: {} with new quantity: {}", inventoryId, inventory.getQuantity());

        return errorHandler.executeVoidOperation(
                () -> {
                    // Use replace operation for updates
                    Object previousValue = inventoryCache.replace(inventoryId, inventory);
                    if (previousValue != null) {
                        logger.debug("Inventory updated successfully in cache: {}", inventoryId);
                    } else {
                        logger.debug("Inventory not found in cache for update, performing put: {}", inventoryId);
                        inventoryCache.put(inventoryId, inventory);
                    }
                },
                "updateCachedInventory",
                String.valueOf(inventoryId));
    }

    /**
     * Remove an inventory record from the cache.
     *
     * @param inventoryId the inventory ID to remove
     * @return true if removal was successful, false otherwise
     */
    public boolean removeFromCache(Long inventoryId) {
        logger.debug("Removing inventory from cache: {}", inventoryId);

        return errorHandler.executeVoidOperation(
                () -> {
                    Object removedValue = inventoryCache.remove(inventoryId);
                    if (removedValue != null) {
                        logger.debug("Inventory removed successfully from cache: {}", inventoryId);
                    } else {
                        logger.debug("Inventory not found in cache for removal: {}", inventoryId);
                    }
                },
                "removeFromCache",
                String.valueOf(inventoryId));
    }

    /**
     * Check if an inventory record exists in the cache.
     *
     * @param inventoryId the inventory ID to check
     * @return true if the inventory exists in cache, false otherwise
     */
    public boolean existsInCache(Long inventoryId) {
        return errorHandler.executeWithFallback(
                () -> {
                    boolean exists = inventoryCache.containsKey(inventoryId);
                    logger.debug("Cache existence check for {}: {}", inventoryId, exists);
                    return exists;
                },
                "existsInCache",
                String.valueOf(inventoryId),
                () -> {
                    logger.debug("Cache existence check failed for {}, assuming false", inventoryId);
                    return false;
                });
    }

    /**
     * Check if the cache is healthy and operational.
     *
     * @return true if cache is healthy, false otherwise
     */
    public boolean isHealthy() {
        return errorHandler.checkCacheHealth(() -> {
            try {
                // Perform a simple operation to test cache connectivity
                inventoryCache.size(); // This will throw if cache is unavailable
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Get current circuit breaker status.
     *
     * @return "OPEN" if circuit breaker is open (cache unavailable), "CLOSED" otherwise
     */
    public String getCircuitBreakerStatus() {
        return errorHandler.isCircuitOpen() ? "OPEN" : "CLOSED";
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
     * Get cache statistics and health information.
     *
     * @return String containing cache statistics
     */
    public String getCacheStats() {
        return errorHandler.executeWithFallback(
                () -> {
                    StringBuilder stats = new StringBuilder();
                    stats.append("Inventory Cache Statistics:\n");
                    stats.append(String.format("  Name: %s\n", inventoryCache.getName()));
                    stats.append(String.format("  Size: %d entries\n", inventoryCache.size()));
                    stats.append(String.format("  Circuit Breaker: %s\n", getCircuitBreakerStatus()));

                    // Add error statistics
                    stats.append("\n").append(errorHandler.getCacheErrorStats());

                    return stats.toString();
                },
                "getCacheStats",
                "inventory-cache",
                () -> "Cache statistics unavailable - circuit breaker is open");
    }

    /**
     * Reset circuit breaker state and clear error statistics.
     * Useful for manual recovery or testing scenarios.
     */
    public void resetCircuitBreaker() {
        errorHandler.resetErrorState();
        logger.info("Inventory cache circuit breaker has been reset");
    }

    /**
     * Find inventory by product code from the cache.
     * This method searches through cache values to find inventory by product code.
     *
     * @param productCode the product code to search for
     * @return Optional containing the inventory if found in cache, empty if not found or cache error
     */
    public Optional<InventoryEntity> findByProductCode(String productCode) {
        logger.debug("Looking up inventory by product code in cache: {}", productCode);

        return errorHandler.executeWithFallback(
                () -> {
                    // Since cache is keyed by Long ID but we need to search by productCode,
                    // we need to iterate through cache values (this is not optimal for large caches)
                    // In a production system, consider using a separate cache with productCode as key
                    for (Object value : inventoryCache.values()) {
                        if (value instanceof InventoryEntity inventoryEntity) {
                            if (productCode.equals(inventoryEntity.getProductCode())) {
                                logger.debug("Inventory found in cache by product code: {}", productCode);
                                return Optional.of(inventoryEntity);
                            }
                        }
                    }
                    logger.debug("Inventory not found in cache by product code: {}", productCode);
                    return Optional.empty();
                },
                "findByProductCode",
                productCode,
                () -> {
                    logger.debug("Cache lookup by product code failed for {}, returning empty", productCode);
                    return Optional.empty();
                });
    }

    /**
     * Find inventory with automatic fallback to database if cache fails.
     * This method integrates with the circuit breaker pattern.
     *
     * @param inventoryId the inventory ID to find
     * @param databaseFallback the fallback function to call if cache fails
     * @return Optional containing the inventory from cache or database
     */
    public Optional<InventoryEntity> findWithAutomaticFallback(
            Long inventoryId, java.util.function.Supplier<Optional<InventoryEntity>> databaseFallback) {

        // First try cache if circuit breaker is closed
        if (!errorHandler.isCircuitOpen()) {
            Optional<InventoryEntity> cacheResult = findById(inventoryId);

            // If cache returns result, use it
            if (cacheResult.isPresent()) {
                return cacheResult;
            }

            // Cache miss - always fallback to database for normal operation
            logger.debug("Cache miss for inventory lookup: {} - using database fallback", inventoryId);
            return databaseFallback.get();
        }

        // Circuit breaker is open, skip cache and use database directly
        logger.debug(
                "Circuit breaker open for inventory cache - using database fallback for inventory ID: {}", inventoryId);
        return databaseFallback.get();
    }

    /**
     * Find inventory by product code with automatic fallback to database if cache fails.
     * This method integrates with the circuit breaker pattern for productCode-based lookups.
     *
     * @param productCode the product code to find
     * @param databaseFallback the fallback function to call if cache fails
     * @return Optional containing the inventory from cache or database
     */
    public Optional<InventoryEntity> findByProductCodeWithFallback(
            String productCode, java.util.function.Supplier<Optional<InventoryEntity>> databaseFallback) {

        // First try cache if circuit breaker is closed
        if (!errorHandler.isCircuitOpen()) {
            Optional<InventoryEntity> cacheResult = findByProductCode(productCode);

            // If cache returns result, use it
            if (cacheResult.isPresent()) {
                return cacheResult;
            }

            // Cache miss - always fallback to database for normal operation
            logger.debug("Cache miss for inventory lookup by product code: {} - using database fallback", productCode);
            return databaseFallback.get();
        }

        // Circuit breaker is open, skip cache and use database directly
        logger.debug(
                "Circuit breaker open for inventory cache - using database fallback for product code: {}", productCode);
        return databaseFallback.get();
    }

    /**
     * Warm up the cache by preloading frequently accessed inventory records.
     *
     * @param inventoryIds collection of inventory IDs to preload
     * @return number of inventory records successfully preloaded
     */
    public int warmUpCache(Iterable<Long> inventoryIds) {
        int successCount = 0;
        logger.info("Starting inventory cache warm-up");

        for (Long id : inventoryIds) {
            boolean warmed = errorHandler.executeWithFallback(
                    () -> {
                        Object value = inventoryCache.get(id);
                        return value != null;
                    },
                    "warmUpCache",
                    String.valueOf(id),
                    () -> false);

            if (warmed) {
                successCount++;
            }
        }

        logger.info("Inventory cache warm-up completed: {} records preloaded", successCount);
        return successCount;
    }
}

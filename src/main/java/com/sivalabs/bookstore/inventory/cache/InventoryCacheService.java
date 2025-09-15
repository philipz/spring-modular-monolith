package com.sivalabs.bookstore.inventory.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.AbstractCacheService;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import com.sivalabs.bookstore.inventory.domain.InventoryEntity;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Service providing cache operations abstraction for Inventory entities.
 *
 * This service extends AbstractCacheService to provide inventory-specific
 * cache operations while inheriting common cache functionality.
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
public class InventoryCacheService extends AbstractCacheService<Long, InventoryEntity> {

    private final InventoryByProductCodeIndex index;

    public InventoryCacheService(
            @Qualifier("inventoryCache") IMap<Long, Object> inventoryCache,
            @Autowired(required = false) CacheErrorHandler errorHandler,
            ObjectProvider<InventoryByProductCodeIndex> indexProvider) {
        super(inventoryCache, errorHandler != null ? errorHandler : new CacheErrorHandler(), InventoryEntity.class);
        this.index = indexProvider != null ? indexProvider.getIfAvailable() : null;
    }

    @Override
    protected String getCacheDisplayName() {
        return "Inventory";
    }

    @Override
    protected Long createHealthCheckKey() {
        return System.currentTimeMillis();
    }

    /**
     * Find an inventory record by its unique ID from the cache.
     *
     * @param inventoryId the inventory ID to search for
     * @return Optional containing the inventory if found in cache, empty if not found or cache error
     */
    public Optional<InventoryEntity> findById(Long inventoryId) {
        return errorHandler.executeWithFallback(
                () -> {
                    Object cachedValue = cache.get(inventoryId);
                    InventoryEntity inventory = safeCast(cachedValue, inventoryId);
                    return Optional.ofNullable(inventory);
                },
                "findById",
                String.valueOf(inventoryId),
                Optional::empty);
    }

    /**
     * Cache an inventory entity.
     *
     * @param inventoryId the inventory ID (cache key)
     * @param inventory the inventory entity to cache
     * @return true if caching was successful, false otherwise
     */
    public boolean cacheInventory(Long inventoryId, InventoryEntity inventory) {
        boolean cached = cacheEntity(inventoryId, inventory);
        if (cached && index != null && inventory != null && inventory.getProductCode() != null) {
            index.updateIndex(inventory.getProductCode(), inventoryId);
        }
        return cached;
    }

    /**
     * Update an existing cached inventory record.
     *
     * @param inventoryId the inventory ID (cache key)
     * @param inventory the updated inventory entity
     * @return true if update was successful, false otherwise
     */
    public boolean updateCachedInventory(Long inventoryId, InventoryEntity inventory) {
        boolean updated = updateCachedEntity(inventoryId, inventory);
        if (updated && index != null && inventory != null && inventory.getProductCode() != null) {
            index.updateIndex(inventory.getProductCode(), inventoryId);
        }
        return updated;
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
                    if (index != null) {
                        Optional<Long> idOpt = index.findInventoryIdByProductCode(productCode);
                        if (idOpt.isPresent()) {
                            return findById(idOpt.get());
                        }
                        logger.debug("Index miss for product code: {}", productCode);
                        return Optional.empty();
                    }

                    // Fallback legacy scan when index not available
                    for (Object value : cache.values()) {
                        if (valueType.isInstance(value)) {
                            InventoryEntity inventoryEntity = (InventoryEntity) value;
                            if (productCode.equals(inventoryEntity.getProductCode())) {
                                logger.debug("Inventory found in cache by product code via scan: {}", productCode);
                                return Optional.of(inventoryEntity);
                            }
                        }
                    }
                    logger.debug("Inventory not found in cache by product code: {}", productCode);
                    return Optional.empty();
                },
                "findByProductCode",
                productCode,
                Optional::empty);
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
}

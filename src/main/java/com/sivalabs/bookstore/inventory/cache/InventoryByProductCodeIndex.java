package com.sivalabs.bookstore.inventory.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.AbstractCacheService;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Index cache for mapping productCode -> inventoryId for O(1) lookups.
 *
 * This service provides a dedicated map where keys are product codes (String)
 * and values are inventory IDs (Long). It avoids scanning values() on the
 * primary inventory cache when looking up by product code.
 */
@Service
@Lazy
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(name = "inventoryByProductCodeCache")
public class InventoryByProductCodeIndex extends AbstractCacheService<String, Long> {

    private static final Logger log = LoggerFactory.getLogger(InventoryByProductCodeIndex.class);

    public InventoryByProductCodeIndex(
            @Qualifier("inventoryByProductCodeCache") IMap<String, Object> indexCache,
            @Autowired(required = false) CacheErrorHandler errorHandler) {
        super(indexCache, errorHandler != null ? errorHandler : new CacheErrorHandler(), Long.class);
        log.info("InventoryByProductCodeIndex initialized for cache: {}", indexCache.getName());
    }

    @Override
    protected String getCacheDisplayName() {
        return "InventoryByProductCodeIndex";
    }

    @Override
    protected String createHealthCheckKey() {
        return "health-check-" + System.currentTimeMillis();
    }

    /**
     * Find inventory ID for a given product code using index cache.
     */
    public Optional<Long> findInventoryIdByProductCode(String productCode) {
        return errorHandler.executeWithFallback(
                () -> {
                    Object cached = cache.get(productCode);
                    Long id = safeCast(cached, productCode);
                    return Optional.ofNullable(id);
                },
                "findInventoryIdByProductCode",
                productCode,
                Optional::empty);
    }

    /**
     * Update or insert index entry for product code.
     */
    public boolean updateIndex(String productCode, Long inventoryId) {
        return cacheEntity(productCode, inventoryId);
    }

    /**
     * Remove index entry for product code.
     */
    public boolean removeFromIndex(String productCode) {
        return removeFromCache(productCode);
    }
}

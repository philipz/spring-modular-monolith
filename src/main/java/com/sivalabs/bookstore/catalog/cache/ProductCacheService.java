package com.sivalabs.bookstore.catalog.cache;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.common.cache.AbstractCacheService;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Service providing cache operations abstraction for Product entities.
 *
 * This service extends AbstractCacheService to provide product-specific
 * cache operations while inheriting common cache functionality.
 */
@Service
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Lazy
public class ProductCacheService extends AbstractCacheService<String, ProductEntity> {

    public ProductCacheService(
            @Qualifier("productsCache") IMap<String, Object> productsCache,
            @Autowired(required = false) CacheErrorHandler errorHandler) {
        super(productsCache, errorHandler != null ? errorHandler : new CacheErrorHandler(), ProductEntity.class);
    }

    @Override
    protected String getCacheDisplayName() {
        return "Products";
    }

    @Override
    protected String createHealthCheckKey() {
        return "health-check-" + System.currentTimeMillis();
    }

    /**
     * Find a product by its unique product code from the cache.
     *
     * @param productCode the product code to search for
     * @return Optional containing the product if found in cache, empty if not found or cache error
     */
    public Optional<ProductEntity> findByProductCode(String productCode) {
        return errorHandler.executeWithFallback(
                () -> {
                    Object cachedValue = cache.get(productCode);
                    ProductEntity product = safeCast(cachedValue, productCode);
                    return Optional.ofNullable(product);
                },
                "findByProductCode",
                productCode,
                Optional::empty);
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
            return fallbackFunction.get();
        }

        Optional<ProductEntity> cached = findByProductCode(productCode);

        if (cached.isEmpty() && !errorHandler.isCircuitOpen()) {
            return fallbackFunction.get();
        }

        return cached;
    }

    /**
     * Cache a product entity.
     *
     * @param productCode the product code (cache key)
     * @param product the product entity to cache
     * @return true if caching was successful, false otherwise
     */
    public boolean cacheProduct(String productCode, ProductEntity product) {
        return cacheEntity(productCode, product);
    }

    /**
     * Update an existing cached product.
     *
     * @param productCode the product code (cache key)
     * @param product the updated product entity
     * @return true if update was successful, false otherwise
     */
    public boolean updateCachedProduct(String productCode, ProductEntity product) {
        return updateCachedEntity(productCode, product);
    }
}

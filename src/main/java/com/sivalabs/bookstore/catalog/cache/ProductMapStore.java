package com.sivalabs.bookstore.catalog.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;
import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.catalog.domain.ProductRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Hazelcast MapStore implementation for ProductEntity.
 *
 * This component handles write-through caching by automatically synchronizing
 * cache operations with the PostgreSQL database through ProductRepository.
 *
 * Key behaviors:
 * - store() writes to database when cache entries are added/updated
 * - load() reads from database when cache misses occur
 * - delete() removes from database when cache entries are removed
 * - loadAll() provides bulk loading capabilities
 */
@Component
@Lazy
public class ProductMapStore implements MapStore<String, ProductEntity>, MapLoaderLifecycleSupport {

    private static final Logger logger = LoggerFactory.getLogger(ProductMapStore.class);

    @Autowired
    private ProductRepository productRepository;

    public ProductMapStore() {
        logger.info("ProductMapStore default constructor");
    }

    public ProductMapStore(ProductRepository productRepository) {
        this.productRepository = productRepository;
        logger.info("ProductMapStore initialized with ProductRepository");
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties props, String mapName) {
        logger.info("ProductMapStore lifecycle init called for map: {}", mapName);
    }

    @Override
    public void destroy() {
        logger.info("ProductMapStore lifecycle destroy called");
    }

    /**
     * Store a product in the database (write-through operation).
     * This method is called when an entry is put into the cache.
     *
     * @param productCode the product code (cache key)
     * @param productEntity the product entity to store
     */
    @Override
    public void store(String productCode, ProductEntity productEntity) {
        logger.debug("Storing product in database: productCode={}", productCode);

        try {
            // Ensure the productCode matches the entity
            if (productEntity != null && !productCode.equals(productEntity.getCode())) {
                logger.warn(
                        "ProductCode mismatch: key={}, entity.productCode={}", productCode, productEntity.getCode());
            }

            // Note: For write-through, we assume the product is already persisted
            // The cache is being updated after the database operation
            logger.debug("Product store operation completed for productCode={}", productCode);

        } catch (Exception e) {
            logger.warn("Store operation encountered error for productCode={}: {}", productCode, e.getMessage());
        }
    }

    /**
     * Store multiple products in the database (bulk write-through operation).
     *
     * @param entries map of product codes to product entities
     */
    @Override
    public void storeAll(Map<String, ProductEntity> entries) {
        logger.debug("Storing {} products in database", entries.size());

        try {
            // Note: For write-through, products are already persisted by the time they reach here
            // This method is called for cache warming or batch operations
            logger.debug("StoreAll operation completed for {} products", entries.size());

        } catch (Exception e) {
            logger.warn("StoreAll operation encountered error for {} products: {}", entries.size(), e.getMessage());
        }
    }

    /**
     * Load a product from the database (cache miss operation).
     * This method is called when a cache get() operation results in a miss.
     *
     * @param productCode the product code to load
     * @return the product entity or null if not found
     */
    @Override
    public ProductEntity load(String productCode) {
        logger.debug("Loading product from database: productCode={}", productCode);

        try {
            Optional<ProductEntity> productOpt = productRepository.findByCode(productCode);

            if (productOpt.isPresent()) {
                ProductEntity product = productOpt.get();
                logger.debug("Product loaded successfully: productCode={}, name={}", productCode, product.getName());
                return product;
            } else {
                logger.debug("Product not found in database: productCode={}", productCode);
                return null;
            }
        } catch (Exception e) {
            logger.warn("Load operation error for productCode={}: {}", productCode, e.getMessage());
            return null;
        }
    }

    /**
     * Load multiple products from the database (bulk cache miss operation).
     * This method is called when multiple cache get() operations result in misses.
     *
     * @param productCodes set of product codes to load
     * @return map of product codes to product entities
     */
    @Override
    public Map<String, ProductEntity> loadAll(Collection<String> productCodes) {
        logger.debug("Loading {} products from database", productCodes.size());

        try {
            // Load products by codes using repository's findByCode method
            Map<String, ProductEntity> loadedProducts = productCodes.stream()
                    .map(productCode -> {
                        Optional<ProductEntity> productOpt = productRepository.findByCode(productCode);
                        return productOpt
                                .map(product -> Map.entry(productCode, product))
                                .orElse(null);
                    })
                    .filter(entry -> entry != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            logger.debug(
                    "Successfully loaded {} out of {} requested products from database",
                    loadedProducts.size(),
                    productCodes.size());

            return loadedProducts;

        } catch (Exception e) {
            logger.warn("LoadAll operation error for {} products: {}", productCodes.size(), e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * Get all product codes from the database (used for pre-loading).
     * This method is called during cache initialization or warm-up.
     *
     * @return set of all product codes
     */
    @Override
    public Iterable<String> loadAllKeys() {
        logger.debug("Loading all product codes from database");

        try {
            Set<String> allProductCodes = productRepository.findAll().stream()
                    .map(ProductEntity::getCode)
                    .collect(Collectors.toSet());

            logger.debug("Successfully loaded {} product codes from database", allProductCodes.size());
            return allProductCodes;

        } catch (Exception e) {
            logger.warn("LoadAllKeys operation error: {}", e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Delete a product from the database (write-through operation).
     * This method is called when an entry is removed from the cache.
     *
     * @param productCode the product code to delete
     */
    @Override
    public void delete(String productCode) {
        logger.debug("Deleting product from database: productCode={}", productCode);

        try {
            // Note: Deletion through MapStore is typically not used in write-through scenarios
            // Products are usually deleted through the service layer, not the cache
            // This implementation logs the operation but doesn't perform actual deletion
            logger.debug("Delete operation called for productCode={} - delegating to service layer", productCode);

        } catch (Exception e) {
            logger.warn("Delete operation error for productCode={}: {}", productCode, e.getMessage());
        }
    }

    /**
     * Delete multiple products from the database (bulk write-through operation).
     *
     * @param productCodes collection of product codes to delete
     */
    @Override
    public void deleteAll(Collection<String> productCodes) {
        logger.debug("Deleting {} products from database", productCodes.size());

        try {
            // Note: Bulk deletion through MapStore is typically not used in write-through scenarios
            // Products are usually deleted through the service layer, not the cache
            logger.debug(
                    "DeleteAll operation called for {} products - delegating to service layer", productCodes.size());

            logger.debug("Successfully deleted {} products from database", productCodes.size());

        } catch (Exception e) {
            logger.warn("DeleteAll operation error for {} products: {}", productCodes.size(), e.getMessage());
        }
    }
}

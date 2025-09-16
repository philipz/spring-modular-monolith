package com.sivalabs.bookstore.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spring.context.SpringManagedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Hazelcast configuration for distributed caching.
 *
 * This configuration creates a Hazelcast instance and configures the orders-cache
 * with appropriate settings for write-through caching.
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HazelcastConfig {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);

    private static final String ORDERS_CACHE_NAME = "orders-cache";
    private static final String PRODUCTS_CACHE_NAME = "products-cache";
    private static final String INVENTORY_CACHE_NAME = "inventory-cache";
    private static final String INVENTORY_BY_PRODUCT_CODE_CACHE_NAME = "inventory-by-product-code-cache";

    /**
     * Creates and configures the main Hazelcast configuration.
     *
     * @param cacheProperties externalized cache configuration
     * @return configured Hazelcast Config instance
     */
    @Bean
    public Config hazelcastConfiguration(CacheProperties cacheProperties) {
        logger.info("Initializing Hazelcast configuration");

        Config config = new Config();
        // Use dynamic instance name to avoid conflicts in tests
        String instanceName = "bookstore-hazelcast-" + System.currentTimeMillis();
        config.setInstanceName(instanceName);
        config.setClusterName("bookstore-cluster");
        config.getJetConfig().setEnabled(true);
        // Allow Hazelcast to inject Spring-managed dependencies into components like MapStore
        config.setManagedContext(new SpringManagedContext());

        // Configure the orders cache map
        MapConfig ordersCacheMapConfig = new MapConfig(ORDERS_CACHE_NAME);

        // Configure eviction policy and max size
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(cacheProperties.getMaxSize());
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        ordersCacheMapConfig.setEvictionConfig(evictionConfig);

        ordersCacheMapConfig.setTimeToLiveSeconds(cacheProperties.getTimeToLiveSeconds());
        ordersCacheMapConfig.setMaxIdleSeconds(cacheProperties.getMaxIdleSeconds());
        ordersCacheMapConfig.setBackupCount(cacheProperties.getBackupCount());
        ordersCacheMapConfig.setReadBackupData(cacheProperties.isReadBackupData());

        // Configure MapStore for orders cache (lazy to avoid circular dependency)
        configureMapStore(ordersCacheMapConfig, "com.sivalabs.bookstore.orders.cache.OrderMapStore", cacheProperties);

        ordersCacheMapConfig.setStatisticsEnabled(cacheProperties.isMetricsEnabled());

        config.addMapConfig(ordersCacheMapConfig);

        // Configure the products cache map (same configuration as orders cache)
        MapConfig productsCacheMapConfig = new MapConfig(PRODUCTS_CACHE_NAME);

        EvictionConfig productsEvictionConfig = new EvictionConfig();
        productsEvictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        productsEvictionConfig.setSize(cacheProperties.getMaxSize());
        productsEvictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        productsCacheMapConfig.setEvictionConfig(productsEvictionConfig);

        productsCacheMapConfig.setTimeToLiveSeconds(cacheProperties.getTimeToLiveSeconds());
        productsCacheMapConfig.setMaxIdleSeconds(cacheProperties.getMaxIdleSeconds());
        productsCacheMapConfig.setBackupCount(cacheProperties.getBackupCount());
        productsCacheMapConfig.setReadBackupData(cacheProperties.isReadBackupData());

        productsCacheMapConfig.setStatisticsEnabled(cacheProperties.isMetricsEnabled());

        // Configure MapStore for products cache (lazy to avoid circular dependency)
        configureMapStore(
                productsCacheMapConfig, "com.sivalabs.bookstore.catalog.cache.ProductMapStore", cacheProperties);

        config.addMapConfig(productsCacheMapConfig);

        // Configure the inventory cache map (same configuration as others but with shorter TTL)
        MapConfig inventoryCacheMapConfig = new MapConfig(INVENTORY_CACHE_NAME);

        EvictionConfig inventoryEvictionConfig = new EvictionConfig();
        inventoryEvictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        inventoryEvictionConfig.setSize(cacheProperties.getMaxSize());
        inventoryEvictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        inventoryCacheMapConfig.setEvictionConfig(inventoryEvictionConfig);

        // Use shorter TTL for inventory data due to volatility
        // Use externalized TTL for inventory cache instead of hardcoded value
        inventoryCacheMapConfig.setTimeToLiveSeconds(cacheProperties.getInventoryTimeToLiveSeconds());
        inventoryCacheMapConfig.setMaxIdleSeconds(cacheProperties.getMaxIdleSeconds());
        inventoryCacheMapConfig.setBackupCount(cacheProperties.getBackupCount());
        inventoryCacheMapConfig.setReadBackupData(cacheProperties.isReadBackupData());

        inventoryCacheMapConfig.setStatisticsEnabled(cacheProperties.isMetricsEnabled());

        // Configure MapStore for inventory cache (lazy to avoid circular dependency)
        configureMapStore(
                inventoryCacheMapConfig, "com.sivalabs.bookstore.inventory.cache.InventoryMapStore", cacheProperties);

        config.addMapConfig(inventoryCacheMapConfig);

        // Configure the inventory-by-product-code index cache (String -> Long)
        MapConfig inventoryByProductCodeMapConfig = new MapConfig(INVENTORY_BY_PRODUCT_CODE_CACHE_NAME);

        EvictionConfig indexEvictionConfig = new EvictionConfig();
        indexEvictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        indexEvictionConfig.setSize(cacheProperties.getMaxSize());
        indexEvictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        inventoryByProductCodeMapConfig.setEvictionConfig(indexEvictionConfig);

        // Align TTL with inventory cache TTL for consistency
        inventoryByProductCodeMapConfig.setTimeToLiveSeconds(cacheProperties.getInventoryTimeToLiveSeconds());
        inventoryByProductCodeMapConfig.setMaxIdleSeconds(cacheProperties.getMaxIdleSeconds());
        inventoryByProductCodeMapConfig.setBackupCount(cacheProperties.getBackupCount());
        inventoryByProductCodeMapConfig.setReadBackupData(cacheProperties.isReadBackupData());
        inventoryByProductCodeMapConfig.setStatisticsEnabled(cacheProperties.isMetricsEnabled());

        // No MapStore for index cache (derived data)

        config.addMapConfig(inventoryByProductCodeMapConfig);

        // Enable metrics if requested
        if (cacheProperties.isMetricsEnabled()) {
            config.getMetricsConfig().setEnabled(true);
        }

        // Configure serialization to support Java records and complex objects
        SerializationConfig serializationConfig = config.getSerializationConfig();

        // For Hazelcast 5.5.0, use portable serialization or JSON for better Java record support
        // Enable check class definition errors to false for better compatibility with records
        serializationConfig.setCheckClassDefErrors(false);
        serializationConfig.setUseNativeByteOrder(false);

        // Enable byte-order compatibility and better record handling
        serializationConfig.setAllowOverrideDefaultSerializers(true);
        serializationConfig.setAllowUnsafe(false);
        serializationConfig.setEnableCompression(false); // Disable compression for better debugging
        serializationConfig.setEnableSharedObject(false); // Disable shared object references for simpler serialization

        logger.info("Configured serialization for better Java record support");

        // Configure management center (disabled for now)
        config.getManagementCenterConfig().setScriptingEnabled(false);

        logger.info(
                "Hazelcast configuration completed - Instance: {}, Cluster: {}, Enhanced serialization: enabled",
                config.getInstanceName(),
                config.getClusterName());

        return config;
    }

    /**
     * Configures MapStore for a cache map using class name to avoid circular dependency.
     */
    private void configureMapStore(MapConfig mapConfig, String mapStoreClassName, CacheProperties cacheProperties) {
        try {
            MapStoreConfig mapStoreConfig = new MapStoreConfig();
            mapStoreConfig.setEnabled(true);
            mapStoreConfig.setClassName(mapStoreClassName);
            mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.LAZY);

            if (cacheProperties.isWriteThrough()) {
                mapStoreConfig.setWriteDelaySeconds(cacheProperties.getWriteDelaySeconds());
                mapStoreConfig.setWriteBatchSize(cacheProperties.getWriteBatchSize());
            }

            mapConfig.setMapStoreConfig(mapStoreConfig);

            logger.debug("MapStore {} configured for cache {}", mapStoreClassName, mapConfig.getName());
        } catch (Exception e) {
            logger.warn(
                    "Failed to configure MapStore {} for cache {}: {}",
                    mapStoreClassName,
                    mapConfig.getName(),
                    e.getMessage());
            // Continue without MapStore if configuration fails
        }
    }

    /**
     * Creates the Hazelcast instance using the provided configuration.
     *
     * @param config the Hazelcast configuration
     * @return HazelcastInstance
     */
    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance(Config hazelcastConfiguration) {
        logger.info("Creating Hazelcast instance");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(hazelcastConfiguration);
        logger.info("Hazelcast instance created successfully: {}", instance.getName());
        return instance;
    }

    /**
     * Creates the orders cache IMap bean.
     * Note: Using Object as value type to avoid module boundary violations.
     * The actual OrderEntity will be handled by the orders module components.
     *
     * @param hazelcastInstance the Hazelcast instance
     * @return IMap for orders cache
     */
    @Bean("ordersCache")
    @Lazy // Lazy initialization to allow MapStore bean to be created first
    public IMap<String, Object> ordersCache(HazelcastInstance hazelcastInstance) {
        logger.info("Creating orders cache map");
        IMap<String, Object> ordersMap = hazelcastInstance.getMap(ORDERS_CACHE_NAME);
        logger.info("Orders cache map created: {} with MapStore support", ORDERS_CACHE_NAME);
        return ordersMap;
    }

    /**
     * Provides cache name constant for other components.
     *
     * @return the orders cache name
     */
    @Bean("ordersCacheName")
    public String ordersCacheName() {
        return ORDERS_CACHE_NAME;
    }

    /**
     * Creates the products cache IMap bean.
     * Note: Using Object as value type to avoid module boundary violations.
     * The actual ProductEntity will be handled by the catalog module components.
     *
     * @param hazelcastInstance the Hazelcast instance
     * @return IMap for products cache
     */
    @Bean("productsCache")
    @Lazy // Keep consistent with ordersCache bean pattern
    public IMap<String, Object> productsCache(HazelcastInstance hazelcastInstance) {
        logger.info("Creating products cache map");
        IMap<String, Object> productsMap = hazelcastInstance.getMap(PRODUCTS_CACHE_NAME);
        logger.info("Products cache map created: {} with MapStore support", PRODUCTS_CACHE_NAME);
        return productsMap;
    }

    /**
     * Provides products cache name constant for other components.
     *
     * @return the products cache name
     */
    @Bean("productsCacheName")
    public String productsCacheName() {
        return PRODUCTS_CACHE_NAME;
    }

    /**
     * Creates the inventory cache IMap bean.
     * Note: Using Object as value type to avoid module boundary violations.
     * The actual InventoryEntity will be handled by the inventory module components.
     * Uses Long keys for inventory ID-based lookups.
     *
     * @param hazelcastInstance the Hazelcast instance
     * @return IMap for inventory cache
     */
    @Bean("inventoryCache")
    @Lazy // Keep consistent with other cache bean patterns
    public IMap<Long, Object> inventoryCache(HazelcastInstance hazelcastInstance) {
        logger.info("Creating inventory cache map");
        IMap<Long, Object> inventoryMap = hazelcastInstance.getMap(INVENTORY_CACHE_NAME);
        logger.info("Inventory cache map created: {} with MapStore support", INVENTORY_CACHE_NAME);
        return inventoryMap;
    }

    /**
     * Provides inventory cache name constant for other components.
     *
     * @return the inventory cache name
     */
    @Bean("inventoryCacheName")
    public String inventoryCacheName() {
        return INVENTORY_CACHE_NAME;
    }

    /**
     * Creates the inventory-by-product-code index cache IMap bean.
     * Keys are product codes (String), values are inventory IDs (Long) stored as Object.
     *
     * @param hazelcastInstance the Hazelcast instance
     * @return IMap for inventory-by-product-code index
     */
    @Bean("inventoryByProductCodeCache")
    @Lazy
    public IMap<String, Object> inventoryByProductCodeCache(HazelcastInstance hazelcastInstance) {
        logger.info("Creating inventory-by-product-code index cache map");
        IMap<String, Object> indexMap = hazelcastInstance.getMap(INVENTORY_BY_PRODUCT_CODE_CACHE_NAME);
        logger.info("Inventory by product code cache map created: {}", INVENTORY_BY_PRODUCT_CODE_CACHE_NAME);
        return indexMap;
    }

    /**
     * Provides inventory-by-product-code cache name constant for other components.
     */
    @Bean("inventoryByProductCodeCacheName")
    public String inventoryByProductCodeCacheName() {
        return INVENTORY_BY_PRODUCT_CODE_CACHE_NAME;
    }
}

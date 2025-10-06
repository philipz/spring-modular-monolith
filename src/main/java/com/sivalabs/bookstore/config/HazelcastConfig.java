package com.sivalabs.bookstore.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spring.context.SpringManagedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
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
    public Config hazelcastConfiguration(
            CacheProperties cacheProperties,
            ObjectProvider<MapConfig> mapConfigs,
            SpringManagedContext springManagedContext) {
        logger.info("Initializing Hazelcast configuration");

        Config config = new Config();
        // Use dynamic instance name to avoid conflicts in tests
        String instanceName = "bookstore-hazelcast-" + System.currentTimeMillis();
        config.setInstanceName(instanceName);
        config.setClusterName("bookstore-cluster");
        config.getJetConfig().setEnabled(true);
        // Allow Hazelcast to inject Spring-managed dependencies into components like MapStore
        config.setManagedContext(springManagedContext);

        // Allow other modules (e.g., orders) to contribute additional map configurations
        mapConfigs.orderedStream().forEach(config::addMapConfig);

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

    @Bean
    @ConditionalOnMissingBean
    public SpringManagedContext springManagedContext(ApplicationContext applicationContext) {
        SpringManagedContext springManagedContext = new SpringManagedContext();
        springManagedContext.setApplicationContext(applicationContext);
        return springManagedContext;
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
    @ConditionalOnMissingBean(name = "ordersCache")
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

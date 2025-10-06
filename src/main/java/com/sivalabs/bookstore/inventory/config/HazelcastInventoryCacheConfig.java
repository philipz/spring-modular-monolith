package com.sivalabs.bookstore.inventory.config;

import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.sivalabs.bookstore.common.cache.SpringAwareMapStoreConfig;
import com.sivalabs.bookstore.inventory.cache.InventoryMapStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Inventory module contribution to Hazelcast configuration.
 *
 * Providing these beans from within the module keeps the configuration aligned
 * with Modulith boundaries while still leveraging the shared Hazelcast setup.
 */
@Configuration
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
class HazelcastInventoryCacheConfig {

    private static final String INVENTORY_CACHE_NAME = "inventory-cache";
    private static final String INVENTORY_BY_PRODUCT_CODE_CACHE_NAME = "inventory-by-product-code-cache";

    @Bean
    MapConfig inventoryCacheMapConfig(Environment environment, InventoryMapStore inventoryMapStore) {
        MapConfig inventoryCacheConfig = new MapConfig(INVENTORY_CACHE_NAME);

        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(getInt(environment, "bookstore.cache.max-size", 1_000));
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        inventoryCacheConfig.setEvictionConfig(evictionConfig);

        inventoryCacheConfig.setTimeToLiveSeconds(
                getInt(environment, "bookstore.cache.inventory-time-to-live-seconds", 1_800));
        inventoryCacheConfig.setMaxIdleSeconds(getInt(environment, "bookstore.cache.max-idle-seconds", 0));
        inventoryCacheConfig.setBackupCount(getInt(environment, "bookstore.cache.backup-count", 1));
        inventoryCacheConfig.setReadBackupData(getBoolean(environment, "bookstore.cache.read-backup-data", true));
        inventoryCacheConfig.setStatisticsEnabled(getBoolean(environment, "bookstore.cache.metrics-enabled", true));

        SpringAwareMapStoreConfig mapStoreConfig = new SpringAwareMapStoreConfig();
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setImplementation(inventoryMapStore);
        mapStoreConfig.setInitialLoadMode(SpringAwareMapStoreConfig.InitialLoadMode.LAZY);

        boolean writeThrough = getBoolean(environment, "bookstore.cache.write-through", true);
        if (writeThrough) {
            mapStoreConfig.setWriteDelaySeconds(0);
            mapStoreConfig.setWriteBatchSize(1);
        } else {
            int writeDelaySeconds = getInt(environment, "bookstore.cache.write-delay-seconds", 1);
            if (writeDelaySeconds <= 0) {
                writeDelaySeconds = 1;
            }
            mapStoreConfig.setWriteDelaySeconds(writeDelaySeconds);
            mapStoreConfig.setWriteBatchSize(getInt(environment, "bookstore.cache.write-batch-size", 100));
        }

        inventoryCacheConfig.setMapStoreConfig(mapStoreConfig);
        return inventoryCacheConfig;
    }

    @Bean
    MapConfig inventoryByProductCodeCacheMapConfig(Environment environment) {
        MapConfig inventoryByProductCodeConfig = new MapConfig(INVENTORY_BY_PRODUCT_CODE_CACHE_NAME);

        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(getInt(environment, "bookstore.cache.max-size", 1_000));
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        inventoryByProductCodeConfig.setEvictionConfig(evictionConfig);

        inventoryByProductCodeConfig.setTimeToLiveSeconds(
                getInt(environment, "bookstore.cache.inventory-time-to-live-seconds", 1_800));
        inventoryByProductCodeConfig.setMaxIdleSeconds(getInt(environment, "bookstore.cache.max-idle-seconds", 0));
        inventoryByProductCodeConfig.setBackupCount(getInt(environment, "bookstore.cache.backup-count", 1));
        inventoryByProductCodeConfig.setReadBackupData(
                getBoolean(environment, "bookstore.cache.read-backup-data", true));
        inventoryByProductCodeConfig.setStatisticsEnabled(
                getBoolean(environment, "bookstore.cache.metrics-enabled", true));

        // No MapStore for this index cache; derived data managed in-memory
        return inventoryByProductCodeConfig;
    }

    private int getInt(Environment environment, String propertyKey, int defaultValue) {
        return environment.getProperty(propertyKey, Integer.class, defaultValue);
    }

    private boolean getBoolean(Environment environment, String propertyKey, boolean defaultValue) {
        return environment.getProperty(propertyKey, Boolean.class, defaultValue);
    }
}

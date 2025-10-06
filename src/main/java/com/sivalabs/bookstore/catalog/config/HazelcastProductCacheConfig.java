package com.sivalabs.bookstore.catalog.config;

import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.sivalabs.bookstore.catalog.cache.ProductMapStore;
import com.sivalabs.bookstore.common.cache.SpringAwareMapStoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Catalog-specific Hazelcast configuration contributed from within the module.
 *
 * Keeping cache wiring close to the owning module maintains Modulith boundaries
 * while still allowing the global Hazelcast configuration to aggregate the map
 * definitions.
 */
@Configuration
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
class HazelcastProductCacheConfig {

    private static final String PRODUCTS_CACHE_NAME = "products-cache";

    @Bean
    MapConfig productsCacheMapConfig(Environment environment, ProductMapStore productMapStore) {
        MapConfig productsCacheConfig = new MapConfig(PRODUCTS_CACHE_NAME);

        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(getInt(environment, "bookstore.cache.max-size", 1_000));
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        productsCacheConfig.setEvictionConfig(evictionConfig);

        productsCacheConfig.setTimeToLiveSeconds(getInt(environment, "bookstore.cache.time-to-live-seconds", 3_600));
        productsCacheConfig.setMaxIdleSeconds(getInt(environment, "bookstore.cache.max-idle-seconds", 0));
        productsCacheConfig.setBackupCount(getInt(environment, "bookstore.cache.backup-count", 1));
        productsCacheConfig.setReadBackupData(getBoolean(environment, "bookstore.cache.read-backup-data", true));
        productsCacheConfig.setStatisticsEnabled(getBoolean(environment, "bookstore.cache.metrics-enabled", true));

        SpringAwareMapStoreConfig mapStoreConfig = new SpringAwareMapStoreConfig();
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setImplementation(productMapStore);
        mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.LAZY);

        boolean writeThrough = getBoolean(environment, "bookstore.cache.write-through", true);
        if (writeThrough) {
            // write-through: synchronous flush
            mapStoreConfig.setWriteDelaySeconds(0);
            mapStoreConfig.setWriteBatchSize(getInt(environment, "bookstore.cache.write-batch-size", 1));
        } else {
            // write-behind needs a positive delay and sensible batch size
            int writeDelaySeconds = getInt(environment, "bookstore.cache.write-delay-seconds", 1);
            if (writeDelaySeconds <= 0) {
                writeDelaySeconds = 1;
            }
            mapStoreConfig.setWriteDelaySeconds(writeDelaySeconds);
            mapStoreConfig.setWriteBatchSize(getInt(environment, "bookstore.cache.write-batch-size", 100));
        }

        productsCacheConfig.setMapStoreConfig(mapStoreConfig);
        return productsCacheConfig;
    }

    private int getInt(Environment environment, String propertyKey, int defaultValue) {
        return environment.getProperty(propertyKey, Integer.class, defaultValue);
    }

    private boolean getBoolean(Environment environment, String propertyKey, boolean defaultValue) {
        return environment.getProperty(propertyKey, Boolean.class, defaultValue);
    }
}

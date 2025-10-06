package com.sivalabs.bookstore.orders.config;

import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.sivalabs.bookstore.common.cache.SpringAwareMapStoreConfig;
import com.sivalabs.bookstore.orders.cache.OrderMapStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Orders-specific Hazelcast configuration keeping cache wiring inside the module.
 *
 * The global Hazelcast configuration now exposes extension points so modules can
 * contribute their own map configuration without creating reverse dependencies.
 */
@Configuration
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HazelcastOrderCacheConfig {

    private static final String ORDERS_CACHE_NAME = "orders-cache";

    @Bean
    public MapConfig ordersCacheMapConfig(Environment environment, OrderMapStore orderMapStore) {
        MapConfig ordersCacheConfig = new MapConfig(ORDERS_CACHE_NAME);

        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(getInt(environment, "bookstore.cache.max-size", 1_000));
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        ordersCacheConfig.setEvictionConfig(evictionConfig);

        ordersCacheConfig.setTimeToLiveSeconds(getInt(environment, "bookstore.cache.time-to-live-seconds", 3_600));
        ordersCacheConfig.setMaxIdleSeconds(getInt(environment, "bookstore.cache.max-idle-seconds", 0));
        ordersCacheConfig.setBackupCount(getInt(environment, "bookstore.cache.backup-count", 1));
        ordersCacheConfig.setReadBackupData(getBoolean(environment, "bookstore.cache.read-backup-data", true));
        ordersCacheConfig.setStatisticsEnabled(getBoolean(environment, "bookstore.cache.metrics-enabled", true));

        SpringAwareMapStoreConfig mapStoreConfig = new SpringAwareMapStoreConfig();
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setImplementation(orderMapStore);
        mapStoreConfig.setInitialLoadMode(SpringAwareMapStoreConfig.InitialLoadMode.LAZY);

        boolean writeThrough = getBoolean(environment, "bookstore.cache.write-through", true);
        if (writeThrough) {
            mapStoreConfig.setWriteDelaySeconds(0);
            mapStoreConfig.setWriteBatchSize(getInt(environment, "bookstore.cache.write-batch-size", 1));
        } else {
            int writeDelaySeconds = getInt(environment, "bookstore.cache.write-delay-seconds", 1);
            if (writeDelaySeconds <= 0) {
                writeDelaySeconds = 1;
            }
            mapStoreConfig.setWriteDelaySeconds(writeDelaySeconds);
            mapStoreConfig.setWriteBatchSize(getInt(environment, "bookstore.cache.write-batch-size", 100));
        }

        ordersCacheConfig.setMapStoreConfig(mapStoreConfig);
        return ordersCacheConfig;
    }

    private int getInt(Environment environment, String propertyKey, int defaultValue) {
        return environment.getProperty(propertyKey, Integer.class, defaultValue);
    }

    private boolean getBoolean(Environment environment, String propertyKey, boolean defaultValue) {
        return environment.getProperty(propertyKey, Boolean.class, defaultValue);
    }
}

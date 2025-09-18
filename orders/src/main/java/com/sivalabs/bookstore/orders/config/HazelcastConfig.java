package com.sivalabs.bookstore.orders.config;

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
import com.sivalabs.bookstore.orders.cache.OrderMapStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HazelcastConfig {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);
    private static final String ORDERS_CACHE_NAME = "orders-cache";

    @Bean
    public Config hazelcastConfiguration(CacheProperties cacheProperties) {
        Config config = new Config();
        config.setInstanceName("orders-hazelcast-" + System.currentTimeMillis());
        config.setClusterName("orders-cluster");
        config.getJetConfig().setEnabled(true);
        config.setManagedContext(new SpringManagedContext());

        MapConfig ordersCacheConfig = new MapConfig(ORDERS_CACHE_NAME);
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(cacheProperties.getMaxSize());
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        ordersCacheConfig.setEvictionConfig(evictionConfig);

        ordersCacheConfig.setTimeToLiveSeconds(cacheProperties.getTimeToLiveSeconds());
        ordersCacheConfig.setMaxIdleSeconds(cacheProperties.getMaxIdleSeconds());
        ordersCacheConfig.setBackupCount(cacheProperties.getBackupCount());
        ordersCacheConfig.setReadBackupData(cacheProperties.isReadBackupData());
        ordersCacheConfig.setStatisticsEnabled(cacheProperties.isMetricsEnabled());

        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setClassName(OrderMapStore.class.getName());
        mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.LAZY);
        if (cacheProperties.isWriteThrough()) {
            mapStoreConfig.setWriteDelaySeconds(cacheProperties.getWriteDelaySeconds());
            mapStoreConfig.setWriteBatchSize(cacheProperties.getWriteBatchSize());
        }
        ordersCacheConfig.setMapStoreConfig(mapStoreConfig);

        config.addMapConfig(ordersCacheConfig);

        if (cacheProperties.isMetricsEnabled()) {
            config.getMetricsConfig().setEnabled(true);
        }

        SerializationConfig serializationConfig = config.getSerializationConfig();
        serializationConfig.setCheckClassDefErrors(false);
        serializationConfig.setUseNativeByteOrder(false);
        serializationConfig.setAllowOverrideDefaultSerializers(true);
        serializationConfig.setAllowUnsafe(false);

        logger.info("Hazelcast orders cache configured with TTL {} seconds", cacheProperties.getTimeToLiveSeconds());

        return config;
    }

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance(Config hazelcastConfiguration) {
        return Hazelcast.newHazelcastInstance(hazelcastConfiguration);
    }

    @Bean("ordersCache")
    @Lazy
    public IMap<String, Object> ordersCache(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap(ORDERS_CACHE_NAME);
    }

    @Bean("ordersCacheName")
    public String ordersCacheName() {
        return ORDERS_CACHE_NAME;
    }
}

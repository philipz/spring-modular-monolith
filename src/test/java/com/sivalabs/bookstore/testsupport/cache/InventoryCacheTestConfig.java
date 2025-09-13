package com.sivalabs.bookstore.testsupport.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@TestConfiguration
@Import(CommonCacheTestConfig.class)
public class InventoryCacheTestConfig {

    @Bean("inventoryCache")
    @Lazy
    @ConditionalOnMissingBean(name = "inventoryCache")
    public IMap<Long, Object> inventoryCache(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("inventory-cache");
    }
}

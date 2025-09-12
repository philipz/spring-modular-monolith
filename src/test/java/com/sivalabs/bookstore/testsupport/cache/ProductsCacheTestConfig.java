package com.sivalabs.bookstore.testsupport.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@TestConfiguration
@Import(HazelcastTestInstanceConfig.class)
public class ProductsCacheTestConfig {

    @Bean("productsCache")
    @Lazy
    @ConditionalOnMissingBean(name = "productsCache")
    public IMap<String, Object> productsCache(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("products-cache");
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler();
    }
}

package com.sivalabs.bookstore.testsupport.cache;

import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(HazelcastTestInstanceConfig.class)
public class CommonCacheTestConfig {

    @Bean
    @ConditionalOnMissingBean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler();
    }
}

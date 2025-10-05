package com.sivalabs.bookstore.testsupport.cache;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@TestConfiguration
public class HazelcastTestInstanceConfig {

    @Bean
    @ConditionalOnMissingBean
    public SpringManagedContext springManagedContext(ApplicationContext applicationContext) {
        return new SpringManagedContext(applicationContext);
    }

    @Bean(destroyMethod = "shutdown")
    @Lazy
    @ConditionalOnMissingBean(HazelcastInstance.class)
    public HazelcastInstance testHazelcastInstance(SpringManagedContext springManagedContext) {
        Config config = new Config();
        config.setInstanceName("test-hz-instance-" + UUID.randomUUID());
        config.setClusterName("test-cluster-" + UUID.randomUUID());
        // Enable Spring integration for @SpringAware and @Autowired in MapStore
        config.setManagedContext(springManagedContext);
        return Hazelcast.newHazelcastInstance(config);
    }
}

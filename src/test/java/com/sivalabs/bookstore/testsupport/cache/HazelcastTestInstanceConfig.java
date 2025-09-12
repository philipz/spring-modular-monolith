package com.sivalabs.bookstore.testsupport.cache;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@TestConfiguration
public class HazelcastTestInstanceConfig {

    @Bean(destroyMethod = "shutdown")
    @Lazy
    @ConditionalOnMissingBean(HazelcastInstance.class)
    public HazelcastInstance testHazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("test-hz-instance-" + UUID.randomUUID());
        config.setClusterName("test-cluster-" + UUID.randomUUID());
        return Hazelcast.newHazelcastInstance(config);
    }
}

package com.sivalabs.bookstore.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.orders.cache.CacheErrorHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheMetricsConfigTests {

    private HazelcastInstance hazelcastInstance;

    private HazelcastInstance newHazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("orders-cache-metrics-" + System.nanoTime());
        config.setClusterName("orders-cache-metrics");
        config.addMapConfig(new MapConfig("orders-cache"));
        return Hazelcast.newHazelcastInstance(config);
    }

    @AfterEach
    void tearDown() {
        if (hazelcastInstance != null) {
            try {
                hazelcastInstance.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("Should register Hazelcast cache gauges")
    void shouldRegisterHazelcastCacheGauges() {
        hazelcastInstance = newHazelcastInstance();
        IMap<String, Object> ordersCache = hazelcastInstance.getMap("orders-cache");
        ordersCache.put("order-1", "cached-value");

        MeterRegistry registry = new SimpleMeterRegistry();
        CacheProperties properties = new CacheProperties();
        properties.setMaxSize(250);
        properties.setTimeToLiveSeconds(1200);
        CacheErrorHandler errorHandler = new CacheErrorHandler();

        CacheMetricsConfig metricsConfig =
                new CacheMetricsConfig(registry, hazelcastInstance, ordersCache, errorHandler, properties);
        metricsConfig.registerCacheMetrics();

        assertThat(registry.get("bookstore.cache.size").gauge().value()).isEqualTo(1.0d);
        assertThat(registry.get("bookstore.cache.hazelcast.cluster.members")
                        .gauge()
                        .value())
                .isGreaterThanOrEqualTo(1.0d);
        assertThat(registry.get("bookstore.cache.circuit.open").gauge().value()).isZero();
        assertThat(registry.get("bookstore.cache.config.max.size").gauge().value())
                .isEqualTo(250d);
    }
}

package com.sivalabs.bookstore.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.orders.cache.CacheErrorHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class CacheHealthIndicatorTests {

    private HazelcastInstance hazelcastInstance;

    private HazelcastInstance newHazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("orders-cache-test-" + System.nanoTime());
        config.setClusterName("orders-cache-tests");
        config.addMapConfig(new MapConfig("orders-cache"));
        return Hazelcast.newHazelcastInstance(config);
    }

    @AfterEach
    void tearDown() {
        if (hazelcastInstance != null) {
            try {
                hazelcastInstance.shutdown();
            } catch (Exception ignored) {
                // Instance already shutdown in individual test cases
            }
        }
    }

    @Test
    @DisplayName("Health indicator should report UP when Hazelcast is operational")
    void shouldReportUpWhenHazelcastIsOperational() {
        hazelcastInstance = newHazelcastInstance();
        IMap<String, Object> ordersCache = hazelcastInstance.getMap("orders-cache");
        CacheProperties properties = new CacheProperties();
        CacheErrorHandler errorHandler = new CacheErrorHandler();

        CacheHealthIndicator indicator =
                new CacheHealthIndicator(hazelcastInstance, ordersCache, properties, errorHandler);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKeys("hazelcast", "ordersCache", "operations", "circuitBreaker");
    }

    @Test
    @DisplayName("Health indicator should report DOWN when Hazelcast has stopped")
    void shouldReportDownWhenHazelcastStopped() {
        hazelcastInstance = newHazelcastInstance();
        IMap<String, Object> ordersCache = hazelcastInstance.getMap("orders-cache");
        CacheProperties properties = new CacheProperties();
        CacheErrorHandler errorHandler = new CacheErrorHandler();

        CacheHealthIndicator indicator =
                new CacheHealthIndicator(hazelcastInstance, ordersCache, properties, errorHandler);

        hazelcastInstance.shutdown();

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKeys("hazelcast", "operations");
        @SuppressWarnings("unchecked")
        var hazelcastDetails =
                (java.util.Map<String, Object>) health.getDetails().get("hazelcast");
        assertThat(hazelcastDetails.get("status")).isIn("NOT_RUNNING", "ERROR");
    }
}

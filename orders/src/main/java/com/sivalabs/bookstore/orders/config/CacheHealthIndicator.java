package com.sivalabs.bookstore.orders.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.orders.cache.CacheErrorHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Orders cache health indicator exposed through Spring Boot Actuator.
 *
 * <p>This indicator performs a lightweight diagnostic of the Hazelcast instance backing the
 * orders cache and exposes circuit breaker state maintained by {@link CacheErrorHandler}. The
 * indicator runs without touching domain services so it remains safe for continuous monitoring.
 */
@Component
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CacheHealthIndicator.class);

    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, Object> ordersCache;
    private final CacheProperties cacheProperties;
    private final CacheErrorHandler cacheErrorHandler;

    public CacheHealthIndicator(
            HazelcastInstance hazelcastInstance,
            @Qualifier("ordersCache") IMap<String, Object> ordersCache,
            CacheProperties cacheProperties,
            CacheErrorHandler cacheErrorHandler) {
        this.hazelcastInstance = hazelcastInstance;
        this.ordersCache = ordersCache;
        this.cacheProperties = cacheProperties;
        this.cacheErrorHandler = cacheErrorHandler;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        boolean hazelcastHealthy = checkHazelcast(details);
        boolean cacheHealthy = checkCache(details);
        boolean operationsHealthy = testBasicOperations(details);

        addCircuitBreakerDetails(details);
        addConfigurationDetails(details);

        details.put("timestamp", Instant.now().toString());

        boolean overallHealthy = hazelcastHealthy && cacheHealthy && operationsHealthy;
        return overallHealthy
                ? Health.up().withDetails(details).build()
                : Health.down().withDetails(details).build();
    }

    private boolean checkHazelcast(Map<String, Object> details) {
        Map<String, Object> hazelcastDetails = new HashMap<>();
        try {
            boolean running = hazelcastInstance != null
                    && hazelcastInstance.getLifecycleService().isRunning();
            int memberCount = hazelcastInstance != null
                    ? hazelcastInstance.getCluster().getMembers().size()
                    : 0;
            boolean clusterSafe = hazelcastInstance != null
                    && hazelcastInstance.getPartitionService().isClusterSafe();

            hazelcastDetails.put("running", running);
            hazelcastDetails.put(
                    "instanceName", hazelcastInstance != null ? hazelcastInstance.getName() : "orders-hazelcast");
            hazelcastDetails.put(
                    "cluster",
                    hazelcastInstance != null ? hazelcastInstance.getConfig().getClusterName() : "orders-cluster");
            hazelcastDetails.put("memberCount", memberCount);
            hazelcastDetails.put("clusterSafe", clusterSafe);

            if (!running) {
                hazelcastDetails.put("status", "NOT_RUNNING");
                details.put("hazelcast", hazelcastDetails);
                return false;
            }

            hazelcastDetails.put("status", "OK");
            details.put("hazelcast", hazelcastDetails);
            return true;
        } catch (Exception ex) {
            logger.warn("Hazelcast health check failed", ex);
            hazelcastDetails.put("status", "ERROR");
            hazelcastDetails.put("error", ex.getMessage());
            details.put("hazelcast", hazelcastDetails);
            return false;
        }
    }

    private boolean checkCache(Map<String, Object> details) {
        Map<String, Object> cacheDetails = new HashMap<>();
        try {
            if (ordersCache == null) {
                cacheDetails.put("status", "MISSING");
                details.put("ordersCache", cacheDetails);
                return false;
            }

            cacheDetails.put("status", "AVAILABLE");
            cacheDetails.put("name", ordersCache.getName());
            cacheDetails.put("size", ordersCache.size());

            var stats = ordersCache.getLocalMapStats();
            if (stats != null) {
                cacheDetails.put(
                        "statistics",
                        Map.of(
                                "ownedEntries", stats.getOwnedEntryCount(),
                                "backupEntries", stats.getBackupEntryCount(),
                                "hits", stats.getHits(),
                                "getOperations", stats.getGetOperationCount(),
                                "putOperations", stats.getPutOperationCount(),
                                "lastUpdateTime", stats.getLastUpdateTime()));
            }

            details.put("ordersCache", cacheDetails);
            return true;
        } catch (Exception ex) {
            logger.warn("Orders cache health check failed", ex);
            cacheDetails.put("status", "ERROR");
            cacheDetails.put("error", ex.getMessage());
            details.put("ordersCache", cacheDetails);
            return false;
        }
    }

    private boolean testBasicOperations(Map<String, Object> details) {
        if (ordersCache == null) {
            details.put("operations", Map.of("status", "SKIPPED"));
            return false;
        }

        String key = "orders-cache-health-check-" + System.currentTimeMillis();
        String value = "health-check";

        try {
            ordersCache.put(key, value);
            Object retrieved = ordersCache.get(key);
            boolean contains = ordersCache.containsKey(key);
            Object removed = ordersCache.remove(key);

            boolean success = value.equals(retrieved) && contains && value.equals(removed);
            details.put(
                    "operations",
                    Map.of(
                            "status",
                            success ? "OK" : "FAIL",
                            "contains",
                            contains,
                            "retrieved",
                            retrieved != null,
                            "removed",
                            removed != null));
            return success;
        } catch (Exception ex) {
            logger.warn("Orders cache basic operations test failed", ex);
            details.put("operations", Map.of("status", "ERROR", "error", ex.getMessage()));
            return false;
        }
    }

    private void addCircuitBreakerDetails(Map<String, Object> details) {
        details.put(
                "circuitBreaker",
                Map.of(
                        "open", cacheErrorHandler.isCircuitOpen(),
                        "consecutiveFailures", cacheErrorHandler.getConsecutiveFailureCount(),
                        "openCount", cacheErrorHandler.getTotalCircuitOpenings(),
                        "fallbackRecommendations", cacheErrorHandler.getFallbackRecommendationCount(),
                        "trackedErrorOperations", cacheErrorHandler.getTrackedErrorCount()));
    }

    private void addConfigurationDetails(Map<String, Object> details) {
        details.put(
                "configuration",
                Map.of(
                        "enabled", cacheProperties.isEnabled(),
                        "metricsEnabled", cacheProperties.isMetricsEnabled(),
                        "writeThrough", cacheProperties.isWriteThrough(),
                        "maxSize", cacheProperties.getMaxSize(),
                        "timeToLiveSeconds", cacheProperties.getTimeToLiveSeconds(),
                        "backupCount", cacheProperties.getBackupCount()));
    }
}

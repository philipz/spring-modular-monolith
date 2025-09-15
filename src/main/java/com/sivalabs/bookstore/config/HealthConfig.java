package com.sivalabs.bookstore.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Unified cache health monitoring configuration for all cache types.
 *
 * This component provides comprehensive health monitoring for:
 * - Orders cache (String keys)
 * - Products cache (String keys)
 * - Inventory cache (Long keys)
 * - Hazelcast infrastructure
 * - Circuit breaker status across all caches
 *
 * Integrates with Spring Boot Actuator for centralized health reporting.
 */
@Component
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true")
public class HealthConfig implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(HealthConfig.class);
    private static final String HEALTH_CHECK_KEY_PREFIX = "health-check-unified-";

    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, Object> ordersCache;
    private final IMap<String, Object> productsCache;
    private final IMap<Long, Object> inventoryCache;

    private final CacheErrorHandler cacheErrorHandler;
    private final CacheProperties cacheProperties;

    // Cache for health check results to avoid excessive checks
    private final AtomicReference<Health> lastHealthResult = new AtomicReference<>();
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CACHE_DURATION = 30000; // 30 seconds

    public HealthConfig(
            HazelcastInstance hazelcastInstance,
            @Qualifier("ordersCache") IMap<String, Object> ordersCache,
            @Qualifier("productsCache") IMap<String, Object> productsCache,
            @Qualifier("inventoryCache") IMap<Long, Object> inventoryCache,
            CacheErrorHandler cacheErrorHandler,
            CacheProperties cacheProperties) {

        this.hazelcastInstance = hazelcastInstance;
        this.ordersCache = ordersCache;
        this.productsCache = productsCache;
        this.inventoryCache = inventoryCache;
        this.cacheErrorHandler = cacheErrorHandler;
        this.cacheProperties = cacheProperties;

        logger.info("HealthConfig initialized with all cache services");
    }

    @Override
    public Health health() {
        long currentTime = System.currentTimeMillis();

        // Use cached result if recent enough
        if (currentTime - lastHealthCheckTime < HEALTH_CACHE_DURATION) {
            Health cached = lastHealthResult.get();
            if (cached != null) {
                return cached;
            }
        }

        try {
            Health result = performHealthCheck();
            lastHealthResult.set(result);
            lastHealthCheckTime = currentTime;
            return result;
        } catch (Exception e) {
            logger.error("Unified cache health check failed with exception", e);
            Health errorHealth = Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
            lastHealthResult.set(errorHealth);
            return errorHealth;
        }
    }

    private Health performHealthCheck() {
        Map<String, Object> details = new HashMap<>();
        boolean overallHealthy = true;

        // 1. Check Hazelcast infrastructure health
        boolean hazelcastHealthy = checkHazelcastHealth(details);
        overallHealthy &= hazelcastHealthy;

        // 2. Check individual cache health
        boolean ordersCacheHealthy = checkOrdersCacheHealth(details);
        boolean productsCacheHealthy = checkProductsCacheHealth(details);
        boolean inventoryCacheHealthy = checkInventoryCacheHealth(details);

        overallHealthy &= (ordersCacheHealthy && productsCacheHealthy && inventoryCacheHealthy);

        // 3. Skip direct cache service health checks to respect module boundaries
        details.put("cacheServices", "SKIPPED");

        // 4. Check circuit breaker status
        boolean circuitBreakerHealthy = checkCircuitBreakerHealth(details);
        overallHealthy &= circuitBreakerHealthy;

        // 5. Perform basic operations test
        if (hazelcastHealthy && overallHealthy) {
            boolean operationsHealthy = testBasicOperations(details);
            overallHealthy &= operationsHealthy;
        }

        // 6. Add aggregate statistics
        addAggregateStatistics(details);

        // 7. Add configuration info
        addConfigurationDetails(details);

        // 8. Add timestamp
        details.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        details.put("healthCheckDuration", System.currentTimeMillis() - lastHealthCheckTime + "ms");

        if (overallHealthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    private boolean checkHazelcastHealth(Map<String, Object> details) {
        try {
            if (hazelcastInstance == null) {
                details.put("hazelcast", "NULL_INSTANCE");
                return false;
            }

            boolean isRunning = hazelcastInstance.getLifecycleService().isRunning();
            boolean isClusterSafe = hazelcastInstance.getPartitionService().isClusterSafe();
            int memberCount = hazelcastInstance.getCluster().getMembers().size();

            details.put(
                    "hazelcast",
                    Map.of(
                            "status",
                            "CONNECTED",
                            "instanceName",
                            hazelcastInstance.getName(),
                            "clusterName",
                            hazelcastInstance.getConfig().getClusterName(),
                            "running",
                            isRunning,
                            "clusterSafe",
                            isClusterSafe,
                            "memberCount",
                            memberCount,
                            "clusterState",
                            hazelcastInstance.getCluster().getClusterState().toString()));

            return isRunning && isClusterSafe;

        } catch (Exception e) {
            details.put("hazelcast", Map.of("status", "ERROR", "error", e.getMessage()));
            logger.warn("Hazelcast health check failed", e);
            return false;
        }
    }

    private boolean checkOrdersCacheHealth(Map<String, Object> details) {
        try {
            if (ordersCache == null) {
                details.put("ordersCache", Map.of("status", "NULL_CACHE"));
                return false;
            }

            long size = 0;
            String name = ordersCache.getName();

            if (ordersCache.getLocalMapStats() != null) {
                size = ordersCache.getLocalMapStats().getOwnedEntryCount();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("status", "AVAILABLE");
            stats.put("name", name);
            stats.put("size", size);

            if (ordersCache.getLocalMapStats() != null) {
                var localStats = ordersCache.getLocalMapStats();
                stats.put(
                        "localStats",
                        Map.of(
                                "hits", localStats.getHits(),
                                "getOperations", localStats.getGetOperationCount(),
                                "putOperations", localStats.getPutOperationCount(),
                                "ownedEntries", localStats.getOwnedEntryCount()));
            }

            details.put("ordersCache", stats);
            return true;

        } catch (Exception e) {
            details.put("ordersCache", Map.of("status", "ERROR", "error", e.getMessage()));
            logger.warn("Orders cache health check failed", e);
            return false;
        }
    }

    private boolean checkProductsCacheHealth(Map<String, Object> details) {
        try {
            if (productsCache == null) {
                details.put("productsCache", Map.of("status", "NULL_CACHE"));
                return false;
            }

            long size = 0;
            String name = productsCache.getName();

            if (productsCache.getLocalMapStats() != null) {
                size = productsCache.getLocalMapStats().getOwnedEntryCount();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("status", "AVAILABLE");
            stats.put("name", name);
            stats.put("size", size);

            if (productsCache.getLocalMapStats() != null) {
                var localStats = productsCache.getLocalMapStats();
                stats.put(
                        "localStats",
                        Map.of(
                                "hits", localStats.getHits(),
                                "getOperations", localStats.getGetOperationCount(),
                                "putOperations", localStats.getPutOperationCount(),
                                "ownedEntries", localStats.getOwnedEntryCount()));
            }

            details.put("productsCache", stats);
            return true;

        } catch (Exception e) {
            details.put("productsCache", Map.of("status", "ERROR", "error", e.getMessage()));
            logger.warn("Products cache health check failed", e);
            return false;
        }
    }

    private boolean checkInventoryCacheHealth(Map<String, Object> details) {
        try {
            if (inventoryCache == null) {
                details.put("inventoryCache", Map.of("status", "NULL_CACHE"));
                return false;
            }

            long size = 0;
            String name = inventoryCache.getName();

            if (inventoryCache.getLocalMapStats() != null) {
                size = inventoryCache.getLocalMapStats().getOwnedEntryCount();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("status", "AVAILABLE");
            stats.put("name", name);
            stats.put("size", size);
            stats.put("keyType", "Long"); // Distinguish Long keys from String keys

            if (inventoryCache.getLocalMapStats() != null) {
                var localStats = inventoryCache.getLocalMapStats();
                stats.put(
                        "localStats",
                        Map.of(
                                "hits", localStats.getHits(),
                                "getOperations", localStats.getGetOperationCount(),
                                "putOperations", localStats.getPutOperationCount(),
                                "ownedEntries", localStats.getOwnedEntryCount()));
            }

            details.put("inventoryCache", stats);
            return true;

        } catch (Exception e) {
            details.put("inventoryCache", Map.of("status", "ERROR", "error", e.getMessage()));
            logger.warn("Inventory cache health check failed", e);
            return false;
        }
    }

    // Removed direct service health checks to avoid cross-module dependencies

    private boolean checkCircuitBreakerHealth(Map<String, Object> details) {
        try {
            boolean circuitOpen = cacheErrorHandler.isCircuitOpen();
            String errorStats = cacheErrorHandler.getCacheErrorStats();

            details.put(
                    "circuitBreaker",
                    Map.of(
                            "status", circuitOpen ? "OPEN" : "CLOSED",
                            "healthy", !circuitOpen,
                            "errorStats", errorStats != null ? errorStats : "No error statistics available"));

            return !circuitOpen; // Healthy when circuit is closed

        } catch (Exception e) {
            details.put("circuitBreaker", Map.of("status", "ERROR", "error", e.getMessage()));
            logger.warn("Circuit breaker health check failed", e);
            return false;
        }
    }

    private boolean testBasicOperations(Map<String, Object> details) {
        Map<String, Object> operationResults = new HashMap<>();
        boolean allOperationsSuccessful = true;

        try {
            // Respect configuration flags
            if (!cacheProperties.isTestBasicOperationsEnabled()) {
                details.put("basicOperations", Map.of("enabled", false, "mode", "DISABLED"));
                details.put("operationsOverall", "SKIPPED");
                return true; // Do not fail health when disabled
            }

            boolean readOnly = cacheProperties.isBasicOperationsReadOnly();
            details.put("basicOperationsConfig", Map.of("enabled", true, "mode", readOnly ? "READ_ONLY" : "FULL"));

            String testKey = HEALTH_CHECK_KEY_PREFIX + System.currentTimeMillis();
            String testValue = "health-test-value";
            Long testLongKey = System.currentTimeMillis();

            // Test orders cache operations
            try {
                boolean ordersSuccess;
                if (readOnly) {
                    ordersCache.get(testKey);
                    ordersSuccess = true; // success if no exception
                } else {
                    ordersCache.put(testKey, testValue);
                    String retrieved = (String) ordersCache.get(testKey);
                    ordersSuccess = testValue.equals(retrieved);
                    ordersCache.remove(testKey);
                }
                operationResults.put("ordersCache", ordersSuccess ? "OK" : "FAILED");
                allOperationsSuccessful &= ordersSuccess;
            } catch (Exception e) {
                operationResults.put("ordersCache", "ERROR: " + e.getMessage());
                allOperationsSuccessful = false;
            }

            // Test products cache operations
            try {
                boolean productsSuccess;
                if (readOnly) {
                    productsCache.get(testKey + "_product");
                    productsSuccess = true; // success if no exception
                } else {
                    productsCache.put(testKey + "_product", testValue);
                    String retrieved = (String) productsCache.get(testKey + "_product");
                    productsSuccess = testValue.equals(retrieved);
                    productsCache.remove(testKey + "_product");
                }
                operationResults.put("productsCache", productsSuccess ? "OK" : "FAILED");
                allOperationsSuccessful &= productsSuccess;
            } catch (Exception e) {
                operationResults.put("productsCache", "ERROR: " + e.getMessage());
                allOperationsSuccessful = false;
            }

            // Test inventory cache operations (Long keys)
            try {
                boolean inventorySuccess;
                if (readOnly) {
                    inventoryCache.get(testLongKey);
                    inventorySuccess = true; // success if no exception
                } else {
                    inventoryCache.put(testLongKey, testValue);
                    String retrieved = (String) inventoryCache.get(testLongKey);
                    inventorySuccess = testValue.equals(retrieved);
                    inventoryCache.remove(testLongKey);
                }
                operationResults.put("inventoryCache", inventorySuccess ? "OK" : "FAILED");
                allOperationsSuccessful &= inventorySuccess;
            } catch (Exception e) {
                operationResults.put("inventoryCache", "ERROR: " + e.getMessage());
                allOperationsSuccessful = false;
            }

            details.put("basicOperations", operationResults);
            details.put("operationsOverall", allOperationsSuccessful ? "OK" : "PARTIAL_FAILURE");

        } catch (Exception e) {
            details.put("basicOperations", "EXCEPTION: " + e.getMessage());
            allOperationsSuccessful = false;
        }

        return allOperationsSuccessful;
    }

    private void addAggregateStatistics(Map<String, Object> details) {
        try {
            long totalCacheSize = 0;
            long totalHits = 0;
            long totalGets = 0;
            long totalPuts = 0;

            // Aggregate orders cache stats
            if (ordersCache != null) {
                if (ordersCache.getLocalMapStats() != null) {
                    totalCacheSize += ordersCache.getLocalMapStats().getOwnedEntryCount();
                    var stats = ordersCache.getLocalMapStats();
                    totalHits += stats.getHits();
                    totalGets += stats.getGetOperationCount();
                    totalPuts += stats.getPutOperationCount();
                }
            }

            // Aggregate products cache stats
            if (productsCache != null) {
                if (productsCache.getLocalMapStats() != null) {
                    totalCacheSize += productsCache.getLocalMapStats().getOwnedEntryCount();
                    var stats = productsCache.getLocalMapStats();
                    totalHits += stats.getHits();
                    totalGets += stats.getGetOperationCount();
                    totalPuts += stats.getPutOperationCount();
                }
            }

            // Aggregate inventory cache stats
            if (inventoryCache != null) {
                if (inventoryCache.getLocalMapStats() != null) {
                    totalCacheSize += inventoryCache.getLocalMapStats().getOwnedEntryCount();
                    var stats = inventoryCache.getLocalMapStats();
                    totalHits += stats.getHits();
                    totalGets += stats.getGetOperationCount();
                    totalPuts += stats.getPutOperationCount();
                }
            }

            double hitRatio = totalGets > 0 ? (double) totalHits / totalGets * 100 : 0.0;

            details.put(
                    "aggregateStatistics",
                    Map.of(
                            "totalCacheSize", totalCacheSize,
                            "totalHits", totalHits,
                            "totalGets", totalGets,
                            "totalPuts", totalPuts,
                            "overallHitRatio", String.format("%.2f%%", hitRatio)));

        } catch (Exception e) {
            details.put("aggregateStatistics", "ERROR: " + e.getMessage());
            logger.warn("Failed to calculate aggregate statistics", e);
        }
    }

    private void addConfigurationDetails(Map<String, Object> details) {
        details.put(
                "configuration",
                Map.of(
                        "cacheEnabled", cacheProperties.isEnabled(),
                        "writeThrough", cacheProperties.isWriteThrough(),
                        "maxSize", cacheProperties.getMaxSize(),
                        "timeToLiveSeconds", cacheProperties.getTimeToLiveSeconds(),
                        "writeDelaySeconds", cacheProperties.getWriteDelaySeconds(),
                        "metricsEnabled", cacheProperties.isMetricsEnabled(),
                        "cacheTypes",
                                Map.of(
                                        "orders", "String keys",
                                        "products", "String keys",
                                        "inventory", "Long keys")));
    }

    /**
     * Get detailed health information for all caches for debugging purposes.
     *
     * @return comprehensive health information
     */
    public Map<String, Object> getDetailedHealthInfo() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            Health health = health();
            healthInfo.put("overallStatus", health.getStatus().getCode());
            healthInfo.putAll(health.getDetails());

            // Add individual cache service reports
            // Skipping direct cache service reports to respect module boundaries

        } catch (Exception e) {
            healthInfo.put("detailedHealthError", e.getMessage());
            healthInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return healthInfo;
    }

    /**
     * Get a summary of all cache health status.
     *
     * @return concise health summary
     */
    public String getHealthSummary() {
        try {
            Health health = health();
            Map<String, Object> details = health.getDetails();

            @SuppressWarnings("unchecked")
            Map<String, Object> aggregateStats = (Map<String, Object>) details.get("aggregateStatistics");

            if (aggregateStats != null) {
                return String.format(
                        "Cache Health: %s | Total Size: %s | Hit Ratio: %s | Circuit: %s",
                        health.getStatus().getCode(),
                        aggregateStats.get("totalCacheSize"),
                        aggregateStats.get("overallHitRatio"),
                        details.containsKey("circuitBreaker")
                                ? ((Map<?, ?>) details.get("circuitBreaker")).get("status")
                                : "UNKNOWN");
            }
        } catch (Exception e) {
            logger.warn("Failed to generate health summary", e);
        }

        return "Cache health summary unavailable";
    }
}

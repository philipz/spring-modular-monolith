package com.sivalabs.bookstore.orders.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.orders.cache.CacheErrorHandler;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Hazelcast cache metrics for the Orders service.
 *
 * <p>Micrometer gauges expose cache size, hit ratio, circuit breaker state, and configuration details to
 * Prometheus. These metrics keep parity with the monolith while remaining lightweight for the standalone
 * service.
 */
@Configuration
@ConditionalOnBean({HazelcastInstance.class, CacheErrorHandler.class})
@ConditionalOnProperty(
        prefix = "bookstore.cache",
        name = {"enabled", "metrics-enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class CacheMetricsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsConfig.class);
    private static final String METRIC_PREFIX = "bookstore.cache";

    private final MeterRegistry meterRegistry;
    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, Object> ordersCache;
    private final CacheErrorHandler cacheErrorHandler;
    private final CacheProperties cacheProperties;

    public CacheMetricsConfig(
            MeterRegistry meterRegistry,
            HazelcastInstance hazelcastInstance,
            @Qualifier("ordersCache") IMap<String, Object> ordersCache,
            CacheErrorHandler cacheErrorHandler,
            CacheProperties cacheProperties) {
        this.meterRegistry = meterRegistry;
        this.hazelcastInstance = hazelcastInstance;
        this.ordersCache = ordersCache;
        this.cacheErrorHandler = cacheErrorHandler;
        this.cacheProperties = cacheProperties;
    }

    @PostConstruct
    void registerCacheMetrics() {
        logger.info("Registering Hazelcast cache metrics for Prometheus exposure");

        Gauge.builder(METRIC_PREFIX + ".size", ordersCache, map -> safeStat(map, () -> (double) map.size()))
                .description("Number of entries currently stored in the orders cache")
                .register(meterRegistry);

        Gauge.builder(
                        METRIC_PREFIX + ".hits",
                        ordersCache,
                        map -> safeStat(
                                map, () -> (double) map.getLocalMapStats().getHits()))
                .description("Total number of cache hits reported by Hazelcast")
                .register(meterRegistry);

        Gauge.builder(
                        METRIC_PREFIX + ".hit.ratio",
                        ordersCache,
                        map -> safeStat(
                                map,
                                () -> calculateHitRatio(
                                        map.getLocalMapStats().getHits(),
                                        map.getLocalMapStats().getGetOperationCount())))
                .description("Cache hit ratio derived from Hazelcast statistics")
                .register(meterRegistry);

        Gauge.builder(
                        METRIC_PREFIX + ".owned.entries",
                        ordersCache,
                        map -> safeStat(
                                map, () -> (double) map.getLocalMapStats().getOwnedEntryCount()))
                .description("Number of entries owned by this node")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".hazelcast.cluster.members", hazelcastInstance, hz ->
                        (double) hz.getCluster().getMembers().size())
                .description("Hazelcast cluster member count")
                .register(meterRegistry);

        Gauge.builder(
                        METRIC_PREFIX + ".hazelcast.running",
                        hazelcastInstance,
                        hz -> hz.getLifecycleService().isRunning() ? 1.0 : 0.0)
                .description("Hazelcast lifecycle running flag (1=running, 0=stopped)")
                .register(meterRegistry);

        Gauge.builder(
                        METRIC_PREFIX + ".circuit.open",
                        cacheErrorHandler,
                        handler -> handler.isCircuitOpen() ? 1.0 : 0.0)
                .description("Circuit breaker state (1=open, 0=closed)")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".circuit.open.count", cacheErrorHandler, handler ->
                        (double) handler.getTotalCircuitOpenings())
                .description("Total times the circuit breaker has opened")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".errors.tracked", cacheErrorHandler, handler ->
                        (double) handler.getTrackedErrorCount())
                .description("Total tracked cache errors across operations")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".fallback.recommendations", cacheErrorHandler, handler ->
                        (double) handler.getFallbackRecommendationCount())
                .description("Number of times cache fallback to database was recommended")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".config.enabled", cacheProperties, props -> props.isEnabled() ? 1.0 : 0.0)
                .description("Flag indicating if caching is enabled")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".config.max.size", cacheProperties, props -> (double) props.getMaxSize())
                .description("Configured cache maximum size")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".config.ttl", cacheProperties, props -> (double) props.getTimeToLiveSeconds())
                .description("Configured cache time-to-live in seconds")
                .register(meterRegistry);
    }

    private double safeStat(IMap<String, Object> map, java.util.concurrent.Callable<Double> supplier) {
        try {
            return supplier.call();
        } catch (Exception ex) {
            logger.debug("Unable to fetch Hazelcast metric from map {}", map != null ? map.getName() : "n/a", ex);
            return 0.0;
        }
    }

    private double calculateHitRatio(long hits, long gets) {
        if (gets == 0) {
            return 0.0;
        }
        return hits / (double) gets;
    }
}

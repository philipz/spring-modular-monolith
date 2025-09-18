package com.sivalabs.bookstore.orders.config;

import com.sivalabs.bookstore.orders.cache.CacheErrorHandler;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adds operational details about the Orders service to the /actuator/info endpoint.
 */
@Component
@ConditionalOnProperty(prefix = "bookstore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrdersInfoContributor implements InfoContributor {

    private final CacheProperties cacheProperties;
    private final CacheErrorHandler cacheErrorHandler;

    public OrdersInfoContributor(CacheProperties cacheProperties, CacheErrorHandler cacheErrorHandler) {
        this.cacheProperties = cacheProperties;
        this.cacheErrorHandler = cacheErrorHandler;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail(
                "ordersService",
                Map.of(
                        "cacheEnabled", cacheProperties.isEnabled(),
                        "cacheMetricsEnabled", cacheProperties.isMetricsEnabled(),
                        "cacheWriteThrough", cacheProperties.isWriteThrough(),
                        "cacheCircuitOpen", cacheErrorHandler.isCircuitOpen(),
                        "cacheConsecutiveFailures", cacheErrorHandler.getConsecutiveFailureCount()));
    }
}

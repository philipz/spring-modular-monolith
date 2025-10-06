package com.sivalabs.bookstore.orders.config;

import com.sivalabs.bookstore.common.grpc.ProductCatalogServiceGrpc;
import com.sivalabs.bookstore.orders.config.GrpcClientProperties.RetryProperties;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC client connections, channels, and stubs.
 *
 * <p>Creates configured {@link ManagedChannel} and service stubs with connection pooling,
 * keep-alive settings, timeouts, and retry behavior based on {@link GrpcClientProperties}.
 * Channels are automatically shut down when the Spring context closes.</p>
 */
@Configuration
@EnableConfigurationProperties(GrpcClientProperties.class)
@ConditionalOnClass(name = "com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc")
@ConditionalOnProperty(name = "grpc.client.orders.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcClientConfiguration {

    /**
     * Creates a managed channel for Orders gRPC service connections.
     *
     * <p>Configures the channel with connection timeout, keep-alive settings, and message size limits
     * based on the {@link GrpcClientProperties}. The channel is automatically shut down during
     * Spring context closure.</p>
     *
     * @param properties gRPC client configuration properties
     * @return configured {@link ManagedChannel} for Orders service
     */
    @Bean
    public ManagedChannel ordersServiceChannel(GrpcClientProperties properties) {
        String[] hostPort = properties.getAddress().split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port)
                .keepAliveTime(properties.getKeepAliveTime().toSeconds(), TimeUnit.SECONDS)
                .keepAliveTimeout(properties.getKeepAliveTimeout().toSeconds(), TimeUnit.SECONDS)
                .keepAliveWithoutCalls(properties.isKeepAliveWithoutCalls())
                .maxInboundMessageSize(4 * 1024 * 1024); // 4MB default

        if (properties.getRetry().isEnabled()) {
            channelBuilder
                    .defaultServiceConfig(buildRetryServiceConfig(properties.getRetry()))
                    .enableRetry();
        } else {
            channelBuilder.disableRetry();
        }

        channelBuilder.usePlaintext(); // Use plaintext for development

        // Apply TLS configuration if enabled
        if (properties.getSecurity().isTlsEnabled()) {
            channelBuilder.useTransportSecurity();
            // Additional TLS configuration can be added here based on properties
        }

        return channelBuilder.build();
    }

    /**
     * Creates an asynchronous stub for Orders gRPC service.
     *
     * <p>The stub is configured with deadline settings from {@link GrpcClientProperties}
     * and can be used for non-blocking gRPC calls. All calls through this stub will
     * have the configured deadline applied.</p>
     *
     * @param channel the managed channel for gRPC communication
     * @param properties gRPC client configuration properties
     * @return configured {@link OrdersServiceGrpc.OrdersServiceStub}
     */
    @Bean
    public OrdersServiceGrpc.OrdersServiceStub ordersServiceStub(
            ManagedChannel channel, GrpcClientProperties properties) {

        OrdersServiceGrpc.OrdersServiceStub stub = OrdersServiceGrpc.newStub(channel);

        // Apply deadline if configured
        Duration deadline = properties.getDeadline();
        if (deadline != null && !deadline.isZero()) {
            stub = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
        }

        return stub;
    }

    /**
     * Creates a blocking stub for Orders gRPC service.
     *
     * <p>The blocking stub provides synchronous gRPC calls and is useful for simple
     * request-response patterns. It includes the same timeout and deadline configuration
     * as the async stub.</p>
     *
     * @param channel the managed channel for gRPC communication
     * @param properties gRPC client configuration properties
     * @return configured {@link OrdersServiceGrpc.OrdersServiceBlockingStub}
     */
    @Bean
    public OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub(
            ManagedChannel channel, GrpcClientProperties properties) {

        OrdersServiceGrpc.OrdersServiceBlockingStub stub = OrdersServiceGrpc.newBlockingStub(channel);

        // Apply deadline if configured
        Duration deadline = properties.getDeadline();
        if (deadline != null && !deadline.isZero()) {
            stub = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
        }

        return stub;
    }

    /**
     * Creates a blocking stub for Product Catalog gRPC service.
     *
     * <p>This stub is used for synchronous product validation calls and includes
     * the same timeout and deadline configuration as other service stubs. It reuses
     * the same managed channel for efficient connection pooling.</p>
     *
     * @param channel the managed channel for gRPC communication
     * @param properties gRPC client configuration properties
     * @return configured {@link ProductCatalogServiceGrpc.ProductCatalogServiceBlockingStub}
     */
    @Bean
    public ProductCatalogServiceGrpc.ProductCatalogServiceBlockingStub productCatalogServiceBlockingStub(
            ManagedChannel channel, GrpcClientProperties properties) {

        ProductCatalogServiceGrpc.ProductCatalogServiceBlockingStub stub =
                ProductCatalogServiceGrpc.newBlockingStub(channel);

        // Apply deadline if configured
        Duration deadline = properties.getDeadline();
        if (deadline != null && !deadline.isZero()) {
            stub = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
        }

        return stub;
    }

    /**
     * Creates an asynchronous stub for Product Catalog gRPC service.
     *
     * <p>The async stub can be used for non-blocking product validation calls
     * when needed. Currently used primarily by the blocking implementation but
     * available for future async operations.</p>
     *
     * @param channel the managed channel for gRPC communication
     * @param properties gRPC client configuration properties
     * @return configured {@link ProductCatalogServiceGrpc.ProductCatalogServiceStub}
     */
    @Bean
    public ProductCatalogServiceGrpc.ProductCatalogServiceStub productCatalogServiceStub(
            ManagedChannel channel, GrpcClientProperties properties) {

        ProductCatalogServiceGrpc.ProductCatalogServiceStub stub = ProductCatalogServiceGrpc.newStub(channel);

        // Apply deadline if configured
        Duration deadline = properties.getDeadline();
        if (deadline != null && !deadline.isZero()) {
            stub = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
        }

        return stub;
    }

    private Map<String, ?> buildRetryServiceConfig(RetryProperties retryProperties) {
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", (double) retryProperties.getMaxAttempts());
        retryPolicy.put("initialBackoff", toSecondsString(retryProperties.getInitialBackoff()));
        retryPolicy.put("maxBackoff", toSecondsString(retryProperties.getMaxBackoff()));
        retryPolicy.put("backoffMultiplier", retryProperties.getMultiplier());
        retryPolicy.put("retryableStatusCodes", List.of("UNAVAILABLE", "DEADLINE_EXCEEDED"));

        Map<String, Object> methodConfig = new HashMap<>();
        methodConfig.put("name", List.of(Map.of()));
        methodConfig.put("retryPolicy", retryPolicy);

        return Map.of("methodConfig", List.of(methodConfig));
    }

    private String toSecondsString(Duration duration) {
        BigDecimal seconds = BigDecimal.valueOf(duration.getSeconds());
        if (duration.getNano() != 0) {
            seconds = seconds.add(BigDecimal.valueOf(duration.getNano(), 9));
        }
        return seconds.stripTrailingZeros().toPlainString() + "s";
    }
}

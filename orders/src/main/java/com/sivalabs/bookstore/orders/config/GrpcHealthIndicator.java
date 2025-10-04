package com.sivalabs.bookstore.orders.config;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * gRPC service health indicator exposed through Spring Boot Actuator.
 *
 * <p>This indicator performs connectivity checks to the gRPC server using the standard
 * gRPC health checking protocol (grpc.health.v1.Health). It validates both the server
 * availability and the Orders service specifically.</p>
 *
 * <p>The health check performs the following validations:</p>
 * <ul>
 *   <li>Server connectivity and response time</li>
 *   <li>Orders service availability through health endpoint</li>
 *   <li>General gRPC server health status</li>
 * </ul>
 */
@Component
@ConditionalOnClass(name = "io.grpc.health.v1.HealthGrpc")
@ConditionalOnProperty(name = "grpc.client.orders.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(GrpcHealthIndicator.class);
    private static final String ORDERS_SERVICE_NAME = "com.sivalabs.bookstore.orders.OrdersService";
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 5;

    private final Channel grpcChannel;
    private final GrpcServerProperties grpcServerProperties;

    public GrpcHealthIndicator(
            @Qualifier("ordersServiceChannel") Channel grpcChannel, GrpcServerProperties grpcServerProperties) {
        this.grpcChannel = grpcChannel;
        this.grpcServerProperties = grpcServerProperties;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        boolean serverHealthy = checkServerHealth(details);
        boolean ordersServiceHealthy = checkOrdersServiceHealth(details);

        addConfigurationDetails(details);
        details.put("timestamp", Instant.now().toString());

        boolean overallHealthy = serverHealthy && ordersServiceHealthy;
        return overallHealthy
                ? Health.up().withDetails(details).build()
                : Health.down().withDetails(details).build();
    }

    private boolean checkServerHealth(Map<String, Object> details) {
        Map<String, Object> serverDetails = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();

            HealthGrpc.HealthBlockingStub healthStub = HealthGrpc.newBlockingStub(grpcChannel)
                    .withDeadlineAfter(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("") // Empty service name checks overall server health
                    .build();

            HealthCheckResponse response = healthStub.check(request);
            long responseTime = System.currentTimeMillis() - startTime;

            boolean isServing = response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;

            serverDetails.put("status", isServing ? "UP" : "DOWN");
            serverDetails.put("servingStatus", response.getStatus().name());
            serverDetails.put("responseTimeMs", responseTime);

            if (!isServing) {
                serverDetails.put("message", "gRPC server is not serving requests");
                details.put("grpcServer", serverDetails);
                return false;
            }

            serverDetails.put("message", "gRPC server is healthy");
            details.put("grpcServer", serverDetails);
            return true;
        } catch (StatusRuntimeException ex) {
            logger.warn(
                    "gRPC server health check failed with status: {}",
                    ex.getStatus().getCode(),
                    ex);
            serverDetails.put("status", "ERROR");
            serverDetails.put("error", ex.getStatus().getCode().name());
            serverDetails.put(
                    "message",
                    ex.getStatus().getDescription() != null
                            ? ex.getStatus().getDescription()
                            : "gRPC server connectivity failed");
            details.put("grpcServer", serverDetails);
            return false;
        } catch (Exception ex) {
            logger.warn("gRPC server health check failed", ex);
            serverDetails.put("status", "ERROR");
            serverDetails.put("error", ex.getClass().getSimpleName());
            serverDetails.put("message", ex.getMessage() != null ? ex.getMessage() : "Unexpected error");
            details.put("grpcServer", serverDetails);
            return false;
        }
    }

    private boolean checkOrdersServiceHealth(Map<String, Object> details) {
        Map<String, Object> serviceDetails = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();

            HealthGrpc.HealthBlockingStub healthStub = HealthGrpc.newBlockingStub(grpcChannel)
                    .withDeadlineAfter(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService(ORDERS_SERVICE_NAME)
                    .build();

            HealthCheckResponse response = healthStub.check(request);
            long responseTime = System.currentTimeMillis() - startTime;

            boolean isServing = response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;

            serviceDetails.put("status", isServing ? "UP" : "DOWN");
            serviceDetails.put("serviceName", ORDERS_SERVICE_NAME);
            serviceDetails.put("servingStatus", response.getStatus().name());
            serviceDetails.put("responseTimeMs", responseTime);

            if (!isServing) {
                serviceDetails.put("message", "Orders gRPC service is not serving");
                details.put("ordersGrpcService", serviceDetails);
                return false;
            }

            serviceDetails.put("message", "Orders gRPC service is healthy");
            details.put("ordersGrpcService", serviceDetails);
            return true;
        } catch (StatusRuntimeException ex) {
            logger.warn(
                    "Orders gRPC service health check failed with status: {}",
                    ex.getStatus().getCode(),
                    ex);
            serviceDetails.put("status", "ERROR");
            serviceDetails.put("serviceName", ORDERS_SERVICE_NAME);
            serviceDetails.put("error", ex.getStatus().getCode().name());

            if (ex.getStatus().getCode().name().equals("NOT_FOUND")) {
                serviceDetails.put(
                        "message", "Orders service health endpoint not found - service may not support health checks");
            } else {
                serviceDetails.put(
                        "message",
                        ex.getStatus().getDescription() != null
                                ? ex.getStatus().getDescription()
                                : "Orders service connectivity failed");
            }

            details.put("ordersGrpcService", serviceDetails);
            return false;
        } catch (Exception ex) {
            logger.warn("Orders gRPC service health check failed", ex);
            serviceDetails.put("status", "ERROR");
            serviceDetails.put("serviceName", ORDERS_SERVICE_NAME);
            serviceDetails.put("error", ex.getClass().getSimpleName());
            serviceDetails.put("message", ex.getMessage() != null ? ex.getMessage() : "Unexpected error");
            details.put("ordersGrpcService", serviceDetails);
            return false;
        }
    }

    private void addConfigurationDetails(Map<String, Object> details) {
        details.put(
                "configuration",
                Map.of(
                        "healthCheckTimeoutSeconds", HEALTH_CHECK_TIMEOUT_SECONDS,
                        "keepAliveTime", grpcServerProperties.keepAliveTime().toString(),
                        "keepAliveTimeout",
                                grpcServerProperties.keepAliveTimeout().toString(),
                        "maxInboundMessageSize",
                                grpcServerProperties.maxInboundMessageSize().toString(),
                        "enableReflection", grpcServerProperties.enableReflection()));
    }
}

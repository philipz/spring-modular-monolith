package com.sivalabs.bookstore.config;

import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base configuration entry point for wiring gRPC server components.
 *
 * Bean definitions will be added in subsequent tasks once the supporting
 * infrastructure classes are in place.
 */
@Configuration
@EnableConfigurationProperties(GrpcProperties.class)
@ConditionalOnClass(Server.class)
public class GrpcServerConfig {

    @Bean
    public Server grpcServer(GrpcProperties grpcProperties, OrdersServiceGrpc.OrdersServiceImplBase ordersGrpcService) {
        var serverProperties = grpcProperties.getServer();

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serverProperties.getPort())
                .addService(ordersGrpcService)
                .maxInboundMessageSize(serverProperties.getMaxInboundMessageSize());

        if (serverProperties.isHealthCheckEnabled()) {
            HealthStatusManager healthStatusManager = new HealthStatusManager();
            serverBuilder.addService(healthStatusManager.getHealthService());
        }

        if (serverProperties.isReflectionEnabled()) {
            serverBuilder.addService(ProtoReflectionService.newInstance());
        }

        return serverBuilder.build();
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(Server grpcServer, GrpcProperties grpcProperties) {
        long gracePeriodSeconds = grpcProperties.getServer().getShutdownGracePeriodSeconds();
        return new GrpcServerLifecycle(grpcServer, gracePeriodSeconds);
    }
}

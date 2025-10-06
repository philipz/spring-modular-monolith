package com.sivalabs.bookstore.config;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "bookstore.grpc.server.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerConfig {

    @Bean
    public Server grpcServer(GrpcProperties grpcProperties, List<BindableService> grpcServices) {
        var serverProperties = grpcProperties.getServer();

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serverProperties.getPort())
                .maxInboundMessageSize(serverProperties.getMaxInboundMessageSize());

        grpcServices.forEach(serverBuilder::addService);

        if (serverProperties.isHealthCheckEnabled()) {
            HealthStatusManager healthStatusManager = new HealthStatusManager();
            serverBuilder.addService(healthStatusManager.getHealthService());
            healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
            grpcServices.forEach(service -> healthStatusManager.setStatus(
                    service.bindService().getServiceDescriptor().getName(), HealthCheckResponse.ServingStatus.SERVING));
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

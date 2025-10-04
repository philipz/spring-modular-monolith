package com.sivalabs.bookstore.orders.config;

import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.util.concurrent.TimeUnit;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrpcServerProperties.class)
@ConditionalOnClass(GrpcServerConfigurer.class)
public class GrpcServerConfiguration {

    @Bean
    GrpcServerConfigurer grpcServerConfigurer(
            GrpcServerProperties properties, ObjectProvider<ServerInterceptor> serverInterceptors) {
        return serverBuilder -> {
            configureBaseServerOptions(serverBuilder, properties);
            configureInterceptors(serverBuilder, serverInterceptors);
            configureReflection(serverBuilder, properties);
        };
    }

    private void configureBaseServerOptions(ServerBuilder<?> serverBuilder, GrpcServerProperties properties) {
        long inboundBytes = properties.maxInboundMessageSize().toBytes();
        int maxInboundSize = inboundBytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) inboundBytes;

        serverBuilder.maxInboundMessageSize(maxInboundSize);
        serverBuilder.keepAliveTime(properties.keepAliveTime().toMillis(), TimeUnit.MILLISECONDS);
        serverBuilder.keepAliveTimeout(properties.keepAliveTimeout().toMillis(), TimeUnit.MILLISECONDS);
        serverBuilder.maxConnectionIdle(properties.maxConnectionIdle().toMillis(), TimeUnit.MILLISECONDS);
        serverBuilder.permitKeepAliveWithoutCalls(properties.keepAliveWithoutCalls());
    }

    private void configureInterceptors(
            ServerBuilder<?> serverBuilder, ObjectProvider<ServerInterceptor> serverInterceptors) {
        serverInterceptors.orderedStream().forEach(serverBuilder::intercept);
    }

    private void configureReflection(ServerBuilder<?> serverBuilder, GrpcServerProperties properties) {
        if (properties.enableReflection()) {
            serverBuilder.addService(ProtoReflectionService.newInstance());
        }
    }
}

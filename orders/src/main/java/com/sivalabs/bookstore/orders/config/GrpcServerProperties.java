package com.sivalabs.bookstore.orders.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "grpc.server")
public record GrpcServerProperties(
        @DefaultValue("9090") int port,
        @DefaultValue("4MB") DataSize maxInboundMessageSize,
        @DefaultValue("PT30S") Duration keepAliveTime,
        @DefaultValue("PT5S") Duration keepAliveTimeout,
        @DefaultValue("true") boolean keepAliveWithoutCalls,
        @DefaultValue("PT60S") Duration maxConnectionIdle,
        @DefaultValue("true") boolean enableReflection,
        SecurityConfig security) {

    public GrpcServerProperties {
        if (security == null) {
            security = new SecurityConfig(false, null, null, null);
        }
    }

    public record SecurityConfig(
            @DefaultValue("false") boolean tlsEnabled,
            String certChainPath,
            String privateKeyPath,
            String trustCertCollectionPath) {}
}

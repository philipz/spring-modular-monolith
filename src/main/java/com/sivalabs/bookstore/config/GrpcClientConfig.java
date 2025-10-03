package com.sivalabs.bookstore.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ManagedChannel.class)
@EnableConfigurationProperties(GrpcProperties.class)
public class GrpcClientConfig {

    private final GrpcProperties grpcProperties;
    private ManagedChannel managedChannel;

    public GrpcClientConfig(GrpcProperties grpcProperties) {
        this.grpcProperties = grpcProperties;
    }

    @Bean
    public GrpcRetryInterceptor grpcRetryInterceptor() {
        return new GrpcRetryInterceptor(grpcProperties.getClient());
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel managedChannel(GrpcRetryInterceptor retryInterceptor) {
        var clientProperties = grpcProperties.getClient();
        ManagedChannelBuilder<?> builder =
                ManagedChannelBuilder.forTarget(clientProperties.getTarget()).usePlaintext();

        builder.keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .idleTimeout(5, TimeUnit.MINUTES);

        if (clientProperties.isRetryEnabled()) {
            builder.intercept(retryInterceptor);
        }

        this.managedChannel = builder.build();
        return managedChannel;
    }

    @PreDestroy
    void shutdownChannel() {
        if (managedChannel == null) {
            return;
        }
        managedChannel.shutdown();
        try {
            if (!managedChannel.awaitTermination(5, TimeUnit.SECONDS)) {
                managedChannel.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            managedChannel.shutdownNow();
        }
    }
}

package com.sivalabs.bookstore.config;

import io.grpc.Server;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Simple Actuator health indicator that reflects the state of the embedded gRPC server.
 */
@Component
@ConditionalOnClass(Server.class)
public class GrpcHealthIndicator implements HealthIndicator {

    private final Server server;

    public GrpcHealthIndicator(Server server) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public Health health() {
        boolean running = !server.isShutdown() && !server.isTerminated();
        if (running) {
            return Health.up()
                    .withDetail("port", server.getPort())
                    .withDetail("services", server.getServices().size())
                    .build();
        }
        return Health.down()
                .withDetail("reason", "gRPC server not running")
                .withDetail("port", server.getPort())
                .build();
    }
}

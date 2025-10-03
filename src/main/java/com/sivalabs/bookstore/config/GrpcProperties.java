package com.sivalabs.bookstore.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for gRPC server and client components.
 *
 * This class exposes nested property groups that will be fleshed out in
 * subsequent tasks to support fine-grained control over the gRPC stack.
 */
@ConfigurationProperties(prefix = "bookstore.grpc")
@Validated
public class GrpcProperties {
    private final ServerProperties server = new ServerProperties();
    private final ClientProperties client = new ClientProperties();

    public ServerProperties getServer() {
        return server;
    }

    public ClientProperties getClient() {
        return client;
    }
    /**
     * Server-side gRPC configuration group.
     */
    public static class ServerProperties {
        @Min(1024) @Max(65535) private int port = 9091;

        @Min(1) private int maxInboundMessageSize = 4_194_304;

        private boolean healthCheckEnabled = true;
        private boolean reflectionEnabled = true;

        @Min(0) private long shutdownGracePeriodSeconds = 30L;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getMaxInboundMessageSize() {
            return maxInboundMessageSize;
        }

        public void setMaxInboundMessageSize(int maxInboundMessageSize) {
            this.maxInboundMessageSize = maxInboundMessageSize;
        }

        public boolean isHealthCheckEnabled() {
            return healthCheckEnabled;
        }

        public void setHealthCheckEnabled(boolean healthCheckEnabled) {
            this.healthCheckEnabled = healthCheckEnabled;
        }

        public boolean isReflectionEnabled() {
            return reflectionEnabled;
        }

        public void setReflectionEnabled(boolean reflectionEnabled) {
            this.reflectionEnabled = reflectionEnabled;
        }

        public long getShutdownGracePeriodSeconds() {
            return shutdownGracePeriodSeconds;
        }

        public void setShutdownGracePeriodSeconds(long shutdownGracePeriodSeconds) {
            this.shutdownGracePeriodSeconds = shutdownGracePeriodSeconds;
        }
    }
    /**
     * Client-side gRPC configuration group.
     */
    public static class ClientProperties {
        private String target = "localhost:9091";

        @Min(1_000) private int deadlineMs = 5_000;

        private boolean retryEnabled = true;

        @Min(1) private int maxRetryAttempts = 3;

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public int getDeadlineMs() {
            return deadlineMs;
        }

        public void setDeadlineMs(int deadlineMs) {
            this.deadlineMs = deadlineMs;
        }

        public boolean isRetryEnabled() {
            return retryEnabled;
        }

        public void setRetryEnabled(boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
        }

        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        @Override
        public String toString() {
            return "ClientProperties{"
                    + "target='" + target + '\''
                    + ", deadlineMs=" + deadlineMs
                    + ", retryEnabled=" + retryEnabled
                    + ", maxRetryAttempts=" + maxRetryAttempts
                    + '}';
        }
    }
}

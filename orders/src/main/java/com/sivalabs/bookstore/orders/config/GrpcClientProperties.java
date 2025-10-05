package com.sivalabs.bookstore.orders.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Orders gRPC client connections.
 *
 * <p>Provides tunable connection, deadline, keep-alive, retry, and TLS settings so different
 * environments can adjust behaviour without code changes.</p>
 */
@ConfigurationProperties(prefix = "grpc.client.orders")
@Validated
public class GrpcClientProperties {

    private String address = "localhost:9090";
    private Duration deadline = Duration.ofSeconds(5);
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration keepAliveTime = Duration.ofSeconds(30);
    private Duration keepAliveTimeout = Duration.ofSeconds(5);
    private boolean keepAliveWithoutCalls = true;
    private RetryProperties retry = new RetryProperties();
    private SecurityProperties security = new SecurityProperties();

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Duration getDeadline() {
        return deadline;
    }

    public void setDeadline(Duration deadline) {
        this.deadline = deadline;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public Duration getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(Duration keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public boolean isKeepAliveWithoutCalls() {
        return keepAliveWithoutCalls;
    }

    public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
        this.keepAliveWithoutCalls = keepAliveWithoutCalls;
    }

    public RetryProperties getRetry() {
        return retry;
    }

    public void setRetry(RetryProperties retry) {
        this.retry = retry;
    }

    public SecurityProperties getSecurity() {
        return security;
    }

    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    /**
     * Resilience and retry configuration for outbound gRPC calls.
     */
    public static class RetryProperties {

        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration initialBackoff = Duration.ofMillis(200);
        private Duration maxBackoff = Duration.ofSeconds(5);
        private double multiplier = 2.0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialBackoff() {
            return initialBackoff;
        }

        public void setInitialBackoff(Duration initialBackoff) {
            this.initialBackoff = initialBackoff;
        }

        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    /**
     * TLS configuration for securing gRPC client connections.
     */
    public static class SecurityProperties {

        private boolean tlsEnabled = false;
        private String certChainPath;
        private String privateKeyPath;
        private String trustCertCollectionPath;

        public boolean isTlsEnabled() {
            return tlsEnabled;
        }

        public void setTlsEnabled(boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
        }

        public String getCertChainPath() {
            return certChainPath;
        }

        public void setCertChainPath(String certChainPath) {
            this.certChainPath = certChainPath;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getTrustCertCollectionPath() {
            return trustCertCollectionPath;
        }

        public void setTrustCertCollectionPath(String trustCertCollectionPath) {
            this.trustCertCollectionPath = trustCertCollectionPath;
        }
    }
}

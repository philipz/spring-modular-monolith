package com.sivalabs.bookstore.orders.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "otlp.grpc")
public class OtlpProperties {

    /**
     * OTLP gRPC endpoint URL (e.g., http://localhost:4317)
     */
    private String endpoint = "http://localhost:4317";

    /**
     * Request timeout for OTLP gRPC calls
     */
    private Duration timeout = Duration.ofSeconds(10);

    /**
     * Compression type for OTLP gRPC (none, gzip)
     */
    private String compression = "none";

    /**
     * Custom headers to include in OTLP gRPC requests
     */
    private Map<String, String> headers = new HashMap<>();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}

package com.sivalabs.bookstore.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenTelemetry OTLP gRPC span exporter.
 *
 * <p>This configuration enables sending traces to HyperDX (or any OTLP-compatible backend) using
 * the gRPC protocol instead of the default HTTP protocol.
 *
 * <p>gRPC benefits:
 * <ul>
 *   <li>Better performance with binary protocol</li>
 *   <li>Lower latency with HTTP/2 multiplexing</li>
 *   <li>Industry standard for OpenTelemetry ecosystem</li>
 * </ul>
 *
 * <p>Configuration properties:
 * <pre>
 * otlp.grpc.endpoint=http://localhost:4317
 * otlp.grpc.timeout=10s
 * otlp.grpc.compression=none
 * otlp.grpc.headers.authorization=your-api-key
 * </pre>
 */
@Configuration
@ConditionalOnClass(OtlpGrpcSpanExporter.class)
@ConditionalOnProperty(prefix = "otlp.grpc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OtlpProperties.class)
public class OtlpGrpcTracingConfig {

    /**
     * Creates an OTLP gRPC span exporter bean.
     *
     * <p>This bean will be picked up by Spring Boot's tracing auto-configuration and used to export
     * spans to the configured OTLP endpoint via gRPC.
     *
     * @param properties OTLP configuration properties
     * @return configured OTLP gRPC span exporter
     */
    @Bean
    public SpanExporter otlpGrpcSpanExporter(OtlpProperties properties) {
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(properties.getEndpoint())
                .setTimeout(properties.getTimeout())
                .setCompression(properties.getCompression());

        // Add custom headers (e.g., authorization header for HyperDX)
        for (Map.Entry<String, String> header : properties.getHeaders().entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }

        return builder.build();
    }
}

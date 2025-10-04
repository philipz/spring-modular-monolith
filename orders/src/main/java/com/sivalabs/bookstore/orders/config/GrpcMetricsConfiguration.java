package com.sivalabs.bookstore.orders.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC server and client metrics collection using Micrometer.
 *
 * <p>This configuration provides comprehensive metrics for gRPC operations including:
 * <ul>
 *   <li>Request duration timing with service, method, and status tags</li>
 *   <li>Request counters for monitoring call volume</li>
 *   <li>Error counters for tracking failure rates</li>
 *   <li>Both server-side and client-side metrics collection</li>
 * </ul>
 *
 * <p>All metrics are tagged with service name, method name, and status code for
 * detailed monitoring and alerting capabilities.</p>
 */
@Configuration
@ConditionalOnClass(name = "io.grpc.ServerInterceptor")
@ConditionalOnProperty(name = "grpc.client.orders.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcMetricsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GrpcMetricsConfiguration.class);

    /**
     * Creates a server interceptor for collecting gRPC server metrics.
     *
     * <p>This interceptor collects metrics for all incoming gRPC requests including:
     * <ul>
     *   <li>grpc.server.request.duration - Timer for request duration</li>
     *   <li>grpc.server.requests.total - Counter for total requests</li>
     *   <li>grpc.server.errors.total - Counter for failed requests</li>
     * </ul>
     *
     * @param meterRegistry the Micrometer meter registry
     * @return configured server metrics interceptor
     */
    @Bean
    public ServerInterceptor grpcServerMetricsInterceptor(MeterRegistry meterRegistry) {
        logger.info("Creating gRPC server metrics interceptor");
        return new GrpcServerMetricsInterceptor(meterRegistry);
    }

    /**
     * Creates a client interceptor for collecting gRPC client metrics.
     *
     * <p>This interceptor collects metrics for all outgoing gRPC requests including:
     * <ul>
     *   <li>grpc.client.request.duration - Timer for request duration</li>
     *   <li>grpc.client.requests.total - Counter for total requests</li>
     *   <li>grpc.client.errors.total - Counter for failed requests</li>
     * </ul>
     *
     * @param meterRegistry the Micrometer meter registry
     * @return configured client metrics interceptor
     */
    @Bean
    public ClientInterceptor grpcClientMetricsInterceptor(MeterRegistry meterRegistry) {
        logger.info("Creating gRPC client metrics interceptor");
        return new GrpcClientMetricsInterceptor(meterRegistry);
    }

    /**
     * Server interceptor implementation for collecting gRPC server metrics.
     */
    private static class GrpcServerMetricsInterceptor implements ServerInterceptor {

        private final MeterRegistry meterRegistry;

        public GrpcServerMetricsInterceptor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

            String serviceName = call.getMethodDescriptor().getServiceName();
            String methodName = call.getMethodDescriptor().getBareMethodName();
            Instant startTime = Instant.now();

            // Increment request counter
            Counter.builder("grpc.server.requests.total")
                    .description("Total number of gRPC server requests")
                    .tag("service", serviceName)
                    .tag("method", methodName)
                    .register(meterRegistry)
                    .increment();

            ServerCall<ReqT, RespT> wrappedCall =
                    new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                        @Override
                        public void close(Status status, Metadata trailers) {
                            // Record request duration
                            Duration duration = Duration.between(startTime, Instant.now());
                            Timer.builder("grpc.server.request.duration")
                                    .description("Duration of gRPC server requests")
                                    .tag("service", serviceName)
                                    .tag("method", methodName)
                                    .tag("status", status.getCode().name())
                                    .register(meterRegistry)
                                    .record(duration);

                            // Record errors
                            if (!status.isOk()) {
                                Counter.builder("grpc.server.errors.total")
                                        .description("Total number of gRPC server errors")
                                        .tag("service", serviceName)
                                        .tag("method", methodName)
                                        .tag("status", status.getCode().name())
                                        .register(meterRegistry)
                                        .increment();
                            }

                            super.close(status, trailers);
                        }
                    };

            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                    next.startCall(wrappedCall, headers)) {
                // Listener implementation can be extended here for additional metrics if needed
            };
        }
    }

    /**
     * Client interceptor implementation for collecting gRPC client metrics.
     */
    private static class GrpcClientMetricsInterceptor implements ClientInterceptor {

        private final MeterRegistry meterRegistry;

        public GrpcClientMetricsInterceptor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

            String serviceName = method.getServiceName();
            String methodName = method.getBareMethodName();
            Instant startTime = Instant.now();

            // Increment request counter
            Counter.builder("grpc.client.requests.total")
                    .description("Total number of gRPC client requests")
                    .tag("service", serviceName)
                    .tag("method", methodName)
                    .register(meterRegistry)
                    .increment();

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    super.start(
                            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                                    responseListener) {
                                @Override
                                public void onClose(Status status, Metadata trailers) {
                                    // Record request duration
                                    Duration duration = Duration.between(startTime, Instant.now());
                                    Timer.builder("grpc.client.request.duration")
                                            .description("Duration of gRPC client requests")
                                            .tag("service", serviceName)
                                            .tag("method", methodName)
                                            .tag("status", status.getCode().name())
                                            .register(meterRegistry)
                                            .record(duration);

                                    // Record errors
                                    if (!status.isOk()) {
                                        Counter.builder("grpc.client.errors.total")
                                                .description("Total number of gRPC client errors")
                                                .tag("service", serviceName)
                                                .tag("method", methodName)
                                                .tag("status", status.getCode().name())
                                                .register(meterRegistry)
                                                .increment();
                                    }

                                    super.onClose(status, trailers);
                                }
                            },
                            headers);
                }
            };
        }
    }
}

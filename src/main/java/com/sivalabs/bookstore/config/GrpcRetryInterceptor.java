package com.sivalabs.bookstore.config;

import com.sivalabs.bookstore.config.GrpcProperties.ClientProperties;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import jakarta.annotation.PreDestroy;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side interceptor providing exponential backoff retries for unary calls.
 */
public class GrpcRetryInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcRetryInterceptor.class);
    private static final Set<Status.Code> RETRYABLE_CODES =
            EnumSet.of(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED);
    private static final long BASE_DELAY_MILLIS = 100L;

    private final ClientProperties clientProperties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public GrpcRetryInterceptor(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        if (!clientProperties.isRetryEnabled() || method.getType() != MethodDescriptor.MethodType.UNARY) {
            return next.newCall(method, callOptions);
        }
        return new RetryingClientCall<>(next, method, callOptions);
    }

    private final class RetryingClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {

        private final Channel channel;
        private final MethodDescriptor<ReqT, RespT> method;
        private final CallOptions callOptions;

        private Listener<RespT> responseListener;
        private Metadata headers;
        private ReqT requestMessage;
        private boolean messageSent;
        private boolean halfClosed;
        private ClientCall<ReqT, RespT> delegate;
        private int attempt;
        private boolean completed;

        private RetryingClientCall(Channel channel, MethodDescriptor<ReqT, RespT> method, CallOptions callOptions) {
            this.channel = channel;
            this.method = method;
            this.callOptions = callOptions;
        }

        @Override
        public void start(Listener<RespT> listener, Metadata headers) {
            this.responseListener = listener;
            this.headers = headers;
            startNewAttempt();
        }

        private void startNewAttempt() {
            attempt++;
            delegate = channel.newCall(method, callOptions);
            delegate.start(new RetryListener(attempt), headers);
            if (messageSent) {
                delegate.sendMessage(requestMessage);
                if (halfClosed) {
                    delegate.halfClose();
                }
            }
            delegate.request(1);
        }

        @Override
        public void request(int numMessages) {
            delegate.request(numMessages);
        }

        @Override
        public void cancel(String message, Throwable cause) {
            completed = true;
            delegate.cancel(message, cause);
        }

        @Override
        public void halfClose() {
            halfClosed = true;
            delegate.halfClose();
        }

        @Override
        public void sendMessage(ReqT message) {
            this.requestMessage = message;
            this.messageSent = true;
            delegate.sendMessage(message);
        }

        private final class RetryListener extends ClientCall.Listener<RespT> {

            private final int currentAttempt;

            RetryListener(int currentAttempt) {
                this.currentAttempt = currentAttempt;
            }

            @Override
            public void onHeaders(Metadata headers) {
                responseListener.onHeaders(headers);
            }

            @Override
            public void onMessage(RespT message) {
                responseListener.onMessage(message);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (completed) {
                    responseListener.onClose(status, trailers);
                    return;
                }

                if (status.isOk() || !shouldRetry(status)) {
                    responseListener.onClose(status, trailers);
                    return;
                }

                int maxAttempts = Math.max(1, clientProperties.getMaxRetryAttempts());
                if (currentAttempt >= maxAttempts) {
                    log.warn(
                            "gRPC call {} exhausted retries (attempts={})", method.getFullMethodName(), currentAttempt);
                    responseListener.onClose(status, trailers);
                    return;
                }

                long delay = computeBackoffDelay(currentAttempt);
                log.debug(
                        "Retrying gRPC call {} attempt {} after {} ms",
                        method.getFullMethodName(),
                        currentAttempt,
                        delay);

                scheduler.schedule(RetryingClientCall.this::startNewAttempt, delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private boolean shouldRetry(Status status) {
        return RETRYABLE_CODES.contains(status.getCode());
    }

    private long computeBackoffDelay(int attempt) {
        return (long) (BASE_DELAY_MILLIS * Math.pow(2, attempt - 1));
    }
}

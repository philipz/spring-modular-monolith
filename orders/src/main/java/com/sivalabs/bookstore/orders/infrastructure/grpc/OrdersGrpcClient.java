package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.grpc.orders.CreateOrderRequest;
import com.sivalabs.grpc.orders.FindOrderRequest;
import com.sivalabs.grpc.orders.FindOrdersRequest;
import com.sivalabs.grpc.orders.OrdersServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * gRPC client implementation for Orders service operations.
 *
 * <p>Provides gRPC client functionality with the same interface patterns as HTTP clients.
 * Converts between domain objects and Protocol Buffer messages using {@link GrpcOrderMapper}.
 * Handles gRPC-specific exceptions and converts them to domain exceptions.</p>
 *
 * <p>Note: Resilience4j annotations are commented out due to dependency resolution issues.
 * They should be uncommented once Resilience4j Spring Boot starter is properly configured.</p>
 */
@Component
@ConditionalOnClass(OrdersServiceGrpc.class)
@ConditionalOnProperty(name = "grpc.client.orders.enabled", havingValue = "true", matchIfMissing = true)
public class OrdersGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(OrdersGrpcClient.class);
    private static final String ORDERS_CIRCUIT_BREAKER = "ordersGrpc";
    private static final int TIMEOUT_SECONDS = 5;

    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceStub;
    private final GrpcOrderMapper mapper;

    public OrdersGrpcClient(OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceStub, GrpcOrderMapper mapper) {
        this.ordersServiceStub = ordersServiceStub;
        this.mapper = mapper;
    }

    /**
     * Creates a new order via gRPC.
     *
     * @param request the order creation request in domain format
     * @return the created order response
     * @throws OrderCreationException if order creation fails
     * @throws OrdersServiceException if gRPC service is unavailable
     */
    // @CircuitBreaker(name = ORDERS_CIRCUIT_BREAKER, fallbackMethod = "handleCreateOrderFailure")
    // @Retry(name = ORDERS_CIRCUIT_BREAKER)
    public CreateOrderResponse createOrder(com.sivalabs.bookstore.orders.api.CreateOrderRequest request) {
        try {
            log.debug(
                    "Creating order via gRPC for customer: {}",
                    request.customer().name());

            CreateOrderRequest grpcRequest = mapper.toProto(request);

            var grpcResponse = ordersServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .createOrder(grpcRequest);

            CreateOrderResponse response = mapper.toDomain(grpcResponse);
            log.debug("Successfully created order: {}", response.orderNumber());

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "gRPC createOrder failed: {} - {}",
                    e.getStatus().getCode(),
                    e.getStatus().getDescription());
            throw convertGrpcException("createOrder", e);
        }
    }

    /**
     * Finds an order by order number via gRPC.
     *
     * @param orderNumber the order number to search for
     * @return the order details
     * @throws OrderNotFoundException if order is not found
     * @throws OrdersServiceException if gRPC service is unavailable
     */
    // @CircuitBreaker(name = ORDERS_CIRCUIT_BREAKER, fallbackMethod = "handleFindOrderFailure")
    // @Retry(name = ORDERS_CIRCUIT_BREAKER)
    public OrderDto findOrder(String orderNumber) {
        try {
            log.debug("Finding order via gRPC: {}", orderNumber);

            FindOrderRequest grpcRequest =
                    FindOrderRequest.newBuilder().setOrderNumber(orderNumber).build();

            var grpcResponse = ordersServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .findOrder(grpcRequest);

            OrderDto response = mapper.toDomain(grpcResponse);
            log.debug("Successfully found order: {}", orderNumber);

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "gRPC findOrder failed: {} - {}",
                    e.getStatus().getCode(),
                    e.getStatus().getDescription());
            throw convertGrpcException("findOrder", e);
        }
    }

    /**
     * Finds all orders via gRPC.
     *
     * @return list of order views
     * @throws OrdersServiceException if gRPC service is unavailable
     */
    // @CircuitBreaker(name = ORDERS_CIRCUIT_BREAKER, fallbackMethod = "handleFindOrdersFailure")
    // @Retry(name = ORDERS_CIRCUIT_BREAKER)
    public List<OrderView> findOrders() {
        try {
            log.debug("Finding all orders via gRPC");

            FindOrdersRequest grpcRequest = FindOrdersRequest.newBuilder().build();

            var grpcResponse = ordersServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .findOrders(grpcRequest);

            List<OrderView> response =
                    grpcResponse.getOrdersList().stream().map(mapper::toDomain).toList();

            log.debug("Successfully found {} orders", response.size());

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "gRPC findOrders failed: {} - {}",
                    e.getStatus().getCode(),
                    e.getStatus().getDescription());
            throw convertGrpcException("findOrders", e);
        }
    }

    /**
     * Fallback method for createOrder failures.
     */
    CreateOrderResponse handleCreateOrderFailure(
            com.sivalabs.bookstore.orders.api.CreateOrderRequest request, Throwable throwable) {
        log.error("Orders gRPC service unavailable for createOrder: {}", throwable.getMessage());
        throw new OrdersServiceException("Unable to create order via gRPC service", throwable);
    }

    /**
     * Fallback method for findOrder failures.
     */
    OrderDto handleFindOrderFailure(String orderNumber, Throwable throwable) {
        log.error("Orders gRPC service unavailable for findOrder {}: {}", orderNumber, throwable.getMessage());
        throw new OrdersServiceException("Unable to find order via gRPC service", throwable);
    }

    /**
     * Fallback method for findOrders failures.
     */
    List<OrderView> handleFindOrdersFailure(Throwable throwable) {
        log.error("Orders gRPC service unavailable for findOrders: {}", throwable.getMessage());
        throw new OrdersServiceException("Unable to find orders via gRPC service", throwable);
    }

    /**
     * Converts gRPC StatusRuntimeException to appropriate domain exceptions.
     */
    private RuntimeException convertGrpcException(String operation, StatusRuntimeException e) {
        Status status = e.getStatus();
        String description = status.getDescription();

        return switch (status.getCode()) {
            case NOT_FOUND -> new OrderNotFoundException(description != null ? description : "Order not found");
            case INVALID_ARGUMENT ->
                new OrderValidationException(description != null ? description : "Invalid order data");
            case UNAVAILABLE, DEADLINE_EXCEEDED ->
                new OrdersServiceException(
                        String.format("Orders service temporarily unavailable for %s", operation), e);
            default ->
                new OrdersServiceException(
                        String.format("Orders gRPC %s failed with status: %s", operation, status.getCode()), e);
        };
    }

    /**
     * Exception thrown when orders service is unavailable.
     */
    public static class OrdersServiceException extends RuntimeException {
        public OrdersServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when order is not found.
     */
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when order validation fails.
     */
    public static class OrderValidationException extends RuntimeException {
        public OrderValidationException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when order creation fails.
     */
    public static class OrderCreationException extends RuntimeException {
        public OrderCreationException(String message) {
            super(message);
        }
    }
}

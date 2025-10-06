package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersRequest;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC client test utility for Orders service operations.
 *
 * <p><strong>TEST UTILITY ONLY:</strong> This class is used exclusively in tests to verify
 * the OrdersGrpcService server implementation. The orders service is a gRPC <strong>server</strong>,
 * not a client. This client wrapper is only used in integration tests to call the server.</p>
 *
 * <p>For the actual gRPC client used by the monolith to call the orders service,
 * see {@code com.sivalabs.bookstore.orders.grpc.OrdersGrpcClient} in the monolith project.</p>
 *
 * <p>Provides gRPC client functionality with the same interface patterns as HTTP clients.
 * Converts between domain objects and Protocol Buffer messages using {@link GrpcOrderMapper}.
 * Handles gRPC-specific exceptions and converts them to domain exceptions.</p>
 */
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
            log.info(
                    "[ORDERS-CLIENT] Starting order creation via gRPC for customer: {}, product: {}",
                    request.customer().name(),
                    request.item().code());

            CreateOrderRequest grpcRequest = mapper.toProto(request);

            log.debug("[ORDERS-CLIENT] Sending gRPC createOrder request");
            var grpcResponse = ordersServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .createOrder(grpcRequest);

            CreateOrderResponse response = mapper.toDomain(grpcResponse);
            log.info("[ORDERS-CLIENT] Successfully created order: {}", response.orderNumber());

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "[ORDERS-CLIENT] gRPC createOrder failed: status={}, description={}",
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
            log.info("[ORDERS-CLIENT] Finding order via gRPC: {}", orderNumber);

            GetOrderRequest grpcRequest =
                    GetOrderRequest.newBuilder().setOrderNumber(orderNumber).build();

            log.debug("[ORDERS-CLIENT] Sending gRPC getOrder request");
            var grpcResponse = ordersServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getOrder(grpcRequest);

            OrderDto response = mapper.toDomain(grpcResponse.getOrder());
            log.info("[ORDERS-CLIENT] Successfully found order: {}, status: {}", orderNumber, response.status());

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "[ORDERS-CLIENT] gRPC getOrder failed for {}: status={}, description={}",
                    orderNumber,
                    e.getStatus().getCode(),
                    e.getStatus().getDescription());
            throw convertGrpcException("getOrder", e);
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
            log.info("[ORDERS-CLIENT] Finding all orders via gRPC");

            ListOrdersRequest grpcRequest = ListOrdersRequest.newBuilder().build();

            log.debug("[ORDERS-CLIENT] Sending gRPC listOrders request");
            var grpcResponse = ordersServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .listOrders(grpcRequest);

            List<OrderView> response =
                    grpcResponse.getOrdersList().stream().map(mapper::toDomain).toList();

            log.info("[ORDERS-CLIENT] Successfully found {} orders", response.size());

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "[ORDERS-CLIENT] gRPC listOrders failed: status={}, description={}",
                    e.getStatus().getCode(),
                    e.getStatus().getDescription());
            throw convertGrpcException("listOrders", e);
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

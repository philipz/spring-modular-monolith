package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.OrdersApiService;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderResponse;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersRequest;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersResponse;
import com.sivalabs.bookstore.orders.grpc.proto.OrderView;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@GrpcService
@ConditionalOnClass(name = "com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc")
public class OrdersGrpcService extends OrdersServiceGrpc.OrdersServiceImplBase {

    private final OrdersApiService ordersApiService;
    private final GrpcOrderMapper mapper;
    private final GrpcExceptionMapper exceptionMapper;
    private static final Logger log = LoggerFactory.getLogger(OrdersGrpcService.class);
    private static final Pattern BASIC_EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public OrdersGrpcService(
            OrdersApiService ordersApiService, GrpcOrderMapper mapper, GrpcExceptionMapper exceptionMapper) {
        this.ordersApiService = ordersApiService;
        this.mapper = mapper;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
        try {
            // Validate request before processing
            log.debug("[ORDERS-SERVER] Validating createOrder request");
            validateCreateOrderRequest(request);

            log.info(
                    "[ORDERS-SERVER] Received createOrder request for customer={}, product={}",
                    request.getCustomer().getEmail(),
                    request.getItem().getCode());

            log.debug("[ORDERS-SERVER] Converting gRPC request to domain object");
            var domainRequest = mapper.toDomain(request);

            log.info("[ORDERS-SERVER] Delegating to OrdersApiService.createOrder()");
            var response = ordersApiService.createOrder(domainRequest);

            log.info("[ORDERS-SERVER] Order created successfully: {}", response.orderNumber());
            log.debug("[ORDERS-SERVER] Sending createOrder response");

            responseObserver.onNext(mapper.toProto(response));
            responseObserver.onCompleted();

            log.debug("[ORDERS-SERVER] createOrder completed successfully");
        } catch (IllegalArgumentException ex) {
            // Handle validation errors with INVALID_ARGUMENT status
            log.warn("[ORDERS-SERVER] Invalid createOrder request: {}", ex.getMessage());
            responseObserver.onError(exceptionMapper.map(ex));
        } catch (Exception ex) {
            // Safe logging - only log if customer and item exist
            String customerEmail = request.hasCustomer() ? request.getCustomer().getEmail() : "unknown";
            String productCode = request.hasItem() ? request.getItem().getCode() : "unknown";
            log.error(
                    "[ORDERS-SERVER] createOrder failed for customer={}, product={}: {}",
                    customerEmail,
                    productCode,
                    ex.getMessage(),
                    ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        try {
            log.info("[ORDERS-SERVER] Received getOrder request for orderNumber={}", request.getOrderNumber());

            log.debug("[ORDERS-SERVER] Querying OrdersApiService.findOrder()");
            ordersApiService
                    .findOrder(request.getOrderNumber())
                    .ifPresentOrElse(
                            order -> {
                                log.info(
                                        "[ORDERS-SERVER] Order found: {}, status: {}",
                                        request.getOrderNumber(),
                                        order.status());
                                log.debug("[ORDERS-SERVER] Sending getOrder response");
                                GetOrderResponse response = GetOrderResponse.newBuilder()
                                        .setOrder(mapper.toProto(order))
                                        .build();
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                            },
                            () -> {
                                log.warn("[ORDERS-SERVER] Order not found: {}", request.getOrderNumber());
                                responseObserver.onError(exceptionMapper.map(
                                        OrderNotFoundException.forOrderNumber(request.getOrderNumber())));
                            });

            log.debug("[ORDERS-SERVER] getOrder completed");
        } catch (Exception ex) {
            log.error(
                    "[ORDERS-SERVER] getOrder failed for orderNumber={}: {}",
                    request.getOrderNumber(),
                    ex.getMessage(),
                    ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    @Override
    public void listOrders(ListOrdersRequest request, StreamObserver<ListOrdersResponse> responseObserver) {
        try {
            log.info("[ORDERS-SERVER] Received listOrders request");

            log.debug("[ORDERS-SERVER] Querying OrdersApiService.findOrders()");
            List<com.sivalabs.bookstore.orders.api.OrderView> orderViews = ordersApiService.findOrders();

            log.debug("[ORDERS-SERVER] Converting {} orders to gRPC format", orderViews.size());
            List<OrderView> protoViews = mapper.toProtoOrderViews(orderViews);
            ListOrdersResponse response =
                    ListOrdersResponse.newBuilder().addAllOrders(protoViews).build();

            log.info("[ORDERS-SERVER] Found {} orders", orderViews.size());
            log.debug("[ORDERS-SERVER] Sending listOrders response");

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.debug("[ORDERS-SERVER] listOrders completed successfully");
        } catch (Exception ex) {
            log.error("[ORDERS-SERVER] listOrders failed: {}", ex.getMessage(), ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    /**
     * Validates the CreateOrderRequest for required fields and proper formats.
     *
     * @param request the gRPC CreateOrderRequest to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCreateOrderRequest(CreateOrderRequest request) {
        // Validate required fields exist
        if (!request.hasCustomer()) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (!request.hasItem()) {
            throw new IllegalArgumentException("Item is required");
        }
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        // Validate Customer fields
        var customer = request.getCustomer();
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        if (!isValidEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Invalid email format: " + customer.getEmail());
        }
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new IllegalArgumentException("Customer phone is required");
        }

        // Validate Item fields
        var item = request.getItem();
        if (item.getCode() == null || item.getCode().isBlank()) {
            throw new IllegalArgumentException("Product code is required");
        }
        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (item.getPrice() == null || item.getPrice().isBlank()) {
            throw new IllegalArgumentException("Product price is required");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(item.getPrice());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Product price must be a valid number", ex);
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than 0");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    /**
     * Validates email format using a simple regex pattern.
     *
     * @param email the email address to validate
     * @return true if email format is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String candidate = email.trim();
        if (!BASIC_EMAIL_PATTERN.matcher(candidate).matches()) {
            return false;
        }
        int atIndex = candidate.indexOf('@');
        String localPart = candidate.substring(0, atIndex);
        String domainPart = candidate.substring(atIndex + 1);

        if (localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")) {
            return false;
        }
        if (domainPart.startsWith("-")
                || domainPart.endsWith("-")
                || domainPart.startsWith(".")
                || domainPart.endsWith(".")
                || domainPart.contains("..")) {
            return false;
        }
        return true;
    }
}

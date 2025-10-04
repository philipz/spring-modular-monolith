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
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@GrpcService
@ConditionalOnClass(name = "com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc")
@ConditionalOnProperty(name = "grpc.client.orders.enabled", havingValue = "true", matchIfMissing = true)
public class OrdersGrpcService extends OrdersServiceGrpc.OrdersServiceImplBase {

    private final OrdersApiService ordersApiService;
    private final GrpcOrderMapper mapper;
    private final GrpcExceptionMapper exceptionMapper;
    private static final Logger log = LoggerFactory.getLogger(OrdersGrpcService.class);

    public OrdersGrpcService(
            OrdersApiService ordersApiService, GrpcOrderMapper mapper, GrpcExceptionMapper exceptionMapper) {
        this.ordersApiService = ordersApiService;
        this.mapper = mapper;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
        try {
            log.info(
                    "📨 [ORDERS-SERVER] Received createOrder request for customer={}, product={}",
                    request.getCustomer().getEmail(),
                    request.getItem().getCode());

            log.debug("🔄 [ORDERS-SERVER] Converting gRPC request to domain object");
            var domainRequest = mapper.toDomain(request);

            log.info("🚀 [ORDERS-SERVER] Delegating to OrdersApiService.createOrder()");
            var response = ordersApiService.createOrder(domainRequest);

            log.info("✅ [ORDERS-SERVER] Order created successfully: {}", response.orderNumber());
            log.debug("📤 [ORDERS-SERVER] Sending createOrder response");

            responseObserver.onNext(mapper.toProto(response));
            responseObserver.onCompleted();

            log.debug("🏁 [ORDERS-SERVER] createOrder completed successfully");
        } catch (Exception ex) {
            log.error(
                    "🚨 [ORDERS-SERVER] createOrder failed for customer={}, product={}: {}",
                    request.getCustomer().getEmail(),
                    request.getItem().getCode(),
                    ex.getMessage(),
                    ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        try {
            log.info("📨 [ORDERS-SERVER] Received getOrder request for orderNumber={}", request.getOrderNumber());

            log.debug("🔍 [ORDERS-SERVER] Querying OrdersApiService.findOrder()");
            ordersApiService
                    .findOrder(request.getOrderNumber())
                    .ifPresentOrElse(
                            order -> {
                                log.info(
                                        "✅ [ORDERS-SERVER] Order found: {}, status: {}",
                                        request.getOrderNumber(),
                                        order.status());
                                log.debug("📤 [ORDERS-SERVER] Sending getOrder response");
                                GetOrderResponse response = GetOrderResponse.newBuilder()
                                        .setOrder(mapper.toProto(order))
                                        .build();
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                            },
                            () -> {
                                log.warn("❌ [ORDERS-SERVER] Order not found: {}", request.getOrderNumber());
                                responseObserver.onError(exceptionMapper.map(
                                        OrderNotFoundException.forOrderNumber(request.getOrderNumber())));
                            });

            log.debug("🏁 [ORDERS-SERVER] getOrder completed");
        } catch (Exception ex) {
            log.error(
                    "🚨 [ORDERS-SERVER] getOrder failed for orderNumber={}: {}",
                    request.getOrderNumber(),
                    ex.getMessage(),
                    ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    @Override
    public void listOrders(ListOrdersRequest request, StreamObserver<ListOrdersResponse> responseObserver) {
        try {
            log.info("📨 [ORDERS-SERVER] Received listOrders request");

            log.debug("📋 [ORDERS-SERVER] Querying OrdersApiService.findOrders()");
            List<com.sivalabs.bookstore.orders.api.OrderView> orderViews = ordersApiService.findOrders();

            log.debug("🔄 [ORDERS-SERVER] Converting {} orders to gRPC format", orderViews.size());
            List<OrderView> protoViews = mapper.toProtoOrderViews(orderViews);
            ListOrdersResponse response =
                    ListOrdersResponse.newBuilder().addAllOrders(protoViews).build();

            log.info("✅ [ORDERS-SERVER] Found {} orders", orderViews.size());
            log.debug("📤 [ORDERS-SERVER] Sending listOrders response");

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.debug("🏁 [ORDERS-SERVER] listOrders completed successfully");
        } catch (Exception ex) {
            log.error("🚨 [ORDERS-SERVER] listOrders failed: {}", ex.getMessage(), ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }
}

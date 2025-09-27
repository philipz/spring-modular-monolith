package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.OrdersApiService;
import com.sivalabs.grpc.orders.CreateOrderRequest;
import com.sivalabs.grpc.orders.CreateOrderResponse;
import com.sivalabs.grpc.orders.FindOrderRequest;
import com.sivalabs.grpc.orders.FindOrdersRequest;
import com.sivalabs.grpc.orders.FindOrdersResponse;
import com.sivalabs.grpc.orders.OrderDto;
import com.sivalabs.grpc.orders.OrderView;
import com.sivalabs.grpc.orders.OrdersServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@GrpcService
@ConditionalOnClass(OrdersServiceGrpc.class)
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
            var domainRequest = mapper.toDomain(request);
            var response = ordersApiService.createOrder(domainRequest);
            responseObserver.onNext(mapper.toProto(response));
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error(
                    "gRPC createOrder failed for customerEmail={} productCode={}",
                    request.getCustomer().getEmail(),
                    request.getItem().getCode(),
                    ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    @Override
    public void findOrder(FindOrderRequest request, StreamObserver<OrderDto> responseObserver) {
        try {
            ordersApiService
                    .findOrder(request.getOrderNumber())
                    .ifPresentOrElse(
                            order -> {
                                responseObserver.onNext(mapper.toProto(order));
                                responseObserver.onCompleted();
                            },
                            () -> responseObserver.onError(exceptionMapper.map(
                                    OrderNotFoundException.forOrderNumber(request.getOrderNumber()))));
        } catch (Exception ex) {
            log.error("gRPC findOrder failed for orderNumber={}", request.getOrderNumber(), ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }

    @Override
    public void findOrders(FindOrdersRequest request, StreamObserver<FindOrdersResponse> responseObserver) {
        try {
            List<com.sivalabs.bookstore.orders.api.OrderView> orderViews = ordersApiService.findOrders();
            List<OrderView> protoViews = mapper.toProtoOrderViews(orderViews);
            FindOrdersResponse response =
                    FindOrdersResponse.newBuilder().addAllOrders(protoViews).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("gRPC findOrders failed", ex);
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }
}

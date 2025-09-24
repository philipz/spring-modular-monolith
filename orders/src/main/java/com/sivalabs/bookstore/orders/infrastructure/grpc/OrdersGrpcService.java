package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.OrdersApiService;
import com.sivalabs.bookstore.orders.grpc.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.CreateOrderResponse;
import com.sivalabs.bookstore.orders.grpc.FindOrderRequest;
import com.sivalabs.bookstore.orders.grpc.FindOrdersRequest;
import com.sivalabs.bookstore.orders.grpc.FindOrdersResponse;
import com.sivalabs.bookstore.orders.grpc.OrderDto;
import com.sivalabs.bookstore.orders.grpc.OrderView;
import com.sivalabs.bookstore.orders.grpc.OrdersServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OrdersGrpcService extends OrdersServiceGrpc.OrdersServiceImplBase {

    private final OrdersApiService ordersApiService;
    private final GrpcOrderMapper mapper;
    private final GrpcExceptionMapper exceptionMapper;

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
            responseObserver.onError(exceptionMapper.map(ex));
        }
    }
}

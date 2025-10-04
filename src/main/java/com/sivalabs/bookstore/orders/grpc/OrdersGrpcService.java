package com.sivalabs.bookstore.orders.grpc;

import com.sivalabs.bookstore.orders.api.OrdersApi;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Collections;
import org.springframework.stereotype.Component;

/**
 * gRPC endpoint implementation for the Orders module.
 *
 * RPC methods will be implemented in subsequent tasks using the injected
 * OrdersApi and GrpcMessageMapper collaborators.
 */
@Component
public class OrdersGrpcService extends OrdersServiceGrpc.OrdersServiceImplBase {

    private final OrdersApi ordersApi;
    private final GrpcMessageMapper messageMapper;
    private final Validator validator;

    public OrdersGrpcService(OrdersApi ordersApi, GrpcMessageMapper messageMapper, Validator validator) {
        this.ordersApi = ordersApi;
        this.messageMapper = messageMapper;
        this.validator = validator;
    }

    @Override
    public void createOrder(
            com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest request,
            io.grpc.stub.StreamObserver<com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse>
                    responseObserver) {
        try {
            var createOrderRequest = messageMapper.toCreateOrderRequest(request);
            validateCreateOrderRequest(createOrderRequest);
            var response = ordersApi.createOrder(createOrderRequest);
            var grpcResponse = messageMapper.toCreateOrderResponse(response);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(GrpcExceptionHandler.handleException(ex));
        }
    }

    @Override
    public void getOrder(
            com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest request,
            io.grpc.stub.StreamObserver<com.sivalabs.bookstore.orders.grpc.proto.GetOrderResponse> responseObserver) {
        try {
            var orderOptional = ordersApi.findOrder(request.getOrderNumber());
            if (orderOptional.isEmpty()) {
                responseObserver.onError(GrpcExceptionHandler.handleException(
                        new com.sivalabs.bookstore.orders.OrderNotFoundException(request.getOrderNumber())));
                return;
            }
            var order = orderOptional.get();
            var grpcOrder = messageMapper.toOrderDto(order);
            var grpcResponse = com.sivalabs.bookstore.orders.grpc.proto.GetOrderResponse.newBuilder()
                    .setOrder(grpcOrder)
                    .build();
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(GrpcExceptionHandler.handleException(ex));
        }
    }

    @Override
    public void listOrders(
            com.sivalabs.bookstore.orders.grpc.proto.ListOrdersRequest request,
            io.grpc.stub.StreamObserver<com.sivalabs.bookstore.orders.grpc.proto.ListOrdersResponse> responseObserver) {
        try {
            var orders = ordersApi.findOrders();
            var grpcOrders = orders.stream().map(messageMapper::toOrderView).toList();

            var grpcResponse = com.sivalabs.bookstore.orders.grpc.proto.ListOrdersResponse.newBuilder()
                    .addAllOrders(grpcOrders)
                    .build();
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(GrpcExceptionHandler.handleException(ex));
        }
    }

    private void validateCreateOrderRequest(com.sivalabs.bookstore.orders.api.CreateOrderRequest request) {
        if (request == null) {
            throw new ConstraintViolationException("CreateOrderRequest must not be null", Collections.emptySet());
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}

package com.sivalabs.bookstore.orders.grpc;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around the generated OrdersService blocking stub. Provides mapping between domain
 * DTOs and protobuf messages.
 */
@Component
public class OrdersGrpcClient {

    private final ManagedChannel channel;
    private final GrpcMessageMapper messageMapper;
    private final long deadlineMs;
    private OrdersServiceGrpc.OrdersServiceBlockingStub blockingStub;

    public OrdersGrpcClient(
            ManagedChannel channel,
            @Value("${bookstore.grpc.client.deadline-ms}") long deadlineMs,
            GrpcMessageMapper messageMapper) {
        this.channel = channel;
        this.messageMapper = messageMapper;
        this.deadlineMs = deadlineMs;
    }

    @PostConstruct
    void init() {
        this.blockingStub = OrdersServiceGrpc.newBlockingStub(channel);
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        var grpcRequest = messageMapper.toCreateOrderRequestProto(request);
        try {
            var grpcResponse = stubWithDeadline().createOrder(grpcRequest);
            return messageMapper.toCreateOrderResponseDto(grpcResponse);
        } catch (StatusRuntimeException ex) {
            throw mapStatusRuntimeException(ex);
        }
    }

    public OrderDto getOrder(String orderNumber) {
        var grpcRequest = com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest.newBuilder()
                .setOrderNumber(orderNumber)
                .build();
        try {
            var grpcResponse = stubWithDeadline().getOrder(grpcRequest);
            return messageMapper.toOrderDtoDto(grpcResponse.getOrder());
        } catch (StatusRuntimeException ex) {
            throw mapStatusRuntimeException(ex);
        }
    }

    public List<OrderView> listOrders() {
        var grpcRequest = com.sivalabs.bookstore.orders.grpc.proto.ListOrdersRequest.getDefaultInstance();
        try {
            var grpcResponse = stubWithDeadline().listOrders(grpcRequest);
            return grpcResponse.getOrdersList().stream()
                    .map(messageMapper::toOrderViewDto)
                    .collect(Collectors.toList());
        } catch (StatusRuntimeException ex) {
            throw mapStatusRuntimeException(ex);
        }
    }

    private OrdersServiceGrpc.OrdersServiceBlockingStub stubWithDeadline() {
        return blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    private RuntimeException mapStatusRuntimeException(StatusRuntimeException ex) {
        var status = ex.getStatus();
        String description = status.getDescription() != null
                ? status.getDescription()
                : status.getCode().name();
        return switch (status.getCode()) {
            case NOT_FOUND -> new OrderNotFoundException(description);
            case INVALID_ARGUMENT -> new InvalidOrderException(description);
            default -> ex;
        };
    }
}

package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.google.protobuf.Timestamp;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GrpcOrderMapper {

    public CreateOrderRequest toDomain(com.sivalabs.bookstore.orders.grpc.CreateOrderRequest request) {
        Customer customer = new Customer(
                request.getCustomer().getName(),
                request.getCustomer().getEmail(),
                request.getCustomer().getPhone());

        OrderItem item = new OrderItem(
                request.getItem().getCode(),
                request.getItem().getName(),
                BigDecimal.valueOf(request.getItem().getPrice()),
                request.getItem().getQuantity());

        return new CreateOrderRequest(customer, request.getDeliveryAddress(), item);
    }

    public com.sivalabs.bookstore.orders.grpc.CreateOrderResponse toProto(CreateOrderResponse response) {
        return com.sivalabs.bookstore.orders.grpc.CreateOrderResponse.newBuilder()
                .setOrderNumber(response.orderNumber())
                .build();
    }

    public com.sivalabs.bookstore.orders.grpc.OrderDto toProto(OrderDto orderDto) {
        var builder = com.sivalabs.bookstore.orders.grpc.OrderDto.newBuilder()
                .setOrderNumber(orderDto.orderNumber())
                .setCustomer(toProto(orderDto.customer()))
                .setItem(toProto(orderDto.item()))
                .setDeliveryAddress(orderDto.deliveryAddress())
                .setStatus(toProto(orderDto.status()));

        if (orderDto.createdAt() != null) {
            builder.setCreatedAt(toTimestamp(orderDto.createdAt()));
        }

        return builder.build();
    }

    public com.sivalabs.bookstore.orders.grpc.OrderView toProto(OrderView orderView) {
        return com.sivalabs.bookstore.orders.grpc.OrderView.newBuilder()
                .setOrderNumber(orderView.orderNumber())
                .setStatus(toProto(orderView.status()))
                .setCustomer(toProto(orderView.customer()))
                .build();
    }

    public List<com.sivalabs.bookstore.orders.grpc.OrderView> toProtoOrderViews(List<OrderView> orderViews) {
        return orderViews.stream().map(this::toProto).toList();
    }

    public List<com.sivalabs.bookstore.orders.grpc.OrderDto> toProtoOrderDtos(List<OrderDto> orderDtos) {
        return orderDtos.stream().map(this::toProto).toList();
    }

    public OrderDto toDomain(com.sivalabs.bookstore.orders.grpc.OrderDto orderDto) {
        OrderItem item = new OrderItem(
                orderDto.getItem().getCode(),
                orderDto.getItem().getName(),
                BigDecimal.valueOf(orderDto.getItem().getPrice()),
                orderDto.getItem().getQuantity());

        Customer customer = new Customer(
                orderDto.getCustomer().getName(),
                orderDto.getCustomer().getEmail(),
                orderDto.getCustomer().getPhone());

        LocalDateTime createdAt = orderDto.hasCreatedAt() ? toLocalDateTime(orderDto.getCreatedAt()) : null;

        return new OrderDto(
                orderDto.getOrderNumber(),
                item,
                customer,
                orderDto.getDeliveryAddress(),
                toDomain(orderDto.getStatus()),
                createdAt);
    }

    public OrderView toDomain(com.sivalabs.bookstore.orders.grpc.OrderView orderView) {
        Customer customer = new Customer(
                orderView.getCustomer().getName(),
                orderView.getCustomer().getEmail(),
                orderView.getCustomer().getPhone());

        return new OrderView(orderView.getOrderNumber(), toDomain(orderView.getStatus()), customer);
    }

    private com.sivalabs.bookstore.orders.grpc.Customer toProto(Customer customer) {
        return com.sivalabs.bookstore.orders.grpc.Customer.newBuilder()
                .setName(customer.name())
                .setEmail(customer.email())
                .setPhone(customer.phone())
                .build();
    }

    private com.sivalabs.bookstore.orders.grpc.OrderItem toProto(OrderItem item) {
        return com.sivalabs.bookstore.orders.grpc.OrderItem.newBuilder()
                .setCode(item.code())
                .setName(item.name())
                .setPrice(item.price().doubleValue())
                .setQuantity(item.quantity())
                .build();
    }

    private com.sivalabs.bookstore.orders.grpc.OrderStatus toProto(OrderStatus status) {
        return switch (status) {
            case NEW -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_NEW;
            case PENDING -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_PENDING;
            case CONFIRMED -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_CONFIRMED;
            case IN_PROCESS -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_IN_PROCESS;
            case SHIPPED -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_SHIPPED;
            case DELIVERED -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_DELIVERED;
            case CANCELLED -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_CANCELLED;
            case ERROR -> com.sivalabs.bookstore.orders.grpc.OrderStatus.ORDER_STATUS_ERROR;
        };
    }

    private OrderStatus toDomain(com.sivalabs.bookstore.orders.grpc.OrderStatus status) {
        return switch (status) {
            case ORDER_STATUS_PENDING -> OrderStatus.PENDING;
            case ORDER_STATUS_CONFIRMED -> OrderStatus.CONFIRMED;
            case ORDER_STATUS_IN_PROCESS -> OrderStatus.IN_PROCESS;
            case ORDER_STATUS_SHIPPED -> OrderStatus.SHIPPED;
            case ORDER_STATUS_DELIVERED -> OrderStatus.DELIVERED;
            case ORDER_STATUS_CANCELLED -> OrderStatus.CANCELLED;
            case ORDER_STATUS_ERROR -> OrderStatus.ERROR;
            case ORDER_STATUS_UNSPECIFIED, UNRECOGNIZED, ORDER_STATUS_NEW -> OrderStatus.NEW;
        };
    }

    private Timestamp toTimestamp(LocalDateTime createdAt) {
        Instant instant = createdAt.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()), ZoneOffset.UTC);
    }
}

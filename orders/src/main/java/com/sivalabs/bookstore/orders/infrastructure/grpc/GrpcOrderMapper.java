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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(name = "com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest")
@ConditionalOnProperty(name = "grpc.client.orders.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcOrderMapper {

    public CreateOrderRequest toDomain(com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest request) {
        Customer customer = new Customer(
                request.getCustomer().getName(),
                request.getCustomer().getEmail(),
                request.getCustomer().getPhone());

        OrderItem item = new OrderItem(
                request.getItem().getCode(),
                request.getItem().getName(),
                new BigDecimal(request.getItem().getPrice()), // Convert from string
                request.getItem().getQuantity());

        return new CreateOrderRequest(customer, request.getDeliveryAddress(), item);
    }

    public com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest toProto(CreateOrderRequest request) {
        return com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest.newBuilder()
                .setCustomer(toProto(request.customer()))
                .setItem(toProto(request.item()))
                .setDeliveryAddress(request.deliveryAddress())
                .build();
    }

    public com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse toProto(CreateOrderResponse response) {
        return com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse.newBuilder()
                .setOrderNumber(response.orderNumber())
                .build();
    }

    public CreateOrderResponse toDomain(com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse response) {
        return new CreateOrderResponse(response.getOrderNumber());
    }

    public com.sivalabs.bookstore.orders.grpc.proto.OrderDto toProto(OrderDto orderDto) {
        var builder = com.sivalabs.bookstore.orders.grpc.proto.OrderDto.newBuilder()
                .setOrderNumber(orderDto.orderNumber())
                .setCustomer(toProto(orderDto.customer()))
                .setItem(toProto(orderDto.item()))
                .setDeliveryAddress(orderDto.deliveryAddress())
                .setStatus(toProto(orderDto.status()))
                .setTotalAmount(orderDto.getTotalAmount().toString()); // Convert BigDecimal to string

        if (orderDto.createdAt() != null) {
            builder.setCreatedAt(toTimestamp(orderDto.createdAt()));
        }

        return builder.build();
    }

    public com.sivalabs.bookstore.orders.grpc.proto.OrderView toProto(OrderView orderView) {
        var builder = com.sivalabs.bookstore.orders.grpc.proto.OrderView.newBuilder()
                .setOrderNumber(orderView.orderNumber())
                .setStatus(toProto(orderView.status()));

        if (orderView.customer() != null) {
            builder.setCustomer(toProto(orderView.customer()));
        }

        return builder.build();
    }

    public List<com.sivalabs.bookstore.orders.grpc.proto.OrderView> toProtoOrderViews(List<OrderView> orderViews) {
        return orderViews.stream().map(this::toProto).toList();
    }

    public List<com.sivalabs.bookstore.orders.grpc.proto.OrderDto> toProtoOrderDtos(List<OrderDto> orderDtos) {
        return orderDtos.stream().map(this::toProto).toList();
    }

    public OrderDto toDomain(com.sivalabs.bookstore.orders.grpc.proto.OrderDto orderDto) {
        OrderItem item = new OrderItem(
                orderDto.getItem().getCode(),
                orderDto.getItem().getName(),
                new BigDecimal(orderDto.getItem().getPrice()), // Convert from string
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

    public OrderView toDomain(com.sivalabs.bookstore.orders.grpc.proto.OrderView orderView) {
        Customer customer = orderView.hasCustomer() ? toDomain(orderView.getCustomer()) : null;
        return new OrderView(orderView.getOrderNumber(), toDomain(orderView.getStatus()), customer);
    }

    private com.sivalabs.bookstore.orders.grpc.proto.Customer toProto(Customer customer) {
        return com.sivalabs.bookstore.orders.grpc.proto.Customer.newBuilder()
                .setName(customer.name())
                .setEmail(customer.email())
                .setPhone(customer.phone())
                .build();
    }

    private Customer toDomain(com.sivalabs.bookstore.orders.grpc.proto.Customer customer) {
        return new Customer(customer.getName(), customer.getEmail(), customer.getPhone());
    }

    private com.sivalabs.bookstore.orders.grpc.proto.OrderItem toProto(OrderItem item) {
        return com.sivalabs.bookstore.orders.grpc.proto.OrderItem.newBuilder()
                .setCode(item.code())
                .setName(item.name())
                .setPrice(item.price().toString()) // Convert BigDecimal to string
                .setQuantity(item.quantity())
                .build();
    }

    private com.sivalabs.bookstore.orders.grpc.proto.OrderStatus toProto(OrderStatus status) {
        return switch (status) {
            case NEW -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.NEW;
            case DELIVERED -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.DELIVERED;
            case CANCELLED -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.CANCELLED;
            case ERROR -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.ERROR;
            // Map other statuses to NEW as default (since new proto has fewer statuses)
            default -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.NEW;
        };
    }

    private OrderStatus toDomain(com.sivalabs.bookstore.orders.grpc.proto.OrderStatus status) {
        return switch (status) {
            case NEW -> OrderStatus.NEW;
            case DELIVERED -> OrderStatus.DELIVERED;
            case CANCELLED -> OrderStatus.CANCELLED;
            case ERROR -> OrderStatus.ERROR;
            case UNSPECIFIED, UNRECOGNIZED -> OrderStatus.NEW;
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

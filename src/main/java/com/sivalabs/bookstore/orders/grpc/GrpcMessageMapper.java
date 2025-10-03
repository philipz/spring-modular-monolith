package com.sivalabs.bookstore.orders.grpc;

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
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Mapping logic between gRPC generated messages and domain DTOs.
 */
@Component
public class GrpcMessageMapper {

    public CreateOrderRequest toCreateOrderRequest(
            com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest request) {
        Customer customer = request.hasCustomer() ? toCustomer(request.getCustomer()) : null;
        OrderItem item = request.hasItem() ? toOrderItem(request.getItem()) : null;
        return new CreateOrderRequest(customer, request.getDeliveryAddress(), item);
    }

    public com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest toCreateOrderRequestProto(
            CreateOrderRequest request) {
        var builder = com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest.newBuilder()
                .setDeliveryAddress(nullToEmpty(request.deliveryAddress()));

        if (request.customer() != null) {
            builder.setCustomer(toCustomerProto(request.customer()));
        }
        if (request.item() != null) {
            builder.setItem(toOrderItemProto(request.item()));
        }

        return builder.build();
    }

    public com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse toCreateOrderResponse(
            CreateOrderResponse response) {
        return com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse.newBuilder()
                .setOrderNumber(response.orderNumber())
                .build();
    }

    public CreateOrderResponse toCreateOrderResponseDto(
            com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse response) {
        return new CreateOrderResponse(response.getOrderNumber());
    }

    public com.sivalabs.bookstore.orders.grpc.proto.OrderDto toOrderDto(OrderDto dto) {
        var builder = com.sivalabs.bookstore.orders.grpc.proto.OrderDto.newBuilder()
                .setOrderNumber(dto.orderNumber())
                .setDeliveryAddress(nullToEmpty(dto.deliveryAddress()))
                .setStatus(toOrderStatusProto(dto.status()));

        if (dto.customer() != null) {
            builder.setCustomer(toCustomerProto(dto.customer()));
        }

        if (dto.item() != null) {
            builder.setItem(toOrderItemProto(dto.item()))
                    .setTotalAmount(dto.getTotalAmount().toPlainString());
        } else {
            builder.setTotalAmount(BigDecimal.ZERO.toPlainString());
        }

        if (dto.createdAt() != null) {
            builder.setCreatedAt(toTimestamp(dto.createdAt()));
        }

        return builder.build();
    }

    public OrderDto toOrderDtoDto(com.sivalabs.bookstore.orders.grpc.proto.OrderDto proto) {
        OrderItem item = proto.hasItem() ? toOrderItem(proto.getItem()) : null;
        Customer customer = proto.hasCustomer() ? toCustomer(proto.getCustomer()) : null;
        LocalDateTime createdAt = proto.hasCreatedAt() ? fromTimestamp(proto.getCreatedAt()) : null;

        return new OrderDto(
                proto.getOrderNumber(),
                item,
                customer,
                proto.getDeliveryAddress(),
                toOrderStatus(proto.getStatus()),
                createdAt);
    }

    public com.sivalabs.bookstore.orders.grpc.proto.OrderView toOrderView(OrderView view) {
        return com.sivalabs.bookstore.orders.grpc.proto.OrderView.newBuilder()
                .setOrderNumber(view.orderNumber())
                .setStatus(toOrderStatusProto(view.status()))
                .build();
    }

    public OrderView toOrderViewDto(com.sivalabs.bookstore.orders.grpc.proto.OrderView proto) {
        return new OrderView(proto.getOrderNumber(), toOrderStatus(proto.getStatus()), null);
    }

    private Customer toCustomer(com.sivalabs.bookstore.orders.grpc.proto.Customer proto) {
        if (proto == null) {
            return null;
        }
        return new Customer(proto.getName(), proto.getEmail(), proto.getPhone());
    }

    private OrderItem toOrderItem(com.sivalabs.bookstore.orders.grpc.proto.OrderItem proto) {
        if (proto == null) {
            return null;
        }
        BigDecimal price = parseBigDecimal(proto.getPrice());
        return new OrderItem(proto.getCode(), proto.getName(), price, proto.getQuantity());
    }

    private com.sivalabs.bookstore.orders.grpc.proto.Customer toCustomerProto(Customer customer) {
        return com.sivalabs.bookstore.orders.grpc.proto.Customer.newBuilder()
                .setName(customer.name())
                .setEmail(customer.email())
                .setPhone(customer.phone())
                .build();
    }

    private com.sivalabs.bookstore.orders.grpc.proto.OrderItem toOrderItemProto(OrderItem item) {
        return com.sivalabs.bookstore.orders.grpc.proto.OrderItem.newBuilder()
                .setCode(item.code())
                .setName(item.name())
                .setPrice(item.price().toPlainString())
                .setQuantity(item.quantity())
                .build();
    }

    private com.sivalabs.bookstore.orders.grpc.proto.OrderStatus toOrderStatusProto(OrderStatus status) {
        if (status == null) {
            return com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.UNSPECIFIED;
        }
        return switch (status) {
            case NEW, IN_PROCESS -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.NEW;
            case DELIVERED -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.DELIVERED;
            case CANCELLED -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.CANCELLED;
            case ERROR -> com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.ERROR;
        };
    }

    private OrderStatus toOrderStatus(com.sivalabs.bookstore.orders.grpc.proto.OrderStatus status) {
        return switch (status) {
            case DELIVERED -> OrderStatus.DELIVERED;
            case CANCELLED -> OrderStatus.CANCELLED;
            case ERROR -> OrderStatus.ERROR;
            case NEW, UNSPECIFIED, UNRECOGNIZED -> OrderStatus.NEW;
        };
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        var zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
        return Timestamp.newBuilder()
                .setSeconds(zonedDateTime.toEpochSecond())
                .setNanos(zonedDateTime.getNano())
                .build();
    }

    private LocalDateTime fromTimestamp(Timestamp timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()), ZoneId.systemDefault());
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

package com.sivalabs.bookstore.orders.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Timestamp;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcMessageMapperTest {

    private GrpcMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GrpcMessageMapper();
    }

    @Test
    void shouldMapCreateOrderRequestProtoToDto() {
        var protoRequest = com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest.newBuilder()
                .setDeliveryAddress("221B Baker Street")
                .setCustomer(com.sivalabs.bookstore.orders.grpc.proto.Customer.newBuilder()
                        .setName("Sherlock Holmes")
                        .setEmail("sherlock@bakerstreet.com")
                        .setPhone("1234567890"))
                .setItem(com.sivalabs.bookstore.orders.grpc.proto.OrderItem.newBuilder()
                        .setCode("BOOK-123")
                        .setName("The Adventures")
                        .setPrice("29.99")
                        .setQuantity(2))
                .build();

        CreateOrderRequest request = mapper.toCreateOrderRequest(protoRequest);

        assertThat(request.deliveryAddress()).isEqualTo("221B Baker Street");
        assertThat(request.customer())
                .extracting(Customer::name, Customer::email, Customer::phone)
                .containsExactly("Sherlock Holmes", "sherlock@bakerstreet.com", "1234567890");
        assertThat(request.item())
                .extracting(OrderItem::code, OrderItem::name)
                .containsExactly("BOOK-123", "The Adventures");
        assertThat(request.item().price()).isEqualTo(new BigDecimal("29.99"));
        assertThat(request.item().quantity()).isEqualTo(2);
    }

    @Test
    void shouldHandleMissingOptionalFields() {
        var protoRequest = com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest.newBuilder()
                .setDeliveryAddress("")
                .build();

        CreateOrderRequest request = mapper.toCreateOrderRequest(protoRequest);

        assertThat(request.deliveryAddress()).isEmpty();
        assertThat(request.customer()).isNull();
        assertThat(request.item()).isNull();
    }

    @Test
    void shouldMapDtoToCreateOrderRequestProto() {
        var request = new CreateOrderRequest(
                new Customer("Sherlock Holmes", "sherlock@bakerstreet.com", "1234567890"),
                "221B Baker Street",
                new OrderItem("BOOK-123", "The Adventures", new BigDecimal("29.99"), 2));

        com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest proto = mapper.toCreateOrderRequestProto(request);

        assertThat(proto.getDeliveryAddress()).isEqualTo("221B Baker Street");
        assertThat(proto.getCustomer())
                .extracting(
                        com.sivalabs.bookstore.orders.grpc.proto.Customer::getName,
                        com.sivalabs.bookstore.orders.grpc.proto.Customer::getEmail,
                        com.sivalabs.bookstore.orders.grpc.proto.Customer::getPhone)
                .containsExactly("Sherlock Holmes", "sherlock@bakerstreet.com", "1234567890");
        assertThat(proto.getItem())
                .extracting(
                        com.sivalabs.bookstore.orders.grpc.proto.OrderItem::getCode,
                        com.sivalabs.bookstore.orders.grpc.proto.OrderItem::getName,
                        com.sivalabs.bookstore.orders.grpc.proto.OrderItem::getPrice,
                        com.sivalabs.bookstore.orders.grpc.proto.OrderItem::getQuantity)
                .containsExactly("BOOK-123", "The Adventures", "29.99", 2);
    }

    @Test
    void shouldMapOrderDtoToProtoAndBack() {
        LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        var dto = new OrderDto(
                "ORDER-1",
                new OrderItem("BOOK-123", "The Adventures", new BigDecimal("19.99"), 3),
                new Customer("Sherlock Holmes", "sherlock@bakerstreet.com", "1234567890"),
                "221B Baker Street",
                OrderStatus.DELIVERED,
                createdAt);

        com.sivalabs.bookstore.orders.grpc.proto.OrderDto proto = mapper.toOrderDto(dto);
        assertThat(proto.getOrderNumber()).isEqualTo("ORDER-1");
        assertThat(proto.getDeliveryAddress()).isEqualTo("221B Baker Street");
        assertThat(proto.getStatus()).isEqualTo(com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.DELIVERED);
        assertThat(proto.getTotalAmount()).isEqualTo("59.97");
        assertThat(proto.getCreatedAt())
                .extracting(Timestamp::getSeconds, Timestamp::getNanos)
                .containsExactly(createdAt.atZone(ZoneId.systemDefault()).toEpochSecond(), createdAt.getNano());

        OrderDto mappedBack = mapper.toOrderDtoDto(proto);
        assertThat(mappedBack.orderNumber()).isEqualTo("ORDER-1");
        assertThat(mappedBack.item().price()).isEqualTo(new BigDecimal("19.99"));
        assertThat(mappedBack.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(mappedBack.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldMapOrderViewProtoToDto() {
        var customer = com.sivalabs.bookstore.orders.grpc.proto.Customer.newBuilder()
                .setName("OrderView Tester")
                .setEmail("orderview@test.com")
                .setPhone("+1-555-VIEW")
                .build();

        var proto = com.sivalabs.bookstore.orders.grpc.proto.OrderView.newBuilder()
                .setOrderNumber("ORDER-2")
                .setStatus(com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.NEW)
                .setCustomer(customer)
                .build();

        OrderView dto = mapper.toOrderViewDto(proto);
        assertThat(dto.orderNumber()).isEqualTo("ORDER-2");
        assertThat(dto.status()).isEqualTo(OrderStatus.NEW);
        assertThat(dto.customer()).isNotNull();
        assertThat(dto.customer().name()).isEqualTo("OrderView Tester");
    }

    @Test
    void createOrderResponseConversionShouldBeSymmetric() {
        var response = new CreateOrderResponse("ORDER-3");
        com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse proto = mapper.toCreateOrderResponse(response);
        assertThat(proto.getOrderNumber()).isEqualTo("ORDER-3");

        CreateOrderResponse roundtrip = mapper.toCreateOrderResponseDto(proto);
        assertThat(roundtrip.orderNumber()).isEqualTo("ORDER-3");
    }
}

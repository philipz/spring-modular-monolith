package com.sivalabs.bookstore.orders.infrastructure.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.google.protobuf.Timestamp;
import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.OrdersApiService;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse;
import com.sivalabs.bookstore.orders.grpc.proto.Customer;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderResponse;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersRequest;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersResponse;
import com.sivalabs.bookstore.orders.grpc.proto.OrderDto;
import com.sivalabs.bookstore.orders.grpc.proto.OrderItem;
import com.sivalabs.bookstore.orders.grpc.proto.OrderStatus;
import com.sivalabs.bookstore.orders.grpc.proto.OrderView;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersGrpcServiceTest {

    @Mock
    private OrdersApiService ordersApiService;

    @Mock
    private GrpcOrderMapper mapper;

    @Mock
    private GrpcExceptionMapper exceptionMapper;

    @Mock
    private StreamObserver<CreateOrderResponse> createOrderResponseObserver;

    @Mock
    private StreamObserver<GetOrderResponse> getOrderResponseObserver;

    @Mock
    private StreamObserver<ListOrdersResponse> listOrdersResponseObserver;

    private OrdersGrpcService ordersGrpcService;

    @BeforeEach
    void setUp() {
        ordersGrpcService = new OrdersGrpcService(ordersApiService, mapper, exceptionMapper);
    }

    // Test fixtures for gRPC CreateOrderRequest
    private CreateOrderRequest createGrpcOrderRequest() {
        Customer customer = Customer.newBuilder()
                .setName("Buyer")
                .setEmail("buyer@example.com")
                .setPhone("999-999-9999")
                .build();

        OrderItem item = OrderItem.newBuilder()
                .setCode("P100")
                .setName("Domain-Driven Design")
                .setPrice("49.99")
                .setQuantity(1)
                .build();

        return CreateOrderRequest.newBuilder()
                .setCustomer(customer)
                .setDeliveryAddress("221B Baker Street")
                .setItem(item)
                .build();
    }

    // Test fixtures for domain CreateOrderRequest
    private com.sivalabs.bookstore.orders.api.CreateOrderRequest createDomainOrderRequest() {
        var customer =
                new com.sivalabs.bookstore.orders.api.model.Customer("Buyer", "buyer@example.com", "999-999-9999");
        var item = new com.sivalabs.bookstore.orders.api.model.OrderItem(
                "P100", "Domain-Driven Design", new BigDecimal("49.99"), 1);
        return new com.sivalabs.bookstore.orders.api.CreateOrderRequest(customer, "221B Baker Street", item);
    }

    // Test fixtures for domain CreateOrderResponse
    private com.sivalabs.bookstore.orders.api.CreateOrderResponse createDomainOrderResponse() {
        return new com.sivalabs.bookstore.orders.api.CreateOrderResponse("BK-123456");
    }

    // Test fixtures for gRPC CreateOrderResponse
    private CreateOrderResponse createGrpcOrderResponse() {
        return CreateOrderResponse.newBuilder().setOrderNumber("BK-123456").build();
    }

    // Test fixtures for domain OrderDto
    private com.sivalabs.bookstore.orders.api.OrderDto createDomainOrderDto() {
        var item = new com.sivalabs.bookstore.orders.api.model.OrderItem(
                "P100", "Domain-Driven Design", new BigDecimal("49.99"), 1);
        var customer =
                new com.sivalabs.bookstore.orders.api.model.Customer("Buyer", "buyer@example.com", "999-999-9999");
        return new com.sivalabs.bookstore.orders.api.OrderDto(
                "BK-123456",
                item,
                customer,
                "221B Baker Street",
                com.sivalabs.bookstore.orders.api.model.OrderStatus.NEW,
                LocalDateTime.parse("2024-01-20T10:15:00"));
    }

    // Test fixtures for gRPC OrderDto
    private OrderDto createGrpcOrderDto() {
        OrderItem item = OrderItem.newBuilder()
                .setCode("P100")
                .setName("Domain-Driven Design")
                .setPrice("49.99")
                .setQuantity(1)
                .build();

        Customer customer = Customer.newBuilder()
                .setName("Buyer")
                .setEmail("buyer@example.com")
                .setPhone("999-999-9999")
                .build();

        return OrderDto.newBuilder()
                .setOrderNumber("BK-123456")
                .setItem(item)
                .setCustomer(customer)
                .setDeliveryAddress("221B Baker Street")
                .setStatus(OrderStatus.NEW)
                .setCreatedAt(Timestamp.newBuilder()
                        .setSeconds(1705742100) // 2024-01-20T10:15:00 UTC
                        .setNanos(0)
                        .build())
                .build();
    }

    // Test fixtures for domain OrderView list
    private com.sivalabs.bookstore.common.models.PagedResult<com.sivalabs.bookstore.orders.api.OrderView>
            createDomainOrderViews() {
        var customer =
                new com.sivalabs.bookstore.orders.api.model.Customer("Buyer", "buyer@example.com", "999-999-9999");
        var view = new com.sivalabs.bookstore.orders.api.OrderView(
                "BK-123456", com.sivalabs.bookstore.orders.api.model.OrderStatus.NEW, customer);
        return new com.sivalabs.bookstore.common.models.PagedResult<>(
                java.util.List.of(view), 1, 1, 1, true, true, false, false);
    }

    // Test fixtures for gRPC OrderView list
    private List<OrderView> createGrpcOrderViews() {
        OrderView orderView = OrderView.newBuilder()
                .setOrderNumber("BK-123456")
                .setStatus(OrderStatus.NEW)
                .build();

        return List.of(orderView);
    }

    // Test fixtures for gRPC GetOrderRequest
    private GetOrderRequest createGetOrderRequest() {
        return GetOrderRequest.newBuilder().setOrderNumber("BK-123456").build();
    }

    // Test fixtures for gRPC ListOrdersRequest
    private ListOrdersRequest createListOrdersRequest() {
        return ListOrdersRequest.newBuilder().build();
    }

    // Test fixtures for gRPC ListOrdersResponse
    private ListOrdersResponse createListOrdersResponse() {
        return ListOrdersResponse.newBuilder()
                .addAllOrders(createGrpcOrderViews())
                .setTotalElements(1)
                .setPageNumber(1)
                .setTotalPages(1)
                .setIsFirst(true)
                .setIsLast(true)
                .setHasNext(false)
                .setHasPrevious(false)
                .build();
    }

    @Test
    @DisplayName("Should create order successfully via gRPC")
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest grpcRequest = createGrpcOrderRequest();
        com.sivalabs.bookstore.orders.api.CreateOrderRequest domainRequest = createDomainOrderRequest();
        com.sivalabs.bookstore.orders.api.CreateOrderResponse domainResponse = createDomainOrderResponse();
        CreateOrderResponse grpcResponse = createGrpcOrderResponse();

        given(mapper.toDomain(grpcRequest)).willReturn(domainRequest);
        given(ordersApiService.createOrder(domainRequest)).willReturn(domainResponse);
        given(mapper.toProto(domainResponse)).willReturn(grpcResponse);

        // When
        ordersGrpcService.createOrder(grpcRequest, createOrderResponseObserver);

        // Then
        org.mockito.Mockito.verify(mapper).toDomain(grpcRequest);
        org.mockito.Mockito.verify(ordersApiService).createOrder(domainRequest);
        org.mockito.Mockito.verify(mapper).toProto(domainResponse);
        org.mockito.Mockito.verify(createOrderResponseObserver).onNext(grpcResponse);
        org.mockito.Mockito.verify(createOrderResponseObserver).onCompleted();
        org.mockito.Mockito.verifyNoInteractions(exceptionMapper);
    }

    @Test
    @DisplayName("Should handle InvalidOrderException and convert to Status.INVALID_ARGUMENT")
    void shouldHandleInvalidOrderExceptionCorrectly() {
        // Given
        CreateOrderRequest grpcRequest = createGrpcOrderRequest();
        com.sivalabs.bookstore.orders.api.CreateOrderRequest domainRequest = createDomainOrderRequest();
        InvalidOrderException exception = new InvalidOrderException("Invalid product quantity");
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription("Invalid product quantity")
                .asRuntimeException();

        given(mapper.toDomain(grpcRequest)).willReturn(domainRequest);
        given(ordersApiService.createOrder(domainRequest)).willThrow(exception);
        given(exceptionMapper.map(exception)).willReturn(grpcException);

        // When
        ordersGrpcService.createOrder(grpcRequest, createOrderResponseObserver);

        // Then
        org.mockito.Mockito.verify(mapper).toDomain(grpcRequest);
        org.mockito.Mockito.verify(ordersApiService).createOrder(domainRequest);
        org.mockito.Mockito.verify(exceptionMapper).map(exception);
        org.mockito.Mockito.verify(createOrderResponseObserver).onError(grpcException);
        org.mockito.Mockito.verify(createOrderResponseObserver, org.mockito.Mockito.never())
                .onNext(any());
        org.mockito.Mockito.verify(createOrderResponseObserver, org.mockito.Mockito.never())
                .onCompleted();
    }

    @Test
    @DisplayName("Should get order successfully via gRPC")
    void shouldGetOrderSuccessfully() {
        // Given
        GetOrderRequest grpcRequest = createGetOrderRequest();
        String orderNumber = "BK-123456";
        com.sivalabs.bookstore.orders.api.OrderDto domainOrderDto = createDomainOrderDto();
        OrderDto grpcOrderDto = createGrpcOrderDto();
        GetOrderResponse grpcResponse =
                GetOrderResponse.newBuilder().setOrder(grpcOrderDto).build();

        given(ordersApiService.findOrder(orderNumber)).willReturn(Optional.of(domainOrderDto));
        given(mapper.toProto(domainOrderDto)).willReturn(grpcOrderDto);

        // When
        ordersGrpcService.getOrder(grpcRequest, getOrderResponseObserver);

        // Then
        org.mockito.Mockito.verify(ordersApiService).findOrder(orderNumber);
        org.mockito.Mockito.verify(mapper).toProto(domainOrderDto);
        org.mockito.Mockito.verify(getOrderResponseObserver).onNext(grpcResponse);
        org.mockito.Mockito.verify(getOrderResponseObserver).onCompleted();
        org.mockito.Mockito.verifyNoInteractions(exceptionMapper);
    }

    @Test
    @DisplayName("Should handle OrderNotFoundException and convert to Status.NOT_FOUND")
    void shouldHandleOrderNotFoundExceptionCorrectly() {
        // Given
        GetOrderRequest grpcRequest = createGetOrderRequest();
        String orderNumber = "BK-123456";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription("Order not found with orderNumber: " + orderNumber)
                .asRuntimeException();

        given(ordersApiService.findOrder(orderNumber)).willReturn(Optional.empty());
        given(exceptionMapper.map(org.mockito.ArgumentMatchers.any(OrderNotFoundException.class)))
                .willReturn(grpcException);

        // When
        ordersGrpcService.getOrder(grpcRequest, getOrderResponseObserver);

        // Then
        org.mockito.Mockito.verify(ordersApiService).findOrder(orderNumber);
        org.mockito.Mockito.verify(exceptionMapper).map(org.mockito.ArgumentMatchers.any(OrderNotFoundException.class));
        org.mockito.Mockito.verify(getOrderResponseObserver).onError(grpcException);
        org.mockito.Mockito.verify(getOrderResponseObserver, org.mockito.Mockito.never())
                .onNext(any());
        org.mockito.Mockito.verify(getOrderResponseObserver, org.mockito.Mockito.never())
                .onCompleted();
    }
}

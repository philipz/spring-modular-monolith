package com.sivalabs.bookstore.orders.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import com.sivalabs.bookstore.catalog.api.ProductApi;
import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.Customer;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersRequest;
import com.sivalabs.bookstore.orders.grpc.proto.OrderItem;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = NONE,
        properties = {
            "bookstore.cache.enabled=false",
            "bookstore.session.hazelcast.enabled=false",
            "bookstore.grpc.server.enabled=false"
        },
        classes = {
            com.sivalabs.bookstore.BookStoreApplication.class,
            com.sivalabs.bookstore.testsupport.session.TestSessionConfiguration.class
        })
@Sql("/test-products-data.sql")
class OrdersGrpcServiceIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("bookstore")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @MockBean
    private ProductApi productApi;

    @Autowired
    private OrdersGrpcService ordersGrpcService;

    @Autowired
    private OrderRepository orderRepository;

    private Server grpcServer;
    private ManagedChannel channel;
    private OrdersServiceGrpc.OrdersServiceBlockingStub blockingStub;

    @BeforeEach
    void setUpServer() throws IOException {
        String inProcessServerName = "orders-grpc-test-server-" + UUID.randomUUID();
        grpcServer = InProcessServerBuilder.forName(inProcessServerName)
                .directExecutor()
                .addService(ordersGrpcService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(inProcessServerName)
                .directExecutor()
                .build();
        blockingStub = OrdersServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDownServer() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (grpcServer != null) {
            grpcServer.shutdownNow();
        }
    }

    OrdersServiceGrpc.OrdersServiceBlockingStub blockingStub() {
        return blockingStub;
    }

    @Test
    void createOrderCreatesPersistentOrderWithGeneratedOrderNumber() {
        orderRepository.deleteAll();

        // Mock ProductApi to return a valid product
        when(productApi.getByCode(anyString()))
                .thenReturn(Optional.of(new ProductDto(
                        "P100", "The Hunger Games", "Book description", "image.jpg", new BigDecimal("34.0"))));

        OrderItem orderItem = OrderItem.newBuilder()
                .setCode("P100")
                .setName("The Hunger Games")
                .setPrice("34.0")
                .setQuantity(2)
                .build();

        Customer customer = Customer.newBuilder()
                .setName("Integration Test Customer")
                .setEmail("integration@test.com")
                .setPhone("+1234567890")
                .build();

        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setCustomer(customer)
                .setDeliveryAddress("221B Baker Street")
                .setItem(orderItem)
                .build();

        var response = blockingStub.createOrder(request);

        assertThat(response.getOrderNumber()).isNotBlank();

        var persistedOrder = orderRepository.findByOrderNumber(response.getOrderNumber());

        assertThat(persistedOrder).isPresent();

        var order = persistedOrder.orElseThrow();
        assertThat(order.getCustomer().name()).isEqualTo(customer.getName());
        assertThat(order.getCustomer().email()).isEqualTo(customer.getEmail());
        assertThat(order.getCustomer().phone()).isEqualTo(customer.getPhone());
        assertThat(order.getDeliveryAddress()).isEqualTo("221B Baker Street");
        assertThat(order.getOrderItem().code()).isEqualTo(orderItem.getCode());
        assertThat(order.getOrderItem().name()).isEqualTo(orderItem.getName());
        assertThat(order.getOrderItem().price().toPlainString()).isEqualTo(orderItem.getPrice());
        assertThat(order.getOrderItem().quantity()).isEqualTo(orderItem.getQuantity());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void getOrderReturnsPersistedOrderWhenOrderExists() {
        orderRepository.deleteAll();

        // Mock ProductApi to return a valid product
        when(productApi.getByCode(anyString()))
                .thenReturn(Optional.of(new ProductDto(
                        "P100", "The Hunger Games", "Book description", "image.jpg", new BigDecimal("34.0"))));

        OrderItem orderItem = OrderItem.newBuilder()
                .setCode("P100")
                .setName("The Hunger Games")
                .setPrice("34.0")
                .setQuantity(1)
                .build();

        Customer customer = Customer.newBuilder()
                .setName("Get Order Customer")
                .setEmail("getorder@test.com")
                .setPhone("+1987654321")
                .build();

        CreateOrderRequest createOrderRequest = CreateOrderRequest.newBuilder()
                .setCustomer(customer)
                .setDeliveryAddress("742 Evergreen Terrace")
                .setItem(orderItem)
                .build();

        var createOrderResponse = blockingStub.createOrder(createOrderRequest);

        GetOrderRequest getOrderRequest = GetOrderRequest.newBuilder()
                .setOrderNumber(createOrderResponse.getOrderNumber())
                .build();

        var getOrderResponse = blockingStub.getOrder(getOrderRequest);

        var order = getOrderResponse.getOrder();
        assertThat(order.getOrderNumber()).isEqualTo(createOrderResponse.getOrderNumber());
        assertThat(order.getCustomer().getName()).isEqualTo(customer.getName());
        assertThat(order.getCustomer().getEmail()).isEqualTo(customer.getEmail());
        assertThat(order.getCustomer().getPhone()).isEqualTo(customer.getPhone());
        assertThat(order.getDeliveryAddress()).isEqualTo("742 Evergreen Terrace");
        assertThat(order.getItem().getCode()).isEqualTo(orderItem.getCode());
        assertThat(order.getItem().getName()).isEqualTo(orderItem.getName());
        assertThat(order.getItem().getPrice()).isEqualTo(orderItem.getPrice());
        assertThat(order.getItem().getQuantity()).isEqualTo(orderItem.getQuantity());
        assertThat(order.getStatus()).isEqualTo(com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.NEW);
    }

    @Test
    void listOrdersReturnsAllCreatedOrders() {
        orderRepository.deleteAll();

        List<String> productCodes = List.of("P100", "P101", "P102");
        List<String> productPrices = List.of("34.0", "45.40", "44.50");

        // Mock ProductApi to return valid products for any code with matching prices
        when(productApi.getByCode("P100"))
                .thenReturn(Optional.of(
                        new ProductDto("P100", "Product P100", "Description", "image.jpg", new BigDecimal("34.0"))));
        when(productApi.getByCode("P101"))
                .thenReturn(Optional.of(
                        new ProductDto("P101", "Product P101", "Description", "image.jpg", new BigDecimal("45.40"))));
        when(productApi.getByCode("P102"))
                .thenReturn(Optional.of(
                        new ProductDto("P102", "Product P102", "Description", "image.jpg", new BigDecimal("44.50"))));

        List<String> createdOrderNumbers = new ArrayList<>();

        for (int index = 0; index < productCodes.size(); index++) {
            OrderItem orderItem = OrderItem.newBuilder()
                    .setCode(productCodes.get(index))
                    .setName("List Orders Product " + index)
                    .setPrice(productPrices.get(index))
                    .setQuantity(1)
                    .build();

            Customer customer = Customer.newBuilder()
                    .setName("List Orders Customer " + index)
                    .setEmail("list-orders-" + index + "@test.com")
                    .setPhone("+123456789" + index)
                    .build();

            CreateOrderRequest createOrderRequest = CreateOrderRequest.newBuilder()
                    .setCustomer(customer)
                    .setDeliveryAddress("221B Baker Street Apt " + index)
                    .setItem(orderItem)
                    .build();

            var createOrderResponse = blockingStub.createOrder(createOrderRequest);
            createdOrderNumbers.add(createOrderResponse.getOrderNumber());
        }

        var listOrdersResponse = blockingStub.listOrders(
                ListOrdersRequest.newBuilder().setPage(1).setPageSize(5).build());

        assertThat(listOrdersResponse.getOrdersCount()).isEqualTo(3);
        assertThat(listOrdersResponse.getTotalElements()).isEqualTo(3);
        assertThat(listOrdersResponse.getPageNumber()).isEqualTo(1);

        var returnedOrderNumbers = listOrdersResponse.getOrdersList().stream()
                .map(com.sivalabs.bookstore.orders.grpc.proto.OrderView::getOrderNumber)
                .toList();

        assertThat(returnedOrderNumbers).containsExactlyInAnyOrderElementsOf(createdOrderNumbers);

        assertThat(listOrdersResponse.getOrdersList()).allSatisfy(orderView -> assertThat(orderView.getStatus())
                .isEqualTo(com.sivalabs.bookstore.orders.grpc.proto.OrderStatus.NEW));
    }

    @Test
    void createOrderWithEmptyCustomerNameReturnsInvalidArgument() {
        orderRepository.deleteAll();

        when(productApi.getByCode("P900"))
                .thenReturn(Optional.of(new ProductDto(
                        "P900", "Validation Product", "Test product", "image.jpg", new BigDecimal("12.50"))));

        OrderItem orderItem = OrderItem.newBuilder()
                .setCode("P900")
                .setName("Validation Product")
                .setPrice("12.50")
                .setQuantity(1)
                .build();

        Customer customer = Customer.newBuilder()
                .setName("")
                .setEmail("blank-name@test.com")
                .setPhone("+15550001111")
                .build();

        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setCustomer(customer)
                .setDeliveryAddress("221B Baker Street")
                .setItem(orderItem)
                .build();

        assertThatThrownBy(() -> blockingStub.createOrder(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> {
                    StatusRuntimeException statusEx = (StatusRuntimeException) ex;
                    assertThat(statusEx.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(statusEx.getStatus().getDescription())
                            .containsIgnoringCase("customer")
                            .contains("Customer Name is required");
                });
    }

    @Test
    void createOrderWithNegativeQuantityReturnsInvalidArgument() {
        orderRepository.deleteAll();

        when(productApi.getByCode("P901"))
                .thenReturn(Optional.of(new ProductDto(
                        "P901", "Negative Quantity Product", "Test product", "image.jpg", new BigDecimal("19.99"))));

        OrderItem orderItem = OrderItem.newBuilder()
                .setCode("P901")
                .setName("Negative Quantity Product")
                .setPrice("19.99")
                .setQuantity(-1)
                .build();

        Customer customer = Customer.newBuilder()
                .setName("Negative Quantity Customer")
                .setEmail("negative-quantity@test.com")
                .setPhone("+15550002222")
                .build();

        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setCustomer(customer)
                .setDeliveryAddress("12 Grimmauld Place")
                .setItem(orderItem)
                .build();

        assertThatThrownBy(() -> blockingStub.createOrder(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> {
                    StatusRuntimeException statusEx = (StatusRuntimeException) ex;
                    assertThat(statusEx.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(statusEx.getStatus().getDescription()).contains("Quantity must be greater than 0");
                });
    }

    @Test
    void createOrderWithInvalidProductCodePropagatesCatalogFailure() {
        orderRepository.deleteAll();

        when(productApi.getByCode("INVALID-CODE")).thenReturn(Optional.empty());

        OrderItem orderItem = OrderItem.newBuilder()
                .setCode("INVALID-CODE")
                .setName("Missing Product")
                .setPrice("29.99")
                .setQuantity(1)
                .build();

        Customer customer = Customer.newBuilder()
                .setName("Catalog Failure Customer")
                .setEmail("invalid-product@test.com")
                .setPhone("+15550003333")
                .build();

        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setCustomer(customer)
                .setDeliveryAddress("4 Privet Drive")
                .setItem(orderItem)
                .build();

        assertThatThrownBy(() -> blockingStub.createOrder(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> {
                    StatusRuntimeException statusEx = (StatusRuntimeException) ex;
                    assertThat(statusEx.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(statusEx.getStatus().getDescription())
                            .contains("Product not found with code: INVALID-CODE");
                });
    }

    @Test
    void getOrderThrowsNotFoundWhenOrderDoesNotExist() {
        orderRepository.deleteAll();

        String missingOrderNumber = "ORD-MISSING-" + UUID.randomUUID();

        GetOrderRequest request =
                GetOrderRequest.newBuilder().setOrderNumber(missingOrderNumber).build();

        assertThatThrownBy(() -> blockingStub.getOrder(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> {
                    StatusRuntimeException statusEx = (StatusRuntimeException) ex;
                    assertThat(statusEx.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(statusEx.getStatus().getDescription()).contains(missingOrderNumber);
                });
    }
}

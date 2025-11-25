package com.sivalabs.bookstore.orders.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import com.sivalabs.bookstore.BookStoreApplication;
import com.sivalabs.bookstore.catalog.api.ProductApi;
import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersRemoteClient;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.testsupport.session.TestSessionConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = NONE,
        properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "bookstore.cache.enabled=false",
            "bookstore.session.hazelcast.enabled=false",
            "bookstore.grpc.server.enabled=false",
            "bookstore.grpc.client.retry-enabled=false"
        },
        classes = {
            BookStoreApplication.class,
            TestSessionConfiguration.class,
            OrdersGrpcClientIntegrationTest.InProcessChannelConfiguration.class
        })
@Testcontainers
@ActiveProfiles("test")
class OrdersGrpcClientIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");
    private static final String IN_PROCESS_SERVER_NAME = "orders-grpc-client-test-" + UUID.randomUUID();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("bookstore")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @DynamicPropertySource
    static void grpcProperties(DynamicPropertyRegistry registry) {
        registry.add("bookstore.grpc.client.deadline-ms", () -> 2_000);
    }

    @MockitoBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ProductApi productApi;

    @Autowired
    private OrdersGrpcService ordersGrpcService;

    @Autowired
    private OrdersRemoteClient ordersGrpcClient;

    @Autowired
    private OrderRepository orderRepository;

    private Server grpcServer;

    @BeforeEach
    void setUp() throws IOException {
        orderRepository.deleteAll();
        grpcServer = InProcessServerBuilder.forName(IN_PROCESS_SERVER_NAME)
                .directExecutor()
                .addService(ordersGrpcService)
                .build()
                .start();
    }

    @AfterEach
    void tearDown() {
        if (grpcServer != null) {
            grpcServer.shutdownNow();
        }
        orderRepository.deleteAll();
        Mockito.reset(productApi);
    }

    @Test
    void createOrderPersistsAndReturnsOrderNumber() {
        when(productApi.getByCode(anyString()))
                .thenReturn(Optional.of(new ProductDto(
                        "P100",
                        "Client Integration Product",
                        "Test description",
                        "image.jpg",
                        new BigDecimal("42.50"))));

        CreateOrderRequest request = new CreateOrderRequest(
                new Customer("Client Test", "client@test.com", "+1234567890"),
                "10 Downing Street",
                new OrderItem("P100", "Client Integration Product", new BigDecimal("42.50"), 2));

        CreateOrderResponse response = ordersGrpcClient.createOrder(request);

        assertThat(response.orderNumber()).isNotBlank();
        assertThat(orderRepository.findByOrderNumber(response.orderNumber())).isPresent();
    }

    @Test
    void getOrderReturnsCreatedOrder() {
        when(productApi.getByCode("P200"))
                .thenReturn(Optional.of(
                        new ProductDto("P200", "Client Product", "Description", "image.jpg", new BigDecimal("15.00"))));

        CreateOrderResponse createResponse = ordersGrpcClient.createOrder(new CreateOrderRequest(
                new Customer("Client Get", "get@test.com", "+1000000001"),
                "742 Evergreen Terrace",
                new OrderItem("P200", "Client Product", new BigDecimal("15.00"), 1)));

        var order = ordersGrpcClient.getOrder(createResponse.orderNumber());

        assertThat(order.orderNumber()).isEqualTo(createResponse.orderNumber());
        assertThat(order.customer().email()).isEqualTo("get@test.com");
        assertThat(order.deliveryAddress()).isEqualTo("742 Evergreen Terrace");
    }

    @Test
    void listOrdersReturnsAllOrders() {
        when(productApi.getByCode("P300"))
                .thenReturn(Optional.of(
                        new ProductDto("P300", "List Product A", "Description", "image.jpg", new BigDecimal("12.00"))));
        when(productApi.getByCode("P301"))
                .thenReturn(Optional.of(
                        new ProductDto("P301", "List Product B", "Description", "image.jpg", new BigDecimal("22.00"))));

        String firstOrderNumber = ordersGrpcClient
                .createOrder(new CreateOrderRequest(
                        new Customer("List One", "list1@test.com", "+2000000001"),
                        "1 Infinite Loop",
                        new OrderItem("P300", "List Product A", new BigDecimal("12.00"), 1)))
                .orderNumber();
        String secondOrderNumber = ordersGrpcClient
                .createOrder(new CreateOrderRequest(
                        new Customer("List Two", "list2@test.com", "+2000000002"),
                        "1600 Amphitheatre Parkway",
                        new OrderItem("P301", "List Product B", new BigDecimal("22.00"), 2)))
                .orderNumber();

        PagedResult<OrderView> orderViews = ordersGrpcClient.listOrders(1, 20);
        List<String> orderNumbers =
                orderViews.data().stream().map(OrderView::orderNumber).toList();

        assertThat(orderNumbers).contains(firstOrderNumber, secondOrderNumber);
    }

    @Test
    void clientMapsGrpcStatusToDomainExceptions() {
        when(productApi.getByCode("P400"))
                .thenReturn(Optional.of(new ProductDto(
                        "P400", "Invalid Quantity Product", "Description", "image.jpg", new BigDecimal("18.00"))));

        CreateOrderRequest invalidQuantityRequest = new CreateOrderRequest(
                new Customer("Invalid Quantity", "invalid@test.com", "+3000000001"),
                "12 Grimmauld Place",
                new OrderItem("P400", "Invalid Quantity Product", new BigDecimal("18.00"), -1));

        assertThatThrownBy(() -> ordersGrpcClient.createOrder(invalidQuantityRequest))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Quantity must be greater than 0");

        String missingOrderNumber = "ORD-MISSING-" + UUID.randomUUID();
        assertThatThrownBy(() -> ordersGrpcClient.getOrder(missingOrderNumber))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(missingOrderNumber);
    }

    @TestConfiguration
    static class InProcessChannelConfiguration {
        @Bean(destroyMethod = "shutdownNow")
        @Primary
        ManagedChannel inProcessManagedChannel() {
            return InProcessChannelBuilder.forName(IN_PROCESS_SERVER_NAME)
                    .directExecutor()
                    .build();
        }
    }
}

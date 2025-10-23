package com.sivalabs.bookstore.orders.infrastructure.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * In-process integration tests for {@link OrdersGrpcService}.
 *
 * <p>Tests the gRPC server service functionality by setting up an in-process gRPC server
 * and verifying that it correctly handles requests, converts messages, and processes orders.</p>
 *
 * <p><strong>In-Process Testing:</strong> This test uses {@link io.grpc.inprocess.InProcessServerBuilder}
 * and {@link io.grpc.inprocess.InProcessChannelBuilder} to create a gRPC server and client
 * that communicate within the same JVM process, without any network I/O. This provides:</p>
 * <ul>
 *   <li>Faster test execution (no network overhead)</li>
 *   <li>More reliable tests (no network flakiness)</li>
 *   <li>Focus on gRPC server logic and message conversion</li>
 * </ul>
 *
 * <p><strong>Note:</strong> For testing real network communication over TCP,
 * see {@link com.sivalabs.bookstore.orders.client.OrdersGrpcServiceNetworkIntegrationTest}.</p>
 */
@SpringBootTest(
        webEnvironment = NONE,
        properties = {
            "grpc.client.orders.enabled=true",
            "grpc.server.port=-1" // Disable the real gRPC server since we use in-process
        })
@Testcontainers(disabledWithoutDocker = true)
class OrdersGrpcServiceInProcessTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("bookstore")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("app.amqp.new-orders.bind", () -> "false");
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> "false");
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private com.sivalabs.bookstore.orders.domain.ProductCatalogPort productCatalogPort;

    @Autowired
    private OrdersGrpcService ordersGrpcService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private GrpcOrderMapper grpcOrderMapper;

    private Server grpcServer;
    private ManagedChannel channel;
    private OrdersGrpcClient grpcClient;

    @BeforeEach
    void setUpServer() throws IOException {
        String inProcessServerName = "orders-grpc-client-test-" + UUID.randomUUID();

        // Start in-process gRPC server with OrdersGrpcService
        grpcServer = InProcessServerBuilder.forName(inProcessServerName)
                .directExecutor()
                .addService(ordersGrpcService)
                .build()
                .start();

        // Create channel for client
        channel = InProcessChannelBuilder.forName(inProcessServerName)
                .directExecutor()
                .build();

        // Create blocking stub
        OrdersServiceGrpc.OrdersServiceBlockingStub blockingStub = OrdersServiceGrpc.newBlockingStub(channel);

        // Create OrdersGrpcClient with the test channel
        grpcClient = new OrdersGrpcClient(blockingStub, grpcOrderMapper);
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

    @Test
    void shouldCreateOrderViaGrpcService() {
        // Arrange
        orderRepository.deleteAll();

        Customer customer = new Customer("Server Test User", "server@test.com", "+9876543210");

        OrderItem orderItem = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 1);

        CreateOrderRequest request = new CreateOrderRequest(customer, "123 Test Street", orderItem);

        // Act
        CreateOrderResponse response = grpcClient.createOrder(request);

        // Assert
        assertThat(response.orderNumber()).isNotBlank();

        // Verify order was persisted in database
        var persistedOrder = orderRepository.findByOrderNumber(response.orderNumber());
        assertThat(persistedOrder).isPresent();

        var order = persistedOrder.orElseThrow();
        assertThat(order.getCustomer().name()).isEqualTo(customer.name());
        assertThat(order.getCustomer().email()).isEqualTo(customer.email());
        assertThat(order.getDeliveryAddress()).isEqualTo("123 Test Street");
        assertThat(order.getOrderItem().code()).isEqualTo(orderItem.code());
    }

    @Test
    void shouldGetOrderViaGrpcService() {
        // Arrange
        orderRepository.deleteAll();

        Customer customer = new Customer("Get Test User", "get@test.com", "+1111111111");
        OrderItem orderItem = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 1);
        CreateOrderRequest createRequest = new CreateOrderRequest(customer, "456 Get Street", orderItem);

        // Create order first
        CreateOrderResponse createResponse = grpcClient.createOrder(createRequest);
        String orderNumber = createResponse.orderNumber();

        // Act
        OrderDto orderDto = grpcClient.findOrder(orderNumber);

        // Assert
        assertThat(orderDto).isNotNull();
        assertThat(orderDto.orderNumber()).isEqualTo(orderNumber);
        assertThat(orderDto.customer().name()).isEqualTo(customer.name());
        assertThat(orderDto.customer().email()).isEqualTo(customer.email());
        assertThat(orderDto.deliveryAddress()).isEqualTo("456 Get Street");
        assertThat(orderDto.item().code()).isEqualTo(orderItem.code());
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> grpcClient.findOrder("NON-EXISTENT-ORDER"))
                .isInstanceOf(OrdersGrpcClient.OrderNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldListOrdersViaGrpcService() {
        // Arrange
        orderRepository.deleteAll();

        // Create multiple orders
        Customer customer1 = new Customer("List User 1", "list1@test.com", "+2222222222");
        OrderItem item1 = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 1);
        grpcClient.createOrder(new CreateOrderRequest(customer1, "111 List Street", item1));

        Customer customer2 = new Customer("List User 2", "list2@test.com", "+3333333333");
        OrderItem item2 = new OrderItem("P101", "To Kill a Mockingbird", new BigDecimal("30.0"), 2);
        grpcClient.createOrder(new CreateOrderRequest(customer2, "222 List Avenue", item2));

        Customer customer3 = new Customer("List User 3", "list3@test.com", "+4444444444");
        OrderItem item3 = new OrderItem("P102", "The Chronicles of Narnia", new BigDecimal("24.0"), 1);
        grpcClient.createOrder(new CreateOrderRequest(customer3, "333 List Boulevard", item3));

        // Act
        var orders = grpcClient.findOrders(1, 20);

        // Assert
        assertThat(orders.data()).hasSize(3);
        assertThat(orders.data()).extracting(OrderView::orderNumber).isNotEmpty();
        assertThat(orders.data()).allMatch(order -> order.orderNumber() != null);
    }

    @Test
    void shouldHandleValidationErrorsViaGrpcService() {
        // Arrange - Create order with invalid data (negative quantity would trigger validation error)
        Customer customer = new Customer("", "", ""); // Empty customer data
        OrderItem invalidItem = new OrderItem("", "", new BigDecimal("0"), 0); // Invalid item
        CreateOrderRequest request = new CreateOrderRequest(customer, "", invalidItem);

        // Act & Assert
        assertThatThrownBy(() -> grpcClient.createOrder(request))
                .isInstanceOf(RuntimeException.class); // Will get some validation error
    }
}

package com.sivalabs.bookstore.orders.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.domain.OrderService;
import com.sivalabs.bookstore.orders.domain.ProductCatalogPort;
import com.sivalabs.bookstore.orders.infrastructure.grpc.GrpcOrderMapper;
import com.sivalabs.bookstore.orders.infrastructure.grpc.OrdersGrpcClient;
import com.sivalabs.bookstore.orders.infrastructure.grpc.OrdersGrpcClient.OrderNotFoundException;
import com.sivalabs.bookstore.orders.support.DockerAvailability;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for gRPC server using real network communication.
 *
 * <p>These tests verify complete gRPC server functionality over real network sockets by:</p>
 * <ul>
 *   <li>Starting an embedded gRPC server on localhost:9090 with OrdersGrpcService</li>
 *   <li>Configuring a gRPC client to connect to the embedded server via TCP</li>
 *   <li>Testing all RPC operations end-to-end with Protocol Buffer serialization over the wire</li>
 *   <li>Validating proper error handling and status code conversion through network layer</li>
 * </ul>
 *
 * <p>Test infrastructure uses:</p>
 * <ul>
 *   <li>Testcontainers for PostgreSQL database</li>
 *   <li>SpringBootTest with embedded gRPC server on port 9090</li>
 *   <li>Real gRPC client and server components communicating over TCP (no mocking)</li>
 *   <li>Complete Protocol Buffer message serialization/deserialization through network</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This tests real network communication. For faster in-process testing,
 * see {@link com.sivalabs.bookstore.orders.infrastructure.grpc.OrdersGrpcServiceInProcessTest}.</p>
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
        properties = {
            "grpc.client.orders.enabled=true", // Enable gRPC for integration testing
            "grpc.client.orders.address=localhost:9090", // Connect to embedded server
            "grpc.server.port=9090", // Configure embedded gRPC server port
            "bookstore.cache.enabled=false", // Disable cache for predictable behavior
            "app.amqp.new-orders.bind=false", // Disable AMQP for isolation
            "spring.rabbitmq.listener.direct.auto-startup=false",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
@DisplayName("Orders gRPC Service Network Integration Tests")
class OrdersGrpcServiceNetworkIntegrationTest {

    static {
        assumeTrue(DockerAvailability.isDockerAvailable(), "Docker is required for Integration Tests");
    }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ordersdb")
            .withUsername("orders")
            .withPassword("orders")
            .withInitScript("db/test-init.sql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        assumeTrue(DockerAvailability.isDockerAvailable(), "Docker is required for Integration Tests");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> true);
        // gRPC server will be configured with random port in test properties
    }

    @Autowired
    private com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc.OrdersServiceBlockingStub
            ordersServiceBlockingStub;

    @Autowired
    private GrpcOrderMapper grpcOrderMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private ProductCatalogPort productCatalogPort;

    private OrderEntity testOrder;
    private OrdersGrpcClient ordersGrpcClient;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        orderRepository.deleteAll();

        // Create test order entity for database operations
        testOrder = buildTestOrderEntity();

        // Create OrdersGrpcClient with the autowired blocking stub and mapper
        ordersGrpcClient = new OrdersGrpcClient(ordersServiceBlockingStub, grpcOrderMapper);

        // Mock ProductCatalogPort to avoid external API calls
        // ProductCatalogPort.validate() just validates price - no return value needed
        // The validation will succeed if no exception is thrown
    }

    @AfterEach
    void cleanUp() {
        // Clean database after each test to prevent test interference
        orderRepository.deleteAll();
    }

    private OrderEntity buildTestOrderEntity() {
        String code = "P-" + UUID.randomUUID().toString().substring(0, 8);
        Customer customer = new Customer("gRPC Tester", "grpc@example.com", "+1-555-0123");
        OrderItem item = new OrderItem(code, "gRPC Test Product", BigDecimal.valueOf(29.99), 2);

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCustomer(customer);
        orderEntity.setDeliveryAddress("123 gRPC Test Street");
        orderEntity.setOrderItem(item);
        orderEntity.setStatus(OrderStatus.NEW);

        return orderEntity;
    }

    private CreateOrderRequest buildCreateOrderRequest() {
        Customer customer = new Customer("gRPC Client Tester", "client@example.com", "+1-555-0456");
        OrderItem item = new OrderItem("P100", "The Hunger Games", BigDecimal.valueOf(34.0), 1);
        return new CreateOrderRequest(customer, "456 Client Test Avenue", item);
    }

    /**
     * Test data setup and fixture creation for integration testing.
     * Validates that test infrastructure is properly configured.
     *
     * This test verifies the basic integration test setup including database
     * connectivity and order creation through the OrderService.
     */
    @Test
    @DisplayName("Should have proper test setup with database access")
    void shouldHaveProperTestSetup() {
        // Given
        assertThat(orderRepository).isNotNull();

        // Verify gRPC client is available and properly configured
        assertThat(ordersGrpcClient).isNotNull();

        // When - Create test order via OrderService to verify database connectivity
        OrderEntity savedOrder = orderService.createOrder(testOrder);

        // Then
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderNumber()).isNotBlank();
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    /**
     * Test createOrder gRPC operation end-to-end.
     * Validates complete client-server roundtrip with Protocol Buffer serialization.
     */
    @Test
    @DisplayName("Should create order successfully via gRPC client-server roundtrip")
    void shouldCreateOrderSuccessfullyViaGrpc() {
        // Given
        CreateOrderRequest request = buildCreateOrderRequest();

        // When
        CreateOrderResponse response = ordersGrpcClient.createOrder(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isNotBlank();
        // Order numbers are UUIDs in the current implementation
        assertThat(response.orderNumber()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        // Verify order was persisted in database
        assertThat(orderRepository.count()).isEqualTo(1);
        var savedOrder = orderRepository.findByOrderNumber(response.orderNumber());
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getCustomer().email()).isEqualTo("client@example.com");
    }

    /**
     * Test findOrder gRPC operation end-to-end.
     * Validates order retrieval with proper data conversion.
     */
    @Test
    @DisplayName("Should find order successfully via gRPC client-server roundtrip")
    void shouldFindOrderSuccessfullyViaGrpc() {
        // Given - Create order in database first
        OrderEntity savedOrder = orderService.createOrder(testOrder);
        String orderNumber = savedOrder.getOrderNumber();

        // When
        OrderDto foundOrder = ordersGrpcClient.findOrder(orderNumber);

        // Then
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.orderNumber()).isEqualTo(orderNumber);
        assertThat(foundOrder.customer().email()).isEqualTo("grpc@example.com");
        assertThat(foundOrder.customer().name()).isEqualTo("gRPC Tester");
        assertThat(foundOrder.deliveryAddress()).isEqualTo("123 gRPC Test Street");
        assertThat(foundOrder.status()).isEqualTo(OrderStatus.NEW);
        assertThat(foundOrder.createdAt()).isNotNull();
    }

    /**
     * Test findOrder gRPC operation with non-existent order.
     * Validates proper error handling and exception conversion.
     */
    @Test
    @DisplayName("Should handle order not found error via gRPC client-server roundtrip")
    void shouldHandleOrderNotFoundErrorViaGrpc() {
        // Given
        String nonExistentOrderNumber = "NON-EXISTENT-123";

        // When & Then
        assertThatThrownBy(() -> ordersGrpcClient.findOrder(nonExistentOrderNumber))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with orderNumber: " + nonExistentOrderNumber);
    }

    /**
     * Test findOrders gRPC operation end-to-end.
     * Validates list operations with multiple orders.
     */
    @Test
    @DisplayName("Should find all orders successfully via gRPC client-server roundtrip")
    void shouldFindAllOrdersSuccessfullyViaGrpc() {
        // Given - Create multiple orders in database
        OrderEntity order1 = orderService.createOrder(testOrder);

        OrderEntity order2 = buildTestOrderEntity();
        OrderEntity savedOrder2 = orderService.createOrder(order2);

        // When
        List<OrderView> orders = ordersGrpcClient.findOrders();

        // Then
        assertThat(orders).isNotNull();
        assertThat(orders).hasSize(2);

        List<String> orderNumbers = orders.stream().map(OrderView::orderNumber).toList();

        assertThat(orderNumbers).containsExactlyInAnyOrder(order1.getOrderNumber(), savedOrder2.getOrderNumber());

        // Verify first order details
        OrderView firstOrder = orders.stream()
                .filter(order -> order.orderNumber().equals(order1.getOrderNumber()))
                .findFirst()
                .orElseThrow();

        assertThat(firstOrder.customer().email()).isEqualTo("grpc@example.com");
        assertThat(firstOrder.status()).isEqualTo(OrderStatus.NEW);
    }

    /**
     * Test findOrders gRPC operation with empty database.
     * Validates list operations return empty results properly.
     */
    @Test
    @DisplayName("Should return empty list when no orders exist via gRPC client-server roundtrip")
    void shouldReturnEmptyListWhenNoOrdersExistViaGrpc() {
        // Given - Database is already cleaned in @BeforeEach
        assertThat(orderRepository.count()).isEqualTo(0);

        // When
        List<OrderView> orders = ordersGrpcClient.findOrders();

        // Then
        assertThat(orders).isNotNull();
        assertThat(orders).isEmpty();
    }
}

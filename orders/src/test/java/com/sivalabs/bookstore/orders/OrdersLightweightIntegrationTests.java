package com.sivalabs.bookstore.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.domain.OrderService;
import com.sivalabs.bookstore.orders.support.DockerAvailability;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
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

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
        properties = {
            "bookstore.cache.enabled=false",
            "app.amqp.new-orders.bind=false",
            "spring.rabbitmq.listener.direct.auto-startup=false",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
@DisplayName("Orders Lightweight Integration Tests")
class OrdersLightweightIntegrationTests {

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
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create order and persist to database")
    void shouldCreateOrderAndPersistToDatabase() {
        OrderEntity orderEntity = buildOrderEntity();

        OrderEntity savedOrder = orderService.createOrder(orderEntity);

        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getOrderNumber()).isNotBlank();

        Optional<OrderEntity> foundOrder = orderRepository.findByOrderNumber(savedOrder.getOrderNumber());
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getCustomer().email())
                .isEqualTo(orderEntity.getCustomer().email());
    }

    @Test
    @DisplayName("Should find order by order number")
    void shouldFindOrderByOrderNumber() {
        OrderEntity orderEntity = buildOrderEntity();
        OrderEntity createdOrder = orderService.createOrder(orderEntity);

        Optional<OrderEntity> foundOrder = orderRepository.findByOrderNumber(createdOrder.getOrderNumber());

        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getOrderNumber()).isEqualTo(createdOrder.getOrderNumber());
    }

    private OrderEntity buildOrderEntity() {
        String code = "P-" + UUID.randomUUID().toString().substring(0, 8);
        Customer customer = new Customer("Integration Tester", "tester@example.com", "+1234567890");
        OrderItem item = new OrderItem(code, "Integration Product", BigDecimal.valueOf(19.99), 1);
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCustomer(customer);
        orderEntity.setDeliveryAddress("221B Baker Street");
        orderEntity.setOrderItem(item);
        orderEntity.setStatus(OrderStatus.NEW);
        return orderEntity;
    }
}

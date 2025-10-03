package com.sivalabs.bookstore.orders.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.Customer;
import com.sivalabs.bookstore.orders.grpc.proto.OrderItem;
import com.sivalabs.bookstore.orders.grpc.proto.OrdersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = NONE)
@ApplicationModuleTest(webEnvironment = RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Sql("/test-products-data.sql")
class OrdersGrpcServiceIntegrationTest {

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
}

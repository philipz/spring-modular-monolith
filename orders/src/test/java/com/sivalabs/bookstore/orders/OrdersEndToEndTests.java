package com.sivalabs.bookstore.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.config.RabbitMQConfig;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.support.DockerAvailability;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Orders End-to-End Tests")
class OrdersEndToEndTests {

    private static final MockWebServer productCatalogServer;

    static {
        try {
            productCatalogServer = new MockWebServer();
            productCatalogServer.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start MockWebServer for product API", ex);
        }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ordersdb")
            .withUsername("orders")
            .withPassword("orders")
            .withInitScript("db/test-init.sql");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.1.3-management-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("product.api.base-url", () -> productCatalogServer.url("/").toString());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CacheManager cacheManager;

    @AfterAll
    static void shutdownMockServer() throws IOException {
        if (productCatalogServer != null) {
            productCatalogServer.shutdown();
        }
    }

    @BeforeEach
    void cleanState() {
        assumeTrue(DockerAvailability.isDockerAvailable(), "Docker is required for end-to-end tests");
        orderRepository.deleteAll();
        clearCache("orders");
        clearCache("ordersList");
    }

    @Test
    @DisplayName("should process orders end-to-end including messaging and caching")
    void shouldProcessOrdersEndToEnd() throws Exception {
        String productCode = "BOOK-" + UUID.randomUUID().toString().substring(0, 6);
        productCatalogServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{" + "\"code\":\""
                        + productCode + "\"," + "\"name\":\"Effective Testing\","
                        + "\"price\":89.99}"));

        CreateOrderRequest request = new CreateOrderRequest(
                new Customer("End To End Tester", "e2e@example.com", "+12025550100"),
                "742 Evergreen Terrace",
                new OrderItem(productCode, "Effective Testing", new BigDecimal("89.99"), 1));

        ResponseEntity<CreateOrderResponse> response =
                restTemplate.postForEntity("/api/orders", request, CreateOrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        String orderNumber = response.getBody().orderNumber();
        assertThat(orderNumber).isNotBlank();

        Awaitility.await("order persisted").atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<OrderEntity> saved = orderRepository.findByOrderNumber(orderNumber);
            assertThat(saved).isPresent();
            assertThat(saved.get().getCustomer().email()).isEqualTo("e2e@example.com");
        });

        Awaitility.await("order cached").atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                        getCacheValue("orders", orderNumber))
                .isNotNull());

        Awaitility.await("order event published").atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            try (Connection connection = createRabbitConnection();
                    Channel channel = connection.createChannel()) {
                GetResponse delivery = channel.basicGet(RabbitMQConfig.QUEUE_NAME, true);
                assertThat(delivery).as("OrderCreated event should be queued").isNotNull();
                String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                assertThat(body).contains("\"orderNumber\":\"" + orderNumber + "\"");
            }
        });

        ResponseEntity<String> webPage = restTemplate.getForEntity("/orders", String.class);
        assertThat(webPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webPage.getBody()).contains(orderNumber);
    }

    @Test
    @DisplayName("should return problem detail when order is missing")
    void shouldReturnProblemDetailWhenOrderMissing() {
        ResponseEntity<ProblemDetail> response = restTemplate.getForEntity(
                "/api/orders/{orderNumber}", ProblemDetail.class, "missing-order-" + UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("missing-order");
    }

    private static Connection createRabbitConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ.getHost());
        factory.setPort(RABBITMQ.getAmqpPort());
        factory.setUsername(RABBITMQ.getAdminUsername());
        factory.setPassword(RABBITMQ.getAdminPassword());
        factory.useNio();
        return factory.newConnection("orders-e2e-tests");
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private Object getCacheValue(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? wrapper.get() : null;
    }
}

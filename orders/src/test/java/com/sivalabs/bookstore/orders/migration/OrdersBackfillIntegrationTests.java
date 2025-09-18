package com.sivalabs.bookstore.orders.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.orders.support.DockerAvailability;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
@TestPropertySource(
        properties = {
            "orders.backfill.enabled=true",
            "orders.backfill.lookback-days=0",
            "orders.backfill.record-limit=50",
            "spring.liquibase.drop-first=true",
            "spring.liquibase.contexts=",
            "spring.autoconfigure.exclude=org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration"
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrdersBackfillIntegrationTests {

    static {
        assumeTrue(DockerAvailability.isDockerAvailable(), "Docker is required for backfill integration tests");
    }

    @Container
    static final PostgreSQLContainer<?> legacyDb = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("legacydb")
            .withUsername("legacy")
            .withPassword("legacy")
            .withInitScript("db/legacy-orders-init.sql");

    @Container
    static final PostgreSQLContainer<?> targetDb = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ordersdb")
            .withUsername("orders")
            .withPassword("orders");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        if (!legacyDb.isRunning()) {
            legacyDb.start();
        }
        if (!targetDb.isRunning()) {
            targetDb.start();
        }

        registry.add("spring.datasource.url", targetDb::getJdbcUrl);
        registry.add("spring.datasource.username", targetDb::getUsername);
        registry.add("spring.datasource.password", targetDb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("orders.backfill.source.url", legacyDb::getJdbcUrl);
        registry.add("orders.backfill.source.username", legacyDb::getUsername);
        registry.add("orders.backfill.source.password", legacyDb::getPassword);
        registry.add("orders.backfill.source.driver-class-name", () -> "org.postgresql.Driver");
    }

    @MockBean
    private OrdersBackfillRunner ordersBackfillRunner;

    @Autowired
    private OrdersBackfillService backfillService;

    @Autowired
    private OrdersBackfillAuditor auditor;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTargetDatabase() {
        orderRepository.deleteAll();
        ensureBackfillAuditTableExists();
        jdbcTemplate.update("DELETE FROM orders.backfill_audit");
    }

    private void ensureBackfillAuditTableExists() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS orders");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders.backfill_audit ("
                + "id BIGSERIAL PRIMARY KEY,"
                + "started_at TIMESTAMP NOT NULL,"
                + "completed_at TIMESTAMP,"
                + "source_since TIMESTAMP,"
                + "record_limit INTEGER,"
                + "records_processed INTEGER DEFAULT 0,"
                + "status TEXT NOT NULL,"
                + "error_message TEXT"
                + ")");
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_backfill_audit_started_at" + " ON orders.backfill_audit (started_at)");
    }

    @Test
    @DisplayName("Backfill copies legacy orders, skips existing entries, and records audit trail")
    void shouldBackfillLegacyOrdersAndRecordAudit() {
        LocalDateTime legacyCreated = LocalDateTime.now().minusDays(1);
        // Seed an existing order that should be skipped during backfill
        OrderEntity existing = OrderEntity.builder()
                .orderNumber("LEG-100")
                .customer(new Customer("Existing", "existing@example.com", "+19998887777"))
                .deliveryAddress("123 Existing Lane")
                .orderItem(new OrderItem("P999", "Existing Item", BigDecimal.TEN, 1))
                .status(OrderStatus.NEW)
                .build();
        existing.setCreatedAt(legacyCreated);
        existing.setUpdatedAt(legacyCreated);
        orderRepository.save(existing);

        BackfillRequest request = new BackfillRequest(null, 25);
        long auditId = auditor.recordStart(request);
        int processed = backfillService.runBackfill(request);
        auditor.recordSuccess(auditId, processed);

        // Expect only the non-duplicate record to be inserted
        assertThat(processed).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(2);

        Optional<OrderEntity> migrated = orderRepository.findByOrderNumber("LEG-101");
        assertThat(migrated).isPresent();
        OrderEntity order = migrated.get();
        assertThat(order.getCustomer().email()).isEqualTo("bob.legacy@example.com");
        assertThat(order.getOrderItem().price()).isEqualTo(new BigDecimal("49.99"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 11, 12, 30));
        assertThat(order.getUpdatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 11, 20, 45));

        // Verify audit entry captured the run
        var audit = jdbcTemplate.queryForMap(
                "SELECT status, records_processed FROM orders.backfill_audit WHERE id = ?", auditId);
        assertThat(audit.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) audit.get("records_processed")).intValue()).isEqualTo(processed);
    }

    @Test
    @DisplayName("Rollback script placeholder remains environment-agnostic")
    void rollbackScriptContainsInstructions() throws IOException {
        String script = Files.readString(Path.of("scripts", "rollback.sql"));
        assertThat(script).contains("rollback_order_numbers");
        assertThat(script).contains("<AUDIT_ID>");
    }
}

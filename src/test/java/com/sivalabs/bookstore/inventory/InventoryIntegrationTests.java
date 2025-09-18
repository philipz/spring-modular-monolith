package com.sivalabs.bookstore.inventory;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.sivalabs.bookstore.TestcontainersConfiguration;
import com.sivalabs.bookstore.inventory.domain.InventoryService;
import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import com.sivalabs.bookstore.orders.api.model.Customer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.jdbc.Sql;

@ApplicationModuleTest(webEnvironment = RANDOM_PORT)
@Import({TestcontainersConfiguration.class, com.sivalabs.bookstore.testsupport.cache.InventoryCacheTestConfig.class})
@Sql("/test-products-data.sql")
class InventoryIntegrationTests {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void handleOrderCreatedEvent(Scenario scenario) {
        var customer = new Customer("Siva", "siva@gmail.com", "9987654");
        String productCode = "P114";
        var event = new OrderCreatedEvent(UUID.randomUUID().toString(), productCode, 2, customer);
        scenario.publish(event).andWaitForStateChange(() -> inventoryService.getStockLevel(productCode) == 598);
    }
}

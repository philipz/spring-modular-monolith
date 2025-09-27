package com.sivalabs.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.sivalabs.bookstore.TestcontainersConfiguration;
import com.sivalabs.bookstore.inventory.domain.InventoryEntity;
import com.sivalabs.bookstore.inventory.domain.InventoryRepository;
import com.sivalabs.bookstore.inventory.domain.InventoryService;
import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.testsupport.EnabledIfDockerAvailable;
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
@EnabledIfDockerAvailable
class InventoryIntegrationTests {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void handleOrderCreatedEvent(Scenario scenario) {
        var customer = new Customer("Siva", "siva@gmail.com", "9987654");
        String productCode = "P114";
        var event = new OrderCreatedEvent(UUID.randomUUID().toString(), productCode, 2, customer);
        scenario.publish(event).andWaitForStateChange(() -> getStockLevelFromDatabase(productCode) == 598L);

        assertThat(inventoryService.getStockLevel(productCode)).isEqualTo(598L);
    }

    @Test
    void shouldDecreaseStockThroughService() {
        String productCode = "P113";

        Long initialStock = inventoryService.getStockLevel(productCode);

        inventoryService.decreaseStockLevel(productCode, 2);

        Long updatedStock = inventoryService.getStockLevel(productCode);

        assertThat(initialStock).isEqualTo(700L);
        assertThat(updatedStock).isEqualTo(698L);
        assertThat(getStockLevelFromDatabase(productCode)).isEqualTo(698L);
    }

    private Long getStockLevelFromDatabase(String productCode) {
        return inventoryRepository
                .findByProductCode(productCode)
                .map(InventoryEntity::getQuantity)
                .orElse(0L);
    }
}

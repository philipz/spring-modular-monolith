package com.sivalabs.bookstore.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sivalabs.bookstore.inventory.cache.InventoryCacheService;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceCachingTests {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryCacheService inventoryCacheService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, inventoryCacheService);
    }

    @Test
    void shouldPopulateCacheAfterDatabaseLookupAndUseCacheSubsequently() {
        String productCode = "P-100";
        InventoryEntity inventory = new InventoryEntity();
        inventory.setId(42L);
        inventory.setProductCode(productCode);
        inventory.setQuantity(150L);

        AtomicReference<Optional<InventoryEntity>> cachedInventory = new AtomicReference<>(Optional.empty());

        lenient().when(inventoryCacheService.isCircuitBreakerOpen()).thenReturn(false);

        lenient()
                .when(inventoryCacheService.findByProductCodeWithFallback(eq(productCode), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Optional<InventoryEntity>> fallback = invocation.getArgument(1);
                    Optional<InventoryEntity> current = cachedInventory.get();
                    if (current.isPresent()) {
                        return current;
                    }
                    Optional<InventoryEntity> result = fallback.get();
                    cachedInventory.set(result);
                    return result;
                });

        lenient()
                .doAnswer(invocation -> {
                    InventoryEntity cached = invocation.getArgument(1);
                    cachedInventory.set(Optional.ofNullable(cached));
                    return true;
                })
                .when(inventoryCacheService)
                .cacheInventory(anyLong(), any(InventoryEntity.class));

        lenient()
                .when(inventoryRepository.findByProductCode(productCode))
                .thenAnswer(invocation -> Optional.of(inventory));

        Long firstStockLevel = inventoryService.getStockLevel(productCode);
        Long secondStockLevel = inventoryService.getStockLevel(productCode);

        assertThat(firstStockLevel).isEqualTo(150L);
        assertThat(secondStockLevel).isEqualTo(150L);

        verify(inventoryRepository, times(1)).findByProductCode(productCode);
    }
}

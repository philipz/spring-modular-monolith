package com.sivalabs.bookstore.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceStockAdjustmentTests {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, null);
    }

    @Test
    void shouldThrowExceptionWhenDecreaseQuantityExceedsAvailableStock() {
        String productCode = "P-200";
        InventoryEntity inventory = new InventoryEntity();
        inventory.setProductCode(productCode);
        inventory.setQuantity(5L);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.decreaseStockLevel(productCode, 10))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(inventory.getQuantity()).isEqualTo(5L);
        verify(inventoryRepository).findByProductCode(productCode);
        verify(inventoryRepository, never()).save(any(InventoryEntity.class));
    }

    @Test
    void shouldDecreaseStockWhenSufficientQuantityAvailable() {
        String productCode = "P-300";
        InventoryEntity inventory = new InventoryEntity();
        inventory.setProductCode(productCode);
        inventory.setQuantity(8L);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.decreaseStockLevel(productCode, 3);

        assertThat(inventory.getQuantity()).isEqualTo(5L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void shouldRejectNonPositiveDecreaseQuantity() {
        String productCode = "P-400";

        assertThatThrownBy(() -> inventoryService.decreaseStockLevel(productCode, 0))
                .isInstanceOf(InvalidInventoryAdjustmentException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(inventoryRepository);
    }
}

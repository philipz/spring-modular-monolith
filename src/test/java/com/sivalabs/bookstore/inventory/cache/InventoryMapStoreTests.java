package com.sivalabs.bookstore.inventory.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hazelcast.core.HazelcastInstance;
import com.sivalabs.bookstore.inventory.domain.InventoryEntity;
import com.sivalabs.bookstore.inventory.domain.InventoryRepository;
import com.sivalabs.bookstore.testsupport.TestObjectProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryMapStore Unit Tests")
class InventoryMapStoreTests {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private HazelcastInstance hazelcastInstance;

    private InventoryMapStore inventoryMapStore;

    private InventoryEntity testInventory;
    private InventoryEntity anotherTestInventory;

    @BeforeEach
    void setUp() {
        inventoryMapStore = new InventoryMapStore(new TestObjectProvider<>(() -> inventoryRepository));

        // Create test inventory data with Long IDs
        testInventory = createTestInventory(1L, "INV-P001", 100L);
        anotherTestInventory = createTestInventory(2L, "INV-P002", 50L);
    }

    @Test
    @DisplayName("Should initialize successfully with InventoryRepository")
    void shouldInitializeWithInventoryRepository() {
        assertThat(inventoryMapStore).isNotNull();
    }

    @Test
    @DisplayName("Should complete lifecycle init without errors")
    void shouldInitializeLifecycle() {
        Properties props = new Properties();
        String mapName = "inventory-cache";

        // Should not throw any exception
        inventoryMapStore.init(hazelcastInstance, props, mapName);
    }

    @Test
    @DisplayName("Should complete lifecycle destroy without errors")
    void shouldDestroyLifecycle() {
        // Should not throw any exception
        inventoryMapStore.destroy();
    }

    @Test
    @DisplayName("Should store inventory successfully with Long key - validation only")
    void shouldStoreInventorySuccessfullyWithLongKey() {
        Long inventoryId = 1L;

        // Store operation should complete without exception
        inventoryMapStore.store(inventoryId, testInventory);

        // Verify no exception was thrown - store method mainly validates
    }

    @Test
    @DisplayName("Should handle store with mismatched inventory ID")
    void shouldHandleStoreWithMismatchedInventoryId() {
        Long differentInventoryId = 999L;

        // Should complete without throwing exception but log warning
        inventoryMapStore.store(differentInventoryId, testInventory);
    }

    @Test
    @DisplayName("Should handle store with null inventory entity")
    void shouldHandleStoreWithNullInventory() {
        Long inventoryId = 1L;

        // Should complete without throwing exception
        inventoryMapStore.store(inventoryId, null);
    }

    @Test
    @DisplayName("Should load inventory successfully from InventoryRepository with Long key")
    void shouldLoadInventorySuccessfullyWithLongKey() {
        Long inventoryId = 1L;
        given(inventoryRepository.findById(inventoryId)).willReturn(Optional.of(testInventory));

        InventoryEntity result = inventoryMapStore.load(inventoryId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(inventoryId);
        assertThat(result.getProductCode()).isEqualTo(testInventory.getProductCode());
        assertThat(result.getQuantity()).isEqualTo(testInventory.getQuantity());
        verify(inventoryRepository).findById(inventoryId);
    }

    @Test
    @DisplayName("Should return null when inventory not found with Long key")
    void shouldReturnNullWhenInventoryNotFoundWithLongKey() {
        Long nonExistentInventoryId = 999999L;
        given(inventoryRepository.findById(nonExistentInventoryId)).willReturn(Optional.empty());

        InventoryEntity result = inventoryMapStore.load(nonExistentInventoryId);

        assertThat(result).isNull();
        verify(inventoryRepository).findById(nonExistentInventoryId);
    }

    @Test
    @DisplayName("Should handle load exception gracefully with Long key")
    void shouldHandleLoadExceptionWithLongKey() {
        Long inventoryId = 1L;
        RuntimeException testException = new RuntimeException("Database error");
        given(inventoryRepository.findById(inventoryId)).willThrow(testException);

        // Should not throw exception and return null
        InventoryEntity result = inventoryMapStore.load(inventoryId);

        assertThat(result).isNull();
        verify(inventoryRepository).findById(inventoryId);
    }

    @Test
    @DisplayName("Should load multiple inventories successfully with Long keys")
    void shouldLoadAllInventoriesSuccessfullyWithLongKeys() {
        Collection<Long> inventoryIds = Arrays.asList(1L, 2L, 3L);
        List<InventoryEntity> foundInventories = Arrays.asList(testInventory, anotherTestInventory);

        given(inventoryRepository.findAllById(inventoryIds)).willReturn(foundInventories);

        Map<Long, InventoryEntity> result = inventoryMapStore.loadAll(inventoryIds);

        assertThat(result).hasSize(2);
        assertThat(result).containsKey(1L);
        assertThat(result).containsKey(2L);
        assertThat(result).doesNotContainKey(3L); // Not found in repository
        assertThat(result.get(1L)).isEqualTo(testInventory);
        assertThat(result.get(2L)).isEqualTo(anotherTestInventory);

        verify(inventoryRepository).findAllById(inventoryIds);
    }

    @Test
    @DisplayName("Should handle loadAll with empty collection of Long keys")
    void shouldHandleLoadAllWithEmptyCollectionOfLongKeys() {
        Collection<Long> emptyInventoryIds = Arrays.asList();

        Map<Long, InventoryEntity> result = inventoryMapStore.loadAll(emptyInventoryIds);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle loadAll exception gracefully with Long keys")
    void shouldHandleLoadAllExceptionWithLongKeys() {
        Collection<Long> inventoryIds = Arrays.asList(1L);
        RuntimeException testException = new RuntimeException("Database error");
        given(inventoryRepository.findAllById(inventoryIds)).willThrow(testException);

        // Should return empty map instead of throwing
        Map<Long, InventoryEntity> result = inventoryMapStore.loadAll(inventoryIds);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return all inventory IDs for loadAllKeys")
    void shouldReturnAllInventoryIdsForLoadAllKeys() {
        List<InventoryEntity> allInventories = Arrays.asList(testInventory, anotherTestInventory);
        given(inventoryRepository.findAll()).willReturn(allInventories);

        Set<Long> result = (Set<Long>) inventoryMapStore.loadAllKeys();

        assertThat(result).hasSize(2);
        assertThat(result).contains(1L, 2L);
        verify(inventoryRepository).findAll();
    }

    @Test
    @DisplayName("Should handle loadAllKeys with empty repository")
    void shouldHandleLoadAllKeysWithEmptyRepository() {
        given(inventoryRepository.findAll()).willReturn(Arrays.asList());

        Set<Long> result = (Set<Long>) inventoryMapStore.loadAllKeys();

        assertThat(result).isEmpty();
        verify(inventoryRepository).findAll();
    }

    @Test
    @DisplayName("Should handle loadAllKeys exception gracefully")
    void shouldHandleLoadAllKeysException() {
        RuntimeException testException = new RuntimeException("Database error");
        given(inventoryRepository.findAll()).willThrow(testException);

        // Should not throw exception
        Set<Long> result = (Set<Long>) inventoryMapStore.loadAllKeys();

        assertThat(result).isEmpty();
        verify(inventoryRepository).findAll();
    }

    @Test
    @DisplayName("Should handle delete operation gracefully with Long key")
    void shouldHandleDeleteOperationWithLongKey() {
        Long inventoryId = 1L;

        // Should complete without throwing exception
        inventoryMapStore.delete(inventoryId);

        // Method should log but not actually delete - deletion handled by service layer
    }

    @Test
    @DisplayName("Should handle deleteAll operation gracefully with Long keys")
    void shouldHandleDeleteAllOperationWithLongKeys() {
        Collection<Long> inventoryIds = Arrays.asList(1L, 2L);

        // Should complete without throwing exception
        inventoryMapStore.deleteAll(inventoryIds);

        // Method should log but not actually delete - deletion handled by service layer
    }

    @Test
    @DisplayName("Should handle storeAll operation gracefully with Long keys")
    void shouldHandleStoreAllOperationWithLongKeys() {
        Map<Long, InventoryEntity> inventories = Map.of(
                1L, testInventory,
                2L, anotherTestInventory);

        // Should complete without throwing exception
        inventoryMapStore.storeAll(inventories);

        // Method should log completion
    }

    @Test
    @DisplayName("Should handle store operation exception with Long key")
    void shouldHandleStoreExceptionWithLongKey() {
        // Create an inventory that will trigger validation warning
        InventoryEntity invalidInventory = createTestInventory(999L, "DIFFERENT-PRODUCT", 100L);
        Long inventoryId = 1L;

        // Should complete without throwing exception but log warning
        inventoryMapStore.store(inventoryId, invalidInventory);
    }

    @Test
    @DisplayName("Should handle complex inventory data in load operation")
    void shouldHandleComplexInventoryDataInLoad() {
        Long inventoryId = 999L;
        InventoryEntity complexInventory = createComplexTestInventory();
        given(inventoryRepository.findById(inventoryId)).willReturn(Optional.of(complexInventory));

        InventoryEntity result = inventoryMapStore.load(inventoryId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(inventoryId);
        assertThat(result.getProductCode()).isEqualTo("COMPLEX-PRODUCT-999");
        assertThat(result.getQuantity()).isEqualTo(500L);
        verify(inventoryRepository).findById(inventoryId);
    }

    @Test
    @DisplayName("Should handle inventories with different quantity ranges")
    void shouldHandleInventoriesWithDifferentQuantityRanges() {
        // Test with zero quantity
        Long zeroQuantityId = 100L;
        InventoryEntity zeroQuantityInventory = createTestInventory(zeroQuantityId, "ZERO-STOCK", 0L);
        given(inventoryRepository.findById(zeroQuantityId)).willReturn(Optional.of(zeroQuantityInventory));

        InventoryEntity result1 = inventoryMapStore.load(zeroQuantityId);
        assertThat(result1).isNotNull();
        assertThat(result1.getQuantity()).isEqualTo(0L);

        // Test with very large quantity
        Long largeQuantityId = 200L;
        InventoryEntity largeQuantityInventory = createTestInventory(largeQuantityId, "LARGE-STOCK", 999999L);
        given(inventoryRepository.findById(largeQuantityId)).willReturn(Optional.of(largeQuantityInventory));

        InventoryEntity result2 = inventoryMapStore.load(largeQuantityId);
        assertThat(result2).isNotNull();
        assertThat(result2.getQuantity()).isEqualTo(999999L);

        verify(inventoryRepository).findById(zeroQuantityId);
        verify(inventoryRepository).findById(largeQuantityId);
    }

    @Test
    @DisplayName("Should handle negative quantities (backorder scenarios)")
    void shouldHandleNegativeQuantities() {
        Long backorderId = 300L;
        InventoryEntity backorderInventory = createTestInventory(backorderId, "BACKORDER-PRODUCT", -10L);
        given(inventoryRepository.findById(backorderId)).willReturn(Optional.of(backorderInventory));

        InventoryEntity result = inventoryMapStore.load(backorderId);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(-10L);
        assertThat(result.getProductCode()).isEqualTo("BACKORDER-PRODUCT");
        verify(inventoryRepository).findById(backorderId);
    }

    @Test
    @DisplayName("Should handle Long key edge values correctly")
    void shouldHandleLongKeyEdgeValuesCorrectly() {
        // Test with Long.MAX_VALUE
        Long maxValueId = Long.MAX_VALUE;
        InventoryEntity maxValueInventory = createTestInventory(maxValueId, "MAX-VALUE-PRODUCT", 1L);
        given(inventoryRepository.findById(maxValueId)).willReturn(Optional.of(maxValueInventory));

        InventoryEntity result1 = inventoryMapStore.load(maxValueId);
        assertThat(result1).isNotNull();
        assertThat(result1.getId()).isEqualTo(maxValueId);

        // Test with 1L (minimum positive value)
        Long minValueId = 1L;
        InventoryEntity minValueInventory = createTestInventory(minValueId, "MIN-VALUE-PRODUCT", 100L);
        given(inventoryRepository.findById(minValueId)).willReturn(Optional.of(minValueInventory));

        InventoryEntity result2 = inventoryMapStore.load(minValueId);
        assertThat(result2).isNotNull();
        assertThat(result2.getId()).isEqualTo(minValueId);

        verify(inventoryRepository).findById(maxValueId);
        verify(inventoryRepository).findById(minValueId);
    }

    @Test
    @DisplayName("Should handle loadAll with mixed success and failure results for Long keys")
    void shouldHandleLoadAllWithMixedResultsForLongKeys() {
        Collection<Long> inventoryIds = Arrays.asList(1L, 999L, 2L);

        // Return only some inventories (simulating partial success)
        List<InventoryEntity> foundInventories = Arrays.asList(testInventory, anotherTestInventory);
        given(inventoryRepository.findAllById(inventoryIds)).willReturn(foundInventories);

        Map<Long, InventoryEntity> result = inventoryMapStore.loadAll(inventoryIds);

        // Should return only the found inventories
        assertThat(result).hasSize(2);
        assertThat(result).containsKey(1L);
        assertThat(result).containsKey(2L);
        assertThat(result).doesNotContainKey(999L); // Not found
    }

    @Test
    @DisplayName("Should validate InventoryEntity fields correctly with Long keys")
    void shouldValidateInventoryEntityFieldsCorrectlyWithLongKeys() {
        Long inventoryId = 500L;

        // Create inventory with all fields populated
        InventoryEntity fullInventory = createTestInventory(inventoryId, "FULL-VALIDATION-PRODUCT", 250L);

        given(inventoryRepository.findById(inventoryId)).willReturn(Optional.of(fullInventory));

        InventoryEntity result = inventoryMapStore.load(inventoryId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(inventoryId);
        assertThat(result.getProductCode()).isEqualTo("FULL-VALIDATION-PRODUCT");
        assertThat(result.getQuantity()).isEqualTo(250L);
    }

    @Test
    @DisplayName("Should handle store with null Long key")
    void shouldHandleStoreWithNullLongKey() {
        Long nullInventoryId = null;

        // Should complete without throwing exception
        inventoryMapStore.store(nullInventoryId, testInventory);
    }

    @Test
    @DisplayName("Should handle delete with null Long key")
    void shouldHandleDeleteWithNullLongKey() {
        Long nullInventoryId = null;

        // Should complete without throwing exception
        inventoryMapStore.delete(nullInventoryId);
    }

    @Test
    @DisplayName("Should handle inventory with special product codes")
    void shouldHandleInventoryWithSpecialProductCodes() {
        Long specialId = 777L;
        InventoryEntity specialInventory = createTestInventory(specialId, "SPECIAL-P#001-@2024", 75L);
        given(inventoryRepository.findById(specialId)).willReturn(Optional.of(specialInventory));

        InventoryEntity result = inventoryMapStore.load(specialId);

        assertThat(result).isNotNull();
        assertThat(result.getProductCode()).isEqualTo("SPECIAL-P#001-@2024");
        assertThat(result.getQuantity()).isEqualTo(75L);
        verify(inventoryRepository).findById(specialId);
    }

    @Test
    @DisplayName("Should handle bulk operations with Long keys efficiently")
    void shouldHandleBulkOperationsWithLongKeysEfficiently() {
        // Test bulk load with large collection of Long keys
        Collection<Long> bulkInventoryIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        List<InventoryEntity> bulkInventories = Arrays.asList(
                testInventory,
                anotherTestInventory,
                createTestInventory(3L, "BULK-P003", 30L),
                createTestInventory(4L, "BULK-P004", 40L)
                // Missing ID 5L to test partial results
                );

        given(inventoryRepository.findAllById(bulkInventoryIds)).willReturn(bulkInventories);

        Map<Long, InventoryEntity> result = inventoryMapStore.loadAll(bulkInventoryIds);

        assertThat(result).hasSize(4);
        assertThat(result).containsKeys(1L, 2L, 3L, 4L);
        assertThat(result).doesNotContainKey(5L);
        verify(inventoryRepository).findAllById(bulkInventoryIds);
    }

    // Helper methods for creating test data

    private InventoryEntity createTestInventory(Long id, String productCode, Long quantity) {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setId(id);
        inventory.setProductCode(productCode);
        inventory.setQuantity(quantity);
        return inventory;
    }

    private InventoryEntity createComplexTestInventory() {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setId(999L);
        inventory.setProductCode("COMPLEX-PRODUCT-999");
        inventory.setQuantity(500L);
        return inventory;
    }
}

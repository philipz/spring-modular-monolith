package com.sivalabs.bookstore.inventory.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import com.sivalabs.bookstore.inventory.domain.InventoryEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("InventoryCacheService Index Integration (unit) Tests")
class InventoryCacheServiceIndexTests {

    @Test
    @DisplayName("findByProductCode uses index for O(1) ID lookup and returns entity from cache")
    void shouldUseIndexForFindByProductCode() {
        // Inventory cache (Long -> Object)
        IMap<Long, Object> inventoryMap = Mockito.mock(IMap.class);
        InventoryEntity entity = new InventoryEntity();
        entity.setId(1L);
        entity.setProductCode("P-INDEX-1");
        when(inventoryMap.get(1L)).thenReturn(entity);

        // Index cache (String -> Long)
        IMap<String, Object> indexMap = Mockito.mock(IMap.class);
        when(indexMap.get("P-INDEX-1")).thenReturn(1L);

        InventoryByProductCodeIndex index = new InventoryByProductCodeIndex(indexMap, new CacheErrorHandler());
        @SuppressWarnings("unchecked")
        ObjectProvider<InventoryByProductCodeIndex> provider =
                (ObjectProvider<InventoryByProductCodeIndex>) Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(index);

        InventoryCacheService service = new InventoryCacheService(inventoryMap, new CacheErrorHandler(), provider);

        Optional<InventoryEntity> result = service.findByProductCode("P-INDEX-1");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getProductCode()).isEqualTo("P-INDEX-1");
    }

    @Test
    @DisplayName("cacheInventory and updateCachedInventory maintain index mappings")
    void shouldMaintainIndexOnWrites() {
        IMap<Long, Object> inventoryMap = Mockito.mock(IMap.class);
        IMap<String, Object> indexMap = Mockito.mock(IMap.class);

        InventoryByProductCodeIndex index = new InventoryByProductCodeIndex(indexMap, new CacheErrorHandler());
        @SuppressWarnings("unchecked")
        ObjectProvider<InventoryByProductCodeIndex> provider =
                (ObjectProvider<InventoryByProductCodeIndex>) Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(index);

        InventoryCacheService service = new InventoryCacheService(inventoryMap, new CacheErrorHandler(), provider);

        InventoryEntity entity = new InventoryEntity();
        entity.setId(99L);
        entity.setProductCode("P-99");

        boolean cached = service.cacheInventory(99L, entity);
        assertThat(cached).isTrue();
        verify(indexMap).put("P-99", 99L);

        InventoryEntity updated = new InventoryEntity();
        updated.setId(99L);
        updated.setProductCode("P-99");
        boolean updatedRes = service.updateCachedInventory(99L, updated);
        assertThat(updatedRes).isTrue();
        verify(indexMap, Mockito.times(2)).put("P-99", 99L);
    }
}

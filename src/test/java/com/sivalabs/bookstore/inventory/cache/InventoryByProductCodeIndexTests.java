package com.sivalabs.bookstore.inventory.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("InventoryByProductCodeIndex Unit Tests")
class InventoryByProductCodeIndexTests {

    @Test
    @DisplayName("findInventoryIdByProductCode returns Optional with ID when present")
    void shouldFindInventoryIdByProductCode() {
        IMap<String, Object> indexMap = Mockito.mock(IMap.class);
        when(indexMap.get("P-123")).thenReturn(42L);

        InventoryByProductCodeIndex index = new InventoryByProductCodeIndex(indexMap, new CacheErrorHandler());

        Optional<Long> result = index.findInventoryIdByProductCode("P-123");
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(42L);
    }

    @Test
    @DisplayName("updateIndex and removeFromIndex delegate to underlying map")
    void shouldUpdateAndRemoveIndex() {
        IMap<String, Object> indexMap = Mockito.mock(IMap.class);
        InventoryByProductCodeIndex index = new InventoryByProductCodeIndex(indexMap, new CacheErrorHandler());

        boolean updated = index.updateIndex("P-200", 200L);
        assertThat(updated).isTrue();
        verify(indexMap).put("P-200", 200L);

        boolean removed = index.removeFromIndex("P-200");
        assertThat(removed).isTrue();
        verify(indexMap).remove("P-200");
    }
}

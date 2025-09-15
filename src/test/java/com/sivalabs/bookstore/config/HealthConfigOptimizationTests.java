package com.sivalabs.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hazelcast.cluster.ClusterState;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.LocalMapStats;
import com.sivalabs.bookstore.common.cache.CacheErrorHandler;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("HealthConfig Optimization Tests")
class HealthConfigOptimizationTests {

    private HazelcastInstance healthyHazelcast() {
        HazelcastInstance hz = Mockito.mock(HazelcastInstance.class, Mockito.RETURNS_DEEP_STUBS);
        when(hz.getLifecycleService().isRunning()).thenReturn(true);
        when(hz.getPartitionService().isClusterSafe()).thenReturn(true);
        when(hz.getCluster().getMembers()).thenReturn(Set.of());
        when(hz.getName()).thenReturn("hz-test");
        when(hz.getConfig().getClusterName()).thenReturn("bookstore-cluster");
        when(hz.getCluster().getClusterState()).thenReturn(ClusterState.ACTIVE);
        return hz;
    }

    private LocalMapStats localStats(long owned) {
        // Return a mock without stubbing final methods to avoid Mockito unfinished stubbing issues
        return mock(LocalMapStats.class);
    }

    @Test
    @DisplayName("Uses local stats (not size) for health")
    void shouldUseLocalStatsNotSize() {
        // Arrange
        IMap<String, Object> orders = mock(IMap.class);
        IMap<String, Object> products = mock(IMap.class);
        IMap<Long, Object> inventory = mock(IMap.class);

        when(orders.getName()).thenReturn("orders-cache");
        when(products.getName()).thenReturn("products-cache");
        when(inventory.getName()).thenReturn("inventory-cache");

        when(orders.getLocalMapStats()).thenReturn(localStats(1));
        when(products.getLocalMapStats()).thenReturn(localStats(2));
        when(inventory.getLocalMapStats()).thenReturn(localStats(3));

        // Make size() blow up if called
        when(orders.size()).thenThrow(new RuntimeException("size() should not be called"));
        when(products.size()).thenThrow(new RuntimeException("size() should not be called"));
        when(inventory.size()).thenThrow(new RuntimeException("size() should not be called"));

        CacheProperties props = new CacheProperties();
        CacheErrorHandler errorHandler = new CacheErrorHandler();
        HealthConfig health = new HealthConfig(healthyHazelcast(), orders, products, inventory, errorHandler, props);

        // Act
        var healthResult = health.health();

        // Assert
        assertThat(healthResult).isNotNull();
        verify(orders, never()).size();
        verify(products, never()).size();
        verify(inventory, never()).size();
        verify(orders, atLeastOnce()).getLocalMapStats();
        verify(products, atLeastOnce()).getLocalMapStats();
        verify(inventory, atLeastOnce()).getLocalMapStats();
    }

    @Test
    @DisplayName("Basic operations read-only mode performs only GETs")
    void basicOperationsReadOnlyMode() {
        IMap<String, Object> orders = mock(IMap.class);
        IMap<String, Object> products = mock(IMap.class);
        IMap<Long, Object> inventory = mock(IMap.class);

        // Provide local stats so cache checks succeed
        when(orders.getName()).thenReturn("orders-cache");
        when(products.getName()).thenReturn("products-cache");
        when(inventory.getName()).thenReturn("inventory-cache");
        when(orders.getLocalMapStats()).thenReturn(localStats(0));
        when(products.getLocalMapStats()).thenReturn(localStats(0));
        when(inventory.getLocalMapStats()).thenReturn(localStats(0));

        CacheProperties props = new CacheProperties();
        props.setBasicOperationsReadOnly(true);
        props.setTestBasicOperationsEnabled(true);

        HealthConfig health =
                new HealthConfig(healthyHazelcast(), orders, products, inventory, new CacheErrorHandler(), props);

        var result = health.health();
        assertThat(result).isNotNull();

        // Verify no write operations in read-only mode
        verify(orders, never()).put(anyString(), any());
        verify(products, never()).put(anyString(), any());
        verify(inventory, never()).put(anyLong(), any());

        verify(orders, never()).remove(anyString());
        verify(products, never()).remove(anyString());
        verify(inventory, never()).remove(anyLong());
    }

    @Test
    @DisplayName("Basic operations disabled skips all ops")
    void basicOperationsDisabledSkipsAll() {
        IMap<String, Object> orders = mock(IMap.class);
        IMap<String, Object> products = mock(IMap.class);
        IMap<Long, Object> inventory = mock(IMap.class);

        when(orders.getName()).thenReturn("orders-cache");
        when(products.getName()).thenReturn("products-cache");
        when(inventory.getName()).thenReturn("inventory-cache");
        when(orders.getLocalMapStats()).thenReturn(localStats(0));
        when(products.getLocalMapStats()).thenReturn(localStats(0));
        when(inventory.getLocalMapStats()).thenReturn(localStats(0));

        CacheProperties props = new CacheProperties();
        props.setTestBasicOperationsEnabled(false);

        HealthConfig health =
                new HealthConfig(healthyHazelcast(), orders, products, inventory, new CacheErrorHandler(), props);

        var result = health.health();
        assertThat(result).isNotNull();

        // Ensure no cache interactions for basic operations
        verify(orders, never()).put(anyString(), any());
        verify(products, never()).put(anyString(), any());
        verify(inventory, never()).put(anyLong(), any());

        verify(orders, never()).get(startsWith("health-check-"));
        verify(products, never()).get(startsWith("health-check-"));
        verify(inventory, never()).get(anyLong());
    }
}

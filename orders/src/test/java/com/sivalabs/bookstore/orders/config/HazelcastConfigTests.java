package com.sivalabs.bookstore.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.sivalabs.bookstore.orders.cache.OrderMapStore;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HazelcastConfigTests {

    private final HazelcastConfig config = new HazelcastConfig();

    @Test
    @DisplayName("Should configure orders cache map based on cache properties")
    void shouldConfigureOrdersCacheMapBasedOnCacheProperties() {
        CacheProperties properties = new CacheProperties();
        properties.setMaxSize(250);
        properties.setTimeToLiveSeconds(7_200);
        properties.setMaxIdleSeconds(30);
        properties.setBackupCount(2);
        properties.setReadBackupData(false);
        properties.setMetricsEnabled(true);
        properties.setWriteThrough(true);
        properties.setWriteDelaySeconds(5);
        properties.setWriteBatchSize(10);

        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        OrderMapStore mapStore = new OrderMapStore(orderRepository);

        Config hazelcastConfig = config.hazelcastConfiguration(properties, mapStore);

        MapConfig mapConfig = hazelcastConfig.getMapConfig("orders-cache");
        assertThat(mapConfig.getName()).isEqualTo("orders-cache");
        assertThat(mapConfig.getEvictionConfig().getMaxSizePolicy()).isEqualTo(MaxSizePolicy.PER_NODE);
        assertThat(mapConfig.getEvictionConfig().getSize()).isEqualTo(250);
        assertThat(mapConfig.getTimeToLiveSeconds()).isEqualTo(7_200);
        assertThat(mapConfig.getMaxIdleSeconds()).isEqualTo(30);
        assertThat(mapConfig.getBackupCount()).isEqualTo(2);
        assertThat(mapConfig.isReadBackupData()).isFalse();
        assertThat(mapConfig.isStatisticsEnabled()).isTrue();

        MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        assertThat(mapStoreConfig).isNotNull();
        assertThat(mapStoreConfig.isEnabled()).isTrue();
        assertThat(mapStoreConfig.getImplementation()).isSameAs(mapStore);
        assertThat(mapStoreConfig.getInitialLoadMode()).isEqualTo(MapStoreConfig.InitialLoadMode.LAZY);
        assertThat(mapStoreConfig.getWriteDelaySeconds()).isEqualTo(5);
        assertThat(mapStoreConfig.getWriteBatchSize()).isEqualTo(10);

        assertThat(hazelcastConfig.getMetricsConfig().isEnabled()).isTrue();
        assertThat(hazelcastConfig.getSerializationConfig().isAllowUnsafe()).isFalse();
    }
}

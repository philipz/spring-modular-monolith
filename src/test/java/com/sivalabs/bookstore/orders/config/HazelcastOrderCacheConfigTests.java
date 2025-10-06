package com.sivalabs.bookstore.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.sivalabs.bookstore.orders.cache.OrderMapStore;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import com.sivalabs.bookstore.testsupport.TestObjectProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

class HazelcastOrderCacheConfigTests {

    private final HazelcastOrderCacheConfig config = new HazelcastOrderCacheConfig();

    @Test
    @DisplayName("Should build orders map configuration using shared cache properties")
    void shouldBuildOrdersMapConfigurationUsingSharedCacheProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("bookstore.cache.max-size", "250")
                .withProperty("bookstore.cache.time-to-live-seconds", "7200")
                .withProperty("bookstore.cache.max-idle-seconds", "30")
                .withProperty("bookstore.cache.backup-count", "2")
                .withProperty("bookstore.cache.read-backup-data", "false")
                .withProperty("bookstore.cache.metrics-enabled", "true")
                .withProperty("bookstore.cache.write-through", "true")
                .withProperty("bookstore.cache.write-delay-seconds", "5")
                .withProperty("bookstore.cache.write-batch-size", "10");

        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        OrderMapStore orderMapStore = new OrderMapStore(new TestObjectProvider<>(() -> orderRepository));

        MapConfig mapConfig = config.ordersCacheMapConfig(environment, orderMapStore);

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
        assertThat(mapStoreConfig.getImplementation()).isSameAs(orderMapStore);
        assertThat(mapStoreConfig.getInitialLoadMode()).isEqualTo(MapStoreConfig.InitialLoadMode.LAZY);
        assertThat(mapStoreConfig.getWriteDelaySeconds()).isEqualTo(5);
        assertThat(mapStoreConfig.getWriteBatchSize()).isEqualTo(10);
    }
}

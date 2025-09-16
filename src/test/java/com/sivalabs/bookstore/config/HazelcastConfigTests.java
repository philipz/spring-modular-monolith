package com.sivalabs.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.sivalabs.bookstore.TestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for HazelcastConfig to verify all cache configurations work correctly.
 *
 * This test suite validates:
 * - Hazelcast instance creation and configuration
 * - All cache bean creation (orders, products, inventory)
 * - MapStore integration for all cache types
 * - Cache map configuration validation
 * - Configuration property integration
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
        properties = {
            "bookstore.cache.enabled=true",
            "bookstore.cache.write-through=true",
            "bookstore.cache.max-size=100",
            "bookstore.cache.time-to-live-seconds=3600",
            "bookstore.cache.write-delay-seconds=1",
            "bookstore.cache.metrics-enabled=true",
            "logging.level.com.sivalabs.bookstore.config.HazelcastConfig=DEBUG"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("HazelcastConfig Integration Tests")
class HazelcastConfigTests {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    // Config is accessible through hazelcastInstance.getConfig()

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    @Qualifier("ordersCache") private IMap<String, Object> ordersCache;

    @Autowired
    @Qualifier("productsCache") private IMap<String, Object> productsCache;

    @Autowired
    @Qualifier("inventoryCache") private IMap<Long, Object> inventoryCache;

    @Autowired
    @Qualifier("ordersCacheName") private String ordersCacheName;

    @Autowired
    @Qualifier("productsCacheName") private String productsCacheName;

    @Autowired
    @Qualifier("inventoryCacheName") private String inventoryCacheName;

    // Note: MapStore beans are tested in their respective module tests
    // to maintain proper module boundaries and avoid cross-module dependencies

    @Nested
    @DisplayName("Hazelcast Configuration")
    class HazelcastConfiguration {

        @Test
        @DisplayName("Should create Hazelcast instance successfully")
        void shouldCreateHazelcastInstanceSuccessfully() {
            assertThat(hazelcastInstance).isNotNull();
            assertThat(hazelcastInstance.getName()).startsWith("bookstore-hazelcast-");
            assertThat(hazelcastInstance.getConfig()).isNotNull();
        }

        @Test
        @DisplayName("Should configure Hazelcast with correct instance settings")
        void shouldConfigureHazelcastWithCorrectInstanceSettings() {
            Config config = hazelcastInstance.getConfig();

            assertThat(config.getInstanceName()).startsWith("bookstore-hazelcast-");
            assertThat(config.getClusterName()).isEqualTo("bookstore-cluster");
            assertThat(config.getNetworkConfig().getJoin().getMulticastConfig().isEnabled())
                    .isFalse();
            assertThat(config.getNetworkConfig().getJoin().getTcpIpConfig().isEnabled())
                    .isFalse();
        }

        @Test
        @DisplayName("Should inject cache properties correctly")
        void shouldInjectCachePropertiesCorrectly() {
            assertThat(cacheProperties).isNotNull();
            assertThat(cacheProperties.isEnabled()).isTrue();
            assertThat(cacheProperties.isWriteThrough()).isTrue();
            assertThat(cacheProperties.getMaxSize()).isEqualTo(100);
            assertThat(cacheProperties.getTimeToLiveSeconds()).isEqualTo(3600);
        }
    }

    @Nested
    @DisplayName("Orders Cache Configuration")
    class OrdersCacheConfiguration {

        @Test
        @DisplayName("Should create orders cache bean successfully")
        void shouldCreateOrdersCacheBeanSuccessfully() {
            assertThat((Object) ordersCache).isNotNull();
            assertThat(ordersCache.getName()).isEqualTo("orders-cache");
        }

        @Test
        @DisplayName("Should create orders cache name bean")
        void shouldCreateOrdersCacheNameBean() {
            assertThat(ordersCacheName).isNotNull();
            assertThat(ordersCacheName).isEqualTo("orders-cache");
        }

        @Test
        @DisplayName("Should configure orders cache map correctly")
        void shouldConfigureOrdersCacheMapCorrectly() {
            Map<String, MapConfig> mapConfigs = hazelcastInstance.getConfig().getMapConfigs();
            MapConfig ordersCacheConfig = mapConfigs.get("orders-cache");

            assertThat(ordersCacheConfig).isNotNull();
            assertThat(ordersCacheConfig.getName()).isEqualTo("orders-cache");
            assertThat(ordersCacheConfig.getTimeToLiveSeconds()).isEqualTo(3600);
            assertThat(ordersCacheConfig.getBackupCount()).isEqualTo(1);

            // MapStore is enabled for orders cache with LAZY initial load
            assertThat(ordersCacheConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(ordersCacheConfig.getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.orders.cache.OrderMapStore");
            assertThat(ordersCacheConfig
                            .getMapStoreConfig()
                            .getInitialLoadMode()
                            .name())
                    .isEqualTo("LAZY");
        }

        @Test
        @DisplayName("Should verify orders cache MapStore integration through configuration")
        void shouldVerifyOrdersCacheMapStoreIntegrationThroughConfiguration() {
            // Verify orders cache MapStore is configured through Hazelcast configuration
            // We don't access OrderMapStore directly to respect module boundaries
            Config config = hazelcastInstance.getConfig();
            MapConfig ordersMapConfig = config.getMapConfig("orders-cache");

            assertThat(ordersMapConfig).isNotNull();
            assertThat(ordersMapConfig.getMapStoreConfig()).isNotNull();
            assertThat(ordersMapConfig.getMapStoreConfig().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Products Cache Configuration")
    class ProductsCacheConfiguration {

        @Test
        @DisplayName("Should create products cache bean successfully")
        void shouldCreateProductsCacheBeanSuccessfully() {
            assertThat((Object) productsCache).isNotNull();
            assertThat(productsCache.getName()).isEqualTo("products-cache");
        }

        @Test
        @DisplayName("Should create products cache name bean")
        void shouldCreateProductsCacheNameBean() {
            assertThat(productsCacheName).isNotNull();
            assertThat(productsCacheName).isEqualTo("products-cache");
        }

        @Test
        @DisplayName("Should configure products cache map correctly")
        void shouldConfigureProductsCacheMapCorrectly() {
            Map<String, MapConfig> mapConfigs = hazelcastInstance.getConfig().getMapConfigs();
            MapConfig productsCacheConfig = mapConfigs.get("products-cache");

            assertThat(productsCacheConfig).isNotNull();
            assertThat(productsCacheConfig.getName()).isEqualTo("products-cache");
            assertThat(productsCacheConfig.getTimeToLiveSeconds()).isEqualTo(3600);
            assertThat(productsCacheConfig.getBackupCount()).isEqualTo(1);

            // MapStore is enabled for products cache with LAZY initial load
            assertThat(productsCacheConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(productsCacheConfig.getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.catalog.cache.ProductMapStore");
            assertThat(productsCacheConfig
                            .getMapStoreConfig()
                            .getInitialLoadMode()
                            .name())
                    .isEqualTo("LAZY");
        }

        @Test
        @DisplayName("Should verify ProductMapStore configuration through Hazelcast config")
        void shouldVerifyProductMapStoreConfigurationThroughHazelcastConfig() {
            // Verify products cache MapStore is configured through Hazelcast configuration
            // We don't access ProductMapStore directly to respect module boundaries
            Config config = hazelcastInstance.getConfig();
            MapConfig productsMapConfig = config.getMapConfig("products-cache");

            assertThat(productsMapConfig).isNotNull();
            assertThat(productsMapConfig.getMapStoreConfig()).isNotNull();
            assertThat(productsMapConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(productsMapConfig.getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.catalog.cache.ProductMapStore");
        }

        @Test
        @DisplayName("Should handle products cache operations correctly")
        void shouldHandleProductsCacheOperationsCorrectly() {
            String testKey = "CACHE-TEST-P001";
            String testValue = "Test Product Cache Value";

            // Test cache put/get operations
            productsCache.put(testKey, testValue);
            Object retrieved = productsCache.get(testKey);

            assertThat(retrieved).isEqualTo(testValue);
            assertThat(productsCache.containsKey(testKey)).isTrue();

            // Clean up
            productsCache.remove(testKey);
            assertThat(productsCache.containsKey(testKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("Inventory Cache Configuration")
    class InventoryCacheConfiguration {

        @Test
        @DisplayName("Should create inventory cache bean successfully")
        void shouldCreateInventoryCacheBeanSuccessfully() {
            assertThat((Object) inventoryCache)
                    .as("InventoryCache should not be null")
                    .isNotNull();
            assertThat(inventoryCache.getName()).isEqualTo("inventory-cache");
        }

        @Test
        @DisplayName("Should create inventory cache name bean")
        void shouldCreateInventoryCacheNameBean() {
            assertThat(inventoryCacheName).isNotNull();
            assertThat(inventoryCacheName).isEqualTo("inventory-cache");
        }

        @Test
        @DisplayName("Should configure inventory cache map correctly")
        void shouldConfigureInventoryCacheMapCorrectly() {
            Map<String, MapConfig> mapConfigs = hazelcastInstance.getConfig().getMapConfigs();
            MapConfig inventoryCacheConfig = mapConfigs.get("inventory-cache");

            assertThat(inventoryCacheConfig).isNotNull();
            assertThat(inventoryCacheConfig.getName()).isEqualTo("inventory-cache");
            // Inventory cache has shorter TTL due to volatility
            assertThat(inventoryCacheConfig.getTimeToLiveSeconds()).isEqualTo(1800);
            assertThat(inventoryCacheConfig.getBackupCount()).isEqualTo(1);

            // Verify MapStore configuration
            assertThat(inventoryCacheConfig.getMapStoreConfig()).isNotNull();
            assertThat(inventoryCacheConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(inventoryCacheConfig.getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.inventory.cache.InventoryMapStore");
            assertThat(inventoryCacheConfig
                            .getMapStoreConfig()
                            .getInitialLoadMode()
                            .name())
                    .isEqualTo("LAZY");
        }

        @Test
        @DisplayName("Should integrate SpringManagedContext with MapStores")
        void shouldIntegrateSpringManagedContextWithMapStores() {
            var config = hazelcastInstance.getConfig();
            assertThat(config.getManagedContext())
                    .as("ManagedContext should be configured")
                    .isNotNull();
            assertThat(config.getManagedContext().getClass().getName()).contains("SpringManagedContext");
        }

        @Test
        @DisplayName("Should verify InventoryMapStore configuration through Hazelcast config")
        void shouldVerifyInventoryMapStoreConfigurationThroughHazelcastConfig() {
            // Verify inventory cache MapStore is configured through Hazelcast configuration
            // We don't access InventoryMapStore directly to respect module boundaries
            Config config = hazelcastInstance.getConfig();
            MapConfig inventoryMapConfig = config.getMapConfig("inventory-cache");

            assertThat(inventoryMapConfig).isNotNull();
            assertThat(inventoryMapConfig.getMapStoreConfig()).isNotNull();
            assertThat(inventoryMapConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(inventoryMapConfig.getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.inventory.cache.InventoryMapStore");
        }

        @Test
        @DisplayName("Should handle inventory cache operations with Long keys correctly")
        void shouldHandleInventoryCacheOperationsWithLongKeysCorrectly() {
            Long testKey = System.currentTimeMillis();
            String testValue = "Test Inventory Cache Value";

            // Test cache put/get operations with Long key
            inventoryCache.put(testKey, testValue);
            Object retrieved = inventoryCache.get(testKey);

            assertThat(retrieved).isEqualTo(testValue);
            assertThat(inventoryCache.containsKey(testKey)).isTrue();

            // Clean up
            inventoryCache.remove(testKey);
            assertThat(inventoryCache.containsKey(testKey)).isFalse();
        }

        @Test
        @DisplayName("Should handle Long key edge values correctly in inventory cache")
        void shouldHandleLongKeyEdgeValuesCorrectlyInInventoryCache() {
            // Test with Long.MAX_VALUE
            Long maxKey = Long.MAX_VALUE;
            String maxValue = "Max Value Test";

            inventoryCache.put(maxKey, maxValue);
            assertThat(inventoryCache.get(maxKey)).isEqualTo(maxValue);
            inventoryCache.remove(maxKey);

            // Test with 1L
            Long minKey = 1L;
            String minValue = "Min Value Test";

            inventoryCache.put(minKey, minValue);
            assertThat(inventoryCache.get(minKey)).isEqualTo(minValue);
            inventoryCache.remove(minKey);
        }
    }

    @Nested
    @DisplayName("Cross-Cache Integration")
    class CrossCacheIntegration {

        @Test
        @DisplayName("Should handle multiple cache operations simultaneously")
        void shouldHandleMultipleCacheOperationsSimultaneously() {
            String orderKey = "CROSS-TEST-ORDER";
            String productKey = "CROSS-TEST-PRODUCT";
            Long inventoryKey = 999L;

            String orderValue = "Test Order";
            String productValue = "Test Product";
            String inventoryValue = "Test Inventory";

            // Put values in all caches
            ordersCache.put(orderKey, orderValue);
            productsCache.put(productKey, productValue);
            inventoryCache.put(inventoryKey, inventoryValue);

            // Verify all values are present
            assertThat(ordersCache.get(orderKey)).isEqualTo(orderValue);
            assertThat(productsCache.get(productKey)).isEqualTo(productValue);
            assertThat(inventoryCache.get(inventoryKey)).isEqualTo(inventoryValue);

            // Clean up all caches
            ordersCache.remove(orderKey);
            productsCache.remove(productKey);
            inventoryCache.remove(inventoryKey);

            // Verify all values are removed
            assertThat(ordersCache.containsKey(orderKey)).isFalse();
            assertThat(productsCache.containsKey(productKey)).isFalse();
            assertThat(inventoryCache.containsKey(inventoryKey)).isFalse();
        }

        @Test
        @DisplayName("Should maintain cache isolation between different cache types")
        void shouldMaintainCacheIsolationBetweenDifferentCacheTypes() {
            String sameKey = "ISOLATION-TEST";
            Long sameLongKey = 12345L;

            String orderValue = "Order Data";
            String productValue = "Product Data";
            String inventoryValue = "Inventory Data";

            // Use same string key for orders and products, Long key for inventory
            ordersCache.put(sameKey, orderValue);
            productsCache.put(sameKey, productValue);
            inventoryCache.put(sameLongKey, inventoryValue);

            // Each cache should maintain its own value
            assertThat(ordersCache.get(sameKey)).isEqualTo(orderValue);
            assertThat(productsCache.get(sameKey)).isEqualTo(productValue);
            assertThat(inventoryCache.get(sameLongKey)).isEqualTo(inventoryValue);

            // Operations on one cache should not affect others
            ordersCache.remove(sameKey);
            assertThat(ordersCache.containsKey(sameKey)).isFalse();
            assertThat(productsCache.containsKey(sameKey)).isTrue(); // Still present
            assertThat(inventoryCache.containsKey(sameLongKey)).isTrue(); // Still present

            // Clean up
            productsCache.remove(sameKey);
            inventoryCache.remove(sameLongKey);
        }

        @Test
        @DisplayName("Should report correct cache statistics for all cache types")
        void shouldReportCorrectCacheStatisticsForAllCacheTypes() {
            // Add some test data to each cache
            ordersCache.put("STATS-ORDER", "Order");
            productsCache.put("STATS-PRODUCT", "Product");
            inventoryCache.put(999L, "Inventory");

            // Verify each cache has at least one entry
            assertThat(ordersCache.size()).isGreaterThan(0);
            assertThat(productsCache.size()).isGreaterThan(0);
            assertThat(inventoryCache.size()).isGreaterThan(0);

            // Clean up
            ordersCache.clear();
            productsCache.clear();
            inventoryCache.clear();

            // Verify cleanup
            assertThat(ordersCache.size()).isEqualTo(0);
            assertThat(productsCache.size()).isEqualTo(0);
            assertThat(inventoryCache.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {

        @Test
        @DisplayName("Should have all required MapStore configurations")
        void shouldHaveAllRequiredMapStoreConfigurations() {
            Map<String, MapConfig> mapConfigs = hazelcastInstance.getConfig().getMapConfigs();

            // Filter out Hazelcast internal maps (like __jet.*, __sql.catalog, default, etc.)
            Map<String, MapConfig> appMapConfigs = mapConfigs.entrySet().stream()
                    .filter(entry ->
                            !entry.getKey().startsWith("__") && !entry.getKey().equals("default"))
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Verify all four cache maps are configured (including index cache)
            assertThat(appMapConfigs).hasSize(4);
            assertThat(appMapConfigs)
                    .containsKeys(
                            "orders-cache", "products-cache", "inventory-cache", "inventory-by-product-code-cache");

            // Inventory, products, and orders caches have MapStore enabled
            assertThat(appMapConfigs.get("inventory-cache").getMapStoreConfig().isEnabled())
                    .isTrue();
            assertThat(appMapConfigs.get("inventory-cache").getMapStoreConfig().getClassName())
                    .isNotEmpty();

            assertThat(appMapConfigs.get("products-cache").getMapStoreConfig().isEnabled())
                    .isTrue();
            assertThat(appMapConfigs.get("products-cache").getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.catalog.cache.ProductMapStore");

            assertThat(appMapConfigs.get("orders-cache").getMapStoreConfig().isEnabled())
                    .isTrue();
            assertThat(appMapConfigs.get("orders-cache").getMapStoreConfig().getClassName())
                    .isEqualTo("com.sivalabs.bookstore.orders.cache.OrderMapStore");

            // Index cache should not have a MapStore (derived data)
            assertThat(appMapConfigs
                            .get("inventory-by-product-code-cache")
                            .getMapStoreConfig()
                            .isEnabled())
                    .isFalse();
        }

        @Test
        @DisplayName("Should configure write-through settings correctly for all caches")
        void shouldConfigureWriteThroughSettingsCorrectlyForAllCaches() {
            Map<String, MapConfig> mapConfigs = hazelcastInstance.getConfig().getMapConfigs();

            // Inventory, products, and orders have write-through enabled
            MapConfig inventoryConfig = mapConfigs.get("inventory-cache");
            assertThat(inventoryConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(inventoryConfig.getMapStoreConfig().getWriteDelaySeconds())
                    .isEqualTo(1);
            assertThat(inventoryConfig.getMapStoreConfig().getWriteBatchSize()).isEqualTo(1);

            MapConfig productsConfig = mapConfigs.get("products-cache");
            assertThat(productsConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(productsConfig.getMapStoreConfig().getWriteDelaySeconds())
                    .isEqualTo(1);
            assertThat(productsConfig.getMapStoreConfig().getWriteBatchSize()).isEqualTo(1);

            MapConfig ordersConfig = mapConfigs.get("orders-cache");
            assertThat(ordersConfig.getMapStoreConfig().isEnabled()).isTrue();
            assertThat(ordersConfig.getMapStoreConfig().getWriteDelaySeconds()).isEqualTo(1);
            assertThat(ordersConfig.getMapStoreConfig().getWriteBatchSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should configure TTL settings appropriately for each cache type")
        void shouldConfigureTtlSettingsAppropriatelyForEachCacheType() {
            Map<String, MapConfig> mapConfigs = hazelcastInstance.getConfig().getMapConfigs();

            // Orders and products should have standard TTL
            assertThat(mapConfigs.get("orders-cache").getTimeToLiveSeconds()).isEqualTo(3600);
            assertThat(mapConfigs.get("products-cache").getTimeToLiveSeconds()).isEqualTo(3600);

            // Inventory should have shorter TTL due to volatility
            assertThat(mapConfigs.get("inventory-cache").getTimeToLiveSeconds()).isEqualTo(1800);
        }
    }
}

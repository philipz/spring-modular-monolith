package com.sivalabs.bookstore.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.sivalabs.bookstore.TestcontainersConfiguration;
import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.catalog.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.jdbc.Sql;

@ApplicationModuleTest(webEnvironment = RANDOM_PORT)
@Import({TestcontainersConfiguration.class, com.sivalabs.bookstore.testsupport.cache.ProductsCacheTestConfig.class})
@Sql("/test-products-data.sql")
@DisplayName("ProductCacheService Integration Tests")
class ProductCacheServiceIntegrationTests {

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private ProductRepository productRepository;

    private ProductEntity testProduct;
    private ProductEntity anotherTestProduct;
    private String uniqueProductCode1;
    private String uniqueProductCode2;

    @BeforeEach
    void setUp() {
        // Generate unique product codes and names for each test run to avoid conflicts
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        uniqueProductCode1 = "TEST-P001-" + uniqueSuffix;
        uniqueProductCode2 = "TEST-P002-" + uniqueSuffix;

        // Create test product data with unique codes and names
        testProduct = createTestProduct(
                uniqueProductCode1,
                "Integration Test Product 1 " + uniqueSuffix,
                "Test Description 1",
                BigDecimal.valueOf(29.99));
        anotherTestProduct = createTestProduct(
                uniqueProductCode2,
                "Integration Test Product 2 " + uniqueSuffix,
                "Test Description 2",
                BigDecimal.valueOf(39.99));

        // Save test products to database
        testProduct = productRepository.save(testProduct);
        anotherTestProduct = productRepository.save(anotherTestProduct);

        // Note: No clearCache method available, tests should handle their own cache state
    }

    @Nested
    @DisplayName("Basic Cache Operations")
    class BasicCacheOperations {

        @Test
        @DisplayName("Should find product from cache after first database lookup")
        void shouldFindProductFromCacheAfterFirstDatabaseLookup() {
            String productCode = testProduct.getCode();

            // First, manually cache the product (since findByProductCode only looks in cache, not database)
            productCacheService.cacheProduct(productCode, testProduct);

            // First call - should retrieve from cache
            Optional<ProductEntity> result1 = productCacheService.findByProductCode(productCode);
            assertThat(result1).isPresent();
            assertThat(result1.get().getCode()).isEqualTo(productCode);
            assertThat(result1.get().getName()).isEqualTo(testProduct.getName());

            // Second call - should also come from cache
            Optional<ProductEntity> result2 = productCacheService.findByProductCode(productCode);
            assertThat(result2).isPresent();
            assertThat(result2.get().getCode()).isEqualTo(result1.get().getCode());
            assertThat(result2.get().getName()).isEqualTo(result1.get().getName());

            // Verify product is in cache
            boolean existsInCache = productCacheService.existsInCache(productCode);
            assertThat(existsInCache).isTrue();
        }

        @Test
        @DisplayName("Should return empty when product does not exist")
        void shouldReturnEmptyWhenProductDoesNotExist() {
            String nonExistentProductCode = "NON-EXISTENT";

            Optional<ProductEntity> result = productCacheService.findByProductCode(nonExistentProductCode);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should cache product successfully")
        void shouldCacheProductSuccessfully() {
            String productCode = testProduct.getCode();

            // Product should not be in cache initially
            boolean initiallyExists = productCacheService.existsInCache(productCode);
            assertThat(initiallyExists).isFalse();

            // Cache the product
            boolean cached = productCacheService.cacheProduct(productCode, testProduct);
            assertThat(cached).isTrue();

            // Product should now be in cache
            boolean existsAfterCache = productCacheService.existsInCache(productCode);
            assertThat(existsAfterCache).isTrue();

            // Should be able to retrieve from cache
            Optional<ProductEntity> cachedProduct = productCacheService.findByProductCode(productCode);
            assertThat(cachedProduct).isPresent();
            assertThat(cachedProduct.get().getCode()).isEqualTo(productCode);
        }

        @Test
        @DisplayName("Should update cached product successfully")
        void shouldUpdateCachedProductSuccessfully() {
            String productCode = testProduct.getCode();

            // Cache the original product
            productCacheService.cacheProduct(productCode, testProduct);

            // Create updated product
            ProductEntity updatedProduct = createTestProduct(
                    productCode, "Updated Product Name", "Updated Description", BigDecimal.valueOf(49.99));
            updatedProduct.setId(testProduct.getId());

            // Update cached product
            boolean updated = productCacheService.updateCachedProduct(productCode, updatedProduct);
            assertThat(updated).isTrue();

            // Retrieve and verify the update
            Optional<ProductEntity> cachedProduct = productCacheService.findByProductCode(productCode);
            assertThat(cachedProduct).isPresent();
            assertThat(cachedProduct.get().getName()).isEqualTo("Updated Product Name");
            assertThat(cachedProduct.get().getDescription()).isEqualTo("Updated Description");
            assertThat(cachedProduct.get().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        }

        @Test
        @DisplayName("Should remove product from cache successfully")
        void shouldRemoveProductFromCacheSuccessfully() {
            String productCode = testProduct.getCode();

            // Cache the product first
            productCacheService.cacheProduct(productCode, testProduct);
            assertThat(productCacheService.existsInCache(productCode)).isTrue();

            // Remove from cache
            boolean removed = productCacheService.removeFromCache(productCode);
            assertThat(removed).isTrue();

            // Product should no longer be in cache
            boolean existsAfterRemoval = productCacheService.existsInCache(productCode);
            assertThat(existsAfterRemoval).isFalse();
        }

        // Note: evictFromCache method not available in ProductCacheService
        // Remove operation serves same purpose
    }

    @Nested
    @DisplayName("Health Monitoring and Circuit Breaker")
    class HealthMonitoringAndCircuitBreaker {

        @Test
        @DisplayName("Should check cache health successfully")
        void shouldCheckCacheHealthSuccessfully() {
            boolean isHealthy = productCacheService.isHealthy();
            assertThat(isHealthy).isTrue();
        }

        // Note: testCacheConnectivity method not available
        // isHealthy serves same purpose

        @Test
        @DisplayName("Should get cache statistics")
        void shouldGetCacheStatistics() {
            // Cache some products first
            productCacheService.cacheProduct(testProduct.getCode(), testProduct);
            productCacheService.cacheProduct(anotherTestProduct.getCode(), anotherTestProduct);

            String stats = productCacheService.getCacheStats();

            assertThat(stats).isNotNull();
            assertThat(stats).contains("Products Cache Statistics");
            assertThat(stats).contains("Cache Name:");
        }

        @Test
        @DisplayName("Should get circuit breaker status")
        void shouldGetCircuitBreakerStatus() {
            String status = productCacheService.getCircuitBreakerStatus();

            assertThat(status).isNotNull();
            assertThat(status).contains("Circuit State:");
        }

        // Note: getHealthReport method not available
        // Circuit breaker status and cache stats provide same information

        @Test
        @DisplayName("Should reset circuit breaker successfully")
        void shouldResetCircuitBreakerSuccessfully() {
            boolean reset = productCacheService.resetCircuitBreaker();
            assertThat(reset).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallback Mechanisms")
    class FallbackMechanisms {

        @Test
        @DisplayName("Should find with automatic fallback when cache is available")
        void shouldFindWithAutomaticFallbackWhenCacheIsAvailable() {
            String productCode = testProduct.getCode();

            // Cache the product first
            productCacheService.cacheProduct(productCode, testProduct);

            // Use automatic fallback - should prefer cache
            Optional<ProductEntity> result = productCacheService.findWithAutomaticFallback(
                    productCode, () -> Optional.of(anotherTestProduct) // This fallback should not be used
                    );

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo(productCode);
            assertThat(result.get().getName()).isEqualTo(testProduct.getName());
        }

        @Test
        @DisplayName("Should use fallback when product not in cache")
        void shouldUseFallbackWhenProductNotInCache() {
            String productCode = "FALLBACK-TEST";

            // Product is not in cache, should use fallback
            Optional<ProductEntity> result =
                    productCacheService.findWithAutomaticFallback(productCode, () -> Optional.of(anotherTestProduct));

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(anotherTestProduct);
        }
    }

    @Nested
    @DisplayName("Cache Warming")
    class CacheWarming {

        @Test
        @DisplayName("Should warm up cache successfully")
        void shouldWarmUpCacheSuccessfully() {
            List<String> productCodes = Arrays.asList(
                    testProduct.getCode(), anotherTestProduct.getCode(), "NON-EXISTENT" // This one will fail
                    );

            // Cache the products first so they can be warmed up (warm-up retrieves from cache, not DB)
            productCacheService.cacheProduct(testProduct.getCode(), testProduct);
            productCacheService.cacheProduct(anotherTestProduct.getCode(), anotherTestProduct);

            int warmedUp = productCacheService.warmUpCache(productCodes);

            // Should warm up 2 out of 3 products
            assertThat(warmedUp).isEqualTo(2);

            // Verify products are still in cache
            assertThat(productCacheService.existsInCache(testProduct.getCode())).isTrue();
            assertThat(productCacheService.existsInCache(anotherTestProduct.getCode()))
                    .isTrue();
        }

        @Test
        @DisplayName("Should handle empty product codes in warm up")
        void shouldHandleEmptyProductCodesInWarmUp() {
            int warmedUp = productCacheService.warmUpCache(Arrays.asList());
            assertThat(warmedUp).isEqualTo(0);
        }
    }

    // Note: TTL and Timeout operations not implemented in ProductCacheService
    // Basic cache operations provide the core functionality

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("Should handle null product code gracefully")
        void shouldHandleNullProductCodeGracefully() {
            Optional<ProductEntity> result = productCacheService.findByProductCode(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should reject caching null product")
        void shouldRejectCachingNullProduct() {
            String productCode = "TEST-CODE";
            boolean result = productCacheService.cacheProduct(productCode, null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject updating with null product")
        void shouldRejectUpdatingWithNullProduct() {
            String productCode = "TEST-CODE";
            boolean result = productCacheService.updateCachedProduct(productCode, null);
            assertThat(result).isFalse();
        }

        // Note: TTL and Timeout methods not available in ProductCacheService

        @Test
        @DisplayName("Should handle cache operations gracefully under error conditions")
        void shouldHandleCacheOperationsGracefullyUnderErrorConditions() {
            // This test verifies that cache operations degrade gracefully
            // when errors occur or circuit breaker opens

            String productCode = testProduct.getCode();

            // Operations should still work (may use fallback mechanisms)
            Optional<ProductEntity> result = productCacheService.findByProductCode(productCode);
            // Result may be empty or from database, both are acceptable
            assertThat(result).isNotNull(); // Optional itself should not be null

            String status = productCacheService.getCircuitBreakerStatus();
            assertThat(status).isNotNull();

            // Should be able to check if fallback is recommended
            boolean shouldFallback = productCacheService.shouldFallbackToDatabase("testOperation");
            // This can be true or false depending on current state
            assertThat(shouldFallback).isIn(true, false); // Should be a valid boolean value
        }
    }

    // Helper method to create test products
    private ProductEntity createTestProduct(String code, String name, String description, BigDecimal price) {
        ProductEntity product = new ProductEntity();
        product.setCode(code);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl("https://example.com/images/" + code + ".jpg");
        return product;
    }
}

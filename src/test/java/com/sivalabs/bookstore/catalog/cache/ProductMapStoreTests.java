package com.sivalabs.bookstore.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hazelcast.core.HazelcastInstance;
import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.catalog.domain.ProductRepository;
import java.math.BigDecimal;
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
@DisplayName("ProductMapStore Unit Tests")
class ProductMapStoreTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private HazelcastInstance hazelcastInstance;

    private ProductMapStore productMapStore;

    private ProductEntity testProduct;
    private ProductEntity anotherTestProduct;

    @BeforeEach
    void setUp() {
        productMapStore = new ProductMapStore(productRepository);

        // Create test product data
        testProduct = createTestProduct("P001", "Test Product 1", "Description 1", new BigDecimal("29.99"));
        testProduct.setId(1L);

        anotherTestProduct = createTestProduct("P002", "Test Product 2", "Description 2", new BigDecimal("39.99"));
        anotherTestProduct.setId(2L);
    }

    @Test
    @DisplayName("Should initialize successfully with ProductRepository")
    void shouldInitializeWithProductRepository() {
        assertThat(productMapStore).isNotNull();
    }

    @Test
    @DisplayName("Should complete lifecycle init without errors")
    void shouldInitializeLifecycle() {
        Properties props = new Properties();
        String mapName = "products-cache";

        // Should not throw any exception
        productMapStore.init(hazelcastInstance, props, mapName);
    }

    @Test
    @DisplayName("Should complete lifecycle destroy without errors")
    void shouldDestroyLifecycle() {
        // Should not throw any exception
        productMapStore.destroy();
    }

    @Test
    @DisplayName("Should store product successfully - validation only")
    void shouldStoreProductSuccessfully() {
        String productCode = "P001";

        // Store operation should complete without exception
        productMapStore.store(productCode, testProduct);

        // Verify no exception was thrown - store method mainly validates
    }

    @Test
    @DisplayName("Should handle store with mismatched product code")
    void shouldHandleStoreWithMismatchedProductCode() {
        String differentProductCode = "P999";

        // Should complete without throwing exception but log warning
        productMapStore.store(differentProductCode, testProduct);
    }

    @Test
    @DisplayName("Should handle store with null product entity")
    void shouldHandleStoreWithNullProduct() {
        String productCode = "P001";

        // Should complete without throwing exception
        productMapStore.store(productCode, null);
    }

    @Test
    @DisplayName("Should load product successfully from ProductRepository")
    void shouldLoadProductSuccessfully() {
        String productCode = "P001";
        given(productRepository.findByCode(productCode)).willReturn(Optional.of(testProduct));

        ProductEntity result = productMapStore.load(productCode);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(productCode);
        assertThat(result.getId()).isEqualTo(testProduct.getId());
        assertThat(result.getName()).isEqualTo(testProduct.getName());
        assertThat(result.getPrice()).isEqualByComparingTo(testProduct.getPrice());
        verify(productRepository).findByCode(productCode);
    }

    @Test
    @DisplayName("Should return null when product not found")
    void shouldReturnNullWhenProductNotFound() {
        String productCode = "NON-EXISTENT";
        given(productRepository.findByCode(productCode)).willReturn(Optional.empty());

        ProductEntity result = productMapStore.load(productCode);

        assertThat(result).isNull();
        verify(productRepository).findByCode(productCode);
    }

    @Test
    @DisplayName("Should handle load exception gracefully")
    void shouldHandleLoadException() {
        String productCode = "P001";
        RuntimeException testException = new RuntimeException("Database error");
        given(productRepository.findByCode(productCode)).willThrow(testException);

        // Should not throw exception and return null
        ProductEntity result = productMapStore.load(productCode);

        assertThat(result).isNull();
        verify(productRepository).findByCode(productCode);
    }

    @Test
    @DisplayName("Should load multiple products successfully")
    void shouldLoadAllProductsSuccessfully() {
        Collection<String> productCodes = Arrays.asList("P001", "P002", "P003");

        given(productRepository.findByCode("P001")).willReturn(Optional.of(testProduct));
        given(productRepository.findByCode("P002")).willReturn(Optional.of(anotherTestProduct));
        given(productRepository.findByCode("P003")).willReturn(Optional.empty());

        Map<String, ProductEntity> result = productMapStore.loadAll(productCodes);

        assertThat(result).hasSize(2);
        assertThat(result).containsKey("P001");
        assertThat(result).containsKey("P002");
        assertThat(result).doesNotContainKey("P003");
        assertThat(result.get("P001")).isEqualTo(testProduct);
        assertThat(result.get("P002")).isEqualTo(anotherTestProduct);

        verify(productRepository).findByCode("P001");
        verify(productRepository).findByCode("P002");
        verify(productRepository).findByCode("P003");
    }

    @Test
    @DisplayName("Should handle loadAll with empty collection")
    void shouldHandleLoadAllWithEmptyCollection() {
        Collection<String> emptyProductCodes = Arrays.asList();

        Map<String, ProductEntity> result = productMapStore.loadAll(emptyProductCodes);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle loadAll exception gracefully")
    void shouldHandleLoadAllException() {
        Collection<String> productCodes = Arrays.asList("P001");
        RuntimeException testException = new RuntimeException("Database error");
        given(productRepository.findByCode("P001")).willThrow(testException);

        // Should return empty map instead of throwing
        Map<String, ProductEntity> result = productMapStore.loadAll(productCodes);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return all product codes for loadAllKeys")
    void shouldReturnAllProductCodesForLoadAllKeys() {
        List<ProductEntity> allProducts = Arrays.asList(testProduct, anotherTestProduct);
        given(productRepository.findAll()).willReturn(allProducts);

        Set<String> result = (Set<String>) productMapStore.loadAllKeys();

        assertThat(result).hasSize(2);
        assertThat(result).contains("P001", "P002");
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle loadAllKeys with empty repository")
    void shouldHandleLoadAllKeysWithEmptyRepository() {
        given(productRepository.findAll()).willReturn(Arrays.asList());

        Set<String> result = (Set<String>) productMapStore.loadAllKeys();

        assertThat(result).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle loadAllKeys exception gracefully")
    void shouldHandleLoadAllKeysException() {
        RuntimeException testException = new RuntimeException("Database error");
        given(productRepository.findAll()).willThrow(testException);

        // Should not throw exception
        Set<String> result = (Set<String>) productMapStore.loadAllKeys();

        assertThat(result).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle delete operation gracefully")
    void shouldHandleDeleteOperation() {
        String productCode = "P001";

        // Should complete without throwing exception
        productMapStore.delete(productCode);

        // Method should log but not actually delete - deletion handled by service layer
    }

    @Test
    @DisplayName("Should handle deleteAll operation gracefully")
    void shouldHandleDeleteAllOperation() {
        Collection<String> productCodes = Arrays.asList("P001", "P002");

        // Should complete without throwing exception
        productMapStore.deleteAll(productCodes);

        // Method should log but not actually delete - deletion handled by service layer
    }

    @Test
    @DisplayName("Should handle storeAll operation gracefully")
    void shouldHandleStoreAllOperation() {
        Map<String, ProductEntity> products = Map.of(
                "P001", testProduct,
                "P002", anotherTestProduct);

        // Should complete without throwing exception
        productMapStore.storeAll(products);

        // Method should log completion
    }

    @Test
    @DisplayName("Should handle store operation exception")
    void shouldHandleStoreException() {
        // Create a product that will trigger validation warning
        ProductEntity invalidProduct =
                createTestProduct("DIFFERENT-CODE", "Invalid Product", "Description", new BigDecimal("99.99"));
        String productCode = "P001";

        // Should complete without throwing exception but log warning
        productMapStore.store(productCode, invalidProduct);
    }

    @Test
    @DisplayName("Should handle complex product data in load operation")
    void shouldHandleComplexProductDataInLoad() {
        String productCode = "P999";
        ProductEntity complexProduct = createComplexTestProduct();
        given(productRepository.findByCode(productCode)).willReturn(Optional.of(complexProduct));

        ProductEntity result = productMapStore.load(productCode);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(productCode);
        assertThat(result.getName()).isEqualTo("Complex Product");
        assertThat(result.getDescription()).contains("detailed description");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("299.99"));
        verify(productRepository).findByCode(productCode);
    }

    @Test
    @DisplayName("Should handle products with different price ranges")
    void shouldHandleProductsWithDifferentPriceRanges() {
        // Test with very small price
        ProductEntity cheapProduct =
                createTestProduct("P-CHEAP", "Cheap Product", "Affordable item", new BigDecimal("0.99"));
        given(productRepository.findByCode("P-CHEAP")).willReturn(Optional.of(cheapProduct));

        ProductEntity result1 = productMapStore.load("P-CHEAP");
        assertThat(result1).isNotNull();
        assertThat(result1.getPrice()).isEqualByComparingTo(new BigDecimal("0.99"));

        // Test with very large price
        ProductEntity expensiveProduct =
                createTestProduct("P-EXPENSIVE", "Expensive Product", "Premium item", new BigDecimal("9999.99"));
        given(productRepository.findByCode("P-EXPENSIVE")).willReturn(Optional.of(expensiveProduct));

        ProductEntity result2 = productMapStore.load("P-EXPENSIVE");
        assertThat(result2).isNotNull();
        assertThat(result2.getPrice()).isEqualByComparingTo(new BigDecimal("9999.99"));

        verify(productRepository).findByCode("P-CHEAP");
        verify(productRepository).findByCode("P-EXPENSIVE");
    }

    @Test
    @DisplayName("Should handle products with special characters in codes")
    void shouldHandleProductsWithSpecialCharactersInCodes() {
        ProductEntity specialProduct =
                createTestProduct("P-001-A#1", "Special Product", "Has special code", new BigDecimal("49.99"));
        given(productRepository.findByCode("P-001-A#1")).willReturn(Optional.of(specialProduct));

        ProductEntity result = productMapStore.load("P-001-A#1");

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("P-001-A#1");
        verify(productRepository).findByCode("P-001-A#1");
    }

    @Test
    @DisplayName("Should handle loadAll with mixed success and failure results")
    void shouldHandleLoadAllWithMixedResults() {
        Collection<String> productCodes = Arrays.asList("P001", "P-ERROR", "P002");

        given(productRepository.findByCode("P001")).willReturn(Optional.of(testProduct));
        given(productRepository.findByCode("P-ERROR")).willThrow(new RuntimeException("Specific error"));
        given(productRepository.findByCode("P002")).willReturn(Optional.of(anotherTestProduct));

        Map<String, ProductEntity> result = productMapStore.loadAll(productCodes);

        // Should continue processing other products even if one fails
        assertThat(result).hasSize(2);
        assertThat(result).containsKey("P001");
        assertThat(result).containsKey("P002");
        assertThat(result).doesNotContainKey("P-ERROR");
    }

    @Test
    @DisplayName("Should validate ProductEntity fields correctly")
    void shouldValidateProductEntityFieldsCorrectly() {
        String productCode = "P-VALIDATION";

        // Create product with all fields populated
        ProductEntity fullProduct =
                createTestProduct(productCode, "Full Product", "Complete description", new BigDecimal("19.99"));
        fullProduct.setImageUrl("https://example.com/image.jpg");

        given(productRepository.findByCode(productCode)).willReturn(Optional.of(fullProduct));

        ProductEntity result = productMapStore.load(productCode);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(productCode);
        assertThat(result.getName()).isEqualTo("Full Product");
        assertThat(result.getDescription()).isEqualTo("Complete description");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    // Helper methods for creating test data

    private ProductEntity createTestProduct(String code, String name, String description, BigDecimal price) {
        ProductEntity product = new ProductEntity();
        product.setCode(code);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl("https://example.com/images/" + code + ".jpg");
        return product;
    }

    private ProductEntity createComplexTestProduct() {
        ProductEntity product = new ProductEntity();
        product.setId(999L);
        product.setCode("P999");
        product.setName("Complex Product");
        product.setDescription(
                "This is a very detailed description of a complex product with many features and specifications");
        product.setPrice(new BigDecimal("299.99"));
        product.setImageUrl("https://example.com/images/complex/P999-main.jpg");
        return product;
    }
}

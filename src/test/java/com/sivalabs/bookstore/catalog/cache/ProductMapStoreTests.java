package com.sivalabs.bookstore.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.catalog.domain.ProductRepository;
import com.sivalabs.bookstore.testsupport.cache.AbstractMapStoreTest;
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
import org.mockito.Mock;

@DisplayName("ProductMapStore Unit Tests")
class ProductMapStoreTests extends AbstractMapStoreTest<String, ProductEntity, ProductMapStore, ProductRepository> {

    @Mock
    private ProductRepository productRepository;

    @Override
    @BeforeEach
    protected void setUp() {
        // Initialize with specific typed mock to avoid ClassCastException
        mapStore = new ProductMapStore(productRepository);
        testEntity1 = createTestEntity1();
        testEntity2 = createTestEntity2();
    }

    @Override
    protected ProductMapStore createMapStore(ProductRepository repository) {
        return new ProductMapStore(repository);
    }

    @Override
    protected ProductEntity createTestEntity1() {
        ProductEntity product = createTestProduct("P001", "Test Product 1", "Description 1", new BigDecimal("29.99"));
        product.setId(1L);
        return product;
    }

    @Override
    protected ProductEntity createTestEntity2() {
        ProductEntity product = createTestProduct("P002", "Test Product 2", "Description 2", new BigDecimal("39.99"));
        product.setId(2L);
        return product;
    }

    @Override
    protected String getTestEntity1Key() {
        return "P001";
    }

    @Override
    protected String getTestEntity2Key() {
        return "P002";
    }

    @Override
    protected String getNonExistentKey() {
        return "NON-EXISTENT";
    }

    @Override
    protected void mockRepositoryFindByKey(String key, ProductEntity entity) {
        given(productRepository.findByCode(key)).willReturn(Optional.of(entity));
    }

    @Override
    protected void mockRepositoryFindByKeyEmpty(String key) {
        given(productRepository.findByCode(key)).willReturn(Optional.empty());
    }

    @Override
    protected void mockRepositoryFindByKeyException(String key, RuntimeException exception) {
        given(productRepository.findByCode(key)).willThrow(exception);
    }

    @Override
    protected void mockRepositoryFindAll(ProductEntity... entities) {
        given(productRepository.findAll()).willReturn(Arrays.asList(entities));
    }

    @Override
    protected void mockRepositoryFindAllException(RuntimeException exception) {
        given(productRepository.findAll()).willThrow(exception);
    }

    @Override
    protected void verifyRepositoryFindByKey(String key) {
        verify(productRepository).findByCode(key);
    }

    @Override
    protected void verifyRepositoryFindAll() {
        verify(productRepository).findAll();
    }

    @Override
    protected ProductEntity loadFromMapStore(String key) {
        return mapStore.load(key);
    }

    @Override
    protected Map<String, ProductEntity> loadAllFromMapStore(Collection<String> keys) {
        return mapStore.loadAll(keys);
    }

    @Override
    protected Iterable<String> loadAllKeysFromMapStore() {
        return mapStore.loadAllKeys();
    }

    @Override
    protected void storeInMapStore(String key, ProductEntity entity) {
        mapStore.store(key, entity);
    }

    @Override
    protected void storeAllInMapStore(Map<String, ProductEntity> entities) {
        mapStore.storeAll(entities);
    }

    @Override
    protected void deleteFromMapStore(String key) {
        mapStore.delete(key);
    }

    @Override
    protected void deleteAllFromMapStore(Collection<String> keys) {
        mapStore.deleteAll(keys);
    }

    @Override
    protected void initMapStore() {
        Properties props = new Properties();
        String mapName = "products-cache";
        mapStore.init(hazelcastInstance, props, mapName);
    }

    @Override
    protected void destroyMapStore() {
        mapStore.destroy();
    }

    @Override
    protected void assertEntitiesEqual(ProductEntity expected, ProductEntity actual) {
        assertThat(actual.getCode()).isEqualTo(expected.getCode());
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getPrice()).isEqualByComparingTo(expected.getPrice());
    }

    // Additional product-specific tests

    @Test
    @DisplayName("Should handle store with mismatched product code")
    void shouldHandleStoreWithMismatchedProductCode() {
        String differentProductCode = "P999";

        // Should complete without throwing exception but log warning
        mapStore.store(differentProductCode, testEntity1);
    }

    @Test
    @DisplayName("Should return all product codes for loadAllKeys")
    void shouldReturnAllProductCodesForLoadAllKeys() {
        List<ProductEntity> allProducts = Arrays.asList(testEntity1, testEntity2);
        given(productRepository.findAll()).willReturn(allProducts);

        Set<String> result = (Set<String>) mapStore.loadAllKeys();

        assertThat(result).hasSize(2);
        assertThat(result).contains("P001", "P002");
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle loadAllKeys with empty repository")
    void shouldHandleLoadAllKeysWithEmptyRepository() {
        given(productRepository.findAll()).willReturn(Arrays.asList());

        Set<String> result = (Set<String>) mapStore.loadAllKeys();

        assertThat(result).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle loadAllKeys exception gracefully")
    void shouldHandleLoadAllKeysException() {
        RuntimeException testException = new RuntimeException("Database error");
        given(productRepository.findAll()).willThrow(testException);

        // Should not throw exception
        Set<String> result = (Set<String>) mapStore.loadAllKeys();

        assertThat(result).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle complex product data in load operation")
    void shouldHandleComplexProductDataInLoad() {
        String productCode = "P999";
        ProductEntity complexProduct = createComplexTestProduct();
        given(productRepository.findByCode(productCode)).willReturn(Optional.of(complexProduct));

        ProductEntity result = mapStore.load(productCode);

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

        ProductEntity result1 = mapStore.load("P-CHEAP");
        assertThat(result1).isNotNull();
        assertThat(result1.getPrice()).isEqualByComparingTo(new BigDecimal("0.99"));

        // Test with very large price
        ProductEntity expensiveProduct =
                createTestProduct("P-EXPENSIVE", "Expensive Product", "Premium item", new BigDecimal("9999.99"));
        given(productRepository.findByCode("P-EXPENSIVE")).willReturn(Optional.of(expensiveProduct));

        ProductEntity result2 = mapStore.load("P-EXPENSIVE");
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

        ProductEntity result = mapStore.load("P-001-A#1");

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("P-001-A#1");
        verify(productRepository).findByCode("P-001-A#1");
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

        ProductEntity result = mapStore.load(productCode);

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
